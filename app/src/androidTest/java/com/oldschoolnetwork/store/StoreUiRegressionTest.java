package com.oldschoolnetwork.store;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class StoreUiRegressionTest {
 @Test public void detailPanelHasNaturalHeightAndProfileHasFavorites() throws Exception {
  Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
  Intent intent = new Intent(instrumentation.getTargetContext(), StoreMainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  StoreMainActivity activity = (StoreMainActivity) instrumentation.startActivitySync(intent);
  try {
   long deadline = SystemClock.uptimeMillis() + 15000;
   while (StoreRepository.getInstance().getAllApps().isEmpty() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(100);
   assertFalse("Live catalog did not load", StoreRepository.getInstance().getAllApps().isEmpty());
   instrumentation.runOnMainSync(() -> {
    activity.findViewById(R.id.nav_profile).performClick();
    assertNotNull(activity.findViewById(R.id.btnFavorites));
    activity.findViewById(R.id.nav_games).performClick();
    activity.openAppDetails(StoreRepository.getInstance().getAllApps().get(0));
   });
   instrumentation.waitForIdleSync(); SystemClock.sleep(700);
   instrumentation.runOnMainSync(() -> activity.findViewById(R.id.btnInitialAction).performClick());
   SystemClock.sleep(500); instrumentation.waitForIdleSync();
   instrumentation.runOnMainSync(() -> {
    View panel = activity.findViewById(R.id.layoutExpandedMenu);
    float density = activity.getResources().getDisplayMetrics().density;
    assertEquals(View.VISIBLE, panel.getVisibility());
    assertEquals("Package details must size to their content",android.view.ViewGroup.LayoutParams.WRAP_CONTENT,panel.getLayoutParams().height);
    assertEquals(android.view.ViewGroup.LayoutParams.WRAP_CONTENT,activity.findViewById(R.id.permissionsList).getLayoutParams().height);
    assertTrue("Package details collapsed incorrectly", panel.getHeight() > 100 * density);
    assertNotNull(activity.findViewById(R.id.guardyBadge));
   });
   java.io.File folder = new java.io.File(activity.getFilesDir(), "qa"); folder.mkdirs();
   android.graphics.Bitmap screenshot = instrumentation.getUiAutomation().takeScreenshot();
   try (java.io.FileOutputStream output = new java.io.FileOutputStream(new java.io.File(folder, "store-detail.png"))) { screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output); }
  } finally { instrumentation.runOnMainSync(activity::finish); }
 }
}
