package edu.umbc.covid19.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.List;

public class DBManager {

    private DatabaseHelper dbHelper;

    private Context context;

    private SQLiteDatabase database;

    public DBManager(Context c) {
        context = c;
        open();
    }

    public DBManager open() throws SQLException {
        dbHelper = new DatabaseHelper(context);
        database = dbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        dbHelper.close();
    }

    public void insert(byte[] eid, String lat, String lng, String rssi) {
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseHelper.EID, eid);
        contentValue.put(DatabaseHelper.LAT, lat);
        contentValue.put(DatabaseHelper.LNG, lng);
        contentValue.put(DatabaseHelper.RSSI, rssi);
        database.insert(DatabaseHelper.TABLE_NAME, null, contentValue);
    }

    public Cursor fetchKeys() {
        Cursor cursor = database.rawQuery("SELECT * FROM "+DatabaseHelper.TABLE_NAME+" WHERE "+DatabaseHelper.TIMESTAMP +" > datetime('now', '-1 day')", null);
        if (cursor != null) {
            cursor.moveToFirst();
        }

        Log.i("DBManager", "fetchKeys: "+cursor.getCount());
        return cursor;
    }

    public Cursor getEphIds(){
        Cursor cursor = database.rawQuery("SELECT * FROM "+DatabaseHelper.E_KEYS_TABLE_NAME+" WHERE "+DatabaseHelper.TIMESTAMP +" > datetime('now', '-1 day')", null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public void insertEKeys(List<byte[]> keys) {
        ContentValues contentValue = new ContentValues();
        for(byte[] val: keys){
            contentValue.put(DatabaseHelper.EID, val);
            database.insert(DatabaseHelper.E_KEYS_TABLE_NAME, null, contentValue);
        }

    }



    public void delete(long _id) {
        database.delete(DatabaseHelper.TABLE_NAME, DatabaseHelper._ID + "=" + _id, null);
    }

}