package com.oldschoolnetwork.store;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
public final class CategoryItem implements Serializable {
    public final String slug,name,description,banner;
    public final List<Integer> appIds=new ArrayList<>();
    public CategoryItem(JSONObject json){slug=json.optString("slug");name=json.optString("name");description=json.optString("description");banner=json.optString("banner_url");JSONArray ids=json.optJSONArray("app_ids");if(ids!=null)for(int i=0;i<ids.length();i++)appIds.add(ids.optInt(i));}
}
