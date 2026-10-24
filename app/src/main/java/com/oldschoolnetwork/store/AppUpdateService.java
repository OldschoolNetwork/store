package com.oldschoolnetwork.store;
import android.app.*;
import android.content.*;
import android.os.*;
import android.net.*;
import androidx.core.app.NotificationCompat;
import org.json.*;
import java.io.*;
import java.net.*;

/** Core owns Core/Store updates; this service handles installed third-party apps. */
public final class AppUpdateService extends Service {
    private volatile boolean running;
    public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(running)return START_NOT_STICKY;running=true;
        TransferNotification progress=new TransferNotification(this,4206,"ON Store updates");
        NotificationCompat.Builder initial=new NotificationCompat.Builder(this,"on_transfers").setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle("Checking application updates").setOngoing(true).setProgress(0,0,true);
        startForeground(4206,initial.build());
        new Thread(()->{int available=0,installed=0;try{
            if(!AppUpdatePolicy.allowed(this))return;
            HttpURLConnection connection=(HttpURLConnection)new URL(StoreRepository.BASE_API_URL+"?action=store_catalog").openConnection();JSONArray apps;
            try{connection.setConnectTimeout(10000);connection.setReadTimeout(15000);StringBuilder text=new StringBuilder();try(BufferedReader reader=new BufferedReader(new InputStreamReader(connection.getInputStream(),"UTF-8"))){String line;while((line=reader.readLine())!=null){text.append(line);if(text.length()>8*1024*1024)throw new IOException("Catalogue too large.");}}JSONObject response=new JSONObject(text.toString());if(!"ok".equals(response.optString("status")))return;apps=response.optJSONArray("apps");}finally{connection.disconnect();}
            if(apps==null)return;
            for(int index=0;index<apps.length();index++){
                if(!AppUpdatePolicy.allowed(this))break;
                JSONObject object=apps.optJSONObject(index);AppItem app=AppItem.fromJson(object);if(app==null)continue;
                String name=app.getPackageName();if(name.equals("com.oldschoolnetwork.core")||name.equals("com.oldschoolnetwork.store"))continue;
                int version;try{version=getPackageManager().getPackageInfo(name,0).versionCode;}catch(Exception ignored){continue;}
                PackageVersionItem best=null;for(PackageVersionItem pkg:app.getPackages())if(pkg.isCompatible()&&pkg.getVersionCode()>version&&pkg.getSha256().matches("(?i)[a-f0-9]{64}")&&(best==null||pkg.getVersionCode()>best.getVersionCode()))best=pkg;
                if(best==null)continue;available++;
                if(!InstallationStatus.complete(this))continue;
                File file=null;try{
                    ApkDownloader downloader=new ApkDownloader();downloader.setHeader("Authorization","Bearer "+ONAccountSessionManager.getInstance(this).getToken());
                    file=downloader.download(best.getDownloadUrl(),new File(getCacheDir(),"downloads"),best.getSha256(),(bytes,total)->{if(!AppUpdatePolicy.allowed(this))downloader.cancel();else progress.progress(bytes,total);});
                    android.content.pm.PackageInfo archive=getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(),0);if(archive==null||!name.equals(archive.packageName)||archive.versionCode!=best.getVersionCode())throw new IOException("Update package does not match.");
                    if(!AppUpdatePolicy.allowed(this))break;
                    progress.installing();PrivilegedInstaller.install(this,file);installed++;
                }catch(Exception error){android.util.Log.w("ONStore","Could not auto-update "+name,error);}finally{if(file!=null)file.delete();}
            }
        }catch(Exception error){android.util.Log.w("ONStore","Update check did not finish",error);}finally{
            stopForeground(true);progress.finish();running=false;
            if(available>0&&getSharedPreferences("OSN_Store_Settings",0).getBoolean("KEY_NOTIFICATIONS",true)){int pendingFlags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=23)pendingFlags|=PendingIntent.FLAG_IMMUTABLE;NotificationCompat.Builder done=new NotificationCompat.Builder(this,"on_transfers").setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle(installed>0?installed+" application updates installed":"Application updates available").setContentText((available-installed)+" updates need your attention.").setAutoCancel(true).setContentIntent(PendingIntent.getActivity(this,4206,new Intent(this,StoreMainActivity.class),pendingFlags));((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(4206,done.build());}
            stopSelf();
        }},"store-app-updates").start();return START_NOT_STICKY;
    }
}
