package com.oldschoolnetwork.store;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class NetworkNotice extends BroadcastReceiver implements StoreRepository.CatalogListener {
    private final Activity activity;
    private final LinearLayout bar;
    private final TextView text;
    private boolean maintenanceShown;
    private boolean wasOnline;
    public NetworkNotice(Activity activity, LinearLayout parent) {
        this.activity=activity;
        bar=new LinearLayout(activity);bar.setGravity(android.view.Gravity.CENTER_VERTICAL);bar.setPadding(16,8,16,8);bar.setBackgroundColor(Color.parseColor("#253845"));
        text=new TextView(activity);text.setTextColor(Color.WHITE);text.setTextSize(14);bar.addView(text,new LinearLayout.LayoutParams(0,-2,1));
        Button retry=new Button(activity);retry.setText("Retry");retry.setTextColor(Color.CYAN);retry.setOnClickListener(v->{StoreRepository.getInstance().fetchCatalog(true);update();});bar.addView(retry,new LinearLayout.LayoutParams(-2,-2));
        parent.addView(bar,0,new LinearLayout.LayoutParams(-1,-2));
        activity.registerReceiver(this,new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        StoreRepository.getInstance().addListener(this);update();
    }
    private void update(){
        ConnectivityManager manager=(ConnectivityManager)activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info=manager==null?null:manager.getActiveNetworkInfo();boolean online=info!=null&&info.isConnected();
        String error=StoreRepository.getInstance().getNetworkMessage();
        if(error.startsWith("MAINTENANCE:")&&!maintenanceShown){maintenanceShown=true;activity.startActivity(new Intent(activity,StoreMaintenanceActivity.class).putExtra("comment",error.substring(12)));}
        if(error.isEmpty())maintenanceShown=false;
        if(error.startsWith("MAINTENANCE:"))error=error.substring(12);
        text.setText(!online?"No internet connection. Showing saved data.":error);
        bar.setVisibility(!online||!error.isEmpty()?View.VISIBLE:View.GONE);
        boolean reconnect=online&&!wasOnline;wasOnline=online;
        if(reconnect)StoreRepository.getInstance().fetchCatalog(true);
    }
    @Override public void onReceive(Context context,Intent intent){update();}
    @Override public void onCatalogUpdated(java.util.List<AppItem> apps,java.util.List<BannerItem> banners){update();}
    public void close(){StoreRepository.getInstance().removeListener(this);activity.unregisterReceiver(this);((ViewGroup)bar.getParent()).removeView(bar);}
}
