package com.baofengtv.supporter;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;

import com.baofengtv.middleware.common.BFTVBroadcastAction;
import com.baofengtv.middleware.tv.BFTVCommonManager;
import com.baofengtv.middleware.tv.BFTVFactoryManager;
import com.baofengtv.middleware.tv.BFTVTVManager;
import com.baofengtv.supporter.autorun.AutoRunBusiness;
import com.baofengtv.supporter.bftv.BftvBusiness;
import com.baofengtv.supporter.bootanim.BootAnimBusiness;
import com.baofengtv.supporter.database.ContentManager;
import com.baofengtv.supporter.game.GameBusiness;
import com.baofengtv.supporter.houyi_ad.HouyiAdBusiness;
import com.egame.tv.services.aidl.EgameInstallAppBean;
import com.egame.tv.services.aidl.IEgameService;
import com.umeng.analytics.MobclickAgent;
import com.umeng.commonsdk.UMConfigure;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author
 * @version v1.0
 * @brief 后台执行网络海报下载的Service，执行各SubBusiness的任务
 *          下载成功后将业务id、url、本地文件的存储路径存到数据库，供launcher查询使用
 * @date 2015/8/2
 */
public class SlaveService extends Service {

    //通知下载海报消息
    private static final int MSG_DOWNLOAD_POSTERS = 1;
    //海报下载完毕后发送广播的消息
    private static final int MSG_POSTERS_FINISHED = 2;
    //注册接收信源事件
    private static final int MSG_REGISTER_INPUT_SOURCE = 3;
    //爱游戏的远程service
    private IEgameService mGameService;

    private static SlaveService sInstance;

    private BFTVTVManager mTVManager;
    //接收是否连接信源接口的消息(eg.插拔HDMI)
    private Handler mInputSourceConnHandler;

    private Thread mWorkThread;

    private BroadcastReceiver mNetworkReceiver;

    private static boolean sStrPowerOnOff = true;
    private static long sStrPowerOnTime = 0L;

    private static boolean sInitFlag =false;

    private int mFUIVersion = 1;

    //STR待机开关广播
    private BroadcastReceiver mPowerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(Intent.ACTION_SCREEN_ON)){
                Trace.Debug("###get broadcast ACTION_SCREEN_ON");
                sStrPowerOnOff = true;
                sStrPowerOnTime = System.currentTimeMillis();
                Trace.Debug("###will download posters after 2mins");
                mHandler.sendEmptyMessageDelayed(MSG_DOWNLOAD_POSTERS, 2 * 60 * 1000);
                Constant.setCachedPosterDir(new File("/data/misc/posters"));

                SupporterReceiver.reportAIMicWhenBoot(getApplicationContext());
            }else if(action.equals(Intent.ACTION_SCREEN_OFF)){
                Trace.Debug("###get broadcast ACTION_SCREEN_OFF");
                sStrPowerOnOff = false;
                mHandler.removeMessages(MSG_DOWNLOAD_POSTERS);
            }else if(action.equals(BFTVBroadcastAction.BFTV_ACTION_EXIT_QUICK_STANDBY)){
                Trace.Debug("###get broadcast BFTV_ACTION_EXIT_QUICK_STANDBY");
                SupporterReceiver.reportAIMicWhenBoot(getApplicationContext());
            }
        }
    };


    private Handler mHandler = new Handler(Looper.getMainLooper()){
        public void handleMessage(Message msg){
            if(msg.what == MSG_DOWNLOAD_POSTERS){
                if(Utils.isConnected(SlaveService.this.getApplicationContext())){
                    Trace.Debug("###start to download posters now!");
                    performDownloadPosters();
                    if(mNetworkReceiver != null){
                        unregisterReceiver(mNetworkReceiver);
                        mNetworkReceiver = null;
                    }
                }else{
                    Trace.Warn("###network is disable");
                    registerNetworkReceiver();
                }
            }else if(msg.what == MSG_POSTERS_FINISHED){
                    Intent intent = new Intent(Constant.BROADCAST_POSTERS_FINISHED);
                    SlaveService.this.getApplicationContext().sendBroadcast(intent);
            }else if(msg.what == MSG_REGISTER_INPUT_SOURCE){
                //一连即播功能
                if(mInputSourceConnHandler == null){
                    mTVManager = BFTVTVManager.getInstance(getApplicationContext());
                    initInputSourceConnHandler();
                    mTVManager.setSrcConnectListener(mInputSourceConnHandler);
                }
            }
        }
    };

    public SlaveService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        Trace.Debug("###onBind()");
        sInstance = this;

        return mBinder;
    }

    @Override
    public void onCreate(){
        Trace.Debug("###onCreate()");
        super.onCreate();
        MobclickAgent.onResume(this);

        mFUIVersion = Utils.getFUIVersion(this);
        Trace.Debug("sInitFlag = " + sInitFlag);
        if(!sInitFlag) {
            initData();
            sInitFlag = true;
        }

        //统计渠道设为平台
        UMConfigure.init(this, "55bddbe1e0f55a7f4e009b35", Build.MODEL/*"Motou"*/, UMConfigure.DEVICE_TYPE_BOX, null);
        MobclickAgent.setScenarioType(this, MobclickAgent.EScenarioType.E_UM_NORMAL);
    }

    private void initData(){
//        //是否设置为测试地址
//        String fileName = "server";
//        String key = "test_url";
//        File file = new File("/data/data/" + getPackageName() + "/shared_prefs/server.xml");
//        boolean isTestServer = false;
//
//        if (file != null && file.exists()) {
//            isTestServer = getSharedPreferences(fileName, Context.MODE_PRIVATE).getBoolean(key, false);
//        } else {
//            getSharedPreferences(fileName, Context.MODE_PRIVATE).edit().putBoolean(key, false).commit();
//        }
//        Constant.setTestServer(isTestServer);

        //设定海报的下载路径文件夹
        Constant.setCachedPosterDir(new File("/data/misc/posters"));
        //后裔广告的下载路径
        Constant.setHouyiAdDir(new File("/data/misc/baofengtv/houyi_ad"));

        if( mFUIVersion == 1 )
            bindGameService();

        //2分钟后开始下载网络海报
        Trace.Debug("###will download posters after 2mins");
        mHandler.sendEmptyMessageDelayed(MSG_DOWNLOAD_POSTERS, 2 * 60 * 1000);
        //mHandler.sendEmptyMessageDelayed(MSG_DOWNLOAD_POSTERS, 5000);
        sInstance = this;

        //20s后再注册接收信源事件，避免有设备已插入的情况下开机后直接跳转到TV了
        mHandler.sendEmptyMessageDelayed(MSG_REGISTER_INPUT_SOURCE, 20 * 1000);

        try{
            resetAndroidID(getApplicationContext());
        }catch (Exception e){
            e.printStackTrace();
        }

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(BFTVBroadcastAction.BFTV_ACTION_EXIT_QUICK_STANDBY);
        registerReceiver(mPowerReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Trace.Debug("###onStartCommand");
        sInstance = this;
        //提升优先级
        Notification notification = new Notification();
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        notification.flags |= Notification.FLAG_NO_CLEAR;
        notification.flags |= Notification.FLAG_FOREGROUND_SERVICE;
        startForeground(1, notification);

        //return START_STICKY;
        //如果return START_STICKY,launcher3D桌面rebind也在start会冲突，导致不断kill-restart
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent){
        Trace.Debug("###onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy(){
        Trace.Debug("###onDestroy()");
        if(mFUIVersion == 1)
            unbindService(mGameServiceConn);
        sInstance = null;
        super.onDestroy();
        MobclickAgent.onPause(this);
        if(mInputSourceConnHandler != null){
            mInputSourceConnHandler = null;
        }
        try {
            unregisterReceiver(mPowerReceiver);
        }catch (Exception e){}

    }

    public static SlaveService getInstance(){
        return sInstance;
    }

    /**
     * 执行下载海报、存储海报
     */
    public void performDownloadPosters(){
        if(mWorkThread != null && mWorkThread.isAlive()){
            Trace.Warn("###SlaveService is running last request. don't perform this once!");
            return;
        }
        mWorkThread = new Thread(){
            public void run(){
                //如有必要则解绑广播
                SupporterReceiver.reportWifi(getApplicationContext());

                if(mFUIVersion == 1){
                    if(Utils.isPackageInstalled(getApplicationContext(),Constant.LAUNCHER_10_PACKAGE)) {
                        //1.下载所有海报
                        ContentManager.readPostersFromDB2CachedMap(getApplicationContext());
                        //2.1各个业务执行解析海报url、下载海报、建立数据库对应关系
                        BftvBusiness.getInstance(getApplicationContext()).getTaskRunnable().run();
                        //2.2
                        GameBusiness.getInstance(getApplicationContext()).getTaskRunnable().run();
                        ContentManager.clearCachedPostersMap();
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                        }
                    }
                }

                //4自启动
                AutoRunBusiness.getInstance(getApplicationContext()).getTaskRunnable().run();

                //5.下载开机动画（视频）
                //改从后裔系统下载，FUI1.0&3.0都改
                Trace.Debug("###now deal with boot animation business");
                BootAnimBusiness.getInstance(getApplicationContext()).getTaskRunnable().run();

                //6.后裔广告系统（下载Launcher左下角广告位、影视/游戏/体育/购物卡片启动图、关机广告图、屏保）
                HouyiAdBusiness.getInstance(getApplicationContext()).getTaskRunnable().run();

                mWorkThread = null;
            }
        };
        mWorkThread.start();
    }

    public void notifyDownloadPostersCompleted(){
        Trace.Debug("###send msg MSG_POSTERS_FINISHED");
        mHandler.sendEmptyMessage(MSG_POSTERS_FINISHED);
    }

    //友盟统计
    public static void onEvent(String event, String value){
        if(sInstance != null) {
            //MobclickAgent.onResume(sInstance);
            MobclickAgent.onEvent(sInstance, event, value);
            //MobclickAgent.onPause(sInstance);
        }
    }
    public static void onEvent(String event, Map<String, String> map){
        if(sInstance != null){
            MobclickAgent.onResume(sInstance);
            MobclickAgent.onEvent(sInstance, event, map);
            MobclickAgent.onPause(sInstance);
        }
    }

    public static void onEventValue(String event, Map<String, String> map, int var){
        if(sInstance != null){
            MobclickAgent.onResume(sInstance);
            MobclickAgent.onEventValue(sInstance, event, map, var);
            MobclickAgent.onPause(sInstance);
        }
    }

    public static void onEvent(String event){
        if(sInstance != null) {
            MobclickAgent.onResume(sInstance);
            MobclickAgent.onEvent(sInstance, event);
            MobclickAgent.onPause(sInstance);
        }
    }

    private void registerNetworkReceiver(){
        if(mNetworkReceiver == null){
            mNetworkReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                        if (Utils.isConnected(context)) {
                            Trace.Warn("###network is ok. perform download posters");
                            mHandler.sendEmptyMessage(MSG_DOWNLOAD_POSTERS);
                        }
                    }
                }
            };
        }
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkReceiver, filter);
    }
    public IEgameService getRemoteGameService(){
        if( (mFUIVersion > 1) ||
                !Utils.isPackageInstalled(getApplicationContext(), Constant.LAUNCHER_10_PACKAGE)){
            return null;
        }
        if(mGameService == null){
            bindGameService();
        }
        return mGameService;
    }
    //绑定爱游戏Service，监控游戏apk的卸载和安装
    private void bindGameService() {
        Trace.Debug("###bindGameService()");
        Intent intent = new Intent();
        intent.setPackage("com.egame.tv");
        intent.setAction("com.egame.tv.services.aidl.IEgameService");
        bindService(intent, mGameServiceConn, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection mGameServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mGameService = null;
        }
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Trace.Debug("###onServiceConnected");
            mGameService = IEgameService.Stub.asInterface(service);
            if (mGameService != null) {
                try {
                    List<EgameInstallAppBean> gameList = (List<EgameInstallAppBean>) mGameService
                            .getValue();
                    if (gameList != null) {
                        int size = gameList.size();
                        Trace.Debug("###installed games size=" + size);
                        for (int i = 0; i < size; i++) {
                            Trace.Debug( "###" + i + ":"
                                    + gameList.get(i).toString());
                        }
                        GameBusiness.getInstance(getApplicationContext()).refreshInstallGameListAndSendBroadcast(gameList);
                    }else{
                        Trace.Warn("###gamelist is null");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else {
                Trace.Warn("###mGameService is null");
            }
        }
    };

    private ISupporterService.Stub mBinder = new ISupporterService.Stub() {
        @Override
        public void launchBFTVById(int businessId){
            Trace.Debug("###launchBFTVById(" + businessId + ")");
            PosterEntry entry = ContentManager.readPosterFromDB(getApplicationContext(), businessId);
            if(entry.businessId == businessId){
                launchBFTV(entry);
            }
            else if(entry.businessId == 0){
                Trace.Warn("###query PosterEntry by businessId failed and launchDefault");
                entry.businessId = businessId;
                launchBFTVDefault(entry);
            }else{
                Trace.Warn("###unknown businessId");
            }
        }

        @Override
        public void launchBFTV(PosterEntry entry){
            Trace.Debug("###launchBFTV()");

            Trace.Debug("###entry.intent = " + entry.intent);
            if(TextUtils.isEmpty(entry.intent)){
                launchBFTVDefault(entry);
            }else if(entry.intent.startsWith("http")){
                //WebViewUtils.loadUrlByBrowser(getApplicationContext(), entry.intent);
                WebViewUtils.getInstance(getApplicationContext()).loadUrl(entry.intent);
            }else{
                try {
                    Trace.Debug("###parseAndLaunchIntent start");
                    //解析服务端配置的下一跳动作，解析成功则跳转
                    IntentUtils.parseAndLaunchIntent(getApplicationContext(), entry.intent);
                    Trace.Debug("###parseAndLaunchIntent end");
                } catch (Exception e) {
                    e.printStackTrace();
                    Trace.Debug("###now launchBFTV by default.");
                    launchBFTVDefault(entry);
                }
            }
        }

        private static final String PACKAGE_NAME = "com.baofeng.bftv";
        private static final String ENTRANCE_NAME = "com.baofeng.bftv.activity.MainActivity";
        private static final String EXTRA_NAME = "Activity";
        private void launchBFTVDefault(PosterEntry entry){
            Trace.Debug("###launchBFTVDefault(" + entry.businessId + ")");
            try{
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                //Bundle bundle = new Bundle();
                ComponentName component;
                switch (entry.businessId){
                    case Constant.ID_TURN_PLAY:
                        Trace.Debug("###start package com.baofeng.bftv");
                        Trace.Debug("###start bftv lunbo. LivePlayerActivity");
                        component = new ComponentName(PACKAGE_NAME, ENTRANCE_NAME);
                        intent.setComponent(component);
                        intent.putExtra(EXTRA_NAME, "com.baofeng.bftv.activity.LivePlayerActivity");
                        startActivity(intent);
                        break;
                    case Constant.ID_QUICK_PLAY:
                        //速播
                        component = new ComponentName(PACKAGE_NAME, ENTRANCE_NAME);
                        intent.setComponent(component);
                        intent.putExtra(EXTRA_NAME, "com.baofeng.bftv.activity.launcher.SuboActivity");
                        startActivity(intent);
                        break;
                    case Constant.ID_FILM_LIBRARY:
                        //影视库
                        component = new ComponentName(PACKAGE_NAME, ENTRANCE_NAME);
                        intent.setComponent(component);
                        startActivity(intent);
                        break;
                    case Constant.ID_FILM_CHILDREN:
                        //少儿
                        component = new ComponentName(PACKAGE_NAME, ENTRANCE_NAME);
                        intent.setComponent(component);
                        intent.putExtra(EXTRA_NAME, "com.baofeng.bftv.activity.launcher.ChildrenVideoListActivity");
                        startActivity(intent);
                        break;
                    case Constant.ID_SPECIAL_SECOND_SUB1:
                        //二级专场1：每日播报
                        component = new ComponentName(PACKAGE_NAME, ENTRANCE_NAME);
                        intent.setComponent(component);
                        intent.putExtra(EXTRA_NAME, "com.baofeng.bftv.activity.launcher.WeeklyVideoActivity");
                        startActivity(intent);
                        break;
                    case Constant.ID_SPECIAL_SECOND_SUB2:
                        //二级专场2：奇异果VIP
                        component = new ComponentName(PACKAGE_NAME, ENTRANCE_NAME);
                        intent.setComponent(component);
                        intent.putExtra(EXTRA_NAME, "com.baofeng.bftv.activity.launcher.VIPVideoListActivity");
                        startActivity(intent);
                        break;
                    case Constant.ID_SPECIAL_SECOND_SUB3:
                        //二级专场3：悦生活
                        component = new ComponentName(PACKAGE_NAME, ENTRANCE_NAME);
                        intent.setComponent(component);
                        intent.putExtra(EXTRA_NAME, "com.baofeng.bftv.activity.launcher.FourKSubjectActivity");
                        startActivity(intent);
                        break;
                    case Constant.ID_SPECIAL_SECOND_SUB4:
                        //二级专场4：4K杜比
                        component = new ComponentName(PACKAGE_NAME, ENTRANCE_NAME);
                        intent.setComponent(component);
                        intent.putExtra(EXTRA_NAME, "com.baofeng.bftv.activity.launcher.DolbySubjectActivity");
                        startActivity(intent);
                        break;
                    case Constant.ID_SPECIAL_SECOND_SUB5:
                        //二级专场5：奥飞全明星
                        component = new ComponentName(PACKAGE_NAME, ENTRANCE_NAME);
                        intent.setComponent(component);
                        intent.putExtra(EXTRA_NAME, "com.baofeng.bftv.activity.launcher.AlphaAnimSubjectActivity");
                        startActivity(intent);
                        break;
                    case Constant.ID_SPECIAL_SECOND_SUB6:
                        //二级专场6：极清MV
                        Trace.Debug("###start vip");
                        component = new ComponentName(PACKAGE_NAME, ENTRANCE_NAME);
                        intent.setComponent(component);
                        intent.putExtra(EXTRA_NAME, "com.baofeng.bftv.activity.launcher.MVSubjectActivity");
                        startActivity(intent);
                        break;
                    case Constant.ID_SPORT:
                        startActivityByJson("json_sport.txt");
                        break;
                    case Constant.ID_SPORT_SUB1:
                        startActivityByJson("json_sport_sub1.txt");
                        break;
                    case Constant.ID_SPORT_SUB2:
                        startActivityByJson("json_sport_sub2.txt");
                        break;
                    case Constant.ID_SHOPPING:
                        startActivityByJson("json_readtv.txt");
                        break;
                    case Constant.ID_SHOPPING_SUB1:
                        startActivityByJson("json_readtv_sub1.txt");
                        break;
                    case Constant.ID_SHOPPING_SUB2:
                        startActivityByJson("json_readtv_sub2.txt");
                        break;
                    default:
                        break;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        private void startActivityByJson(String fileName){
            String jsonStr = "";
            try {
                InputStream is = getAssets().open(fileName);
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();
                jsonStr = new String(buffer, "utf8");
                Trace.Debug("###jsonStr=" + jsonStr);

                if( !TextUtils.isEmpty(jsonStr) ){
                    IntentUtils.parseAndLaunchIntent(getApplicationContext(), jsonStr);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public List<GameEntry> requestGamesInfo() throws RemoteException {
            Trace.Debug("###requestGamesInfo");
            int size = GameBusiness.getInstallGameList().size();
            if( size >= 3) {
                List<GameEntry> list = new ArrayList<GameEntry>();
                GameEntry game1 = new GameEntry();
                game1.businessId = Constant.ID_GAME_INSTALLED_SUB1;
                game1.type = 0;
                game1.packageName = GameBusiness.getInstallGameList().get(0).getPackageName();
                game1.iconPath = Constant.CACHED_DIR + game1.packageName + ".png";
                list.add(game1);

                GameEntry game2 = new GameEntry();
                game2.businessId = Constant.ID_GAME_INSTALLED_SUB2;
                game2.type = 0;
                game2.packageName = GameBusiness.getInstallGameList().get(1).getPackageName();
                game2.iconPath = Constant.CACHED_DIR + game1.packageName + ".png";
                list.add(game2);

                GameEntry game3 = new GameEntry();
                game3.businessId = Constant.ID_GAME_INSTALLED_SUB3;
                game3.type = 0;
                game3.packageName = GameBusiness.getInstallGameList().get(2).getPackageName();
                game3.iconPath = Constant.CACHED_DIR + game1.packageName + ".png";
                list.add(game3);

                return list;
            }else{
                //发广播
                GameBusiness.getInstance(getApplicationContext())
                        .refreshInstallGameListAndSendBroadcast(GameBusiness.getInstallGameList());
            }
            return null;
        }

        //跳转至游戏应用首页
        @Override
        public void launchGameApp()throws RemoteException {
            Trace.Debug("###launchGameApp");
            //爱游戏
            launchAppByPackageName("com.egame.tv");
        }

        //进入我的游戏（已安装）
        @Override
        public void launchMyGames()throws RemoteException {
            Trace.Debug("###launchMyGames");
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            intent.setClassName("com.egame.tv",
                    "com.egame.tv.activitys.PreLancherActivity");
            //recordType==4表示进入已安装的游戏列表页
            bundle.putString("RECOMMEND", "{recordType:'4',gameid:'',aid:'',linkurl:'',downloadfrom:'',actioncode:'',name:''}");
            intent.putExtra("openFlag", 1);
            intent.setAction("android.intent.action.VIEW");
            intent.putExtras(bundle);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(intent);
        }

        @Override
        public void launchOneGameApp(GameEntry game) throws RemoteException {
            Trace.Debug("###launchOneGameApp");
            if(game.businessId >= Constant.ID_GAME_INSTALLED_SUB1 &&
                    game.businessId <= Constant.ID_GAME_INSTALLED_SUB3){
                //根据包名启动已安装的游戏
                launchAppByPackageName(game.packageName);
            }
            else if(game.businessId >= Constant.ID_GAME_RECOMMEND_SUB1 &&
                    game.businessId <= Constant.ID_GAME_RECOMMEND_SUB3){
                //隐式启动推荐游戏，进入推荐游戏的详情页
                Intent intent = new Intent();
                Bundle bundle = new Bundle();
                intent.setClassName("com.egame.tv", "com.egame.tv.activitys.PreLancherActivity");
                bundle.putString("RECOMMEND", game.intent);
                //"{recordType:'1',gameid:'730011',aid:'9285',linkurl:'',downloadfrom:'1',actioncode:'1'}"
                intent.setAction("android.intent.action.VIEW");
                intent.putExtras(bundle);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                startActivity(intent);
            }
        }

        @Override
        public void launchMusicApp() throws RemoteException {
            Trace.Debug("###launchMusicApp");
            launchAppByPackageName("com.baofengtv.hifimusic");
        }

        @Override
        public void launchMyMusics() throws RemoteException {
            Trace.Debug("###launchMyMusics");
            Intent intent = new Intent();
            ComponentName cn = new ComponentName("com.baofengtv.hifimusic",
                    "com.tongyong.xxbox.activity.MyMusicActivity");
            intent.setComponent(cn);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(intent);
        }

        @Override
        public void launchRandomMusics() throws RemoteException {
            Trace.Debug("###launchRandomMusics");
            //改用虾米的随便听听接口
            Uri uri = Uri.parse("xiami://music/recommend");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(intent);
        }

        @Override
        public void launchMarketApp() throws RemoteException {
            Trace.Debug("###launchMarketApp");
            launchAppByPackageName("com.baofeng.dangbeimarket");
        }

        @Override
        public void launchTurnPlayApp() throws RemoteException {
            Trace.Debug("###launchTurnPlayApp");
            launchBFTVById(Constant.ID_TURN_PLAY);
        }

        @Override
        public void launchAlbumApp() throws RemoteException{
            Trace.Debug("###launchAlbumApp()。nothing to do");
        }

        @Override
        public PosterEntry queryPosterById(int businessId) throws RemoteException {
            Trace.Debug("###queryPosterById");
            return ContentManager.readPosterFromDB(getApplicationContext(), businessId);
        }

        @Override
        public List<PosterEntry> queryPosters() throws RemoteException {
            Trace.Debug("###queryPosters");
            return ContentManager.readPosterFromDB(getApplicationContext());
        }

        private void launchAppByPackageName(String packageName) {
            // 通过包名获取此APP详细信息，包括Activities、services、versioncode、name等等
            PackageInfo packageinfo = null;
            try {
                packageinfo = getPackageManager().getPackageInfo(packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            if (packageinfo == null) {
                return;
            }

            Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
            resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            resolveIntent.setPackage(packageinfo.packageName);

            List<ResolveInfo> resolveInfoList = getPackageManager()
                    .queryIntentActivities(resolveIntent, 0);

            Iterator<ResolveInfo> it = resolveInfoList.iterator();
            if(it.hasNext()){
                ResolveInfo resolveinfo = it.next();
                if (resolveinfo != null) {
                    String pkgName = resolveinfo.activityInfo.packageName;
                    String className = resolveinfo.activityInfo.name;
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_LAUNCHER);

                    ComponentName cn = new ComponentName(pkgName, className);
                    intent.setComponent(cn);
                    if(pkgName.equals("com.baofeng.dangbeimarket")){
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    startActivity(intent);
                }
            }

        }
    };

    private static final int TYRANT_STATE_CLOSE = 0;
    private static final int TYRANT_STATE_HINT = 1;
    private static final int TYRANT_STATE_OPEN = 2;
    private void initInputSourceConnHandler(){
        mInputSourceConnHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                //msg.arg1:source; msg.what:1,on;0,off
                BFTVCommonManager.EN_BFTV_INPUT_SOURCE source = BFTVCommonManager.EN_BFTV_INPUT_SOURCE.values()[msg.arg1];
                BFTVCommonManager.EN_BFTV_COMMON_ON_OFF connect = BFTVCommonManager.EN_BFTV_COMMON_ON_OFF.values()[msg.what];
                Trace.Debug("###source = " + source);
                Trace.Debug("###connect = " + connect);

                String topPackageName = Utils.getTopPackageName(SlaveService.this.getApplicationContext());
                if(topPackageName.equals("com.baofengtv.setupwizard") ){
                    //开机向导不弹一连即播
                    return;
                }

                if(connect == BFTVCommonManager.EN_BFTV_COMMON_ON_OFF.ON){
                    if(topPackageName.equals("com.baofengtv.screensaver")){
                        //先关闭屏保
                        Trace.Debug("###stop screen saver first.");
                        Intent intent2 = new Intent(BFTVBroadcastAction.BFTV_ACTION_STOP_SCREEN_SAVER);
                        SlaveService.this.getApplicationContext().sendBroadcast(intent2);
                    }
                    int state = getTyrantState();
                    Trace.Debug("###tyrant_state = " + state);
                    if(state == TYRANT_STATE_OPEN){
                        Trace.Debug("###state=open");
                        //如果是str待机开机后会收到不合法消息，需要屏蔽
                        if( !sStrPowerOnOff ){
                            Trace.Warn("ignore this tyrant message. it's str power off");
                            return;
                        }
                        long duration = System.currentTimeMillis() - sStrPowerOnTime;
                        Trace.Debug("###current time - power on time = " + duration);
                        if( duration < 8000 ){
                            Trace.Warn("ignore this tyrant message. it's str power on");
                            return;
                        }

                        Trace.Debug("###jmup to TVPlayer. source=" + source.name());

                        Intent intent = new Intent();
                        intent.setClassName("com.baofengtv.tvplayer",
                                "com.baofengtv.tvplayer.MainActivity");
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                        intent.putExtra("baofengtv.extra.INPUT_SOURCE_INDEX", source.ordinal());

                        SlaveService.this.getApplicationContext().startActivity(intent);

                        SlaveService.this.onEvent(UMengUtils.EVENT_INPUT_SOURCE_JUMP);
                    }else if(state == TYRANT_STATE_HINT){
                        Trace.Debug("###state=hint. ignore");
                    }
                }
            }

            //一连即播开关
            private int getTyrantState(){
                if(mFUIVersion >= 3){
                    //com.baofengtv.middleware.server.BFTVSettingManager sm
                    //        = com.baofengtv.middleware.server.BFTVSettingManager.getInstance(getApplicationContext());
                    //return sm.getTyrantValue();
                    try {
                        return BFTVCommonManager.getInstance(getApplicationContext()).getTyrantValue();
                    }catch (Throwable e){
                        return TYRANT_STATE_CLOSE;
                    }
                }
                try {
                    //读取shared_pref的keyword已经被系统设置app更改，注意推送至旧系统平台的兼容问题
                    SharedPreferences sharedPref = getTargetContext().getSharedPreferences("tvplayer_prefs",
                            Context.MODE_WORLD_READABLE | Context.MODE_MULTI_PROCESS);
                    //return sharedPref.getInt("tyrant_state", TYRANT_STATE_HINT);
                    //如果BFTVSupporter推送到旧平台上使用以下代码
                    int systemSettingVersionCode = -1;
                    PackageInfo pkgInfo = SlaveService.this.getPackageManager().getPackageInfo("com.baofengtv.settings", 0);
                    systemSettingVersionCode = pkgInfo.versionCode;
                    if(systemSettingVersionCode < 250){
                        boolean ret = sharedPref.getBoolean("tyrant_support", false);
                        if(ret)
                            return TYRANT_STATE_OPEN;
                        else
                            return TYRANT_STATE_HINT;
                    }else{
                        return sharedPref.getInt("tyrant_state", TYRANT_STATE_HINT);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                return TYRANT_STATE_CLOSE;
            }

            private Context getTargetContext() throws PackageManager.NameNotFoundException {
                return createPackageContext("com.baofengtv.settings", Context.CONTEXT_IGNORE_SECURITY);
            }
        };
    }

    /**
     * 如有必要重置ANDROID_ID使其唯一，避免友盟统计不准
     * @param context
     */
    private void resetAndroidID(Context context){
        String androidID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        Trace.Debug("###read android_id = " + androidID);
        String sn = BFTVFactoryManager.getInstance(context).getSerialNumber();
        Trace.Debug("###serialNumber = " + sn);

        if( TextUtils.isEmpty(sn) ) {
            return;
        }

        String crcValue = Utils.getCrc32Value(sn);
        Trace.Debug("###crcValue = " + crcValue);

        if( TextUtils.isEmpty(androidID) || !androidID.equals(crcValue)){
            Trace.Debug("###write crcValue to ANDROID_ID");
            Settings.Secure.putString(getContentResolver(),
                    Settings.Secure.ANDROID_ID, crcValue);
        }
        //String androidID2 = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        //Trace.Debug("###End222 read android_id = " + androidID2);
    }

}
