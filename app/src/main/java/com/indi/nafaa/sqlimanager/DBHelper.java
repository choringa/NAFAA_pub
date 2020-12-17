package com.indi.nafaa.sqlimanager;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class DBHelper extends SQLiteOpenHelper {

    private static final String TAG = "IKPSqliDBHelper";

    public static final String DB_NAME = "signal_lib_client_database";
    private static final int DB_VERSION = 3;

    //TABLE IDENTY KEY
    public static final String TABLE_IDENTITY_KEY_PAIR_TABLE  = "IdentityKeyPair_Table";
    public static final String IKP_ID = "_id";
    public static final String IKP_SIGNAL_ADDRESS_PROTOCOL_COLUMN = "SignalAddressProtocol_Col";
    public static final String IKP_IDENTITY_KEY_COLUMN = "IdentityKey_Col";

    //TABLE SIGEND PRE KEY RECORD
    public static final String TABLE_SIGNED_PRE_KEY_RECORD  = "SignedPreKeyRecord_Table";
    public static final String SPKR_ID = "_id";
    public static final String SPKR_SIGNED_PRE_KEY_RECORD_COLUMN = "SignedPreKeyRecord_Column";
    public static final String SPKR_ID_COLUMN = "IdentityKey_Column";

    //TABLE PRE KEY STORED
    public static final String TABLE_PRE_KEY_STORED  = "SignedPreKeyRecord_Table";
    public static final String PKS_ID = "_id";
    public static final String PKS_PRE_KEY_STORED_COLUMN = "PreKeyStored_Column";
    public static final String PKS_ID_COLUMN = "PreKeyId_Column";

    private static final String CREATE_TABLE_IDENTY_KEY_PAIR = "CREATE TABLE " + TABLE_IDENTITY_KEY_PAIR_TABLE + " (" +
            IKP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            IKP_SIGNAL_ADDRESS_PROTOCOL_COLUMN + " TEXT, " +
            IKP_IDENTITY_KEY_COLUMN + " BLOB " + ")";

    private static final String CREATE_TABLE_SIGNED_PRE_KEY_RECORD = "CREATE TABLE " + TABLE_SIGNED_PRE_KEY_RECORD + " (" +
            SPKR_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            SPKR_SIGNED_PRE_KEY_RECORD_COLUMN + " BLOB, " +
            SPKR_ID_COLUMN + " INTEGER " + ")";

    private static final String CREATE_TABLE_PRE_KEY_STORED = "CREATE TABLE " + TABLE_PRE_KEY_STORED + " (" +
            PKS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            PKS_PRE_KEY_STORED_COLUMN + " BLOB, " +
            PKS_ID_COLUMN + " INTEGER " + ")";


    public DBHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "onCreate DB Table: " + TABLE_IDENTITY_KEY_PAIR_TABLE);
        db.execSQL(CREATE_TABLE_IDENTY_KEY_PAIR);
        db.execSQL(CREATE_TABLE_SIGNED_PRE_KEY_RECORD);
        db.execSQL(CREATE_TABLE_PRE_KEY_STORED);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "onUpgrade DB Table");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_IDENTITY_KEY_PAIR_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SIGNED_PRE_KEY_RECORD);
        onCreate(db);
    }

    public static byte[] objectToBlobByte1(SignedPreKeyRecord modeldata) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(modeldata);
            byte[] employeeAsBytes = baos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(employeeAsBytes);
            return employeeAsBytes;
        } catch (IOException e) {
            Log.e(TAG, "objectToBlobByte --> ERROR convirtiendo objeto1 a blob: " + e.getStackTrace());
        }

        return null;
    }

    public static byte[] objectToBlobByte(Object modeldata) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(modeldata);
            byte[] employeeAsBytes = baos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(employeeAsBytes);
            return employeeAsBytes;
        } catch (IOException e) {
            Log.e(TAG, "objectToBlobByte --> ERROR convirtiendo objeto a blob: " + e.getLocalizedMessage());
        }

        return null;
    }

    public static Object blobByteToObject(byte[] data) {
        try {
            ByteArrayInputStream baip = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(baip);
            return ois.readObject();
        } catch (IOException e) {
            Log.e(TAG, "blobByteToObject --> ERROR x: " + e.getLocalizedMessage());
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "blobByteToObject --> ERROR convirtiendo de blob a objeto: " + e.getLocalizedMessage());
        }
        return null;
    }
}
