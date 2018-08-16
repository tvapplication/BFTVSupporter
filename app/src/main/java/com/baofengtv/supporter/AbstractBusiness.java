package com.baofengtv.supporter;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

/**
 * @author LiLiang
 * @version v1.0
 * @brief
 * @date 2015/8/3
 */
public abstract class AbstractBusiness {

    //每个subBusiness将任务放在mTaskRunnable中
    protected Runnable mTaskRunnable = null;

    protected static Handler handler;
    static{
        handler = new Handler(Looper.getMainLooper());
    }

    /**
     * 每个subBusiness的任务
     * @return
     */
    public abstract Runnable getTaskRunnable();

    public Handler getHandler(){
        return handler;
    }
}
