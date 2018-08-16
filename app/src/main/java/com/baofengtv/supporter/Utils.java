package com.baofengtv.supporter;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.text.TextUtils;

import com.baofengtv.middleware.tv.BFTVCommonManager;
import com.baofengtv.middleware.tv.BFTVFactoryManager;

import org.w3c.dom.Text;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.zip.CRC32;

/**
 * @author LiLiang
 * @version v1.0
 * @brief 一些工具辅助类
 * @date 2015/8/3
 */
public class Utils {

    private static MessageDigest md5 = null;
    static {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * 获取字符串对应的md5值
     * @param str
     * @return
     */
    public static String getMD5(String str){
        try{
            byte[] bs = md5.digest(str.getBytes());
            StringBuilder sb = new StringBuilder(40);
            for(byte x:bs) {
                if((x & 0xff)>>4 == 0) {
                    sb.append("0").append(Integer.toHexString(x & 0xff));
                } else {
                    sb.append(Integer.toHexString(x & 0xff));
                }
            }
            return sb.toString();
        }catch (Exception e){
            return str;
        }
    }

    /**
     * 判断网络是否连通
     * @param context
     * @return
     */
    public static boolean isConnected(Context context){
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        return ( cm != null && cm.getActiveNetworkInfo() != null
                && cm.getActiveNetworkInfo().isConnected());
    }

    /**
     * 获取TopActivity
     * @return
     */
    public static String getTopActivityClassName(Context context){
        //Trace.Debug("###getTopActivityClassName");
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        String topClassName = "";
        if(tasks != null && tasks.size() > 0){
            topClassName = tasks.get(0).topActivity.getClassName();
        }
        Trace.Debug("###TopActivity=" + topClassName);
        return topClassName;
    }

    public static String getTopPackageName(Context context){
        //Trace.Debug("###getTopActivityClassName");
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        String topPackageName = "";
        if(tasks != null && tasks.size() > 0){
            topPackageName = tasks.get(0).topActivity.getPackageName();
        }
        Trace.Debug("###topPackageName=" + topPackageName);
        return topPackageName;
    }

    public static boolean isPackageInstalled(Context context, String pkgName){
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(pkgName, 0);
        }catch (PackageManager.NameNotFoundException e) {
            packageInfo = null;
            //e.printStackTrace();
        }
        if(packageInfo == null){
            return false;
        }else{
            return true;
        }
    }

    /**
     * 获取后缀
     * @param url
     * @return
     */
    public static String getPostfix(String url){
        int index = url.lastIndexOf('.');
        if(index == -1){
            return "";
        }
        return url.substring(index, url.length());
    }

    /**
     * 获取文件md5值
     * @param filePath
     * @return
     * @throws IOException
     */
    public static String fileMD5(String filePath){
        int bufferSize = 256 * 1024;
        FileInputStream fileInputStream = null;
        DigestInputStream digestInputStream = null;

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            fileInputStream = new FileInputStream(filePath);
            digestInputStream = new DigestInputStream(fileInputStream,
                    messageDigest);
            byte[] buffer = new byte[bufferSize];
            while (digestInputStream.read(buffer) > 0)
                ;
            // 获取最终的MessageDigest
            messageDigest = digestInputStream.getMessageDigest();
            // 拿到结果，也是字节数组，包含16个元素
            byte[] resultByteArray = messageDigest.digest();
            // 同样，把字节数组转换成字符串
            return byteArrayToHex(resultByteArray);
        } catch (NoSuchAlgorithmException e) {
            //e.printStackTrace();
            return "";
        } catch (IOException e){
            //e.printStackTrace();
            return "";
        } finally {
            try {
                digestInputStream.close();
            } catch (Exception e) {}

            try {
                fileInputStream.close();
            } catch (Exception e) {}
        }
    }

    private static String byteArrayToHex(byte[] byteArray) {
        char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F' };
        char[] resultCharArray = new char[byteArray.length * 2];
        int index = 0;
        for (byte b : byteArray) {
            resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];
            resultCharArray[index++] = hexDigits[b & 0xf];
        }
        // 字符数组组合成字符串返回
        return new String(resultCharArray);
    }

    /**
     * 获取字符串的CRC32值
     * @param str
     * @return
     */
    public static String getCrc32Value(String str){
        byte[] buf = str.getBytes();
        CRC32 crc = new CRC32();
        crc.update(buf);
        return String.format("%1$016x", crc.getValue());
    }

    //private static String sLitchiPkgName = null;
    /**
     * 获取内置的litchi包名，兼容新旧版本的不同包名

    public static String getLitchiPkgName(Context context){
        if(sLitchiPkgName != null)
            return sLitchiPkgName;

        String packageName = "com.js.litchi.stormtv";
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            sLitchiPkgName = packageName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            sLitchiPkgName = "com.js.litchi";
        }
        return sLitchiPkgName;
    }*/

    /**
     * 获取电视串号
     * @param context
     * @return
     */
    private static String serialNumber = null;
    public static String getSerialNumber(Context context){
        if(!TextUtils.isEmpty(serialNumber)){
            return serialNumber;
        }
        String sn = BFTVFactoryManager.getInstance(context).getSerialNumber();
        if(TextUtils.isEmpty(sn)){
            return "";
        }
        int end = sn.lastIndexOf("_");
        if(end != -1){
            sn = sn.substring(0, end);
        }
        serialNumber = sn;
        Trace.Debug("###serial number = " + sn);
        return serialNumber;
    }

    /**
     * return serial_number with 4 postfix
     * @param context
     * @return
     */
    public static String getSerialNumberWithPostfix(Context context){
        String sn = BFTVFactoryManager.getInstance(context).getSerialNumber();
        if(TextUtils.isEmpty(sn)){
            sn = "";
        }
        return sn;
    }

    public static String curPlatform = null;
    public static String getCurPlatform(Context context){
        if(!TextUtils.isEmpty(curPlatform))
            return curPlatform;
        curPlatform = BFTVCommonManager.getInstance(context).getPlatform();
        Trace.Debug("###platform = " + curPlatform);
        return TextUtils.isEmpty(curPlatform) ? "":curPlatform;
    }

    private static String softId = null;
    public static String getSoftId(Context context){
        if(!TextUtils.isEmpty(softId))
            return softId;
        softId = BFTVCommonManager.getInstance(context).getSoftwareID();
        Trace.Debug("###software id = " + softId);
        return TextUtils.isEmpty(softId) ? "":softId;
    }

    private static String sysVer = null;
    public static String getSystemVersion(Context context){
        if(!TextUtils.isEmpty(sysVer))
            return sysVer;
        sysVer = BFTVCommonManager.getInstance(context).getVersion();
        Trace.Debug("###software version = " + sysVer);
        return TextUtils.isEmpty(sysVer) ? "unknown":sysVer;
    }

    private static String mac = null;
    public static String getEth0MacAddress(Context context){
        if(!TextUtils.isEmpty(mac))
            return mac;
        mac = BFTVFactoryManager.getInstance(context).getMac();
        //B0A37E332D1B
        if(mac != null && mac.length() == 12 && !mac.contains(":")){
            String temp = "";
            for(int i = 0; i <= 10; i = i + 2){
                if(i == 10)
                    temp = temp + mac.substring(i, i + 2);
                else
                    temp = temp + mac.substring(i, i + 2) + ":";
            }
            mac = temp;
        }
        Trace.Debug("###eth0 mac address = " + mac);
        return TextUtils.isEmpty(mac) ? "":mac;
    }

    private static String buildId = null;
    public static String getBuildId(Context context){
        if(!TextUtils.isEmpty(buildId))
            return buildId;
        buildId = BFTVCommonManager.getInstance(context).getMainSWBuildNumber();
        Trace.Debug("###buildId = " + buildId);
        return buildId;
    }

    public static boolean isSupportVR(){
        String ret = "false";
        try{
            final String key = "baofengtv.en.VR";
            Class<?> className = Class.forName("android.os.SystemProperties");
            Method getMethod = className.getDeclaredMethod("get", String.class, String.class);
            ret = (String) getMethod.invoke(className, key, "false");
            Trace.Debug("isSupportVR : " + ret);
        }catch(Exception e){
            e.printStackTrace();
            Trace.Debug("error return false.");
        }
        if(ret != null && ret.equals("true")){
            return true;
        }else{
            return false;
        }
    }
//
//    public static boolean isFUI30(Context context){
//        int fuiVersion = 1;
//        try {
//            fuiVersion = BFTVCommonManager.getInstance(context).getUiVersion();
//        }catch (Throwable e){
//            fuiVersion = 1;
//        }
//        Trace.Debug("###FUI version = " + fuiVersion);
//        return (fuiVersion == 3);
//    }

    public static int getFUIVersion(Context context) {
        int fuiVersion = 1;
        try {
            fuiVersion = BFTVCommonManager.getInstance(context).getUiVersion();
        }catch (Throwable e){
            fuiVersion = 1;
        }
        Trace.Debug("###FUI version = " + fuiVersion);
        return fuiVersion;
    }

    /**
     * 判断是否是会员登录
     * @param context
     * @return true or false
     */
    private static boolean isVip2(Context context){
        if(getFUIVersion(context) >= 3){
            Uri userCenterUri = Uri.parse("content://com.bftv.fui.usercenter.content.provider.UserCenterContentProvider/query");
            try {
                Cursor cursor = context.getContentResolver().query(userCenterUri, null, null, null, null);
                cursor.moveToFirst();
                String isVip = cursor.getString(cursor.getColumnIndex("is_vip"));
                String expiredTimeStr = cursor.getString(cursor.getColumnIndex("vip_out_time"));
                Trace.Debug("is_vip:" + isVip);
                Trace.Debug("vip_out_time:" + expiredTimeStr);
                long curTime = System.currentTimeMillis()/1000;
                Trace.Debug("current time:" + curTime);
                if(TextUtils.isEmpty(isVip) || TextUtils.isEmpty(expiredTimeStr)){
                    return false;
                }
                //"is_vip"为1并且vip_out_time大于当前时间
                long expiredTime = Long.parseLong(expiredTimeStr);
                if(isVip.equals("1") && curTime < expiredTime){
                    return true;
                }
            }catch (Exception e){
                e.printStackTrace();
                return false;
            }
        }else{
            try {
                Context remoteContext = context.createPackageContext("com.baofengtv.launcher3d", Context.CONTEXT_IGNORE_SECURITY);  //创建Launcher的context
                SharedPreferences sp = remoteContext.getSharedPreferences("user_account_state", Context.MODE_WORLD_READABLE);//获取SharedPreferences
                boolean isLogin = sp.getBoolean("isLogin", false);//登录状态
                boolean isVip = sp.getBoolean("isVip", false);//VIP状态
                return isLogin && isVip;
            }catch (Exception e){
                e.printStackTrace();
                return false;
            }

        }
        return false;
    }

    /**
     * 判断是否已登录会员
     * @param context
     * @return "1"表示已登录，"0"表示未登录
     */
    public static String isVip(Context context){
        boolean isVip = isVip2(context);
        Trace.Debug("###isVip? " + isVip);
        return (isVip == true)? "1" : "0";
    }

    /**
     * 是否是京东渠道定制机
     * @param context
     * @return
     */
    public static boolean isJDChannel(Context context){
        boolean isJDChannel = false;
        try {
            BFTVCommonManager.EN_BFTV_SALES_TYPE from =
                    BFTVCommonManager.getInstance(context).getSalesType();
            isJDChannel = (from == BFTVCommonManager.EN_BFTV_SALES_TYPE.JD);
        }catch (Throwable e){
            //e.printStackTrace();
        }
        return isJDChannel;
    }

}
