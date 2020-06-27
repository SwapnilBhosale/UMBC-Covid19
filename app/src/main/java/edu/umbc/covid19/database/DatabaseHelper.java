package edu.umbc.covid19.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Table Name
    public static final String TABLE_NAME = "encounters";

    // Table columns
    public static final String _ID = "_id";
    public static final String EID = "eid";
    public static final String LAT = "lat";
    public static final String LNG = "lng";
    public static final String TIMESTAMP = "timestamp";
    public static final String RSSI = "rssi";


    // Database Information
    static final String DB_NAME = "UMBC.USM.COVID.APP";

    // database version
    static final int DB_VERSION = 1;

    // Creating table query
    private static final String CREATE_TABLE = "create table " + TABLE_NAME + "(" + _ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT, " + EID + " TEXT NOT NULL, " + LAT + " TEXT, "+ LNG+" TEXT, "+RSSI+" TEXT, "+TIMESTAMP+" DATETIME);";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

}