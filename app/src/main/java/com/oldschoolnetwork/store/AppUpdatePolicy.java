package com.oldschoolnetwork.store;

import android.content.*;
import android.net.*;

/** Re-read settings throughout a transfer, including before installation. */
public final class AppUpdatePolicy {
    public static boolean allowed(Context context) {
        SharedPreferences settings=context.getSharedPreferences("OSN_Store_Settings",0);
        if(!settings.getBoolean("KEY_AUTO_UPDATE",true))return false;
        ConnectivityManager manager=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network=manager==null?null:manager.getActiveNetworkInfo();
        return network!=null&&network.isConnected()&&(settings.getInt("KEY_UPDATE_NET",1)!=1||network.getType()==ConnectivityManager.TYPE_WIFI);
    }
}
