package com.baofengtv.supporter.game;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.baofengtv.supporter.Constant;
import com.baofengtv.supporter.Trace;
import com.baofengtv.supporter.Utils;

/**
 * @author LiLiang
 * @version v1.0
 * @brief
 * @date 2015/11/3
 */
public class GameUninstallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int uiVer = Utils.getFUIVersion(context.getApplicationContext());
        if( (uiVer >= 3) ||
                !Utils.isPackageInstalled(context.getApplicationContext(), Constant.LAUNCHER_10_PACKAGE)){
            return;
        }
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
            Trace.Debug("###get broadcast ACTION_PACKAGE_REMOVED");
            try {
                if (Utils.getTopPackageName(context).equals("com.egame.tv")){
                    return;
                }
                String packageName = intent.getData().getSchemeSpecificPart();
                Trace.Debug("###remove package : " + packageName);

                GameBusiness.getInstance(context).refreshInstalledGames(packageName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
