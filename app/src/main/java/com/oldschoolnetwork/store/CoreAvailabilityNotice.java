package com.oldschoolnetwork.store;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.graphics.Color;

/** Core is optional for browsing; it is required for ON Account and ON Auth. */
public final class CoreAvailabilityNotice extends BroadcastReceiver {
    private final Activity activity;
    private final LinearLayout notice;
    private boolean dismissed;
    public CoreAvailabilityNotice(Activity activity,LinearLayout parent){
        this.activity=activity;notice=new LinearLayout(activity);notice.setOrientation(LinearLayout.VERTICAL);int pad=(int)(16*activity.getResources().getDisplayMetrics().density);notice.setPadding(pad,pad/2,pad,pad/2);notice.setBackgroundResource(R.drawable.bg_gradient_violet);
        TextView text=new TextView(activity);text.setText("ON Core is not installed. Download Core to sign in to ON Account and use ON Auth. You can keep browsing Store without it.");text.setTextColor(Color.WHITE);text.setTextSize(14);notice.addView(text);
        LinearLayout actions=new LinearLayout(activity);actions.setOrientation(LinearLayout.HORIZONTAL);
        Button download=new Button(activity);download.setText("Download Core");download.setTextColor(Color.CYAN);download.setOnClickListener(v->{
            download.setEnabled(false);
            org.json.JSONObject request=new org.json.JSONObject();try{request.put("package_name","com.oldschoolnetwork.core");}catch(Exception ignored){}
            StoreRepository.getInstance().requestAccountAction("store_app_details",request,ONAccountSessionManager.getInstance(activity),(result,error)->{
                download.setEnabled(true);if(activity.isFinishing())return;
                AppItem core=result==null?null:AppItem.fromJson(result.optJSONObject("app"));
                if(core!=null&&activity instanceof StoreMainActivity){((StoreMainActivity)activity).openAppDetails(core);return;}
                Toast.makeText(activity,error!=null?error:"Core has not been published yet.",Toast.LENGTH_LONG).show();
            });
        });
        Button dismiss=new Button(activity);dismiss.setText("Dismiss");dismiss.setOnClickListener(v->{dismissed=true;update();});
        actions.addView(download,new LinearLayout.LayoutParams(0,-2,1));actions.addView(dismiss,new LinearLayout.LayoutParams(0,-2,1));notice.addView(actions);
        CheckBox hide=new CheckBox(activity);hide.setText("Don't show again");hide.setTextColor(Color.LTGRAY);hide.setOnCheckedChangeListener((button,checked)->activity.getSharedPreferences("ONStore",0).edit().putBoolean("hide_missing_core",checked).apply());notice.addView(hide);
        parent.addView(notice,Math.min(1,parent.getChildCount()),new LinearLayout.LayoutParams(-1,-2));
        IntentFilter filter=new IntentFilter();filter.addAction(Intent.ACTION_PACKAGE_ADDED);filter.addAction(Intent.ACTION_PACKAGE_REMOVED);filter.addAction(Intent.ACTION_PACKAGE_CHANGED);filter.addDataScheme("package");activity.registerReceiver(this,filter);update();
    }
    public static boolean installed(Context context){try{return context.getPackageManager().getApplicationInfo("com.oldschoolnetwork.core",0).enabled;}catch(Exception ignored){return false;}}
    public void update(){notice.setVisibility(installed(activity)||dismissed||activity.getSharedPreferences("ONStore",0).getBoolean("hide_missing_core",false)?View.GONE:View.VISIBLE);}
    @Override public void onReceive(Context context,Intent intent){update();}
    public void close(){activity.unregisterReceiver(this);((ViewGroup)notice.getParent()).removeView(notice);}
}
