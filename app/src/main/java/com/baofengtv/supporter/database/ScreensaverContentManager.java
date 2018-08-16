package com.baofengtv.supporter.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.baofengtv.supporter.ScreensaverEntry;
import com.baofengtv.supporter.Trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * @author LiLiang
 * @version 1.0
 * @title 类的名称
 * @description 该类主要功能描述
 * @company 暴风TV
 * @created 2016/6/14 15:51
 * @changeRecord [修改记录] <br/>
 */
public class ScreensaverContentManager {
    private static Object WRITE_LOCK;
    private static Object READ_LOCK;

    static{
        WRITE_LOCK = new Object();
        READ_LOCK = new Object();
    }

    public static void writeScreensaver2DB(final Context context, ScreensaverEntry entry){
        Observable.just(entry)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ScreensaverEntry>() {
                    @Override
                    public void call(ScreensaverEntry entry) {
                        try {
                            synchronized (WRITE_LOCK){
                                ContentValues values = new ContentValues();
                                values.put(DBHelper._BUSINESS_ID, entry.businessId);
                                values.put(DBHelper._URL, entry.url);
                                values.put(DBHelper._FILE_PATH, entry.localPath);

                                if(entry.desc == null){
                                    entry.desc = "";
                                }
                                values.put(DBHelper._DESC, entry.desc);

                                if(entry.intent == null){
                                    entry.intent = "";
                                }
                                values.put(DBHelper._INTENT, entry.intent);

                                //时间戳使用sort字段代替
                                //values.put(DBHelper._TIMESTAMP, System.currentTimeMillis());
                                values.put(DBHelper._TIMESTAMP, entry.timestamp);

                                if(entry.type == null){
                                    entry.type = "";
                                }
                                values.put(DBHelper._TYPE, entry.type);

                                if(entry.md5 == null){
                                    entry.md5 = "";
                                }
                                values.put(DBHelper._MD5, entry.md5);

                                context.getContentResolver().update(MyContentProvider.SCREEN_SAVER_CONTENT_URI, values,
                                        DBHelper._BUSINESS_ID + "=" + entry.businessId, null);

                                Trace.Debug("###write poster to db. businessId=" + entry.businessId);
                            }
                        }catch (Exception e){
                            Trace.Warn("###write screen_saver database failed. businessId=" + entry.businessId);
                            e.printStackTrace();
                        }
                    }
                });
    }

    /**
     * 删除超过上限个数的屏保
     * @return 返回删除的元素集合
     */
    public static void deleteOldScreensaver(Context context){
        /*Trace.Debug("deleteOldScreensaver");
        //删除旧的图片屏保
        long timestamp = 0L;
        int size = 0;

        List<ScreensaverEntry> picList = readScreensaverFromDB(context, ScreensaverBusiness.TYPE_PIC);
        size = picList.size();
        if(size > ScreensaverBusiness.MAX_PIC_COUNT){
            timestamp = picList.get(ScreensaverBusiness.MAX_PIC_COUNT-1).timestamp;
            context.getContentResolver().delete(MyContentProvider.SCREEN_SAVER_CONTENT_URI, DBHelper._TYPE + " = ? AND "
                    + DBHelper._TIMESTAMP + " < ? ",
                    new String[]{ScreensaverBusiness.TYPE_PIC, String.valueOf(timestamp)});

            for(int i=ScreensaverBusiness.MAX_PIC_COUNT; i<size; i++){
                File file = new File(picList.get(i).localPath);
                if(file.exists()){
                    file.delete();
                    Trace.Debug("###delete file. index=" + i);
                }
            }
        }

        //删除旧的视频
        List<ScreensaverEntry> videoList = readScreensaverFromDB(context, ScreensaverBusiness.TYPE_VIDEO);
        size = videoList.size();
        if(videoList.size() > ScreensaverBusiness.MAX_VIDEO_COUNT){
            timestamp = videoList.get(ScreensaverBusiness.MAX_VIDEO_COUNT-1).timestamp;
            context.getContentResolver().delete(MyContentProvider.SCREEN_SAVER_CONTENT_URI, DBHelper._TYPE + " = ? AND "
                            + DBHelper._TIMESTAMP + " < ? ",
                    new String[]{ScreensaverBusiness.TYPE_VIDEO, String.valueOf(timestamp)});

            for(int j=ScreensaverBusiness.MAX_VIDEO_COUNT; j<size; j++){
                File file = new File(videoList.get(j).localPath);
                if(file.exists()){
                    file.delete();
                    Trace.Debug("###delete file. index=" + j);
                }
            }
        }*/
    }

    public static void deleteOldScreensaver(Context context, String md5) {
        //删除旧的图片屏保
        context.getContentResolver().delete(MyContentProvider.SCREEN_SAVER_CONTENT_URI, DBHelper._MD5 + " = ? ",
                new String[]{md5});
    }

        public static List<ScreensaverEntry> readScreensaverFromDB(Context context, String type){
        List<ScreensaverEntry> list = new ArrayList<ScreensaverEntry>();
        synchronized (READ_LOCK){
            try {
                //按时间戳倒序
                Cursor cursor = context.getContentResolver().query(MyContentProvider.SCREEN_SAVER_CONTENT_URI,
                        null, DBHelper._TYPE + " = ? ", new String[]{type}, DBHelper._TIMESTAMP + " DESC");
                //Cursor cursor = context.getContentResolver().query("content://com.baofengtv.supporter.provider/screen_saver",
                //      null, "_type = ? ", "video"/*"pic"*/, "_timestamp DESC");

                if(cursor == null)
                    return list;

                if(cursor.moveToFirst()){
                    int count = cursor.getCount();
                    for(int i=0; i<count; i++){
                        cursor.moveToPosition(i);
                        ScreensaverEntry entry = new ScreensaverEntry();
                        entry.businessId = cursor.getInt(1);
                        entry.url = cursor.getString(2);
                        entry.localPath = cursor.getString(3);
                        entry.desc = cursor.getString(4);
                        entry.intent = cursor.getString(5);
                        entry.timestamp = cursor.getLong(6);
                        entry.type = cursor.getString(7);
                        entry.md5 = cursor.getString(8);
                        //Trace.Debug(i + " : " + entry.toString());
                        list.add(entry);
                    }
                }
                cursor.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        return list;
    }

    public static HashMap<String, ScreensaverEntry> readScreensaverFromDB2Map(Context context){
        HashMap<String, ScreensaverEntry> map = new HashMap<String, ScreensaverEntry>();
        synchronized (READ_LOCK){
            try {
                Cursor cursor = context.getContentResolver().query(MyContentProvider.SCREEN_SAVER_CONTENT_URI,
                        null, null, null, null);

                if(cursor == null)
                    return map;

                if(cursor.moveToFirst()){
                    int count = cursor.getCount();
                    for(int i=0; i<count; i++){
                        cursor.moveToPosition(i);
                        ScreensaverEntry entry = new ScreensaverEntry();
                        entry.businessId = cursor.getInt(1);
                        entry.url = cursor.getString(2);
                        entry.localPath = cursor.getString(3);
                        entry.desc = cursor.getString(4);
                        entry.intent = cursor.getString(5);
                        entry.timestamp = cursor.getLong(6);
                        entry.type = cursor.getString(7);
                        entry.md5 = cursor.getString(8);
                        //Trace.Debug(i + " : " + entry.toString());
                        if(!TextUtils.isEmpty(entry.md5)) {
                            map.put(entry.md5.toLowerCase(), entry);
                        }
                    }
                }
                cursor.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        return map;
    }

}
