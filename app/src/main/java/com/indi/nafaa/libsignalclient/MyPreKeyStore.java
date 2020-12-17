package com.indi.nafaa.libsignalclient;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.indi.nafaa.sqlimanager.DBHelper;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.PreKeyStore;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;

import java.io.IOException;
import java.util.ArrayList;

public class MyPreKeyStore implements PreKeyStore {

    private final static String TAG = "MyPreKeyStore";
    private DBHelper dbHelper;

    public MyPreKeyStore(Context context) {
        this.dbHelper = new DBHelper(context);
    }

    @Override
    public PreKeyRecord loadPreKey(int preKeyId) throws InvalidKeyIdException {
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        String[] projection = {
                DBHelper.PKS_ID_COLUMN,
                DBHelper.PKS_PRE_KEY_STORED_COLUMN
        };

        String selection =
                DBHelper.PKS_ID_COLUMN + " like ?";

        String[] selectionArgs = {"%" + preKeyId + "%"};

        Cursor cursor = database.query(
                DBHelper.TABLE_PRE_KEY_STORED,   // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // don't sort
        );

        PreKeyRecord resp = null;
        Log.d("TAG", "loadSignedPreKeys --> Numero de IdentityKeys (deberia ser 1): " + cursor.getCount());
        if (cursor.getCount() > 0) {
            for (int i = 0; i < cursor.getCount(); i++) {
                try {
                    cursor.moveToNext();
                    byte[] byteObject = cursor.getBlob(i);
                    resp = new PreKeyRecord(byteObject);
                } catch (IOException e) {
                    Log.e(TAG, "loadPreKey --> ERROR conviertiendo blob a objeto PreKeyRecord: " + e.getLocalizedMessage());
                }
            }
        }
        cursor.close();
        database.close();
        return resp;
    }

    @Override
    public void storePreKey(int preKeyId, PreKeyRecord record) {
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.PKS_ID_COLUMN, preKeyId);
        values.put(DBHelper.PKS_PRE_KEY_STORED_COLUMN, record.serialize());
        long resp = database.insert(DBHelper.TABLE_PRE_KEY_STORED, null, values);
        database.close();
        if(resp == -1)
            Log.e(TAG, "storePreKey --> No se pudo agregar el nuevo PreKeyRecord a la db con id: " + preKeyId);
        else
            Log.i(TAG,"storePreKey --> Se agrego bien el PreKeyRecord de id: " + preKeyId);
    }

    @Override
    public boolean containsPreKey(int preKeyId) {
        return false;
    }

    @Override
    public void removePreKey(int preKeyId) {

    }
}
