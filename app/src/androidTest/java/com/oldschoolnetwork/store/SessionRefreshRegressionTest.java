package com.oldschoolnetwork.store;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Looper;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.oldschoolnetwork.core.IONCoreService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/** Uses isolated preferences and a fake Core: no account or network access. */
@RunWith(AndroidJUnit4.class)
public class SessionRefreshRegressionTest {
    private ONAccountSessionManager session;
    private Object previousInstance;
    private Context context;
    private SharedPreferences preferences;
    private final AtomicInteger binds = new AtomicInteger();
    private final AtomicInteger coreCalls = new AtomicInteger();
    private final AtomicInteger mainThreadCoreCalls = new AtomicInteger();
    private volatile String name = "Test User";
    private CountDownLatch coreEntered;
    private CountDownLatch releaseCore;

    @Before
    public void setUp() throws Exception {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        preferences = target.getSharedPreferences("session_regression_test", Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
        context = new ContextWrapper(target) {
            @Override public Context getApplicationContext() { return this; }
            @Override public SharedPreferences getSharedPreferences(String key, int mode) {
                return preferences;
            }
            @Override public boolean bindService(Intent intent, ServiceConnection connection, int flags) {
                binds.incrementAndGet();
                return true;
            }
        };
        Constructor<ONAccountSessionManager> constructor =
                ONAccountSessionManager.class.getDeclaredConstructor(Context.class);
        constructor.setAccessible(true);
        session = constructor.newInstance(context);
        previousInstance = field("instance").get(null);
        field("instance").set(null, session);
        IONCoreService core = (IONCoreService) Proxy.newProxyInstance(
                IONCoreService.class.getClassLoader(), new Class<?>[] { IONCoreService.class },
                (proxy, method, args) -> {
                    coreCalls.incrementAndGet();
                    if (Looper.myLooper() == Looper.getMainLooper()) mainThreadCoreCalls.incrementAndGet();
                    switch (method.getName()) {
                        case "isLoggedIn": return true;
                        case "getEmail":
                            if (coreEntered != null) {
                                coreEntered.countDown();
                                if (!releaseCore.await(5, TimeUnit.SECONDS)) {
                                    throw new IllegalStateException("Test did not release Core");
                                }
                            }
                            return "test@example.invalid";
                        case "getToken": return "qa-token";
                        case "getUsername": return "test_user";
                        case "getOnId": return "TEST_ID";
                        case "getName": return name;
                        default: return "";
                    }
                });
        field("coreService").set(session, core);
    }

    @After
    public void tearDown() throws Exception {
        if (releaseCore != null) releaseCore.countDown();
        drainSync();
        ((ExecutorService) field("syncExecutor").get(session)).shutdownNow();
        field("instance").set(null, previousInstance);
        preferences.edit().clear().commit();
    }

    @Test
    public void readingSessionFromUiCallbackDoesNotCreateAnUpdateLoop() throws Exception {
        AtomicInteger notifications = new AtomicInteger();
        CountDownLatch firstUpdate = new CountDownLatch(1);
        CountDownLatch unexpectedUpdate = new CountDownLatch(1);
        session.addListener(() -> {
            // Same access pattern as navbar/avatar and ProfileFragment refreshes.
            ONAccountSessionManager current = ONAccountSessionManager.getInstance(context);
            current.isLoggedIn();
            current.getFullName();
            if (notifications.incrementAndGet() > 1) unexpectedUpdate.countDown();
            firstUpdate.countDown();
        });
        onMain(session::syncFromCoreService);
        assertTrue(firstUpdate.await(3, TimeUnit.SECONDS));
        drainSync();
        assertFalse("Rendering must not schedule another update", unexpectedUpdate.await(300, TimeUnit.MILLISECONDS));
        assertEquals(1, notifications.get());
        assertEquals(8, coreCalls.get());

        onMain(session::syncFromCoreService);
        drainSync();
        assertEquals("Unchanged account must not redraw listeners", 1, notifications.get());
        name = "Changed Name";
        onMain(session::syncFromCoreService);
        drainSync();
        assertEquals("A real profile change must still reach the UI", 2, notifications.get());
        assertEquals("Changed Name", session.getFullName());
        assertEquals(0, mainThreadCoreCalls.get());
    }

    @Test
    public void slowCoreDoesNotBlockUiOrQueueRepeatedSyncs() throws Exception {
        coreEntered = new CountDownLatch(1);
        releaseCore = new CountDownLatch(1);
        onMain(session::syncFromCoreService);
        assertTrue(coreEntered.await(3, TimeUnit.SECONDS));
        CountDownLatch uiResponsive = new CountDownLatch(1);
        onMain(() -> {
            for (int i = 0; i < 100; i++) {
                session.syncFromCoreService();
                session.isLoggedIn();
            }
            uiResponsive.countDown();
        });
        assertTrue("UI must respond while Core is blocked", uiResponsive.await(1, TimeUnit.SECONDS));
        releaseCore.countDown();
        drainSync();
        assertEquals("Only one snapshot should be fetched", 8, coreCalls.get());
        assertEquals(0, mainThreadCoreCalls.get());
        assertTrue(session.isLoggedIn());
    }

    @Test
    public void gettingSessionIsReadOnlyAndPendingBindingIsReused() throws Exception {
        field("coreService").set(session, null);
        onMain(() -> {
            for (int i = 0; i < 100; i++) {
                assertSame(session, ONAccountSessionManager.getInstance(context));
                session.isLoggedIn();
            }
        });
        assertEquals(0, binds.get());
        onMain(() -> {
            for (int i = 0; i < 100; i++) session.bindCoreService();
        });
        assertEquals("A pending connection must not be bound repeatedly", 1, binds.get());
        assertEquals(0, coreCalls.get());
    }

    private void drainSync() throws Exception {
        ((ExecutorService) field("syncExecutor").get(session)).submit(() -> {}).get(5, TimeUnit.SECONDS);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static void onMain(Runnable runnable) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
    }

    private static Field field(String name) throws Exception {
        Field field = ONAccountSessionManager.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
