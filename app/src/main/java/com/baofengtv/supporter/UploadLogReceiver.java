package com.baofengtv.supporter;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.baofengtv.middleware.storage.BFTVStorageManager;
import com.baofengtv.supporter.net.OkHttpUtils;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MultipartBody;
import okhttp3.Response;

/**
 * @author LiLiang
 * @version 1.0
 * @title 类的名称
 * @description 该类主要功能描述
 * @company 暴风TV
 * @created 2016/9/2 15:17
 * @changeRecord [修改记录] <br/>
 */
public class UploadLogReceiver extends BroadcastReceiver {

    public static final String UPLOAD_LOG_ACTION = "baofengtv.action.UPLOAD_LOG";

    public static final int MSG_UPLOAD = 1;

    private static final long LIMIT_HOUR = 60*60*1000;

    private static WeakReference<Dialog> sTipsDialog;

    private Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg){
            if(msg.what == MSG_UPLOAD){
                Trace.Debug("###get msg upload log.");
                final String tmpLogPath = (String)msg.obj;
                final File tmpLogFile = new File(tmpLogPath);

                final String logPath = tmpLogPath.substring(0, tmpLogPath.lastIndexOf('.'));
                final String fileName = logPath.substring(logPath.lastIndexOf('/')+1, logPath.length());

                if(tmpLogFile.exists()){
                    mUploadThread = new Thread(){
                        public void run(){
                            Trace.Debug("7.generate log success. prepare to upload log now.");
                            if( !Utils.isConnected(mContext.getApplicationContext()) ){
                                Trace.Debug("3.network is unavailable");
                                ArrayList<String> usbList = getExternalStorage(mContext.getApplicationContext());
                                if(usbList.size() > 0){
                                    Trace.Debug("copy log to usb device.");
                                    //网络不通时无法上传，保存日志到U盘
                                    File logDir = new File(usbList.get(0) + "/BFTVLog");
                                    if( !logDir.exists() )
                                        logDir.mkdir();

                                    File logFile = new File(logDir + "/" + fileName);
                                    try {
                                        RandomAccessFile randomAccessFile = new RandomAccessFile(logFile, "rw");
                                        //写入基本信息
                                        randomAccessFile.write(getSystemInfoBytes());

                                        FileInputStream fis = new FileInputStream(tmpLogFile);
                                        byte[] buf = new byte[10240];
                                        int len = 0;
                                        while( (len = fis.read(buf)) != -1 ) {
                                            randomAccessFile.write(buf, 0, len);
                                        }
                                        randomAccessFile.writeBytes("\n");
                                        Trace.Debug("### log file length = " + randomAccessFile.length());
                                        fis.close();
                                        randomAccessFile.close();
                                        showToast("当前网络不通无法上报日志，保存日志到U盘");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } else{
                                    showToast("当前网络不通,无法上报日志");
                                }
                            }else{
                                try {
                                    /*String logPath = tmpLogPath.substring(0, tmpLogPath.lastIndexOf('.'));
                                    File logFile = new File(logPath);
                                    RandomAccessFile randomAccessFile = new RandomAccessFile(logFile, "rw");
                                    //写入基本信息
                                    randomAccessFile.write(getSystemInfoBytes());

                                    FileInputStream fis = new FileInputStream(tmpLogFile);
                                    byte[] buf = new byte[10240];
                                    int len = 0;
                                    while( (len = fis.read(buf)) != -1 ) {
                                        randomAccessFile.write(buf, 0, len);
                                    }
                                    randomAccessFile.writeBytes("\n");
                                    Trace.Debug("### log file length = " + randomAccessFile.length());
                                    try {
                                        fis.close();
                                        randomAccessFile.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }*/

                                    //zip压缩
                                    FileInputStream in = null;
                                    ZipOutputStream zos = null;
                                    String zipFilePath = tmpLogPath.replace(".txt.tmp", ".zip");
                                    File zipFile = new File(zipFilePath);
                                    try {
                                        zos = new ZipOutputStream(new FileOutputStream(zipFile));
                                        byte[] buffer = new byte[4096];
                                        int bytes_read;
                                        in = new FileInputStream(tmpLogFile);
                                        ZipEntry entry = new ZipEntry(/*tmpLogFile.getName()*/fileName);
                                        zos.putNextEntry(entry);
                                        //写入基本信息
                                        zos.write(getSystemInfoBytes());

                                        while ((bytes_read = in.read(buffer)) != -1) {
                                            zos.write(buffer, 0, bytes_read);
                                        }
                                        zos.closeEntry();

                                    }catch (IOException e){
                                        e.printStackTrace();
                                    } finally {
                                        if (in != null) {
                                            try {in.close();} catch (IOException ex) {}
                                        }
                                        if (zos != null) {
                                            try {zos.close();} catch (IOException ex) {}
                                        }
                                        if(zipFile.exists()) {
                                            showToast("生成日志完毕，开始上传...");
                                            uploadFile(zipFilePath, mUUid);
                                        }
                                    }
                                }catch (Exception e){
                                    e.printStackTrace();
                                    showToast("生成日志文件失败...");
                                }
                            }
                            setUploadingFlag(false);
                        }
                    };
                    mUploadThread.start();
                }else{
                    showToast("生成日志文件失败");
                }
            }else if(msg.what == 101){
                if (sTipsDialog != null && sTipsDialog.get() != null) {
                    Trace.Debug("###dismiss_msg.what_超时");
                    sTipsDialog.get().dismiss();
                }
            }
        }
    };

    private static boolean sIsUploading = false;

    private Context mContext;

    private Thread mLogThread;
    private Thread mUploadThread;
    private String mUUid = "";

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        Trace.Debug("1.get broadcast action= " + UPLOAD_LOG_ACTION);
        String action = intent.getAction();
        if (!action.equals(UPLOAD_LOG_ACTION))
            return;

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mHandler.removeMessages(101);
                //15s后消失弹窗
                mHandler.sendEmptyMessageDelayed(101, 15000);

                if (sTipsDialog != null && sTipsDialog.get() != null &&
                        sTipsDialog.get().isShowing()) {
                    Trace.Debug("###Dialog_isShowing");
                    return;
                }

                /*Dialog dialog = new AlertDialog.Builder(mContext.getApplicationContext())
                        .setTitle("温馨提示")
                        .setMessage("您将上报日志，请确认目前电视遇到了异常")
                        .setPositiveButton("上报", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                prepareUploadLog();
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create();
                Window window = dialog.getWindow();
                window.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                window.setGravity(Gravity.CENTER);*/


                if (sTipsDialog != null && sTipsDialog.get() != null) {
                    TipsDialog dialog = new TipsDialog(mContext, R.style.upload_dialog);
                    sTipsDialog.clear();
                    sTipsDialog = new WeakReference<Dialog>(dialog);

                    sTipsDialog.get().show();
                }else{
                    TipsDialog dialog = new TipsDialog(mContext, R.style.upload_dialog);
                    sTipsDialog = new WeakReference<Dialog>(dialog);
                    sTipsDialog.get().show();
                }
            }
        });

    }

    private void prepareUploadLog(){
        if(isUploading()){
            showToast("后台正在上传日志，请稍后再试");
            Trace.Debug("2.an exist uploading task. please try it later.");
            return;
        }

        mUUid = Utils.getSerialNumber(mContext);
        if(TextUtils.isEmpty(mUUid)){
            showToast("非法uuid，无法上传日志");
            Trace.Debug("4.uuid is invalid. no uploading");
            return;
        }

		//upload dropbox logs @yangfeng 2017/5/5
		
		Intent intent = new Intent("com.baofengtv.dropboxcollector.UPLOADIMMEDIATE");
        mContext.sendBroadcast(intent);
        //delete old log files.
        final File dir = mContext.getFilesDir();
        final File[] files = dir.listFiles();
        if(files != null){
            long current = System.currentTimeMillis();
            String fileName;
            int count = 0;
            for(File file : files){
                fileName = file.getName();
                if(fileName.startsWith(mUUid)){
                    try {
                        if(fileName.endsWith(".tmp")){
                            file.delete();
                        }else if(fileName.endsWith(".zip")){
                            String timestampStr = fileName.substring(fileName.lastIndexOf('_')+1, fileName.indexOf('.'));
                            long timestamp = Long.parseLong(timestampStr);
                            if(current - timestamp > LIMIT_HOUR) {//超过1小时的log file删除
                                file.delete();
                            }else{
                                count += 1;
                            }
                        }

                    }catch (Exception e){
                        file.delete();
                    }
                }
            }
            if(count >= 8 ){
                showToast("操作过去频繁，请稍后尝试");
                return;
            }

        }

        //showToast("准备上传日志");
        Trace.Debug("5. prepare to generate log now.");
        setUploadingFlag(true);
        mLogThread = new Thread(){
            public void run(){
                try{
                    String fileName = mUUid + "_" + System.currentTimeMillis() + ".txt.tmp";
                    String logPath = dir.getAbsolutePath() + "/" + fileName;
                    File tmpFile = new File(logPath);

                    showToast("正在生成日志...");
                    Trace.Debug("6.generating log...");
                    List<String> commands = new ArrayList<String>();
                    tmpFile.createNewFile();
                    tmpFile.setReadable(true, false);
                    tmpFile.setWritable(true, false);
                    tmpFile.setExecutable(true, false);

                    Message msg = Message.obtain();
                    msg.what = MSG_UPLOAD;
                    msg.obj = logPath;
                    mHandler.sendMessageDelayed(msg, 3000);

                    commands.add("logcat -v time -f " + logPath);
                    //阻塞
                    ShellUtils.execCommand(commands, false, false);
                }catch (Exception e){
                    e.printStackTrace();
                    showToast("执行失败，请稍后重试");
                    Trace.Debug("9.generate log failed. no upload.");
                }
            }
        };
        mLogThread.start();

    }

    private void setUploadingFlag(boolean flag){
        sIsUploading = flag;
    }

    private boolean isUploading(){
        return sIsUploading;
    }

    private void showToast(final String text){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext.getApplicationContext(), text, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void uploadFile(String path, String uuid){
        File logFile = new File(path);
        String requestUrl = Constant.UPLOAD_LOG_URL;
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("fileName", logFile.getName());
        params.put("uuid", uuid);

        Callback callback = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                showToast("上传日志失败");
                Trace.Debug("onFailure() call=" + call);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Trace.Debug("upload success. response ----->" + response);
                    showToast("上传日志完毕");
                } else {
                    Trace.Debug("upload failed. response ----->" + response);
                    showToast("上传日志失败~");
                }
            }
        };
        OkHttpUtils.doUploadAsync(requestUrl, logFile, params, callback);
    }

    public static ArrayList<String> getExternalStorage(Context context) {
        Trace.Debug("####getExternalStorage()");
        BFTVStorageManager stm = BFTVStorageManager.getInstance(context);
        String[] volumes = stm.getVolumePaths();

        ArrayList<String> usbList = new ArrayList<String>();
        if (volumes == null) {
            return usbList;
        }
        //Trace.Debug("volumes.length=" + volumes.length);
        for (int i = 0; i < volumes.length; ++i) {
            String state = stm.getVolumeState(volumes[i]);

            if (state == null || !state.equals(Environment.MEDIA_MOUNTED)) {
                continue;
            }
            if (volumes[i].startsWith("/storage/emulated/")) {
                Trace.Debug("skipped inner storage: " + volumes[i]);
                continue;
            } else {
                String label = stm.getVolumeLabel(volumes[i]);
                Trace.Debug("###label=" + label);
                Trace.Debug("###path=" + volumes[i]);
                usbList.add(volumes[i]);
            }
        }
        Trace.Debug("sUsbList.Size()=" + usbList.size());
        return usbList;
    }

    private byte[] getSystemInfoBytes(){
        StringBuilder sb = new StringBuilder();
        sb.append("platform:" + Utils.getCurPlatform(mContext));
        sb.append("\n");
        sb.append("softId:" + Utils.getSoftId(mContext));
        sb.append("\n");
        sb.append("systemVersion:" + Utils.getSystemVersion(mContext));
        sb.append("\n\n");
        //所有安装app的信息
        PackageManager pm = mContext.getPackageManager();
        List<PackageInfo> packageInfoList = pm.getInstalledPackages(PackageManager.GET_ACTIVITIES);
        if(packageInfoList != null){
            for(PackageInfo pkgInfo : packageInfoList){
                String pkgInfoStr = pkgInfo.packageName+ "#" + pkgInfo.versionName + "#" + pkgInfo.versionCode;
                //Trace.Debug(pkgInfoStr);
                sb.append(pkgInfoStr);
                sb.append("\n");
            }
        }
        sb.append("\n");
        return sb.toString().getBytes();
    }

    public class TipsDialog extends Dialog{

        public TipsDialog(Context context) {
            super(context);
        }

        public TipsDialog(Context context, int theme){
            super(context, theme);
        }

        @Override
        public void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
            setContentView(R.layout.dialog_upload_log);
            Window window = getWindow();
            window.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            window.setGravity(Gravity.CENTER);
            initDialogView();
        }

        private void initDialogView(){
            TextView uuidTV = (TextView)findViewById(R.id.dialog_upload_log_uuid);
            uuidTV.setText("设备uuid：" + Utils.getSerialNumberWithPostfix(mContext));
            findViewById(R.id.dialog_upload_btn).setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    prepareUploadLog();
                    dismiss();
                }
            });
            findViewById(R.id.dialog_upload_cancel_btn).setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }
    }
}
