package com.oldschoolnetwork.store;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Color;
import android.view.*;
import android.widget.ImageView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class StoreSafetyUiTest {
 @Test public void favoriteAccentAndProfileDivider() throws Exception {
  Instrumentation i=InstrumentationRegistry.getInstrumentation();StoreMainActivity a=(StoreMainActivity)i.startActivitySync(new Intent(i.getTargetContext(),StoreMainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  JSONObject raw=new JSONObject().put("id",999993).put("name","Favourite fixture").put("package_name","qa.favourite.fixture").put("is_favorite",true);
  try{i.runOnMainSync(()->a.openAppDetails(AppItem.fromJson(raw)));i.waitForIdleSync();i.runOnMainSync(()->{try{
   AppDetailsFragment details=(AppDetailsFragment)a.getSupportFragmentManager().findFragmentById(R.id.detail_fragment_container);java.lang.reflect.Field version=AppDetailsFragment.class.getDeclaredField("requestVersion");version.setAccessible(true);version.setInt(details,version.getInt(details)+1);
   java.lang.reflect.Field field=AppDetailsFragment.class.getDeclaredField("app");field.setAccessible(true);AppItem app=(AppItem)field.get(details);app.setFavorite(true);
   java.lang.reflect.Method render=AppDetailsFragment.class.getDeclaredMethod("renderFavorite");render.setAccessible(true);render.invoke(details);ImageView heart=a.findViewById(R.id.btnLike);assertEquals(Color.CYAN,androidx.core.widget.ImageViewCompat.getImageTintList(heart).getDefaultColor());assertTrue(heart.isSelected());app.setFavorite(false);render.invoke(details);assertEquals(Color.WHITE,androidx.core.widget.ImageViewCompat.getImageTintList(heart).getDefaultColor());assertFalse(heart.isSelected());
   View profile=LayoutInflater.from(a).inflate(R.layout.fragment_store_profile,null);View favourites=profile.findViewById(R.id.btnFavorites);ViewGroup parent=(ViewGroup)favourites.getParent();int index=parent.indexOfChild(favourites);assertTrue(index>0);View divider=parent.getChildAt(index-1);assertEquals(View.class,divider.getClass());assertTrue(divider.getLayoutParams().height>0);
  }catch(Exception e){throw new RuntimeException(e);}});}finally{i.runOnMainSync(a::finish);}
 }
}
