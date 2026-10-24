package com.oldschoolnetwork.store;

import android.app.*;
import android.content.*;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public final class TransferNotification {
    private static final String CHANNEL="on_transfers";
    private final Context context;private final int id;private final String title;
    public TransferNotification(Context context,int id,String title){this.context=context.getApplicationContext();this.id=id;this.title=title;
        if(Build.VERSION.SDK_INT>=26){NotificationManager manager=(NotificationManager)this.context.getSystemService(Context.NOTIFICATION_SERVICE);manager.createNotificationChannel(new NotificationChannel(CHANNEL,"Downloads and installation",NotificationManager.IMPORTANCE_LOW));}
    }
    public void progress(long bytes,long total){show("Downloading…",total>0?(int)Math.min(100,bytes*100/total):0,total<=0);}
    public void preparing(long bytes,long total){show("Preparing APK…",total>0?(int)Math.min(100,bytes*100/total):0,total<=0);}
    public void installing(){show("Installing…",0,true);}
    private void show(String text,int progress,boolean indeterminate){
        Intent launch=context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
        NotificationCompat.Builder builder=new NotificationCompat.Builder(context,CHANNEL).setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle(title).setContentText(text).setOngoing(true).setOnlyAlertOnce(true).setProgress(100,progress,indeterminate).setPriority(NotificationCompat.PRIORITY_LOW);
        if(launch!=null)builder.setContentIntent(PendingIntent.getActivity(context,id,launch,flags));
        try{NotificationManagerCompat.from(context).notify(id,builder.build());}catch(SecurityException ignored){}
    }
    public void finish(){NotificationManagerCompat.from(context).cancel(id);}
}
