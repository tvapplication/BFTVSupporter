package com.baofengtv.supporter.game;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.baofengtv.supporter.Constant;
import com.baofengtv.supporter.SlaveService;
import com.baofengtv.supporter.Trace;
import com.baofengtv.supporter.Utils;
import com.egame.tv.services.aidl.EgameInstallAppBean;
import com.egame.tv.services.aidl.IEgameService;

import java.util.List;

/**
 * @author LiLiang
 * @version v1.0
 * @brief 游戏apk安装或卸载app操作时对应的广播
 * @date 2015/8/2
 */
public class GameInstallChangedReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        int uiVer = Utils.getFUIVersion(context.getApplicationContext());
        if( (uiVer >= 3)
                || !Utils.isPackageInstalled(context.getApplicationContext(), Constant.LAUNCHER_10_PACKAGE)){
            return;
        }
        String action = intent.getAction();
        Trace.Debug("onReceive broadcast. action=" + action);
        if (action.equals("com.egame.tv.CHANGE_INSTALLED_APP")){
            if (SlaveService.getInstance() != null) {
                IEgameService service = SlaveService.getInstance().getRemoteGameService();
                if (service != null) {
                    try {
                        List<EgameInstallAppBean> gameList = (List<EgameInstallAppBean>) service.getValue();
                        if (gameList != null) {
                            int size = gameList.size();
                            for (int i = 0; i < size; i++) {
                                Log.i("EGameChangedReceiver", i + ":"
                                        + gameList.get(i).toString());
                            }
                            GameBusiness.getInstance(context.getApplicationContext())
                                    .refreshInstallGameListAndSendBroadcast(gameList);
                        }else{
                            Trace.Debug("gamelist is null");
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }else {
                    Trace.Warn("service is null");
                }
            }else{
                Trace.Warn("SlaveService.getInstance() is null");
            }
        }
    }
}
