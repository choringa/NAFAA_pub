package com.indi.nafaa.libsignalclient;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.indi.nafaa.sqlimanager.DBHelper;

import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyStore;

import java.util.ArrayList;
import java.util.List;

public class MySignedPreKeyStore implements SignedPreKeyStore {

    private static final String TAG = "MySignedPreKeyStore";
    private Context context;
    private DBHelper dbHelper;

    public MySignedPreKeyStore(Context context) {
        this.context = context;
        dbHelper = new DBHelper(context);
    }

    @Override
    public SignedPreKeyRecord loadSignedPreKey(int signedPreKeyId) throws InvalidKeyIdException {
        return null;
    }

    @Override
    public List<SignedPreKeyRecord> loadSignedPreKeys() {
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        String[] projection = {
                DBHelper.SPKR_SIGNED_PRE_KEY_RECORD_COLUMN
        };
        /*
        String selection =
                SampleSQLiteDBHelper.PERSON_COLUMN_NAME + " like ? and " +
                        SampleSQLiteDBHelper.PERSON_COLUMN_AGE + " > ? and " +
                        SampleSQLiteDBHelper.PERSON_COLUMN_GENDER + " like ?";

        String[] selectionArgs = {"%" + name + "%", age, "%" + gender + "%"};
         */
        Cursor cursor = database.query(
                DBHelper.TABLE_SIGNED_PRE_KEY_RECORD,   // The table to query
                projection,                               // The columns to return
                null,//selection,                                // The columns for the WHERE clause
                null,//selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // don't sort
        );

        ArrayList<SignedPreKeyRecord> keys = new ArrayList<>();

        Log.d("TAG", "loadSignedPreKeys --> Numero de keys: " + cursor.getCount());
        if (cursor.getCount() > 0) {
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToNext();
                byte[] byteObject = cursor.getBlob(i);
                SignedPreKeyRecord temp = (SignedPreKeyRecord) DBHelper.blobByteToObject(byteObject);
                keys.add(temp);
            }
        }
        cursor.close();
        database.close();
        return keys;
    }

    @Override
    public void storeSignedPreKey(int signedPreKeyId, SignedPreKeyRecord record) {
        Log.i(TAG, "storeSignedPreKey --> Se quiere crear un nuevo resgostro en la tabla : " + DBHelper.TABLE_SIGNED_PRE_KEY_RECORD + ", con los valores signedPreKeyId: " + signedPreKeyId + ", SignedPreKeyRecord: " + record);
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBHelper.SPKR_SIGNED_PRE_KEY_RECORD_COLUMN, record.serialize());
        values.put(DBHelper.SPKR_ID_COLUMN, signedPreKeyId);
        long newRowId = database.insert(DBHelper.TABLE_SIGNED_PRE_KEY_RECORD, null, values);

        if(newRowId != -1)
            Log.i(TAG, "storeSignedPreKey-->row id: " + newRowId);
        else
            Log.e(TAG, "storeSignedPreKey--> ERROR: creando registro en la base: " + newRowId);
        database.close();
    }

    @Override
    public boolean containsSignedPreKey(int signedPreKeyId) {
        //TODO
        return false;
    }

    @Override
    public void removeSignedPreKey(int signedPreKeyId) {
        //TODO
    }
}
