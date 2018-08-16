package com.baofengtv.supporter.net;

import com.baofengtv.supporter.Trace;
import com.baofengtv.supporter.loader.DownloadParam;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @author LiLiang
 * @version 1.0
 * @title 类的名称
 * @description wrapper OkHttp. replace of HttpClient.
 * @company 暴风TV
 * @created 2017/6/6 10:54
 * @changeRecord [修改记录] <br/>
 */

public class OkHttpUtils {
    public static final String CODE = "code";
    public static final String RETURN = "return";
    public static final int CODE_OK = 200;
    //断点续传的成功结果码
    public static final int CODE_RESUME_OK = 206;

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static OkHttpClient sHttpClient;

    static {
        sHttpClient = new OkHttpClient().newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)//设置超时时间
                .readTimeout(10, TimeUnit.SECONDS)//设置读取超时时间
                .writeTimeout(10, TimeUnit.SECONDS)//设置写入超时时间
                .build();
    }

    /**
     * http get
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static Map<String, String> doGet(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        Response response = sHttpClient.newCall(request).execute();
        Trace.Debug("[http get response] " + response.toString());

        int statusCode = response.code();
        String content = response.body().string();

        Map<String, String> resultMap = new HashMap<String, String>();
        resultMap.put(CODE, String.valueOf(statusCode));
        resultMap.put(RETURN, content);

        return resultMap;
    }

    public static Map<String, String> doPost(String baseUrl, Map<String, String> paramsMap) throws IOException {
        FormBody.Builder builder = new FormBody.Builder();
        for (String key : paramsMap.keySet()) {
            builder.add(key, paramsMap.get(key));
        }
        RequestBody formBody = builder.build();

        Request request = new Request.Builder().url(baseUrl).post(formBody).build();
        Response response = sHttpClient.newCall(request).execute();

        Trace.Debug("[http post response] " + response.toString());

        int statusCode = response.code();
        String content = response.body().string();

        Map<String, String> resultMap = new HashMap<String, String>();
        resultMap.put(CODE, String.valueOf(statusCode));
        resultMap.put(RETURN, content);

        return resultMap;
    }

    /**
     * 下载
     *
     * @param downParam
     * @return
     * @throws IOException
     * @throws IllegalStateException
     */
    public static int doDownload(DownloadParam downParam) throws IOException, IllegalStateException {
        RandomAccessFile randomAccessFile = null;
        String urlStr = downParam.src;

        File tmpFile = new File(downParam.dst + ".tmp");
        if(tmpFile.exists() && !downParam.resumeFlag){
            tmpFile.delete();
        }
        //保证下载的file权限足够
        tmpFile.createNewFile();
        tmpFile.setReadable(true, false);
        tmpFile.setWritable(true, false);
        tmpFile.setExecutable(true, false);

        randomAccessFile = new RandomAccessFile(tmpFile, "rw");
        long endPos = randomAccessFile.length();

        Request.Builder builder = new Request.Builder();

        //断点续传
        if(downParam.resumeFlag){
            randomAccessFile.seek(endPos);
            String sProperty = "bytes=" + endPos + "-";
            builder.addHeader("Range", sProperty);
        }
        Request request = builder.url(urlStr).build();
        Response response = sHttpClient.newCall(request).execute();
        //Trace.Debug("[http download response] " + response.toString());

        int statusCode = response.code();
        if(response.isSuccessful()){
            InputStream is = response.body().byteStream();
            byte[] buf = new byte[10240];
            int len = 0;
            long bytes = endPos;
            while( (len = is.read(buf)) != -1 ) {
                randomAccessFile.write(buf, 0, len);
                bytes += len;
            }
            is.close();
            if(bytes == 0){
                tmpFile.delete();
                tmpFile = null;
            }
        }else{
            try {
                tmpFile.delete();
                tmpFile = null;
            }catch (Exception e){}
        }

        randomAccessFile.close();
        if(tmpFile != null) {
            tmpFile.renameTo(new File(downParam.dst));
        }
        return statusCode;
    }

    /**
     * 异步上传
     * @param url
     * @param file
     * @param paramsMap
     * @param responseCallback
     */
    public static void doUploadAsync(String url, File file, Map<String, String> paramsMap,
                              okhttp3.Callback responseCallback) {
        MultipartBody.Builder builder = new MultipartBody.Builder();
        //设置类型
        builder.setType(MultipartBody.FORM);
        for (String key : paramsMap.keySet()) {
            builder.addFormDataPart(key, paramsMap.get(key));
        }
        //File
        builder.addFormDataPart("formFile", file.getName(), RequestBody.create(null, file));
        //创建RequestBody
        RequestBody body = builder.build();
        //创建Request
        final Request request = new Request.Builder().url(url).post(body).build();
        final Call call = sHttpClient.newCall(request);
        call.enqueue(responseCallback);

    }
}
