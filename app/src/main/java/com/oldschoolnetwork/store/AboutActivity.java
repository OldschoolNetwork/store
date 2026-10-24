package com.oldschoolnetwork.store;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        findViewById(R.id.btnBackAbout).setOnClickListener(v -> finish());

        // Находим TextView версии и подставляем данные из getAppVersion()
        TextView tvAboutVersion = findViewById(R.id.tvAboutVersion);
        if (tvAboutVersion != null) {
            tvAboutVersion.setText("Version: " + getAppVersion());
        }

        // Клик по кнопке открывает сайт в браузере смартфона
        findViewById(R.id.btnAboutNetwork).setOnClickListener(v -> {
            try {
                // Ссылка на ваш сайт (пока заглушка, меняй на свой URL при надобности)
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://oldschoolnetwork.online"));
                startActivity(browserIntent);
            } catch (Exception e) {
                Toast.makeText(this, "Browser application not found!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Тот самый метод getVersion, вытаскивающий versionName из манифеста/gradle
    private String getAppVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "2.42-Beta"; // Дефолтный фолбэк
        }
    }
}