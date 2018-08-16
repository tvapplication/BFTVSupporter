package com.baofengtv.supporter.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.baofengtv.supporter.PosterEntry;
import com.baofengtv.supporter.Trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * @author LiLiang
 * @version v1.0
 * @brief
 * @date 2015/8/3
 */
public class ContentManager {
    public static Map<Integer, PosterEntry> sReadCachedPosterMap;
    private static Object WRITE_LOCK;
    private static Object READ_LOCK;

    static{
        WRITE_LOCK = new Object();
        READ_LOCK = new Object();
        sReadCachedPosterMap = new HashMap<Integer, PosterEntry>();
    }

    /**
     * 存储海报图片的本地下载路径
     * @param writePoster
     */
    public static void writePoster2DB(final Context context, PosterEntry writePoster){
        Observable.just(writePoster)
                .observeOn(AndroidSchedulers.mainThread())  //主线程写db
                .subscribe(new Action1<PosterEntry>() {
                    @Override
                    public void call(PosterEntry poster) {
                        try {
                            synchronized (WRITE_LOCK){
                                ContentValues values = new ContentValues();
                                values.put(DBHelper._BUSINESS_ID, poster.businessId);
                                values.put(DBHelper._URL, poster.url);
                                values.put(DBHelper._FILE_PATH, poster.imagePath);

                                if(poster.desc == null){
                                    poster.desc = "";
                                }
                                values.put(DBHelper._DESC, poster.desc);

                                if(poster.intent == null){
                                    poster.intent = "";
                                }
                                values.put(DBHelper._INTENT, poster.intent);
                                values.put(DBHelper._TIMESTAMP, System.currentTimeMillis());

                                //context.getContentResolver().insert(MyContentProvider.POSTER_CONTENT_URI, values);
                                context.getContentResolver().update(MyContentProvider.POSTER_CONTENT_URI, values,
                                        DBHelper._BUSINESS_ID + "=" + poster.businessId, null);

                                Trace.Debug("###write poster to db. businessId=" + poster.businessId);
                            }
                        }catch (Exception e){
                            Trace.Warn("###write database failed. businessId=" + poster.businessId);
                            e.printStackTrace();
                        }
                    }
                });
    }

    /**
     * 获取其在本地下载成功的海报图片
     * @return 如果没有则返回null
     */
    public static List<PosterEntry> readPosterFromDB(Context context){
        List<PosterEntry> list = new ArrayList<PosterEntry>();
        try {
            Cursor cursor = context.getContentResolver().query(MyContentProvider.POSTER_CONTENT_URI,
                    null, null, null, null);

            if(cursor == null)
                return list;

            if(cursor.moveToFirst()){
                int count = cursor.getCount();
                for(int i=0; i<count; i++){
                    cursor.moveToPosition(i);
                    PosterEntry entry = new PosterEntry();
                    entry.businessId = cursor.getInt(1);
                    entry.url = cursor.getString(2);
                    entry.imagePath = cursor.getString(3);
                    entry.desc = cursor.getString(4);
                    entry.intent = cursor.getString(5);
                    list.add(entry);
                    sReadCachedPosterMap.put(entry.businessId, entry);
                }
            }
            cursor.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 一次把所有数据库内容读到缓存列表sReadCachedPosterMap
     * @param context
     */
    public static void readPostersFromDB2CachedMap(Context context) {
        sReadCachedPosterMap.clear();
        readPosterFromDB(context);
    }

    public static void clearCachedPostersMap(){
        sReadCachedPosterMap.clear();
    }

    /**
     * 根据业务id获取其在本地下载成功的海报图片
     * @param context 上下文
     * @param businessId 业务id
     * @return
     */
    public static PosterEntry readPosterFromDB(Context context, int businessId){
        if(sReadCachedPosterMap.containsKey(businessId)){
            return sReadCachedPosterMap.get(businessId);
        }
        Trace.Debug("read poster entry from database. businessId=" + businessId);
        synchronized (READ_LOCK){
            PosterEntry entry = new PosterEntry();
            try {
                Cursor cursor = context.getContentResolver().query(MyContentProvider.POSTER_CONTENT_URI,
                        null, DBHelper._BUSINESS_ID + "=" + businessId, null, null);

                if(cursor == null){
                    return entry;
                }
                else if(cursor.getCount() == 0){
                    cursor.close();
                    return entry;
                }

                cursor.moveToFirst();
                entry.businessId = cursor.getInt(1);
                entry.url = cursor.getString(2);
                entry.imagePath = cursor.getString(3);
                entry.desc = cursor.getString(4);
                entry.intent = cursor.getString(5);
                cursor.close();
            } catch (Exception e){
                Trace.Warn("###read database failed. businessId=" + businessId);
                e.printStackTrace();
            }
            return entry;
        }
    }

    public static void deletePosterDB(Context context, int minBusinessId, int maxBusinessId){
        synchronized (WRITE_LOCK){
            try {
                context.getContentResolver().delete(MyContentProvider.POSTER_CONTENT_URI,
                        DBHelper._BUSINESS_ID+" >= ? AND " + DBHelper._BUSINESS_ID + " <= ?",
                        new String[]{String.valueOf(minBusinessId), String.valueOf(maxBusinessId)});
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static int deletePosterDB(Context context, int bussinessId){
        synchronized (WRITE_LOCK){
            try {
                return context.getContentResolver().delete(MyContentProvider.POSTER_CONTENT_URI,
                        DBHelper._BUSINESS_ID+" = " + bussinessId, null);
            }catch (Exception e){
                e.printStackTrace();
            }
            return -1;
        }
    }

}
