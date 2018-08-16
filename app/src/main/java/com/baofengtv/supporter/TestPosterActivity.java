package com.baofengtv.supporter;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import com.baofengtv.supporter.autorun.AutoRunBusiness;
import com.baofengtv.supporter.bootanim.BootAnimBusiness;
import com.baofengtv.supporter.bftv.BftvBusiness;
import com.baofengtv.supporter.houyi_ad.HouyiAdBusiness;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author LiLiang
 * @version v1.0
 * @brief   测试
 * @date 2015/8/2
 */
public class TestPosterActivity extends Activity implements View.OnClickListener {
//    static{
//        System.loadLibrary("houyi");
//    }
    ActivityManager am;
    // 拉取网络海报
    private Button mGetPosterBtn;
    // 跳转至已安装游戏列表
    private Button mJumpInstallPageBtn;
    // 拉取推荐列表
    private Button mGetRecommendGamesBtn;

    private Button mGetMainPostersBtn;

    //启动杰兔画报
    private Button mStartAlbumBtn;

    private Button mGetScreensaverBtn;

    private String mJsonStr;

    @Override
    public void onCreate(Bundle saveInstanceState){
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(this, SlaveService.class);
        startService(intent);

        mGetScreensaverBtn = (Button)findViewById(R.id.btn_get_screen_saver);
        mGetScreensaverBtn.setOnClickListener(this);

        findViewById(R.id.btn_get_houyi_ad).setOnClickListener(this);
        findViewById(R.id.btn_auto_run).setOnClickListener(this);
        findViewById(R.id.btn_upload_log).setOnClickListener(this);

        findViewById(R.id.btn_get_houyi_ad2).setOnClickListener(this);

        mGetPosterBtn = (Button) findViewById(R.id.btn_get_poster);
        mGetPosterBtn.setOnClickListener(this);

        mJumpInstallPageBtn = (Button) findViewById(R.id.btn_to_install_page);
        mJumpInstallPageBtn.setOnClickListener(this);

        mGetRecommendGamesBtn = (Button) findViewById(R.id.btn_get_recommend_games);
        mGetRecommendGamesBtn.setOnClickListener(this);

        mGetMainPostersBtn = (Button)findViewById(R.id.get_main_poster);
        mGetMainPostersBtn.setOnClickListener(this);

        mStartAlbumBtn = (Button)findViewById(R.id.btn_start_webview);
        mStartAlbumBtn.setOnClickListener(this);

    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mJsonStr = "";
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.get_main_poster:
                final Runnable task = BftvBusiness.getInstance(getApplicationContext()).getTaskRunnable();
                new Thread(){
                    public void run(){
                        task.run();
                    }
                }.start();

                //startActivityByJson("json_readtv_sub2.txt");
                //startActivityByJson("json_shiyun_sport_sub2.txt");

                //测试获取视频第一帧截图
                /*String bmpPath  = "/storage/external_storage/sda1/bfcloud.jpg";
                try {
                    FFmpegMediaMetadataRetriever media = new FFmpegMediaMetadataRetriever();
                    media.setDataSource("/storage/external_storage/sda1/bfcloud.mp4");
                    Bitmap bitmap = media.getFrameAtTime(0);
                    media.release();

                    File f = new File(bmpPath);
                    if (f.exists()) {
                        f.delete();
                    }
                    if (bitmap == null)
                        return;
                    FileOutputStream out = new FileOutputStream(f);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush();
                    out.close();
                }catch (Exception e){
                    e.printStackTrace();
                }*/
                break;
            case R.id.btn_get_screen_saver:
//                final Runnable getScreensaverTask = ScreensaverBusiness.getInstance(getApplicationContext()).getTaskRunnable();
//                new Thread(){
//                    public void run(){
//                        getScreensaverTask.run();
//                    }
//                }.start();
                File file1 = new File("/data/misc/wifi/wpa_supplicant.conf");
                File file2 = new File("/data/misc/wifi/softap.conf");
                File file3 = new File("/data/temp.txt");
                if(file1.exists()){
                    Trace.Debug(file1.getAbsolutePath() + " exists.");
                    readFileByLines(file1.getAbsolutePath());
                }else{
                    Trace.Debug(file1.getAbsolutePath() + " no exists.");
                }

                if(file2.exists()){
                    Trace.Debug(file2.getAbsolutePath() + " exists");
                    readFileByLines(file2.getAbsolutePath());
                }else{
                    Trace.Debug(file2.getAbsolutePath() + " no exists");
                }

                if(file3.exists()){
                    Trace.Debug(file3.getAbsolutePath() + " exists");
                    readFileByLines(file3.getAbsolutePath());
                }else{
                    Trace.Debug(file3.getAbsolutePath() + " no exists");
                }

                Runtime runtime = Runtime.getRuntime();
                try {
                    runtime.exec("su");
                    java.lang.Process process = runtime.exec("cat /data/misc/wifi/softap.conf");
                    InputStream is = process.getInputStream();

                    ArrayList<String> contents = inputStreamToArrayString(is);
                }catch (IOException e){
                    e.printStackTrace();
                }


                break;
            case R.id.btn_get_houyi_ad:
                final Runnable bootRunnable = BootAnimBusiness.getInstance(getApplicationContext()).getTaskRunnable();
                new Thread(){
                    public void run(){
                        bootRunnable.run();
                    }
                }.start();
                break;
            case R.id.btn_get_houyi_ad2:
                final Runnable houyiRunnable = HouyiAdBusiness.getInstance(getApplicationContext()).getTaskRunnable();
                new Thread(){
                    public void run(){
                        houyiRunnable.run();
                    }
                }.start();
                break;
            case R.id.btn_auto_run:
                final Runnable autoRunTask = AutoRunBusiness.getInstance(getApplicationContext()).getTaskRunnable();
                new Thread(){
                    public void run(){
                        autoRunTask.run();
                    }
                }.start();
                break;
            case R.id.btn_upload_log:
                Intent uploadIntent = new Intent("baofengtv.action.UPLOAD_LOG");
                sendBroadcast(uploadIntent);
                break;
            case R.id.btn_start_webview:
                //String url = "http://www.baidu.com";
                String url = "https://v.qq.com/x/cover/sx5xljydk45g1pp/m0026rju5ro.html";
                WebViewUtils.getInstance(getApplicationContext()).loadUrl(url);
                break;
            case R.id.btn_get_poster:
                getPosterAsync();
                break;
            case R.id.btn_to_install_page:
                Intent intent = new Intent();
                Bundle bundle = new Bundle();
                intent.setClassName("com.egame.tv",
                        "com.egame.tv.activitys.PreLancherActivity");
                //recordType==4表示进入已安装的游戏列表页
                bundle.putString("RECOMMEND", "{recordType:'4',gameid:'',aid:'',linkurl:'',downloadfrom:'',actioncode:'',name:''}");
                intent.putExtra("openFlag", 1);
                intent.setAction("android.intent.action.VIEW");
                intent.putExtras(bundle);
                startActivity(intent);
                break;
            case R.id.btn_get_recommend_games:
                Trace.Debug("mJsonStr=" + mJsonStr);
                if( !TextUtils.isEmpty(mJsonStr) ){
                    Intent i = new Intent();
                    Bundle b = new Bundle();
                    i.setClassName("com.egame.tv", "com.egame.tv.activitys.PreLancherActivity");
                    b.putString("RECOMMEND", mJsonStr);
                    //"{recordType:'1',gameid:'731471',aid:'9287',linkurl:'',downloadfrom:'1',actioncode:'1'}"
                    //"{recordType:'1',gameid:'730011',aid:'9285',linkurl:'',downloadfrom:'1',actioncode:'1'}"
                    i.setAction("android.intent.action.VIEW");
                    i.putExtras(b);
                    startActivity(i);
                }else{
                    getRecommendGamesAsync();
                }
                break;
            default:
                break;
        }
    }

    private void launchAppByPackageName(String packagename) {
        // 通过包名获取此APP详细信息，包括Activities、services、versioncode、name等等
        PackageInfo packageinfo = null;
        try {
            packageinfo = getPackageManager().getPackageInfo(packagename, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageinfo == null) {
            return;
        }

        // 创建一个类别为CATEGORY_LAUNCHER的该包名的Intent
        Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
        resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        resolveIntent.setPackage(packageinfo.packageName);

        // 通过getPackageManager()的queryIntentActivities方法遍历
        List<ResolveInfo> resolveinfoList = getPackageManager()
                .queryIntentActivities(resolveIntent, 0);

        ResolveInfo resolveinfo = resolveinfoList.iterator().next();
        if (resolveinfo != null) {
            // packagename = 参数packname
            String packageName = resolveinfo.activityInfo.packageName;
            // 这个就是我们要找的该APP的LAUNCHER的Activity[组织形式：packagename.mainActivityname]
            String className = resolveinfo.activityInfo.name;
            // LAUNCHER Intent

            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);

            // 设置ComponentName参数1:packagename参数2:MainActivity路径
            ComponentName cn = new ComponentName(packageName, className);

            intent.setComponent(cn);
            startActivity(intent);
        }
    }


    private void getPosterAsync() {
    }

    private void getRecommendGamesAsync() {
        new Thread(){
            public void run(){

            }
        }.start();
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

    public static void readFileByLines(String fileName) {
        File file = new File(fileName);
        BufferedReader reader = null;
        try {
            Trace.Debug("====start read file.====");
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            int line = 1;
            while ((tempString = reader.readLine()) != null) {
                Trace.Debug("line " + line + ": " + tempString);
                line++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
            Trace.Debug("====end read file.======");
        }
    }

    public static ArrayList<String> inputStreamToArrayString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        ArrayList<String> contents = new ArrayList<>();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                //Log.i(TAG, "@@@" + line);
                line = line.trim();
                if(line.startsWith("Window #")){
                    contents.add(sb.toString());
                    sb = new StringBuilder();
                }
                sb.append(line + "/n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        contents.add(sb.toString());
        //int size = contents.size();
        //for(int i=0; i<size; i++){
        //    Log.i(TAG, "###" + i + "###" + contents.get(i));
        //}
        Trace.Debug("read file:" + contents);
        return contents;
    }

}
