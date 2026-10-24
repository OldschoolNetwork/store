package com.oldschoolnetwork.store;
import android.content.Context;
import android.content.pm.ApplicationInfo;

/** An updated system app can run from /data while retaining its system base. */
public final class InstallationStatus {
    public static boolean isSystem(Context context,String packageName) {
        try { int flags=context.getPackageManager().getApplicationInfo(packageName,0).flags;
            return (flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
        } catch(Exception missing) { return false; }
    }
    public static boolean complete(Context context) {
        return isSystem(context,"com.oldschoolnetwork.core") && isSystem(context,context.getPackageName());
    }
}
