package com.oldschoolnetwork.store;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class WalletActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);
        findViewById(R.id.btnBackWallet).setOnClickListener(v -> finish());
    }
}