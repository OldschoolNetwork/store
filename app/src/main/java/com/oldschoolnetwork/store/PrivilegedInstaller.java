package com.oldschoolnetwork.store;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import java.io.*;
import java.util.*;

/** Uses the system package service without its UI only after a full, root-authorised setup. */
public final class PrivilegedInstaller {
    public static void install(Context context,File apk)throws Exception{
        if(!InstallationStatus.complete(context))throw new IOException("Full installation is required.");
        File allowed=new File(context.getCacheDir(),"downloads").getCanonicalFile();
        if(!apk.getCanonicalFile().getParentFile().equals(allowed))throw new IOException("Unsupported APK path.");
        PackageManager manager=context.getPackageManager();PackageInfo archive=manager.getPackageArchiveInfo(apk.getAbsolutePath(),PackageManager.GET_SIGNATURES);
        if(archive==null||archive.signatures==null||archive.signatures.length==0)throw new IOException("APK signing information is unavailable.");
        try{PackageInfo existing=manager.getPackageInfo(archive.packageName,PackageManager.GET_SIGNATURES);if(existing.versionCode>archive.versionCode)throw new IOException("A newer version is installed.");if(!Arrays.equals(existing.signatures,archive.signatures))throw new IOException("The signing certificate differs from the installed app.");}catch(PackageManager.NameNotFoundException ignored){}
        if(android.os.Build.VERSION.SDK_INT>=21 && context.checkCallingOrSelfPermission("android.permission.INSTALL_PACKAGES")==PackageManager.PERMISSION_GRANTED){
            installWithSystemPermission(context,apk);return;
        }
        String path="'"+apk.getCanonicalPath().replace("'","'\\''")+"'";
        Process process=new ProcessBuilder("su","-c","test \"$(id -u)\" = 0 && pm install -r "+path).redirectErrorStream(true).start();
        Timer timeout=new Timer(true);timeout.schedule(new TimerTask(){public void run(){process.destroy();}},90000);
        try{StringBuilder result=new StringBuilder();try(BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream()))){String line;while((line=reader.readLine())!=null){if(result.length()<8000)result.append(line).append('\n');}}
            if(process.waitFor()!=0||!result.toString().contains("Success"))throw new IOException("Root installation was not confirmed. "+result);
            if(manager.getPackageInfo(archive.packageName,0).versionCode<archive.versionCode)throw new IOException("Android did not confirm the installed version.");
        }finally{timeout.cancel();}
    }
    private static void installWithSystemPermission(Context context,File apk)throws Exception{
        android.content.pm.PackageInstaller installer=context.getPackageManager().getPackageInstaller();
        android.content.pm.PackageInstaller.SessionParams params=new android.content.pm.PackageInstaller.SessionParams(android.content.pm.PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setSize(apk.length());int id=installer.createSession(params);
        String action=context.getPackageName()+".INSTALL_RESULT."+java.util.UUID.randomUUID();
        java.util.concurrent.CountDownLatch done=new java.util.concurrent.CountDownLatch(1);String[] failure={"Installation result was not received."};
        android.content.BroadcastReceiver receiver=new android.content.BroadcastReceiver(){public void onReceive(Context c,android.content.Intent intent){int status=intent.getIntExtra(android.content.pm.PackageInstaller.EXTRA_STATUS,android.content.pm.PackageInstaller.STATUS_FAILURE);failure[0]=status==android.content.pm.PackageInstaller.STATUS_SUCCESS?null:(intent.getStringExtra(android.content.pm.PackageInstaller.EXTRA_STATUS_MESSAGE)==null?"Android rejected installation.":intent.getStringExtra(android.content.pm.PackageInstaller.EXTRA_STATUS_MESSAGE));if(status==android.content.pm.PackageInstaller.STATUS_PENDING_USER_ACTION)failure[0]="Android requires user confirmation.";done.countDown();}};
        context.registerReceiver(receiver,new android.content.IntentFilter(action));android.app.PendingIntent callback=null;
        try(android.content.pm.PackageInstaller.Session session=installer.openSession(id)){
            try(InputStream input=new FileInputStream(apk);OutputStream output=session.openWrite("base.apk",0,apk.length())){byte[] buffer=new byte[32768];int count;while((count=input.read(buffer))!=-1)output.write(buffer,0,count);session.fsync(output);}
            callback=android.app.PendingIntent.getBroadcast(context,id,new android.content.Intent(action).setPackage(context.getPackageName()),android.app.PendingIntent.FLAG_UPDATE_CURRENT);
            session.commit(callback.getIntentSender());if(!done.await(90,java.util.concurrent.TimeUnit.SECONDS))throw new IOException("Installation timed out.");if(failure[0]!=null)throw new IOException(failure[0]);
        }catch(Exception error){try{installer.abandonSession(id);}catch(Exception ignored){}throw error;}
        finally{context.unregisterReceiver(receiver);if(callback!=null)callback.cancel();}
    }
}
