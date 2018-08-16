package com.baofengtv.supporter.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.baofengtv.supporter.Trace;

/**
 * @author LiLiang
 * @version v1.0
 * @brief 数据库创建相关
 * @date 2015/8/3
 */
public class DBHelper extends SQLiteOpenHelper{

    public static final String DB_NAME = "poster.db";

    public static final String TABLE_POSTER = "poster";

    public static final String _ID = "_id";
    public static final String _BUSINESS_ID = "_business_id";
    public static final String _URL = "_url";
    public static final String _FILE_PATH = "_file_path";
    public static final String _DESC = "_desc";
    public static final String _INTENT = "_intent";
    public static final String _TIMESTAMP = "_timestamp";

    public static final String CMD_CREATE_POSTER = "CREATE TABLE IF NOT EXISTS " +
            TABLE_POSTER + "(" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + _BUSINESS_ID + " INTEGER NOT NULL," + _URL + " TEXT," +
            _FILE_PATH + " TEXT NOT NULL," + _DESC + " TEXT,"
            + _INTENT + " TEXT," + _TIMESTAMP + " TEXT);";

    public static final int VERSION = 4;

    //======LiLiang added screen_saver.db on 2016-6-14=====
    public static final String TABLE_SCREEN_SAVER = "screen_saver";
    public static final String _TYPE = "_type";
    public static final String _MD5 = "_md5";

    public static final String CMD_CREATE_SCREEN_SAVER = "CREATE TABLE IF NOT EXISTS " +
            TABLE_SCREEN_SAVER + "(" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + _BUSINESS_ID + " INTEGER NOT NULL," + _URL + " TEXT," +
            _FILE_PATH + " TEXT NOT NULL," + _DESC + " TEXT,"
            + _INTENT + " TEXT," + _TIMESTAMP +" TEXT," + _TYPE + " TEXT," + _MD5 + " TEXT);";
    //==========end screen_saver.db============================

    public DBHelper(Context context, String name) {
        super(context, name, null, VERSION, null);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Trace.Debug("###onCreate()");
        db.execSQL(CMD_CREATE_POSTER);
        db.execSQL(CMD_CREATE_SCREEN_SAVER);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Trace.Debug("###onUpgrade");
        db.execSQL("drop table if exists " + TABLE_POSTER);
        db.execSQL("drop table if exists " + TABLE_SCREEN_SAVER);
        onCreate(db);
    }
}
