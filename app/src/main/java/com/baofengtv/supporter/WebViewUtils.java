package com.baofengtv.supporter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import java.lang.ref.WeakReference;

/**
 * @author LiLiang
 * @version v1.0
 * @brief 加载从CMS服务端配置的http url
 * @date 2015/9/8
 */
public class WebViewUtils {

    private static WebViewUtils sInstance;
    public static WebViewUtils getInstance(Context context){
        if(sInstance == null){
            sInstance = new WebViewUtils(context);
        }
        return sInstance;
    }

    private Context mContext;
    private WebView mWebView;
    //private ProgressBar mProgressBar;

    private Handler mHandler;

    private WebViewUtils(Context context){
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());
    }

    public void loadUrl(final String url){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    showDialog(url);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    private void showDialog(String url){

        if(TextUtils.isEmpty(url)){
            return;
        }

        StringBuilder url2 = new StringBuilder(url);
        if(url.contains("?")){
            url2.append("&");
        }else{
            url2.append("?");
        }
        String param = "uuid=" + Utils.getSerialNumber(mContext)+
                "&platform=" + Utils.getCurPlatform(mContext) +
                "&sysver=" + Utils.getSystemVersion(mContext) +
                "&softid=" + Utils.getSoftId(mContext);
        url2.append(param);
        Trace.Warn("[url]" + url2.toString());

        Dialog dialog = new AlertDialog.Builder(mContext.getApplicationContext(), R.style.Dialog_Fullscreen).create();
        Window window = dialog.getWindow();
        window.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
        window.setContentView(R.layout.dialog_webview);
        mWebView = (WebView)window.findViewById(R.id.webview);
        //mProgressBar = (ProgressBar)window.findViewById(R.id.progressbar);

        WebSettings settings = mWebView.getSettings();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        mWebView.loadUrl(url2.toString());
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Trace.Debug("###shouldOverrideUrlLoading");
                //返回值是true的时候控制去WebView打开，为false调用系统浏览器或第三方浏览器
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Trace.Debug("###onPageStarted");
//                if (mProgressBar != null) {
//                    mProgressBar.setVisibility(View.VISIBLE);
//                }
            }

            public void onPageFinished(WebView view, String url) {
                Trace.Debug("###onPageFinished");
//                if (mProgressBar != null) {
//                    mProgressBar.setVisibility(View.GONE);
//                }
            }
        });
        //mWebView.loadDataWithBaseURL(null, url2.toString(), "text/html", "utf-8", null);
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        Trace.Debug("###back key.");
                        if (mWebView != null && mWebView.canGoBack()) {
                            Trace.Debug("###WebView deal with back key.");
                            mWebView.goBack();
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener(){
            @Override
            public void onDismiss(DialogInterface dialog){
                Trace.Debug("###onDismiss");
                if(mWebView != null){
                    mWebView.destroy();
                }
            }
        });
    }

    public static void loadUrlByBrowser(Context context, String url){
        Trace.Debug("###url:" + url);
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        Uri content_url = Uri.parse(url);
        intent.setData(content_url);
        //制定浏览器
        //intent.setClassName("com.android.browser","com.android.browser.BrowserActivity");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        context.startActivity(intent);
    }
}
