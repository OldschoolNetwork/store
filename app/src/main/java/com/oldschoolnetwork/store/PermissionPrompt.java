package com.oldschoolnetwork.store;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.app.NotificationManagerCompat;

public final class PermissionPrompt {
    private static boolean showing;
    public static void show(Activity activity,boolean force){
        android.content.SharedPreferences prefs=activity.getSharedPreferences("ONPermissions",Context.MODE_PRIVATE);
        boolean install=Build.VERSION.SDK_INT<26||activity.getPackageManager().canRequestPackageInstalls();
        boolean notifications=NotificationManagerCompat.from(activity).areNotificationsEnabled();
        if(showing||(!force&&(prefs.getBoolean("hide",false)||(install&&notifications))))return;
        LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);int pad=(int)(20*activity.getResources().getDisplayMetrics().density);body.setPadding(pad,8,pad,8);
        TextView text=new TextView(activity);text.setText("Allow installation from this app to install APKs. Enable notifications to see download and installation progress. You can change these permissions later in the app settings.");body.addView(text);
        CheckBox hide=new CheckBox(activity);hide.setText("Don't show again");hide.setChecked(prefs.getBoolean("hide",false));body.addView(hide);showing=true;
        androidx.appcompat.app.AlertDialog dialog=new androidx.appcompat.app.AlertDialog.Builder(activity).setTitle("App permissions").setView(body).setPositiveButton("Installation settings",(d,w)->{
            Intent intent=Build.VERSION.SDK_INT>=26?new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName())):new Intent(Settings.ACTION_SECURITY_SETTINGS);open(activity,intent);
        }).setNeutralButton("Notification settings",(d,w)->{
            Intent intent=Build.VERSION.SDK_INT>=26?new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,activity.getPackageName()):new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+activity.getPackageName()));open(activity,intent);
        }).setNegativeButton("Later",null).create();
        dialog.setOnDismissListener(d->{showing=false;prefs.edit().putBoolean("hide",hide.isChecked()).apply();});dialog.show();
    }
    private static void open(Activity activity,Intent intent){try{activity.startActivity(intent);}catch(Exception ignored){activity.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+activity.getPackageName())));}}
}
