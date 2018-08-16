package com.baofengtv.supporter.autorun;

import android.content.Context;

import com.baofengtv.middleware.tv.BFTVFactoryManager;
import com.baofengtv.supporter.AbstractBusiness;
import com.baofengtv.supporter.SlaveService;
import com.baofengtv.supporter.Trace;
import com.baofengtv.supporter.UMengUtils;
import com.baofengtv.supporter.Utils;
import com.baofengtv.supporter.net.OkHttpUtils;
import com.umeng.analytics.MobclickAgent;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author LiLiang
 * @version 1.0
 * @title 类的名称
 * @description 该类主要功能描述
 * @company 暴风TV
 * @created 2016/7/29 15:45
 * @changeRecord [修改记录] <br/>
 */
public class AutoRunBusiness extends AbstractBusiness {

    public static final String PROFILE = "/data/misc/baofengtv/autorun.txt";

    private Context mContext;
    private static AutoRunBusiness sInstance;

    public static AutoRunBusiness getInstance(Context context){
        if(sInstance == null){
            sInstance = new AutoRunBusiness(context);
        }
        return sInstance;
    }

    private AutoRunBusiness(Context context){
        mContext = context;

        mTaskRunnable = new Runnable() {
            @Override
            public void run() {
                String baseUrl = "http://bfm.fengmi.tv/bfm/bfm/tactics/gettacticsinfo.ajax?";
                String platform = Utils.getCurPlatform(mContext);
                String uuid = BFTVFactoryManager.getInstance(mContext).getSerialNumber();
                String softwareId = Utils.getSoftId(mContext);
                String version = Utils.getSystemVersion(mContext);
                String buildId = Utils.getBuildId(mContext);
                String mac = Utils.getEth0MacAddress(mContext);
                String url = baseUrl + "platformId=" + platform + "&"
                        +"uuid=" + uuid + "&" + "softwareId=" + softwareId + "&"
                        +"version=" + version + "&" + "buildId=" + buildId +"&"
                        +"mac=" + mac;

                int retryCount = 3;
                boolean ret = false;
                while (retryCount > 0){
                    try {
                        Map<String, String> resultMap = OkHttpUtils.doGet(url);
                        int statusCode = Integer.parseInt(resultMap.get(OkHttpUtils.CODE));
                        Trace.Debug("###statusCode=" + statusCode);
                        String content = resultMap.get(OkHttpUtils.RETURN);
                        Trace.Debug("###content=" + content);
                        if (statusCode == 200) {
                            try {
                                JSONObject json = new JSONObject(content);
                                boolean success = json.getBoolean("success");
                                if (success){
                                    //获取数据成功,写入本地文件
                                    String datajson = json.getJSONObject("data").toString();
                                    Trace.Debug("data:" + datajson);
                                    //把datajson写入文件
                                    File file = new File(PROFILE);
                                    if( !file.exists() ){
                                        file.createNewFile();
                                        file.setReadable(true, false);
                                        file.setWritable(true, false);
                                        file.setExecutable(true, false);
                                    }
                                    BufferedWriter writer = null;
                                    try{
                                        writer = new BufferedWriter(new FileWriter(PROFILE));
                                        writer.write(datajson);
                                    }catch (IOException e){
                                        e.printStackTrace();
                                    }finally{
                                        try{
                                            if (writer != null)
                                                writer.close( );
                                        }catch (IOException e){
                                            e.printStackTrace();
                                        }
                                    }

                                }else{
                                    //获取数据失败
                                    String resultMsg = json.getString("message");
                                    Trace.Error("resultMsg:" + resultMsg);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            ret = true;
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        MobclickAgent.reportError(mContext, e);
                    }
                    retryCount--;
                }
                Map<String, String> umengMap = new HashMap<String, String>();
                umengMap.put("value", "auto_run interface:" + String.valueOf(ret));
                umengMap.put("interface", "auto_run");
                SlaveService.onEvent(UMengUtils.EVENT_REQUEST_RESULT, umengMap);
            }
        };
    }

    @Override
    public Runnable getTaskRunnable() {
        return mTaskRunnable;
    }
}
