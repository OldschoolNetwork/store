package com.oldschoolnetwork.store;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent; // ИСПРАВЛЕНО: Добавили пропущенный импорт Intent
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "OSN_Store_Settings";
    private SharedPreferences prefs;

    private CheckBox cbAutoUpdate, cbNotifications, cbRecommendations;
    private Spinner spinnerDownloadNet, spinnerUpdateNet;

    private final String[] networkOptions = {"Any network", "Wi-Fi only"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        android.widget.Button permissions=new android.widget.Button(this);permissions.setText("Permissions and notifications");permissions.setTextColor(android.graphics.Color.CYAN);permissions.setOnClickListener(v->PermissionPrompt.show(this,true));
        ((ViewGroup)findViewById(R.id.btnAboutApp).getParent()).addView(permissions,new ViewGroup.LayoutParams(-1,-2));
        android.widget.Button showNotice=new android.widget.Button(this);showNotice.setText("Restore installation notices");showNotice.setOnClickListener(v->{getSharedPreferences("ONStore",0).edit().putBoolean("hide_installation_notice",false).putBoolean("hide_missing_core",false).apply();android.widget.Toast.makeText(this,"Installation notice restored.",android.widget.Toast.LENGTH_SHORT).show();});
        ((ViewGroup)findViewById(R.id.btnAboutApp).getParent()).addView(showNotice,new ViewGroup.LayoutParams(-1,-2));
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Кнопка назад закрывает активити настроек
        findViewById(R.id.btnBackSettings).setOnClickListener(v -> finish());

        // Связываем интерактивные элементы
        cbAutoUpdate = findViewById(R.id.cbAutoUpdate);
        cbNotifications = findViewById(R.id.cbNotifications);
        cbRecommendations = findViewById(R.id.cbRecommendations);
        spinnerDownloadNet = findViewById(R.id.spinnerDownloadNet);
        spinnerUpdateNet = findViewById(R.id.spinnerUpdateNet);

        // --- НАСТРОЙКА СПИННЕРОВ С ХОЛО-ТИПОГРАФИКОЙ ---
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, networkOptions) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(Color.WHITE);
                tv.setTextSize(15);
                return tv;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerDownloadNet.setAdapter(adapter);
        spinnerUpdateNet.setAdapter(adapter);

        // --- ЗАГРУЗКА СОХРАНЕННЫХ ДАННЫХ ИЗ ПАМЯТИ ---
        cbAutoUpdate.setChecked(prefs.getBoolean("KEY_AUTO_UPDATE", true));
        cbNotifications.setChecked(prefs.getBoolean("KEY_NOTIFICATIONS", true));
        cbRecommendations.setChecked(prefs.getBoolean("KEY_RECOMMENDATIONS", true));
        spinnerDownloadNet.setSelection(prefs.getInt("KEY_DOWNLOAD_NET", 0)); // 0 - Any, 1 - Wi-Fi
        spinnerUpdateNet.setSelection(prefs.getInt("KEY_UPDATE_NET", 1));     // По умолчанию Wi-Fi (1)

        // --- ОТСЛЕЖИВАНИЕ ИЗМЕНЕНИЙ И АВТОСОХРАНЕНИЕ ---
        cbAutoUpdate.setOnCheckedChangeListener((buttonView, isChecked) ->
                {prefs.edit().putBoolean("KEY_AUTO_UPDATE", isChecked).apply();AppUpdateReceiver.schedule(this);});

        cbNotifications.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("KEY_NOTIFICATIONS", isChecked).apply());

        cbRecommendations.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean("KEY_RECOMMENDATIONS", isChecked).apply());

        spinnerDownloadNet.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt("KEY_DOWNLOAD_NET", position).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerUpdateNet.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt("KEY_UPDATE_NET", position).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // --- КЛИК НА "ABOUT OSN STORE" (Перенаправляет на новую Активити) ---
        findViewById(R.id.btnAboutApp).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, AboutActivity.class);
            startActivity(intent);
        });
    } // ИСПРАВЛЕНО: Закрыли метод onCreate
} // ИСПРАВЛЕНО: Закрыли класс SettingsActivity