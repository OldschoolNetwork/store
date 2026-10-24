package com.oldschoolnetwork.store;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PlatformUiTest {
    @Test public void updateAppearsBesideOpenOnlyForNewerCompatiblePackage() throws Exception {
        Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        android.content.Context context=instrumentation.getTargetContext();
        android.content.SharedPreferences settings=context.getSharedPreferences("OSN_Store_Settings",0);
        boolean previousAuto=settings.getBoolean("KEY_AUTO_UPDATE",true);
        settings.edit().putBoolean("KEY_AUTO_UPDATE",false).commit();
        assertFalse(AppUpdatePolicy.allowed(context));
        int installed=context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionCode;
        JSONObject newer=new JSONObject().put("id",91001).put("version_code",installed+1);
        JSONObject incompatible=new JSONObject().put("id",91002).put("version_code",installed+2).put("min_android","SDK 999");
        JSONObject raw=new JSONObject().put("id",999998).put("name","Update fixture").put("package_name",context.getPackageName()).put("packages",new JSONArray().put(newer).put(incompatible));
        StoreMainActivity activity=(StoreMainActivity)instrumentation.startActivitySync(new Intent(context,StoreMainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            instrumentation.runOnMainSync(()->activity.openAppDetails(AppItem.fromJson(raw)));instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(()->{
                View open=activity.findViewById(R.id.btnOpenApp),update=activity.findViewById(R.id.btnUpdateApp);
                assertEquals(View.VISIBLE,update.getVisibility());assertEquals(View.VISIBLE,open.getVisibility());
                assertSame(open.getParent(),update.getParent());assertEquals("Open",((TextView)open).getText().toString());assertEquals("Update",((TextView)update).getText().toString());
                AppDetailsFragment fragment=(AppDetailsFragment)activity.getSupportFragmentManager().findFragmentById(R.id.detail_fragment_container);
                try {
                    java.lang.reflect.Field field=AppDetailsFragment.class.getDeclaredField("app");field.setAccessible(true);
                    raw.put("packages",new JSONArray().put(new JSONObject().put("id",91003).put("version_code",installed)).put(incompatible));field.set(fragment,AppItem.fromJson(raw));
                    java.lang.reflect.Method render=AppDetailsFragment.class.getDeclaredMethod("renderInstallState");render.setAccessible(true);render.invoke(fragment);
                    assertEquals(View.GONE,update.getVisibility());assertEquals(View.VISIBLE,open.getVisibility());
                } catch(Exception error){throw new AssertionError(error);}
            });
        } finally {instrumentation.runOnMainSync(activity::finish);settings.edit().putBoolean("KEY_AUTO_UPDATE",previousAuto).commit();}
    }
    @Test public void nullReplyIsNotShownAsText() throws Exception {
        ReviewItem item=ReviewItem.fromJson(new JSONObject().put("reply_text",JSONObject.NULL).put("reply_username",JSONObject.NULL).put("reply_role",JSONObject.NULL));
        assertEquals("",item.getReplyText());assertEquals("",item.getReplyUsername());
    }
    @Test public void unknownFeatureShowsStoreNotFoundPage() throws Exception {
        Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(StoreNotFoundActivity.class.getName(),null,false);
        Intent intent=new Intent(instrumentation.getTargetContext(),StoreMainActivity.class).setAction(Intent.ACTION_VIEW).setData(Uri.parse("https://oldschoolnetwork.online/store/unknown-feature")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        StoreMainActivity main=(StoreMainActivity)instrumentation.startActivitySync(intent);
        android.app.Activity missing=monitor.waitForActivityWithTimeout(5000);
        try{assertNotNull("Unknown Store routes must show a dedicated missing page",missing);assertNotNull(missing.findViewById(R.id.backToStoreHome));}
        finally{instrumentation.removeMonitor(monitor);if(missing!=null)instrumentation.runOnMainSync(missing::finish);instrumentation.runOnMainSync(main::finish);}
    }
    @Test public void libraryFiltersAreBehindAButton() throws Exception {
        Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        LibraryActivity activity=(LibraryActivity)instrumentation.startActivitySync(new Intent(instrumentation.getTargetContext(),LibraryActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try{instrumentation.waitForIdleSync();instrumentation.runOnMainSync(()->{assertEquals(View.GONE,activity.findViewById(R.id.collectionFilters).getVisibility());assertTrue(((TextView)activity.findViewById(R.id.collectionStatus)).getText().toString().contains("Sign in"));});}
        finally{instrumentation.runOnMainSync(activity::finish);}
    }
    @Test public void reviewUsesCustomVectorsAndPublicNickname() throws Exception {
        Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
        StoreMainActivity activity=(StoreMainActivity)instrumentation.startActivitySync(new Intent(instrumentation.getTargetContext(),StoreMainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        JSONObject raw=new JSONObject().put("id",999999).put("name","QA app").put("package_name","qa.fixture.game");
        try{
            instrumentation.runOnMainSync(()->activity.openAppDetails(AppItem.fromJson(raw)));instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(()->{
                AppDetailsFragment details=(AppDetailsFragment)activity.getSupportFragmentManager().findFragmentById(R.id.detail_fragment_container);
                assertNotNull(details);
                try{JSONArray reviews=new JSONArray().put(new JSONObject().put("username","public_alias").put("user_display_name","PRIVATE_NAME").put("user_surname","PRIVATE_SURNAME").put("on_id","PRIVATE_ID").put("rating",4).put("reply_text","Maintainer response").put("reply_username","public_maintainer").put("reply_role","maintained"));java.lang.reflect.Method render=AppDetailsFragment.class.getDeclaredMethod("setReviews",JSONArray.class);render.setAccessible(true);render.invoke(details,reviews);}catch(Exception e){throw new AssertionError(e);}
                LinearLayout stars=activity.findViewById(R.id.reviewRating);assertNotNull(stars);assertEquals(5,stars.getChildCount());for(int i=0;i<5;i++)assertTrue(stars.getChildAt(i) instanceof androidx.appcompat.widget.AppCompatImageView);
                assertEquals("@public_alias",((TextView)activity.findViewById(R.id.reviewAuthor)).getText().toString());
                assertEquals(View.VISIBLE,activity.findViewById(R.id.reviewReply).getVisibility());
                assertEquals("@public_maintainer · Maintainer",((TextView)activity.findViewById(R.id.reviewReplyAuthor)).getText().toString());
                assertEquals(android.graphics.Color.parseColor("#FF9800"),((TextView)activity.findViewById(R.id.reviewReplyAuthor)).getCurrentTextColor());
                assertEquals("Maintainer response",((TextView)activity.findViewById(R.id.reviewReplyText)).getText().toString());
                assertNotNull(activity.findViewById(R.id.reportReplyContent));
                try{java.lang.reflect.Field installing=AppDetailsFragment.class.getDeclaredField("silentInstalling");installing.setAccessible(true);installing.setBoolean(details,true);java.lang.reflect.Method render=AppDetailsFragment.class.getDeclaredMethod("renderInstallState");render.setAccessible(true);render.invoke(details);assertEquals(View.VISIBLE,activity.findViewById(R.id.layoutDownloadProgress).getVisibility());assertEquals(View.GONE,activity.findViewById(R.id.btnFinalDownload).getVisibility());assertEquals(View.GONE,activity.findViewById(R.id.btnCancelDownload).getVisibility());assertEquals("Installing…",((TextView)activity.findViewById(R.id.tvProgressText)).getText().toString());installing.setBoolean(details,false);render.invoke(details);}catch(Exception error){throw new AssertionError(error);}
            });
        }finally{instrumentation.runOnMainSync(activity::finish);}
    }
}
