package com.oldschoolnetwork.store;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
public final class StoreNotFoundActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state){super.onCreate(state);setContentView(R.layout.activity_store_not_found);findViewById(R.id.backToStoreHome).setOnClickListener(v->{startActivity(new Intent(this,StoreMainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("store_home",true));finish();});}
}
