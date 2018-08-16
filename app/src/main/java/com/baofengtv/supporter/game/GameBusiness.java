package com.baofengtv.supporter.game;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import com.baofengtv.supporter.AbstractBusiness;
import com.baofengtv.supporter.Constant;
import com.baofengtv.supporter.GameEntry;
import com.baofengtv.supporter.PosterEntry;
import com.baofengtv.supporter.Trace;
import com.baofengtv.supporter.Utils;
import com.baofengtv.supporter.database.ContentManager;
import com.baofengtv.supporter.game.json.RecommendGame;
import com.baofengtv.supporter.loader.DownloadParam;
import com.baofengtv.supporter.net.OkHttpUtils;
import com.egame.tv.services.aidl.EgameInstallAppBean;
import com.umeng.analytics.MobclickAgent;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * @author LiLiang
 * @version v1.04
 * @brief 游戏业务：获取爱游戏的海报、已安装apk列表的包名及海报、推荐游戏列表的intent和海报
 * @date 2015/8/3
 * @brief GameBusiness不再取海报，只拉取推荐游戏
 */
public class GameBusiness extends AbstractBusiness {

    private Context mContext;
    //进入爱游戏首页的大海报
    private String mPosterUrl;

    private List<ResolveInfo> apps = new ArrayList<ResolveInfo>();

    private static GameBusiness sInstance;

    public static GameBusiness getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new GameBusiness(context);
        }
        return sInstance;
    }

    //当前安装的游戏apk列表
    private static List<EgameInstallAppBean> sInstallGameList;
    //推荐游戏列表，当sInstallGameList.size<3时有效
    private static List<RecommendGame> sRecommendGameList;

    static {
        sInstallGameList = new ArrayList<EgameInstallAppBean>();
        sRecommendGameList = new ArrayList<RecommendGame>();
    }

    /**
     * 任务
     *
     * @return
     */
    public Runnable getTaskRunnable() {
        return mTaskRunnable;
    }

    private GameBusiness(Context context) {
        mContext = context;

        mTaskRunnable = new Runnable() {
            @Override
            public void run() {
                //游戏海报不再依赖从爱游戏服务器获取. 2015-11-19
                if (sInstallGameList.size() < 3) {
                    doRecommendTask();
                }
            }
        };
    }

    private void doRecommendTask() {
        //3.1如果已安装的游戏个数小于3，网络获取推荐游戏，成功后sRecommendGameList得到赋值
        Trace.Debug("###3.1 get recommend games poster url");
        if (sRecommendGameList.size() < 3) {
            mRecommendRunnable.run();
            sendGameBroadcast();
        }
        //3.2下载每个推荐游戏的apk图标
        Trace.Debug("###3.2 download recommend games poster image");
        int size = sRecommendGameList.size();
        for (int i = 0; i < size; i++) {
            final String url = sRecommendGameList.get(i).imageurl;
            if (TextUtils.isEmpty(url)) {
                Trace.Warn("###" + i + ": url is empty");
                continue;
            }
            final String fileMD5 = Utils.getMD5(url) + Utils.getPostfix(url);

            final int businessId = Constant.ID_GAME_RECOMMEND_SUB1 + i;
            PosterEntry readPoster = ContentManager.readPosterFromDB(mContext, businessId);
            Trace.Debug("###readPoster=" + readPoster);
            boolean needDownload = false;

            if (TextUtils.isEmpty(readPoster.imagePath)) {
                needDownload = true;
            } else {
                File file = new File(readPoster.imagePath);
                if (!file.exists()) {
                    needDownload = true;
                } else if (file.length() == 0) {
                    file.delete();
                    needDownload = true;
                } else if (!(readPoster.imagePath.equals(Constant.CACHED_DIR + fileMD5))) {
                    file.delete();
                    needDownload = true;
                }
            }

            final String jumpIntent = sRecommendGameList.get(i).recommended_id;

            if (needDownload) {
                DownloadParam param = new DownloadParam();
                param.src = url;
                param.dst = Constant.CACHED_DIR + fileMD5;

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
                                if (isSucc) {
                                    //做20px圆角apk图标
                                    File file = new File(Constant.CACHED_DIR + fileMD5);
                                    File newRoundFile = toRoundImageFile(Constant.CACHED_DIR + fileMD5);
                                    if (newRoundFile != null) {
                                        newRoundFile.renameTo(file);
                                    }

                                    PosterEntry writePoster = new PosterEntry();
                                    writePoster.businessId = businessId;
                                    writePoster.url = url;
                                    writePoster.imagePath = Constant.CACHED_DIR + fileMD5;
                                    writePoster.desc = "recommend";
                                    writePoster.intent = jumpIntent;
                                    //写数据库
                                    ContentManager.writePoster2DB(mContext, writePoster);
                                }
                            }
                        });
            } else {
                Trace.Debug("###get poster from cache. no need to download it. businessId="
                        + readPoster.businessId);
            }
        }
    }

    //获取推荐游戏，当本地没有安装或安装少于3个游戏时，获取推荐游戏
    private Runnable mRecommendRunnable = new Runnable() {
        @Override
        public void run() {
            final Subscriber<GameRequest.RecommendResult<List<RecommendGame>>> subscriber
                    = new Subscriber<GameRequest.RecommendResult<List<RecommendGame>>>() {
                @Override
                public void onCompleted() {
                }

                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                    MobclickAgent.reportError(mContext, e);
                }

                @Override
                public void onNext(GameRequest.RecommendResult<List<RecommendGame>> result) {
                    if (result != null) {
                        List<RecommendGame> gameList = result.ext;
                        if (gameList != null && gameList.size() > 0) {
                            sRecommendGameList.clear();
                            for (RecommendGame game : gameList) {
                                Trace.Debug(game.toString());
                                sRecommendGameList.add(game);
                            }
                        }
                    }
                }
            };
            GameRequest.getRecommendData(subscriber);
        }
    };

    //拉取游戏列表，发广播
    public void refreshInstallGameListAndSendBroadcast(List<EgameInstallAppBean> list) {
        apps.clear();
        sInstallGameList.clear();
        sInstallGameList.addAll(list);

        //获取本地游戏图标
        dealWithInstallGamesIcon();

        if (sInstallGameList.size() >= 3) {
            sendGameBroadcast();
        } else {
            //当sInstallGameList.size<3时，取推荐游戏
            if (sRecommendGameList.size() >= 3) {
                sendGameBroadcast();
            } else {
                new Thread() {
                    public void run() {
                        doRecommendTask();
                    }
                }.start();
            }
        }
    }

    public static List<EgameInstallAppBean> getInstallGameList() {
        return sInstallGameList;
    }

    private void sendGameBroadcast() {
        final ArrayList<GameEntry> gameList = new ArrayList<GameEntry>();

        int count = 0;
        int size = sInstallGameList.size();
        size = (size < 3) ? size : 3;
        for (int i = 0; i < size; i++) {
            count++;
            GameEntry installGame = new GameEntry();
            //ID_GAME_INSTALLED_SUB1 | ID_GAME_INSTALLED_SUB2 | ID_GAME_INSTALLED_SUB3
            installGame.businessId = Constant.ID_GAME_INSTALLED_SUB1 + i;
            installGame.type = 0;
            installGame.packageName = sInstallGameList.get(i).getPackageName();
            installGame.iconPath = Constant.CACHED_DIR + installGame.packageName + ".png";
            gameList.add(installGame);
        }

        if (count < 3) {
            int size2 = sRecommendGameList.size();
            for (int j = 0; j < size2; j++) {
                count++;
                GameEntry recommendGame = new GameEntry();
                //ID_GAME_RECOMMEND_SUB1 | ID_GAME_RECOMMEND_SUB2 | ID_GAME_RECOMMEND_SUB3
                recommendGame.businessId = Constant.ID_GAME_RECOMMEND_SUB1 + j;
                recommendGame.type = 1;
                recommendGame.intent = sRecommendGameList.get(j).recommended_id;
                //LiLiang added. 推荐类型的packageName字段传递name，用于统计
                recommendGame.packageName = sRecommendGameList.get(j).name;
                recommendGame.iconPath = Constant.CACHED_DIR +
                        Utils.getMD5(sRecommendGameList.get(j).imageurl) +
                        Utils.getPostfix(sRecommendGameList.get(j).imageurl);
                gameList.add(recommendGame);
                if (count == 3) {
                    break;
                }
            }
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                int size3 = gameList.size();
                for (int k = 0; k < 3; k++) {
                    if (k >= size3)
                        break;
                    PosterEntry gamePosterEntry = new PosterEntry();
                    gamePosterEntry.businessId = Constant.ID_GAME_SUB1 + k;
                    gamePosterEntry.imagePath = gameList.get(k).iconPath;
                    ContentManager.writePoster2DB(mContext, gamePosterEntry);
                }

                Intent intent = new Intent();
                intent.setAction(Constant.BROADCAST_GAMEINFO);
                intent.putParcelableArrayListExtra("game", gameList);
                mContext.sendBroadcast(intent);
            }
        });

    }

    private void dealWithInstallGamesIcon() {
        int size = sInstallGameList.size();
        int count = (size < 3) ? size : 3;
        String packageName;
        String iconPath;
        for (int i = 0; i < count; i++) {
            packageName = sInstallGameList.get(i).getPackageName();
            PosterEntry readPoster = ContentManager.readPosterFromDB(mContext, Constant.ID_GAME_INSTALLED_SUB1 + i);
            iconPath = Constant.CACHED_DIR + packageName + ".png";
            if ((!TextUtils.isEmpty(readPoster.imagePath)) && readPoster.imagePath.equals(iconPath)) {
                Trace.Debug("no need to parse apk icon");
            } else {
                Trace.Debug("update apk icon");
                PosterEntry writePoster = new PosterEntry();
                writePoster.businessId = Constant.ID_GAME_INSTALLED_SUB1 + i;

                File icon = new File(iconPath);
                if (icon.exists()) {
                    writePoster.imagePath = iconPath;
                } else {
                    File iconFile = getAPKIcon(packageName);
                    if (iconFile != null) {
                        writePoster.imagePath = iconFile.getAbsolutePath();
                    }
                }
                ContentManager.writePoster2DB(mContext, writePoster);
            }
        }
    }

    private File getAPKIcon(String packageName) {
        Trace.Debug("getAPKIcon(" + packageName + ")");
        if (apps.size() == 0) {
            final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            apps.addAll(mContext.getPackageManager().queryIntentActivities(mainIntent, 0));
            Trace.Debug("find " + apps.size() + " packages ");
        }

        for (int i = 0; i < apps.size(); i++) {
            if (packageName.equals(apps.get(i).activityInfo.applicationInfo.packageName)) {
                Trace.Debug("find a package : " + apps.get(i).activityInfo.applicationInfo.packageName);
                Drawable drawable = apps.get(i).loadIcon(mContext.getPackageManager());
                if (drawable instanceof BitmapDrawable) {
                    Bitmap bmp = ((BitmapDrawable) drawable).getBitmap();
                    if (bmp != null) {
                        //cache路径+包名.png
                        File file = new File(Constant.CACHED_DIR + packageName + ".png");
                        try {
                            file.createNewFile();
                            file.setReadable(true, false);
                            file.setWritable(true, false);
                            file.setExecutable(true, false);

                            int w = bmp.getWidth();
                            int h = bmp.getHeight();
                            Bitmap newBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(newBmp);
                            final Paint paint = new Paint();
                            final Rect rect = new Rect(0, 0, w, h);
                            final RectF rectF = new RectF(rect);
                            paint.setAntiAlias(true);
                            canvas.drawARGB(0, 0, 0, 0);

                            float round = (20.0f * w) / 92;
                            Trace.Debug("###round = " + round);
                            canvas.drawRoundRect(rectF, round, round, paint);
                            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
                            canvas.drawBitmap(bmp, rect, rect, paint);

                            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                            newBmp.compress(Bitmap.CompressFormat.PNG, 100, bos);
                            bos.flush();
                            bos.close();
                            return file;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return file;
                    }
                } else {
                    Trace.Warn("drawable not instance of BitmapDrawable");
                }
            }
        }
        return null;
    }

    /**
     * 如果卸载的是爱游戏平台的游戏apk，要通知更新Launcher的游戏图标
     *
     * @param gamePackageName
     */
    public void refreshInstalledGames(String gamePackageName) {
        if (TextUtils.isEmpty(gamePackageName))
            return;

        int size = sInstallGameList.size();
        Trace.Debug("###sInstallGameList::size=" + size);
        int delIndex = -1;
        for (int i = 0; i < size; i++) {
            Trace.Debug("###[" + i + "] " + sInstallGameList.get(i).getPackageName());
            if (sInstallGameList.get(i).getPackageName().equals(gamePackageName)) {
                delIndex = i;
            }
        }
        if (delIndex != -1) {
            Trace.Debug("###remove item " + delIndex);
            sInstallGameList.remove(delIndex);
            Trace.Debug("###send game info broadcast after remove game apk.");
            sendGameBroadcast();
        }
    }

    private File toRoundImageFile(String filePath) {
        try {
            Bitmap bmp = BitmapFactory.decodeFile(filePath);
            File file = new File(filePath + ".round");
            file.createNewFile();
            file.setReadable(true, false);
            file.setWritable(true, false);
            file.setExecutable(true, false);

            int w = bmp.getWidth();
            int h = bmp.getHeight();
            Bitmap newBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(newBmp);
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, w, h);
            final RectF rectF = new RectF(rect);
            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);

            float round = (20.0f * w) / 92;
            //Trace.Debug("###round = " + round);
            canvas.drawRoundRect(rectF, round, round, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(bmp, rect, rect, paint);

            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            newBmp.compress(Bitmap.CompressFormat.PNG, 100, bos);
            bos.flush();
            bos.close();

            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
