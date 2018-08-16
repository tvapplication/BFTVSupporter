package com.baofengtv.supporter.bootanim;

import android.content.Context;
import android.text.TextUtils;

import com.baofeng.houyi.ad.HouyiAdLoader;
import com.baofeng.houyi.ad.entity.AdEntity;
import com.baofeng.houyi.constants.AdMatType;
import com.baofeng.houyi.constants.ErrorCode;
import com.baofeng.houyi.count.HouyiCountEngine;
import com.baofeng.houyi.interfaces.HouyiAdListener;
import com.baofengtv.supporter.AbstractBusiness;
import com.baofengtv.supporter.Constant;
import com.baofengtv.supporter.Trace;
import com.baofengtv.supporter.Utils;
import com.baofengtv.supporter.loader.DownloadParam;
import com.baofengtv.supporter.net.OkHttpUtils;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.List;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * @author LiLiang
 * @version v1.0
 * @brief 开机动画图（视频）下载业务
 * 说明：视频的优先级高于动画，当视频文件和动画文件同时存在时，系统会播放开机视频
 * @date 2015/8/21
 */
public class BootAnimBusiness extends AbstractBusiness {

    public static final String BOOT_ANIM_FILE = "/data/misc/baofengtv/ADBoot/bootanimation.zip";
    public static final String BOOT_VIDEO_FILE = "/data/misc/baofengtv/ADBoot/bootvideo";

    public static final int TYPE_BOOT_ANIM = 2;
    public static final int TYPE_BOOT_VIDEO = 3;

    public static final int AdMatType_MAT_TYPE_ZIP = 3;

    private Context mContext;

    private static BootAnimBusiness sInstance;

    public static BootAnimBusiness getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new BootAnimBusiness(context);
        }
        return sInstance;
    }

    private BootAnimBusiness(Context context) {
        mContext = context;

        final LinkedHashMap<String, String> customParams;
        customParams = new LinkedHashMap<String, String>();
        customParams.put("custom_platform", Utils.getCurPlatform(mContext));
        customParams.put("custom_uuid", Utils.getSerialNumberWithPostfix(mContext));
        customParams.put("custom_fui_version", String.valueOf(Utils.getFUIVersion(mContext)));
        customParams.put("custom_is_vip", Utils.isVip(mContext));//"1"是vip "0"不是
        customParams.put("custom_system_version", Utils.getSystemVersion(mContext));
        customParams.put("custom_soft_id", Utils.getSoftId(mContext));

        mTaskRunnable = new Runnable() {
            @Override
            public void run() {
                //开机广告和其他场景不同，直接走即时协商接口-下载文件，下次开机展示
                HouyiAdLoader.loadAd(Constant.HOUYI_AD_BOOT, new HouyiAdListener() {
                    @Override
                    public void onAdFailed(String adPositionId, int errorCode, Exception e) {
                        Trace.Error("onAdFailed(AD_BOOT" + adPositionId + ")" + Constant.HouyiAdMap.get(adPositionId));
                        Trace.Debug("errorCode=" + errorCode);
                        e.printStackTrace();
                        if(errorCode == ErrorCode.NO_FILL_ERROR) {
                            //删掉本地的超期缓存
                            delOldBootAnimFileIfExist();
                            delOldBootVideoFileIfExist();
                        }
                    }

                    @Override
                    public void onAdSuccess(String adPositionId, List<AdEntity> list) {
                        Trace.Debug("onAdSuccess(AD_BOOT" + adPositionId + ")");
                        if (list == null) {
                            return;
                        }
                        int size = list.size();
                        if (size == 0) {
                            //删掉本地的超期缓存
                            delOldBootAnimFileIfExist();
                            delOldBootVideoFileIfExist();
                        }
                        //优先下载开机视频，其次下载开机动画zip包
                        int animIndex = -1;
                        int videoIndex = -1;

                        for (int i = 0; i < size; i++) {
                            AdEntity entity = list.get(i);
                            Trace.Debug("[HOUYI_AD_BOOT] " + entity.toString());
                            if (entity.getMattype() == AdMatType.MAT_TYPE_VIDEO) {
                                videoIndex = i;
                            } else if (entity.getMattype() == AdMatType_MAT_TYPE_ZIP) {
                                animIndex = i;
                            }
                        }
                        //优先下载开机视频，没有视频再判断有无动画下载
                        if (videoIndex >= 0 && videoIndex < size) {
                            //下载视频，无需下载动画zip包
                            downloadNewBootFilesIfNeed(list.get(videoIndex));
                        } else if (animIndex >= 0 && animIndex < size) {
                            //如果有旧的本地开机视频文件，要删掉。否则即使下载了新的开机动画系统策略还是会播放旧的视频
                            delOldBootVideoFileIfExist();
                            //下载开机动画
                            downloadNewBootFilesIfNeed(list.get(animIndex));
                        }

                    }
                }, customParams);
            }
        };
    }

    @Override
    public Runnable getTaskRunnable() {
        return mTaskRunnable;
    }

    /**
     * 删除旧的开机动画
     */
    private void delOldBootAnimFileIfExist() {
        File oldAnimFile = new File(BOOT_ANIM_FILE);
        if (oldAnimFile.exists()) {
            Trace.Debug("delOldBootAnimFile");
            oldAnimFile.delete();
        }
    }

    /**
     * 删除旧的开机视频
     */
    private void delOldBootVideoFileIfExist() {
        File oldVideoFile = new File(BOOT_VIDEO_FILE);
        if (oldVideoFile.exists()) {
            Trace.Debug("delOldBootVideoFile");
            oldVideoFile.delete();
        }
    }

    private void downloadNewBootFilesIfNeed(AdEntity entity) {
        File file = null;
        if (entity.getMattype() == AdMatType_MAT_TYPE_ZIP) {
            file = new File(BOOT_ANIM_FILE);
        } else if (entity.getMattype() == AdMatType.MAT_TYPE_VIDEO) {
            file = new File(BOOT_VIDEO_FILE);
        }
        if (file == null)
            return;

        if (!file.exists()) {
            //下载
            Trace.Debug("###no old boot files. now download");
            downloadNewBootFiles(entity);
        } else {
            //比较MD5
            String fileMD5 = Utils.fileMD5(file.getAbsolutePath());
            if (!fileMD5.equalsIgnoreCase(entity.getUrlMd5())) {
                Trace.Warn("###md5 is not match. delete old boot files");
                //文件的MD5值不匹配，需要重新下载
                if (entity.getMattype() == AdMatType_MAT_TYPE_ZIP) {
                    delOldBootAnimFileIfExist();
                } else if (entity.getMattype() == AdMatType.MAT_TYPE_VIDEO) {
                    delOldBootVideoFileIfExist();
                }
                //下载
                downloadNewBootFiles(entity);
            } else {
                Trace.Debug("###md5 is match. no need to download boot files.");
                HouyiCountEngine.countAdDisplaySuccess(entity.getAdId(), Constant.HOUYI_AD_BOOT);
                if (entity.getPv() != null) {
                    int size = entity.getPv().size();
                    for (int i = 0; i < size; i++) {
                        final String pvUrl = entity.getPv().get(i).getPvUrl();
                        int time = entity.getPv().get(i).getPvTime();
                        Trace.Debug("###countThird(" + pvUrl + ") time=" + time);
                        getHandler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                HouyiCountEngine.countThird(pvUrl);
                            }
                        }, time * 1000);
                    }
                }
            }
        }

    }

    private void downloadNewBootFiles(final AdEntity entity) {
        if (TextUtils.isEmpty(entity.getContentOrUrl()))
            return;
        //后裔广告系统-统计计数
        HouyiCountEngine.countAdDisplayTry(entity.getAdId(), Constant.HOUYI_AD_BOOT);
        Trace.Debug("###now download new boot files from server.");

        final String tmpDestPath = "/data/misc/baofengtv/ADBoot/boot.tmp";
        final File tmpFile = new File(tmpDestPath);
        if (tmpFile.exists()) {
            tmpFile.delete();
        }

        String _destPath = BOOT_VIDEO_FILE;
        if (entity.getMattype() == 3) {
            _destPath = BOOT_ANIM_FILE;
        }
        final String destPath = _destPath;

        DownloadParam param = new DownloadParam();
        param.src = entity.getContentOrUrl();
        param.dst = tmpDestPath;
        param.srcMD5 = entity.getUrlMd5();

        Observable.just(param)
                .map(new Func1<DownloadParam, DownloadParam>() {
                    @Override
                    public DownloadParam call(DownloadParam param1) {
                        Trace.Debug("[RxJava download boot]" + param1.toString());
                        try {
                            int statusCode = OkHttpUtils.doDownload(param1);
                            param1.code = statusCode;
                        } catch (IOException e) {
                            e.printStackTrace();
                            param1.code = -1;
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                            param1.code = -1;
                        }
                        return param1;
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe(new Action1<DownloadParam>() {
                    @Override
                    public void call(DownloadParam param2) {
                        Trace.Debug("[RxJava download boot finished]" + param2.toString());
                        boolean isSucc = (param2.code == OkHttpUtils.CODE_OK || param2.code == OkHttpUtils.CODE_RESUME_OK);
                        if (isSucc) {
                            param2.dstMD5 = Utils.fileMD5(param2.dst);
                            if (TextUtils.isEmpty(param2.dstMD5) || param2.dstMD5.equalsIgnoreCase(param2.srcMD5)) {
                                //重命名 tmpDestPath --> destPath
                                boolean renameRet = tmpFile.renameTo(new File(destPath));
                                Trace.Debug("###rename result=" + renameRet);
                                HouyiCountEngine.countAdDisplaySuccess(entity.getAdId(), Constant.HOUYI_AD_BOOT);
                                if (entity.getPv() != null) {
                                    int size = entity.getPv().size();
                                    for (int i = 0; i < size; i++) {
                                        final String pvUrl = entity.getPv().get(i).getPvUrl();
                                        int time = entity.getPv().get(i).getPvTime();
                                        Trace.Debug("###countThird(" + pvUrl + ") time=" + time);
                                        getHandler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                HouyiCountEngine.countThird(pvUrl);
                                            }
                                        }, time * 1000);
                                    }
                                }
                            } else {
                                Trace.Warn("###md5 is not match!");
                                HouyiCountEngine.countAdDisplayFail(entity.getAdId(), Constant.HOUYI_AD_BOOT);
                            }
                        } else {
                            HouyiCountEngine.countAdDisplayFail(entity.getAdId(), Constant.HOUYI_AD_BOOT);
                        }
                    }
                });
    }
}
