package com.oldschoolnetwork.store;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class LibraryAndSearchRegressionTest {
 @Test public void emptySearchShowsFourMostRecentDistinctQueries() throws Exception {
  Context context=new ContextWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext()) {
   @Override public SharedPreferences getSharedPreferences(String name,int mode){return super.getSharedPreferences("qa_recent_"+name,mode);}
  };
  context.getSharedPreferences("ONStore_search",0).edit().clear().commit();
  try {
   for(String query:new String[]{"one","two","three","four","five","TWO"}){SearchHistory.record(context,query);Thread.sleep(3);}
   JSONArray result=SearchHistory.suggestions(context,"");assertEquals(4,result.length());assertEquals("TWO",result.getJSONObject(0).getString("text"));assertEquals("five",result.getJSONObject(1).getString("text"));
  } finally {context.getSharedPreferences("ONStore_search",0).edit().clear().commit();}
 }
 @Test public void accountSupportBelongsToIndividualPackageAndSurvivesCache() throws Exception {
  JSONObject app=new JSONObject("{id:14,name:'QA',package_name:'com.example.qa',installed_device_ids:['device-a'],packages:[{id:1,supports_on_account:true},{id:2,supports_on_account:false}]}");
  AppItem parsed=AppItem.fromJson(app);assertTrue(parsed.getPackages().get(0).supportsOnAccount());assertFalse(parsed.getPackages().get(1).supportsOnAccount());
  AppItem cached=AppItem.fromJson(parsed.toJson());assertTrue(cached.getPackages().get(0).supportsOnAccount());assertFalse(cached.getPackages().get(1).supportsOnAccount());assertEquals("device-a",cached.getInstalledDeviceIds().get(0));
 }
 @Test public void ordinaryStoreInstallationIsNotReportedAsFull(){assertFalse(InstallationStatus.complete(InstrumentationRegistry.getInstrumentation().getTargetContext()));}
}
