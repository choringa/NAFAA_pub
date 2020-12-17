package com.indi.nafaa.libsignalclient;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.indi.nafaa.sqlimanager.DBHelper;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.IdentityKeyStore;

public class MyIdentityKeyStore implements IdentityKeyStore {
    private static final String TAG = "MyIdentityKeyStore";
    private Context context;
    private DBHelper dbHelper;

    public MyIdentityKeyStore(Context context) {
        this.context = context;
        dbHelper = new DBHelper(context);
    }

    @Override
    public IdentityKeyPair getIdentityKeyPair() {
        return null;
    }

    @Override
    public int getLocalRegistrationId() {
        return 0;
    }

    @Override
    public boolean saveIdentity(SignalProtocolAddress address, IdentityKey identityKey) {
        Log.i(TAG, "saveIdentity --> Se quiere crear un nuevo resgostro en la tabla : " + DBHelper.TABLE_IDENTITY_KEY_PAIR_TABLE + ", con los valores SignalProtoclAddress: " + address + ", IdentityKey: " + identityKey);
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.IKP_SIGNAL_ADDRESS_PROTOCOL_COLUMN, address.getName());
        values.put(DBHelper.IKP_IDENTITY_KEY_COLUMN, identityKey.serialize());
        long newRowId = database.insert(DBHelper.TABLE_IDENTITY_KEY_PAIR_TABLE, null, values);

        Log.i(TAG, "saveToDB-->IdentyKey and signal ProtolAddress row id: " + newRowId);
        database.close();
        if(newRowId == -1)
            return false;
        else
            return true;
    }

    @Override
    public boolean isTrustedIdentity(SignalProtocolAddress address, IdentityKey identityKey, Direction direction) {
        return false;
    }

    @Override
    public IdentityKey getIdentity(SignalProtocolAddress address) {

        SQLiteDatabase database = dbHelper.getReadableDatabase();
        String[] projection = {
                DBHelper.IKP_IDENTITY_KEY_COLUMN,
                DBHelper.IKP_SIGNAL_ADDRESS_PROTOCOL_COLUMN
        };

        String selection =
                DBHelper.IKP_SIGNAL_ADDRESS_PROTOCOL_COLUMN + " like ?";

        byte[] blob = DBHelper.objectToBlobByte(address);
        String[] selectionArgs = {"%" + blob + "%"};

        Cursor cursor = database.query(
                DBHelper.TABLE_SIGNED_PRE_KEY_RECORD,   // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // don't sort
        );

        IdentityKey respIdentityKey = null;
        Log.d("TAG", "loadSignedPreKeys --> Numero de IdentityKeys (deberia ser 1): " + cursor.getCount());
        if (cursor.getCount() > 0) {
            for (int i = 0; i < cursor.getCount(); i++) {
                try {
                    cursor.moveToNext();
                    byte[] byteObject = cursor.getBlob(i);
                    respIdentityKey = new IdentityKey(byteObject, 0);
                } catch (InvalidKeyException e) {
                    Log.e(TAG, "loadPreKey --> ERROR conviertiendo blob a objeto IdentityKey: " + e.getLocalizedMessage());
                }
            }
        }
        cursor.close();
        database.close();
        return respIdentityKey;
    }
}
