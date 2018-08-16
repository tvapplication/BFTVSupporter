package com.baofengtv.supporter;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;

import com.baofengtv.middleware.common.BFTVBroadcastAction;
import com.baofengtv.middleware.tv.BFTVCommonManager;
import com.baofengtv.middleware.tv.BFTVFactoryManager;
import com.baofengtv.middleware.tv.BFTVTVManager;
import com.baofengtv.supporter.autorun.AutoRunBusiness;
import com.baofengtv.supporter.bootanim.BootAnimBusiness;
import com.baofengtv.supporter.houyi_ad.HouyiAdBusiness;
import com.umeng.analytics.MobclickAgent;
import com.umeng.commonsdk.UMConfigure;

import java.io.File;
import java.util.Map;

/**
 * @author
 * @version v1.0
 * @brief
 * @date 2015/8/2
 */
public class SlaveService extends Service {

    //通知下载海报消息
    private static final int MSG_DOWNLOAD_POSTERS = 1;
    //注册接收信源事件
    private static final int MSG_REGISTER_INPUT_SOURCE = 3;

    private static SlaveService sInstance;

    private BFTVTVManager mTVManager;
    //接收是否连接信源接口的消息(eg.插拔HDMI)
    private Handler mInputSourceConnHandler;

    private Thread mWorkThread;

    private BroadcastReceiver mNetworkReceiver;

    private static boolean sStrPowerOnOff = true;
    private static long sStrPowerOnTime = 0L;

    private static boolean sInitFlag =false;

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
                    //如有必要则解绑广播
                    SupporterReceiver.reportWifi(getApplicationContext());
                    if(mNetworkReceiver != null){
                        unregisterReceiver(mNetworkReceiver);
                        mNetworkReceiver = null;
                    }
                }else{
                    Trace.Warn("###network is disable");
                    registerNetworkReceiver();
                }
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

        return null;
    }

    @Override
    public void onCreate(){
        Trace.Debug("###onCreate()");
        super.onCreate();
        MobclickAgent.onResume(this);

        if(!sInitFlag) {
            initData();
            sInitFlag = true;
        }

        //统计渠道设为平台
        UMConfigure.init(this, "55bddbe1e0f55a7f4e009b35", Build.MODEL/*"Motou"*/, UMConfigure.DEVICE_TYPE_BOX, null);
        MobclickAgent.setScenarioType(this, MobclickAgent.EScenarioType.E_UM_NORMAL);
    }

    private void initData(){
        //设定海报的下载路径文件夹
        Constant.setCachedPosterDir(new File("/data/misc/posters"));
        //后裔广告的下载路径
        Constant.setHouyiAdDir(new File("/data/misc/baofengtv/houyi_ad"));

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
                //4自启动
                AutoRunBusiness.getInstance(getApplicationContext()).getTaskRunnable().run();

                //5.下载开机动画（视频）
                Trace.Debug("###now deal with boot animation business");
                BootAnimBusiness.getInstance(getApplicationContext()).getTaskRunnable().run();

                //6.后裔广告系统（下载Launcher左下角广告位、影视/游戏/体育/购物卡片启动图、关机广告图、屏保）
                HouyiAdBusiness.getInstance(getApplicationContext()).getTaskRunnable().run();

                mWorkThread = null;
            }
        };
        mWorkThread.start();
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
                //com.baofengtv.middleware.server.BFTVSettingManager sm
                //        = com.baofengtv.middleware.server.BFTVSettingManager.getInstance(getApplicationContext());
                //return sm.getTyrantValue();
                try {
                    return BFTVCommonManager.getInstance(getApplicationContext()).getTyrantValue();
                }catch (Throwable e){
                    return TYRANT_STATE_CLOSE;
                }
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
