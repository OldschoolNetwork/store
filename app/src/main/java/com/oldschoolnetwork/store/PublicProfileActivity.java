package com.oldschoolnetwork.store;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.bumptech.glide.Glide;
import org.json.*;
import java.util.*;

public final class PublicProfileActivity extends AppCompatActivity {
    private String username;private boolean studio,creator;private int publicId;private AppAdapter adapter;
    @Override protected void onCreate(Bundle state){super.onCreate(state);setContentView(R.layout.activity_public_profile);username=getIntent().getStringExtra("username");creator=getIntent().getBooleanExtra("creator",false);studio=creator||getIntent().getBooleanExtra("studio",false);
        ((TextView)findViewById(R.id.publicProfileHeading)).setText(creator?"Original creator":studio?"Studio":"Public profile");findViewById(R.id.backPublicProfile).setOnClickListener(v->finish());findViewById(R.id.publicProfileStatus).setOnClickListener(v->load());findViewById(R.id.reportPublicProfile).setOnClickListener(v->report());
        androidx.recyclerview.widget.RecyclerView list=findViewById(R.id.publicStudioApps);list.setLayoutManager(new LinearLayoutManager(this));adapter=new AppAdapter(new ArrayList<>());list.setAdapter(adapter);adapter.setOnAppClickListener(app->{AppDetailsFragment details=new AppDetailsFragment();Bundle args=new Bundle();args.putSerializable("APP_ITEM",app);details.setArguments(args);findViewById(R.id.publicProfileAppDetails).setVisibility(View.VISIBLE);getSupportFragmentManager().beginTransaction().replace(R.id.publicProfileAppDetails,details).addToBackStack(null).commit();});
        getSupportFragmentManager().addOnBackStackChangedListener(()->findViewById(R.id.publicProfileAppDetails).setVisibility(getSupportFragmentManager().getBackStackEntryCount()>0?View.VISIBLE:View.GONE));load();
    }
    private void image(int id,String source){android.widget.ImageView image=findViewById(id);Object model=source;if(source!=null&&!source.isEmpty()&&!source.startsWith("https://")&&!source.startsWith("http://"))try{model=android.util.Base64.decode(source.substring(source.indexOf(',')+1),android.util.Base64.DEFAULT);}catch(Exception ignored){model=null;}Glide.with(this).load(model).placeholder(R.drawable.ic_profile_placeholder).into(image);}
    private void load(){JSONObject request=new JSONObject();try{request.put(creator?"creator":"username",username==null?"":username);}catch(Exception ignored){}
        StoreRepository.getInstance().requestAccountAction(creator?"public_creator":studio?"public_studio":"public_profile",request,ONAccountSessionManager.getInstance(this),(result,error)->{if(isFinishing())return;if("NOT_FOUND".equals(error)){startActivity(new Intent(this,StoreNotFoundActivity.class));finish();return;}if(error!=null){((TextView)findViewById(R.id.publicProfileStatus)).setText(error+" · Tap to retry");return;}
            JSONObject profile=result.optJSONObject("profile"),info=result.optJSONObject("studio");if(profile==null)return;publicId=profile.optInt("public_id");String publicName="@"+profile.optString("username");
            ((TextView)findViewById(R.id.publicProfileName)).setText(studio&&info!=null?(info.optString("name").isEmpty()?"Developer studio":info.optString("name")):publicName);image(R.id.publicProfileAvatar,studio?(info==null?"":info.optString("logo_url")):profile.optString("avatar_base64"));
            List<AppItem> apps=new ArrayList<>();JSONArray array=result.optJSONArray("apps");boolean maintained=false,own=false;if(array!=null)for(int i=0;i<array.length();i++){AppItem app=AppItem.fromJson(array.optJSONObject(i));if(app!=null){apps.add(app);if("maintained".equals(app.getAppMode()))maintained=true;else own=true;}}
            adapter.setItems(new ArrayList<Object>(apps));((TextView)findViewById(R.id.publicProfileRole)).setText(creator?"Original creator":studio?(maintained?(own?"Developer and maintainer":"Maintainer"):"Developer"):(maintained?"Maintainer":"Member"));
            ((TextView)findViewById(R.id.publicProfileStatus)).setText(apps.size()+ (creator?" created applications":studio?" published applications":" maintained applications"));
            if(studio&&info!=null){((TextView)findViewById(R.id.publicStudioDescription)).setText(info.optString("description"));if(!info.optString("banner_url").isEmpty()){findViewById(R.id.publicStudioBanner).setVisibility(View.VISIBLE);image(R.id.publicStudioBanner,info.optString("banner_url"));}}
        });
    }
    private void report(){ONAccountSessionManager session=ONAccountSessionManager.getInstance(this);if(!session.isLoggedIn()){Toast.makeText(this,"Sign in to report content.",Toast.LENGTH_LONG).show();return;}EditText text=new EditText(this);text.setHint("Additional details (optional)");text.setMinLines(3);text.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(1600)});
        androidx.appcompat.app.AlertDialog dialog=new androidx.appcompat.app.AlertDialog.Builder(this).setTitle(studio?"Report studio":"Report profile").setView(text).setNegativeButton("Cancel",null).setPositiveButton("Send report",null).create();dialog.setOnShowListener(d->dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener(v->{String message=text.getText().toString().trim();JSONObject request=new JSONObject();try{request.put("target_type",creator?"creator":studio?"studio":"profile");request.put("target_id",publicId);request.put("reason","Other");request.put("details",message);}catch(Exception ignored){}StoreRepository.getInstance().requestAccountAction("platform_report",request,session,(result,error)->{Toast.makeText(this,error!=null?error:"Report sent.",Toast.LENGTH_LONG).show();if(error==null)dialog.dismiss();});}));dialog.show();
    }
}
