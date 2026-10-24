package com.oldschoolnetwork.store;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.ArrayList;

public final class CategoryFragment extends Fragment {
    private LinearLayout root;
    private TextView title,description,status;
    private ImageView banner;
    private AppAdapter adapter;
    private int generation;
    public static CategoryFragment create(String slug){CategoryFragment fragment=new CategoryFragment();Bundle args=new Bundle();args.putString("slug",slug);fragment.setArguments(args);return fragment;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle state){
        root=new LinearLayout(requireContext());root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(12),dp(16),0);root.setBackgroundResource(R.drawable.bg_gradient_violet);
        ImageButton back=new androidx.appcompat.widget.AppCompatImageButton(requireContext());back.setImageResource(R.drawable.ic_arrow_right);back.setRotation(180);back.setColorFilter(Color.WHITE);back.setBackgroundColor(Color.TRANSPARENT);back.setContentDescription("Back");back.setOnClickListener(v->requireActivity().onBackPressed());root.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));
        title=new TextView(requireContext());title.setText("Loading category…");title.setTextSize(26);title.setTextColor(Color.CYAN);root.addView(title);
        description=new TextView(requireContext());description.setTextColor(Color.LTGRAY);description.setTextSize(14);root.addView(description);
        banner=new ImageView(requireContext());banner.setScaleType(ImageView.ScaleType.CENTER_CROP);banner.setVisibility(View.GONE);root.addView(banner,new LinearLayout.LayoutParams(-1,dp(140)));
        status=new TextView(requireContext());status.setTextColor(Color.LTGRAY);status.setPadding(0,dp(8),0,dp(8));status.setOnClickListener(v->load());root.addView(status);
        RecyclerView list=new RecyclerView(requireContext());list.setLayoutManager(new LinearLayoutManager(requireContext()));adapter=new AppAdapter(new ArrayList<>());list.setAdapter(adapter);adapter.setOnAppClickListener(app->{if(getActivity() instanceof StoreMainActivity)((StoreMainActivity)getActivity()).openAppDetails(app);});root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        PullRefreshLayout refresh=new PullRefreshLayout(requireContext());refresh.setContent(root,this::load);load();return refresh;
    }
    private void load(){
        if(root==null)return;int request=++generation;status.setText("Loading…");
        JSONObject body=new JSONObject();try{body.put("category",getArguments().getString("slug"));}catch(Exception ignored){}
        StoreRepository.getInstance().requestAccountAction("store_category",body,ONAccountSessionManager.getInstance(requireContext()),(result,error)->{
            if(root==null||request!=generation)return;
            if("NOT_FOUND".equals(error)){startActivity(new Intent(requireContext(),StoreNotFoundActivity.class));return;}
            if(error!=null){status.setText(error+" · Tap to retry");return;}
            JSONObject info=result.optJSONObject("category");if(info==null){status.setText("Could not load category. Tap to retry.");return;}
            CategoryItem category=new CategoryItem(info);title.setText(category.name);description.setText(category.description);
            if(!category.banner.isEmpty()){banner.setVisibility(View.VISIBLE);banner.setContentDescription(category.name);com.bumptech.glide.Glide.with(this).load(category.banner).override(960,400).centerCrop().into(banner);}
            else banner.setVisibility(View.GONE);
            ArrayList<Object> apps=new ArrayList<>();JSONArray items=result.optJSONArray("apps");if(items!=null)for(int i=0;i<items.length();i++){AppItem app=AppItem.fromJson(items.optJSONObject(i));if(app!=null)apps.add(app);}adapter.setItems(apps);status.setText(apps.isEmpty()?"No applications in this category yet.":"");
        });
    }
    public void onDestroyView(){generation++;root=null;super.onDestroyView();}
}
