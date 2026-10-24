package com.oldschoolnetwork.store;

import android.content.Context;
import android.content.pm.PackageManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class CoreRemovalRegressionTest {
    @Test public void cachedLoginIsDiscardedWithoutCore() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        try{context.getPackageManager().getPackageInfo("com.oldschoolnetwork.core",0);fail("Run with Core removed from the test device.");}catch(PackageManager.NameNotFoundException expected){}
        context.getSharedPreferences("ONStore_Account",0).edit().putBoolean("logged_in",true).putString("username","qa_cached").putString("token","qa-expired-core-token").commit();
        ONAccountSessionManager session=ONAccountSessionManager.getInstance(context);
        assertFalse(session.isLoggedIn());assertEquals("",session.getToken());assertEquals("",session.getAccountIdentifier());
    }
}
