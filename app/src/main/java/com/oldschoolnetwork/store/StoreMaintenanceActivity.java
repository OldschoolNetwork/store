package com.oldschoolnetwork.store;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
public final class StoreMaintenanceActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state){super.onCreate(state);setContentView(R.layout.activity_store_maintenance);((TextView)findViewById(R.id.maintenanceComment)).setText(getIntent().getStringExtra("comment"));findViewById(R.id.retryMaintenance).setOnClickListener(v->retry());}
    private void retry(){findViewById(R.id.retryMaintenance).setEnabled(false);JSONObject request=new JSONObject();try{request.put("service","store");}catch(Exception ignored){}
        StoreRepository.getInstance().requestAccountAction("service_status",request,ONAccountSessionManager.getInstance(this),(result,error)->{if(isFinishing())return;findViewById(R.id.retryMaintenance).setEnabled(true);JSONObject status=result==null?null:result.optJSONObject("maintenance");if(error!=null||status==null){((TextView)findViewById(R.id.maintenanceComment)).setText(error!=null?error:"Could not check service status.");return;}if(status.optBoolean("enabled")){((TextView)findViewById(R.id.maintenanceComment)).setText(status.optString("comment"));return;}StoreRepository.getInstance().fetchCatalog(true);startActivity(new Intent(this,StoreMainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("store_home",true));finish();});
    }
}
