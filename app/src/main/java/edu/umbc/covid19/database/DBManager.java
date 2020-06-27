package edu.umbc.covid19.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class DBManager {

    private DatabaseHelper dbHelper;

    private Context context;

    private SQLiteDatabase database;

    public DBManager(Context c) {
        context = c;
    }

    public DBManager open() throws SQLException {
        dbHelper = new DatabaseHelper(context);
        database = dbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        dbHelper.close();
    }

    public void insert(String eid, String lat, String lng, String timestamp, String rssi) {
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseHelper.EID, eid);
        contentValue.put(DatabaseHelper.LAT, lat);
        contentValue.put(DatabaseHelper.LNG, lng);
        contentValue.put(DatabaseHelper.RSSI, rssi);
        contentValue.put(DatabaseHelper.TIMESTAMP, String.valueOf(System.currentTimeMillis()));
        database.insert(DatabaseHelper.TABLE_NAME, null, contentValue);
    }

    public Cursor fetch(Long time) {
        String[] columns = new String[] { DatabaseHelper._ID, DatabaseHelper.EID, DatabaseHelper.LAT, DatabaseHelper.LNG,  DatabaseHelper.RSSI, DatabaseHelper.TIMESTAMP};
        Cursor cursor = database.rawQuery("SELECT * FROM "+DatabaseHelper.TABLE_NAME+" WHERE "+DatabaseHelper.TIMESTAMP +" > datetime('now', '-1 day')", null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }



    public void delete(long _id) {
        database.delete(DatabaseHelper.TABLE_NAME, DatabaseHelper._ID + "=" + _id, null);
    }

}