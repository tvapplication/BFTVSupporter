package com.baofengtv.supporter;

import android.app.Application;

import com.baofeng.houyi.HouyiSDK;
import com.baofeng.houyi.utils.LogHelper;

/**
 * @author LiLiang
 * @version 1.0
 * @title 类的名称
 * @description 该类主要功能描述
 * @company 暴风TV
 * @created 2017/2/21 17:56
 * @changeRecord [修改记录] <br/>
 */

public class SuppApplication extends Application{

    @Override
    public void onCreate() {
        Trace.Debug("SuppApplcation::onCreate()");
        super.onCreate();
        //广告系统sdk
        LogHelper.setDebugEnable(false);
        HouyiSDK.init(this);
    }
}
