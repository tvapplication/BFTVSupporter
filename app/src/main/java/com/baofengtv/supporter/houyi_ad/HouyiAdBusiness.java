package com.baofengtv.supporter.houyi_ad;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;

import com.baofeng.houyi.ad.HouyiAdLoader;
import com.baofeng.houyi.ad.entity.PreLoadEntity;
import com.baofeng.houyi.count.HouyiCountEngine;
import com.baofeng.houyi.interfaces.HouyiPreLoadListener;
import com.baofengtv.supporter.AbstractBusiness;
import com.baofengtv.supporter.Constant;
import com.baofengtv.supporter.Trace;
import com.baofengtv.supporter.UMengUtils;
import com.baofengtv.supporter.Utils;
import com.baofengtv.supporter.loader.DownloadParam;
import com.baofengtv.supporter.net.OkHttpUtils;
import com.umeng.analytics.MobclickAgent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import wseemann.media.FFmpegMediaMetadataRetriever;

/**
 * @author LiLiang
 * @version 1.0
 * @title 类的名称
 * @description Launcher对接后裔广告系统的业务，左下角广告位、影视/游戏/体育/购物卡片启动图
 * @company 暴风TV
 * @created 2017/2/23 18:25
 * @changeRecord [修改记录] <br/>
 */

public class HouyiAdBusiness extends AbstractBusiness {

    private Context mContext;

    private static HouyiAdBusiness sInstance;

    private HashMap<String, Integer> countMap;

    public static HouyiAdBusiness getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new HouyiAdBusiness(context);
        }
        return sInstance;
    }

    private HouyiAdBusiness(Context context) {
        mContext = context;
        final int FUIVersion = Utils.getFUIVersion(context);

        boolean isJDChannel = Utils.isJDChannel(mContext);
        Trace.Debug("isJDChannel? " + isJDChannel);
        if(isJDChannel){
            //京东渠道定制机是另外的屏保位id
            Constant.HOUYI_AD_SCREEN_SAVER = Constant.HOUYI_AD_JD_SCREEN_SAVER;
        }

        mTaskRunnable = new Runnable() {
            @Override
            public void run() {
                countMap = new HashMap<String, Integer>();
                countMap.put(Constant.HOUYI_AD_POWERDOWN, 0);
                countMap.put(Constant.HOUYI_AD_LAUNCHER_ADV, 0);
                countMap.put(Constant.HOUYI_AD_LAUNCHER_VIDEO, 0);
                countMap.put(Constant.HOUYI_AD_LAUNCHER_GAME, 0);
                countMap.put(Constant.HOUYI_AD_LAUNCHER_SPORT, 0);
                countMap.put(Constant.HOUYI_AD_LAUNCHER_SHOPPING, 0);
                countMap.put(Constant.HOUYI_AD_SCREEN_SAVER, 0);

                //关机广告预下载
                HouyiAdLoader.preDownload(Constant.HOUYI_AD_POWERDOWN, mHouyiPreLoadListener);
                sleep(300);

                if (FUIVersion == 1) {
                    //FUI1.0使用
                    //Launcher左下角广告位预下载
                    HouyiAdLoader.preDownload(Constant.HOUYI_AD_LAUNCHER_ADV, mHouyiPreLoadListener);
                    sleep(300);
                    //影视卡片-启动图预下载
                    HouyiAdLoader.preDownload(Constant.HOUYI_AD_LAUNCHER_VIDEO, mHouyiPreLoadListener);
                    sleep(300);
                    //游戏卡片-启动图预下载
                    HouyiAdLoader.preDownload(Constant.HOUYI_AD_LAUNCHER_GAME, mHouyiPreLoadListener);
                    sleep(300);
                    //体育卡片-启动图预下载
                    HouyiAdLoader.preDownload(Constant.HOUYI_AD_LAUNCHER_SPORT, mHouyiPreLoadListener);
                    sleep(300);
                    //购物卡片-启动图预下载
                    HouyiAdLoader.preDownload(Constant.HOUYI_AD_LAUNCHER_SHOPPING, mHouyiPreLoadListener);
                    sleep(300);
                }

                //屏保
                HouyiAdLoader.preDownload(Constant.HOUYI_AD_SCREEN_SAVER, mHouyiPreLoadListener);
            }
        };
    }

    /**
     * 后裔广告预下载回调
     */
    private HouyiPreLoadListener mHouyiPreLoadListener = new HouyiPreLoadListener() {
        @Override
        public void onAdFailed(String adPositionId, Exception e) {
            Trace.Error("###onAdFailed(" + adPositionId + Constant.HouyiAdMap.get(adPositionId) + ")");
            e.printStackTrace();
            if (e instanceof SocketTimeoutException) {
                MobclickAgent.onEvent(mContext, UMengUtils.PRELOAD_AD_RESULT,
                        "failed_socket_timeout" + Constant.HouyiAdMap.get(adPositionId));
            } else {
                MobclickAgent.onEvent(mContext, UMengUtils.PRELOAD_AD_RESULT,
                        "failed_" + Constant.HouyiAdMap.get(adPositionId));
            }
        }

        @Override
        public void onAdSuccess(String adPositionId, List<PreLoadEntity> list) {
            Trace.Debug("###onAdSuccess(" + adPositionId + Constant.HouyiAdMap.get(adPositionId) + ")");
            MobclickAgent.onEvent(mContext, UMengUtils.PRELOAD_AD_RESULT,
                    "success_" + Constant.HouyiAdMap.get(adPositionId));
            if (list == null)
                return;
            ArrayList<String> fileNameList = new ArrayList<String>();

            //后裔广告系统-统计计数
            HouyiCountEngine.countAppPreLoadTry(adPositionId);

            for (PreLoadEntity entity : list) {
                Trace.Debug("###@@@" + entity.toString());
                //文件命名：广告id_md5.后缀格式
                String url = entity.getUrl();
                if (TextUtils.isEmpty(url))
                    continue;
                countMap.put(adPositionId, countMap.get(adPositionId) + 1);
                String fileName = adPositionId + "_" + Utils.getMD5(url) + Utils.getPostfix(url);
                fileNameList.add(fileName);
                downloadAdEntity(adPositionId, entity, fileName);
            }

            if (fileNameList.size() > 0) {
                //处理旧文件
                deleteOldFiles(adPositionId, fileNameList);
            }
        }
    };

    private void deleteOldFiles(String adPositionId, List<String> list) {
        //找出前缀adPostionId开头的所有广告文件
        File[] files = new File(Constant.HOUYI_AD_DIR).listFiles(new MyFilter(adPositionId));
        List<File> delList = new ArrayList<File>();
        //包含在list中的则保留
        if(files != null){
            for (File f : files) {
                delList.add(f);
                String name = f.getName();
                for (String fileName : list) {
                    if (name.startsWith(fileName)) {
                        delList.remove(f);
                    }
                }
            }
        }

        for (File f : delList) {
            if (f.exists()) {
                Trace.Debug("###delete old file " + f.getAbsolutePath());
                f.delete();
            }
        }
    }

    static class MyFilter implements FilenameFilter {
        private String prefix;

        public MyFilter(String prefix) {
            this.prefix = prefix;
        }

        public boolean accept(File dir, String name) {
            return name.startsWith(prefix);
        }
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (Exception e) {
        }
    }

    @Override
    public Runnable getTaskRunnable() {
        return mTaskRunnable;
    }

    private void downloadAdEntity(final String adPositionId, PreLoadEntity entity, String fileName) {
        final String url = entity.getUrl();
        final String serverFileMD5 = entity.getUrlMd5();

        Trace.Debug("###downloadAdEntity " + url);
        final String filePath = Constant.HOUYI_AD_DIR + fileName;
        Trace.Debug("###filePath=" + filePath);
        final File file = new File(filePath);
        if (file.exists()) {
            String localFileMd5 = Utils.fileMD5(filePath);
            //Trace.Debug("###localFileMD5=" + localFileMd5);
            if (TextUtils.isEmpty(serverFileMD5) || localFileMd5.equalsIgnoreCase(serverFileMD5)) {
                Trace.Debug("###has cached file. and md5 match. don't download. " + adPositionId);
                int count = countMap.get(adPositionId) - 1;
                countMap.put(adPositionId, count);
                return;
            }
            Trace.Warn("md5 dismatch. now download files again.");
            file.delete();
        }


        final DownloadParam param = new DownloadParam();
        param.src = url;
        param.dst = filePath;

        Observable.just(param)
                .map(new Func1<DownloadParam, Boolean>() {
                    @Override
                    public Boolean call(DownloadParam param) {
                        try {
                            int statusCode = OkHttpUtils.doDownload(param);
                            return statusCode == OkHttpUtils.CODE_OK || statusCode == OkHttpUtils.CODE_RESUME_OK;
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                        }
                        return false;
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean isSucc) {
                        int count = countMap.get(adPositionId) - 1;
                        countMap.put(adPositionId, count);
                        if (isSucc) {
                            //校验MD5,serverFileMD5为空时跳过校验
                            String fileMD5 = Utils.fileMD5(filePath);
                            if (TextUtils.isEmpty(fileMD5) || TextUtils.isEmpty(serverFileMD5)
                                    || fileMD5.equalsIgnoreCase(serverFileMD5)) {
                                if (count == 0) {
                                    Trace.Debug("###报数成功" + Constant.HouyiAdMap.get(adPositionId));
                                    HouyiCountEngine.countAppPreLoadSuccess(adPositionId);
                                }

                                if(adPositionId.equals(Constant.HOUYI_AD_SCREEN_SAVER)){
                                    //视频屏保需要获取首帧图
                                    if(filePath.endsWith(".mp4") || filePath.endsWith(".MP4")
                                            ||filePath.endsWith(".wmv") || filePath.endsWith(".mkv")
                                            ||filePath.endsWith(".rm") || filePath.endsWith(".rmvb")){
                                        String bmpPath = param.dst + ".jpg";
                                        try {
                                            FFmpegMediaMetadataRetriever media = new FFmpegMediaMetadataRetriever();
                                            media.setDataSource(filePath);
                                            Bitmap bitmap = media.getFrameAtTime(0);
                                            media.release();
                                            saveBitmap(bmpPath, bitmap);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            File bmpFile = new File(bmpPath);
                                            if (bmpFile.exists())
                                                bmpFile.delete();
                                        }
                                    }
                                }
                            } else {
                                Trace.Warn("###md5 dismatch!" + Constant.HouyiAdMap.get(adPositionId));
                                Trace.Warn("localMD5 = " + fileMD5 + "; serverMD5=" + serverFileMD5);

                                if (count == 0) {
                                    Trace.Debug("###报数失败" + Constant.HouyiAdMap.get(adPositionId));
                                    HouyiCountEngine.countAppPreLoadFail(adPositionId);
                                }
                            }
                        } else {
                            if (count == 0) {
                                Trace.Debug("###报数失败" + Constant.HouyiAdMap.get(adPositionId));
                                HouyiCountEngine.countAppPreLoadFail(adPositionId);
                            }
                        }
                    }
                });

    }

    private void saveBitmap(String path, Bitmap bitmap) throws Exception {
        File f = new File(path);
        if (f.exists()) {
            f.delete();
        }
        if (bitmap == null)
            return;

        f.createNewFile();
        f.setReadable(true, false);
        f.setWritable(true, false);
        f.setExecutable(true, false);

        FileOutputStream out = new FileOutputStream(f);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        out.flush();
        out.close();
    }
}

