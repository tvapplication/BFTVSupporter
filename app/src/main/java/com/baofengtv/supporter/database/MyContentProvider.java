package com.baofengtv.supporter.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/**
 * @author LiLiang
 * @version v1.0
 * @brief   海报图片下载成功后将路径存储到数据库，供launcher提取
 * @date 2015/8/3
 */
public class MyContentProvider extends ContentProvider{

    public static final String AUTHORITY = "com.baofengtv.supporter.provider";

    public static final Uri POSTER_CONTENT_URI =
            Uri.parse("content://" + AUTHORITY + "/" + DBHelper.TABLE_POSTER);

    private static UriMatcher sUriMatcher;
    private static final int MATCH_POSTER = 11;

    //screen_saver.db
    public static final Uri SCREEN_SAVER_CONTENT_URI =
            Uri.parse("content://" + AUTHORITY + "/" + DBHelper.TABLE_SCREEN_SAVER);
    private static final int MATCH_SCREEN_SAVER = 12;

    static {
        //Uri匹配
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, DBHelper.TABLE_POSTER, MATCH_POSTER);
        sUriMatcher.addURI(AUTHORITY, DBHelper.TABLE_SCREEN_SAVER, MATCH_SCREEN_SAVER);
    }

    private DBHelper mDBHelper;

    @Override
    public boolean onCreate() {
        mDBHelper = new DBHelper(getContext(), DBHelper.DB_NAME);
        return (mDBHelper != null);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        try {
            Cursor c = null;
            SQLiteDatabase db = mDBHelper.getReadableDatabase();
            String table;
            if(sUriMatcher.match(uri) == MATCH_POSTER){
                table = DBHelper.TABLE_POSTER;
            }else if(sUriMatcher.match(uri) == MATCH_SCREEN_SAVER){
                table = DBHelper.TABLE_SCREEN_SAVER;
            }else{
                return null;
            }
            c = db.query(table, projection, selection, selectionArgs, null, null, sortOrder);
            return c;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        try {
            SQLiteDatabase db = mDBHelper.getWritableDatabase();
            String table;
            Uri contentUri;
            if(sUriMatcher.match(uri) == MATCH_POSTER){
                table = DBHelper.TABLE_POSTER;
                contentUri = POSTER_CONTENT_URI;
            }else if(sUriMatcher.match(uri) == MATCH_SCREEN_SAVER){
                table = DBHelper.TABLE_SCREEN_SAVER;
                contentUri = SCREEN_SAVER_CONTENT_URI;
            }else{
                return null;
            }

            long id = db.insert(table, null, values);
            if(id > -1){
                Uri insertId = ContentUris.withAppendedId(contentUri, id);
                getContext().getContentResolver().notifyChange(insertId, null);
                return insertId;
            }
        } catch (Exception e){
            e.printStackTrace();
        }finally {
            mDBHelper.close();
        }
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        try {
            SQLiteDatabase db = mDBHelper.getWritableDatabase();
            String table;

            switch(sUriMatcher.match(uri)){
                case MATCH_POSTER:
                    table = DBHelper.TABLE_POSTER;
                    break;
                case MATCH_SCREEN_SAVER:
                    table = DBHelper.TABLE_SCREEN_SAVER;
                    break;
                default:
                    return 0;
            }
            int count = db.delete(table, selection, selectionArgs);
            if(count > 0){
                getContext().getContentResolver().notifyChange(uri, null);
            }
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } finally {
            mDBHelper.close();
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        try {
            SQLiteDatabase db = mDBHelper.getWritableDatabase();
            String table;

            switch(sUriMatcher.match(uri)){
                case MATCH_POSTER:
                    table = DBHelper.TABLE_POSTER;
                    break;
                case MATCH_SCREEN_SAVER:
                    table = DBHelper.TABLE_SCREEN_SAVER;
                    break;
                default:
                    return 0;
            }
            int count = db.update(table, values, selection, selectionArgs);
            if(count == 0){
                insert(uri, values);
            }
            else{
                getContext().getContentResolver().notifyChange(uri, null);
            }
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } finally {
            mDBHelper.close();
        }
    }
}
