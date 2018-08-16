package com.baofengtv.supporter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * @author LiLiang
 * @version v1.0
 * @brief 将从CMS服务端配置的启动json解析成intent并启动
 * @date 2015/9/3
 */
public class IntentUtils {

    public static final String LAUNCH_ACTIVITY = "startActivity";
    public static final String LAUNCH_SERVICE = "startService";
    public static final String LAUNCH_BROADCAST = "sendBroadcast";

    public static final String EXPLICIT = "explicit";
    public static final String IMPLICIT = "implicit";


    /**
     * 解析json字符串里的content字段内容并执行跳转
     *
     */
    public static void parseAndLaunchIntent(Context context, String jsonStr) throws Exception {
        JSONObject baseJson = new JSONObject(jsonStr);

        if(baseJson.has("minClientAppVersionCode")){
            //如果有minClientAppVersionCode，则判断屏保的versionCode。如果低于minClientAppVersionCode,则抛异常
            int minClientAppVersionCode = baseJson.getInt("minClientAppVersionCode");
            Trace.Debug("limited minClientAppVersionCode = " + minClientAppVersionCode);
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            int packageVersionCode = info.versionCode;
            Trace.Debug("packageVersionCode = " + packageVersionCode);
            if(minClientAppVersionCode > packageVersionCode){
                throw new IllegalStateException(context.getPackageName() + "version code is " + packageVersionCode
                        + "! the limited versionCode is" + minClientAppVersionCode);
            }
        }

        JSONObject json = baseJson.getJSONObject("content");
        if(json.has("packageName")){
            String packageName = json.getString("packageName");
            if( !Utils.isPackageInstalled(context, packageName) ){
                Trace.Debug(packageName + " is uninstalled!");
                if(baseJson.has("uninstalledContent")){
                    /**
                     * 当content字段里包含的packageName未安装时，解析json字符串里的uninstallContent字段内容并执行跳转
                     * 场景：比如电视淘宝卡片，当电视淘宝App未安装时，点击执行解析uninstallContent内容，进入应用商店下载页；
                     *      当电视淘宝App已安装时，点击执行解析content内容，进入淘宝App页面
                     */
                    JSONObject json2 = baseJson.getJSONObject("uninstalledContent");
                    parseAndLaunchIntent(context, json2);
                    return;
                }
            }
        }
        parseAndLaunchIntent(context, json);
    }

    private static void parseAndLaunchIntent(Context context, JSONObject json) throws Exception{
        String launchType = json.getString("launchType");
        Log.i("TAG", "launchType=" + launchType);
        String intentType = "";
        if(json.has("intentType")){
            intentType = json.getString("intentType");
        }

        String action = "";
        if(json.has("action")){
            action = json.getString("action");
        }

        String packageName = "";
        if(json.has("packageName")){
            packageName = json.getString("packageName");
        }

        String className="";
        if(json.has("className")){
            className = json.getString("className");
        }

        String extraStr = "";
        if(json.has("extra")){
            extraStr = json.getString("extra");
        }

        String bundleStr = "";
        if(json.has("bundle")){
            bundleStr = json.getString("bundle");
        }

        //增加versionCode可选字段。如果minVersionCode>packageName的versionCode抛弃
        int minVersionCode = -1;
        if(json.has("minVersionCode")){
            minVersionCode = json.getInt("minVersionCode");
        }

        //增加setData(Uri)解析
        String uriStr = "";
        if(json.has("uri")){
            uriStr = json.getString("uri");
        }

        Intent intent = new Intent();

        if(launchType.equals(LAUNCH_ACTIVITY)){
            if(intentType.equals(EXPLICIT)){
                Log.i("TAG", "###start explicit activity");
                if( !TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(className)){

                    if(minVersionCode != -1){
                        PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
                        int packageVersionCode = info.versionCode;
                        if(minVersionCode > packageVersionCode){
                            throw new IllegalStateException("version code dismatch!");
                        }
                    }

                    ComponentName cn = new ComponentName(packageName, className);
                    intent.setComponent(cn);

                    if(!TextUtils.isEmpty(action)){
                        intent.setAction(action);
                    }

                    if(!TextUtils.isEmpty(extraStr)){
                        dealWithExtra(intent, extraStr);
                    }

                    if(!TextUtils.isEmpty(bundleStr)){
                        Bundle bundle = new Bundle();
                        dealWithBundle(bundle, bundleStr);
                        intent.putExtras(bundle);
                    }

                    if(!TextUtils.isEmpty(uriStr)){
                        Uri uri = Uri.parse(uriStr);
                        intent.setData(uri);
                    }

                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(intent);                    
                }else{
                    throw new IllegalStateException("packageName or className error.");
                }
            }else if(intentType.equals(IMPLICIT)){
                Log.i("TAG", "###start implicit activity");
                if(!TextUtils.isEmpty(action)){
                    intent.setAction(action);

                    if(!TextUtils.isEmpty(extraStr)){
                        dealWithExtra(intent, extraStr);
                    }

                    if(!TextUtils.isEmpty(bundleStr)){
                        Bundle bundle = new Bundle();
                        dealWithBundle(bundle, bundleStr);
                        intent.putExtras(bundle);
                    }

                    if(!TextUtils.isEmpty(uriStr)){
                        Uri uri = Uri.parse(uriStr);
                        intent.setData(uri);
                    }

                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(intent);
                }else{
                    throw new IllegalStateException("action is empty error.");
                }
            }else{
                throw new IllegalStateException("unknown intent type");
            }
        }else if(launchType.equals(LAUNCH_SERVICE)){
            Log.i("TAG", "###start service(" + action + ")");
            if(!TextUtils.isEmpty(action)){
                intent.setAction(action);

                if(!TextUtils.isEmpty(extraStr)){
                    dealWithExtra(intent, extraStr);
                }

                if(!TextUtils.isEmpty(bundleStr)){
                    Bundle bundle = new Bundle();
                    dealWithBundle(bundle, bundleStr);
                    intent.putExtras(bundle);
                }

                if(!TextUtils.isEmpty(uriStr)){
                    Uri uri = Uri.parse(uriStr);
                    intent.setData(uri);
                }

                context.startService(intent);
            }else{
                throw new IllegalStateException("action is empty error.");
            }
        }else if(launchType.equals(LAUNCH_BROADCAST)){
            Log.i("TAG", "###sendBroadcast(" + action + ")");
            if(!TextUtils.isEmpty(action)){
                intent.setAction(action);

                if(!TextUtils.isEmpty(extraStr)){
                    dealWithExtra(intent, extraStr);
                }

                if(!TextUtils.isEmpty(bundleStr)){
                    Bundle bundle = new Bundle();
                    dealWithBundle(bundle, bundleStr);
                    intent.putExtras(bundle);
                }

                if(!TextUtils.isEmpty(uriStr)){
                    Uri uri = Uri.parse(uriStr);
                    intent.setData(uri);
                }

                context.sendBroadcast(intent);
            }else{
                throw new IllegalStateException("action is empty error.");
            }
        }else{
            //未知类型
            throw new IllegalStateException("unknown launch type");
        }
    }

    private static void dealWithExtra(Intent intent, String extraStr) throws JSONException {
        JSONObject extraJson = new JSONObject(extraStr);
        String key;
        Object value;
        Iterator<String> it = extraJson.keys();
        while(it.hasNext()){
            key = it.next();
            value = extraJson.get(key);
            Log.i("TAG", "###extra:key=" + key + "; value=" + String.valueOf(value));

            if(value instanceof Integer){
                Log.i("TAG", "###" + key + "instanceof Integer");
                int var = (Integer)value;
                intent.putExtra(key, var);
            }else if(value instanceof String){
                intent.putExtra(key, String.valueOf(value));
            }else if(value instanceof Boolean){
                Log.i("TAG", "###" + key + "instanceof boolean");
                boolean var = (Boolean)value;
                intent.putExtra(key, var);
            }else if(value instanceof Float){
                Log.i("TAG", "###" + key + "instanceof float");
                float var = (Float)value;
                intent.putExtra(key, var);
            }else if(value instanceof Long){
                Log.i("TAG", "###" + key + "instanceof Long");
                long var = (Long)value;
                intent.putExtra(key, var);
            }
        }
    }

    private static void dealWithBundle(Bundle bundle, String bundleStr) throws JSONException{
        JSONObject bundleJson = new JSONObject(bundleStr);
        Iterator<String> it = bundleJson.keys();
        String key;
        Object value;
        while(it.hasNext()){
            key = it.next();
            value = bundleJson.get(key);
            Log.i("TAG", "###bundle:key=" + key + "; value=" + String.valueOf(value));

            if(value instanceof Integer){
                Log.i("TAG", "###"+key+"instanceof Integer");
                int var = (Integer)value;
                bundle.putInt(key, var);
            }else if(value instanceof String){
                bundle.putString(key, String.valueOf(value));
            }else if(value instanceof Boolean){
                Log.i("TAG", "###"+key+"instanceof boolean");
                boolean var = (Boolean)value;
                bundle.putBoolean(key, var);
            }else if(value instanceof Float){
                Log.i("TAG", "###"+key+"instanceof float");
                float var = (Float)value;
                bundle.putFloat(key, var);
            }else if(value instanceof Long){
                Log.i("TAG", "###"+key+"instanceof Long");
                long var = (Long)value;
                bundle.putLong(key, var);
            }
        }
    }   

    //获取下一跳地址中的包名
    public static String getPkgNameByJsonIntent(String jsonStr){
        String packageName = "";
        try {
            JSONObject baseJson = new JSONObject(jsonStr);
            JSONObject json = baseJson.getJSONObject("content");

            if(json.has("packageName")){
                packageName = json.getString("packageName");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        Trace.Debug("getPkgNameByJsonIntent() return : " + packageName);
        return packageName;
    }

    //json指令串是否包含uninstalledContent字段
    public static boolean hasUninstalledJsonIntent(String jsonStr){
        try {
            JSONObject baseJson = new JSONObject(jsonStr);
            return baseJson.has("uninstalledContent");
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
}
