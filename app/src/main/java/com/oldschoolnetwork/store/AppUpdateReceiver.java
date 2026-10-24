package com.oldschoolnetwork.store;
import android.app.*;
import android.content.*;
import android.os.Build;
public final class AppUpdateReceiver extends BroadcastReceiver {
    public static void schedule(Context context){
        int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pending=PendingIntent.getBroadcast(context,4206,new Intent(context,AppUpdateReceiver.class),flags);AlarmManager alarm=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);alarm.cancel(pending);
        if(context.getSharedPreferences("OSN_Store_Settings",0).getBoolean("KEY_AUTO_UPDATE",true))alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+120000,AlarmManager.INTERVAL_DAY,pending);
    }
    @Override public void onReceive(Context context,Intent intent){
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){schedule(context);return;}
        if(!context.getSharedPreferences("OSN_Store_Settings",0).getBoolean("KEY_AUTO_UPDATE",true))return;
        Intent service=new Intent(context,AppUpdateService.class);if(Build.VERSION.SDK_INT>=26)context.startForegroundService(service);else context.startService(service);
    }
}
