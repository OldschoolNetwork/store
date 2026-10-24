package com.oldschoolnetwork.store;
import android.app.Instrumentation;
import android.content.Intent;
import android.view.View;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class OctoberFeatureRegressionTest {
    private View findDescription(View view,String text){if(text.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;if(view instanceof android.view.ViewGroup)for(int i=0;i<((android.view.ViewGroup)view).getChildCount();i++){View found=findDescription(((android.view.ViewGroup)view).getChildAt(i),text);if(found!=null)return found;}return null;}
    @Test public void publishedReviewHasEditCardAndEditsAutosave() throws Exception {
        Instrumentation i=InstrumentationRegistry.getInstrumentation();android.content.Context context=i.getTargetContext();android.content.SharedPreferences prefs=context.getSharedPreferences("ONStore_Account",0);String previous=prefs.getString("username",null);prefs.edit().putString("username","qa_reviewer").commit();
        JSONObject app=new JSONObject().put("id",999996).put("name","Review fixture").put("package_name","qa.review.fixture");
        JSONArray reviews=new JSONArray().put(new JSONObject().put("id",91009).put("username","qa_reviewer").put("rating",4).put("review_text","Published review fixture").put("likes",12).put("dislikes",2).put("shares",4));
        StoreMainActivity activity=(StoreMainActivity)i.startActivitySync(new Intent(context,StoreMainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try{i.runOnMainSync(()->activity.openAppDetails(AppItem.fromJson(app)));i.waitForIdleSync();
            i.runOnMainSync(()->{try{
                AppDetailsFragment details=(AppDetailsFragment)activity.getSupportFragmentManager().findFragmentById(R.id.detail_fragment_container);
                java.lang.reflect.Field version=AppDetailsFragment.class.getDeclaredField("requestVersion");version.setAccessible(true);version.setInt(details,version.getInt(details)+1);
                prefs.edit().putString("username","qa_reviewer").commit();java.lang.reflect.Field owner=AppDetailsFragment.class.getDeclaredField("account");owner.setAccessible(true);owner.set(details,"qa_reviewer");
                java.lang.reflect.Field own=AppDetailsFragment.class.getDeclaredField("ownReview");own.setAccessible(true);own.set(details,ReviewItem.fromJson(reviews.optJSONObject(0)));
                java.lang.reflect.Method render=AppDetailsFragment.class.getDeclaredMethod("setReviews",JSONArray.class);render.setAccessible(true);render.invoke(details,reviews);
                java.lang.reflect.Method composer=AppDetailsFragment.class.getDeclaredMethod("renderComposer");composer.setAccessible(true);composer.invoke(details);
                assertEquals(View.GONE,activity.findViewById(R.id.reviewComposer).getVisibility());View edit=findDescription(details.getView(),"Edit your review");assertNotNull(edit);assertSame(details.getView().findViewById(R.id.reviewAuthor).getParent(),edit.getParent());assertEquals(android.widget.ImageView.ScaleType.FIT_CENTER,((android.widget.ImageButton)edit).getScaleType());edit.performClick();assertEquals(View.VISIBLE,activity.findViewById(R.id.reviewComposer).getVisibility());
                ((EditText)activity.findViewById(R.id.etReviewInput)).setText("Changed draft survives leaving the page");
                assertEquals("Changed draft survives leaving the page",context.getSharedPreferences("ONStore_review_drafts",0).getString("qa_reviewer:999996",""));
            }catch(Exception failure){throw new AssertionError(failure);}});
        }finally{i.runOnMainSync(activity::finish);if(previous==null)prefs.edit().remove("username").commit();else prefs.edit().putString("username",previous).commit();context.getSharedPreferences("ONStore_review_drafts",0).edit().remove("qa_reviewer:999996").remove("qa_reviewer:999996:stars").commit();}
    }
    @Test public void maintainedIdentityDoesNotUseStudioBrand() throws Exception {
        JSONObject raw=new JSONObject().put("id",999997).put("app_mode","maintained").put("publisher_username","maintainer").put("publisher_studio","PRIVATE_STUDIO").put("publisher_studio_logo","PRIVATE_LOGO").put("publisher_person_avatar","PUBLIC_AVATAR");
        AppItem maintained=AppItem.fromJson(raw);assertEquals("PUBLIC_AVATAR",maintained.getPublisherAvatar());
        raw.put("app_mode","own");AppItem own=AppItem.fromJson(raw);assertEquals("PRIVATE_LOGO",own.getPublisherAvatar());assertEquals("PRIVATE_STUDIO",own.getDeveloper());
    }
    @Test public void selectedVersionUsesItsOwnPermissionsAndCanHaveNone() throws Exception {
        Instrumentation i=InstrumentationRegistry.getInstrumentation();
        JSONObject raw=new JSONObject().put("id",999997).put("name","Permission fixture").put("package_name","qa.permissions.fixture").put("permissions_list",new JSONArray().put("android.permission.CAMERA"))
            .put("packages",new JSONArray().put(new JSONObject().put("id",91001).put("version_name","1").put("permissions_list",new JSONArray().put("android.permission.INTERNET").put("android.permission.ACCESS_NETWORK_STATE").put("android.permission.READ_EXTERNAL_STORAGE").put("android.permission.WRITE_EXTERNAL_STORAGE").put("android.permission.INTERNET"))).put(new JSONObject().put("id",91002).put("version_name","2").put("permissions_list",new JSONArray())));
        StoreMainActivity a=(StoreMainActivity)i.startActivitySync(new Intent(i.getTargetContext(),StoreMainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try{i.runOnMainSync(()->a.openAppDetails(AppItem.fromJson(raw)));i.waitForIdleSync();i.runOnMainSync(()->a.findViewById(R.id.btnInitialAction).performClick());Thread.sleep(300);i.waitForIdleSync();
            i.runOnMainSync(()->{LinearLayout list=a.findViewById(R.id.permissionsList);assertEquals(2,list.getChildCount());View group=list.getChildAt(0);TextView identifiers=group.findViewById(R.id.permissionGroupIdentifiers);assertEquals(View.GONE,identifiers.getVisibility());assertEquals("2",((TextView)group.findViewById(R.id.permissionGroupCount)).getText().toString());group.findViewById(R.id.permissionGroupHeader).performClick();assertEquals(View.VISIBLE,identifiers.getVisibility());assertEquals("android.permission.INTERNET\n\nandroid.permission.ACCESS_NETWORK_STATE",identifiers.getText().toString());group.findViewById(R.id.permissionGroupHeader).performClick();assertEquals(View.GONE,identifiers.getVisibility());Spinner choices=a.findViewById(R.id.androidVersionSpinner);choices.setSelection(1);});i.waitForIdleSync();
            i.runOnMainSync(()->{LinearLayout list=a.findViewById(R.id.permissionsList);assertTrue(list.getChildAt(0) instanceof TextView);assertTrue(((TextView)list.getChildAt(0)).getText().toString().contains("No permissions"));assertEquals(View.VISIBLE,a.findViewById(R.id.btnCollapseMenu).getVisibility());assertEquals(View.GONE,a.findViewById(R.id.btnInitialAction).getVisibility());});
        }finally{i.runOnMainSync(a::finish);}
    }
    @Test public void categoryLinkStaysInStoreAndHandlesBack() throws Exception {
        Instrumentation i=InstrumentationRegistry.getInstrumentation();StoreMainActivity a=(StoreMainActivity)i.startActivitySync(new Intent(i.getTargetContext(),StoreMainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try{i.runOnMainSync(()->a.openStoreUrl("https://oldschoolnetwork.online/store/?category=qa-category"));i.waitForIdleSync();i.runOnMainSync(()->{assertTrue(a.getSupportFragmentManager().findFragmentById(R.id.detail_fragment_container) instanceof CategoryFragment);a.onBackPressed();});i.waitForIdleSync();i.runOnMainSync(()->assertEquals(0,a.getSupportFragmentManager().getBackStackEntryCount()));}
        finally{i.runOnMainSync(a::finish);}
    }
    @Test public void reviewsExposeServerReactionCountsAndFriendlyCustomPermissions() throws Exception {
        ReviewItem review=ReviewItem.fromJson(new JSONObject().put("likes",12).put("dislikes",2).put("shares",4).put("my_reaction",-1).put("reply_text",JSONObject.NULL));
        assertEquals(12,review.getLikes());assertEquals(2,review.getDislikes());assertEquals(4,review.getShares());assertEquals(-1,review.getMyReaction());assertEquals("",review.getReplyText());
        assertEquals("Background operation",PermissionCatalog.describe("android.permission.WAKE_LOCK").title);
        assertEquals("Do custom work",PermissionCatalog.describe("example.permission.DO_CUSTOM_WORK").title);
    }
}
