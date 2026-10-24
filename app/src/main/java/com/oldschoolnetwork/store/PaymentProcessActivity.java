package com.oldschoolnetwork.store;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

public class PaymentProcessActivity extends AppCompatActivity {

    private View stateConfirm, stateProcessing, stateSuccess, stateFailed;
    private AnimationDrawable dotsAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_process);

        // Находим 4 слоя-состояния
        stateConfirm = findViewById(R.id.stateConfirm);
        stateProcessing = findViewById(R.id.stateProcessing);
        stateSuccess = findViewById(R.id.stateSuccess);
        stateFailed = findViewById(R.id.stateFailed);

        // Инициализируем анимацию точек
        ImageView ivLoadingDots = findViewById(R.id.ivLoadingDots);
        if (ivLoadingDots != null && ivLoadingDots.getDrawable() instanceof AnimationDrawable) {
            dotsAnimation = (AnimationDrawable) ivLoadingDots.getDrawable();
        }

        // --- СОСТОЯНИЕ 1: ПОДТВЕРЖДЕНИЕ ---
        findViewById(R.id.btnThinkMore).setOnClickListener(v -> finish()); // Назад в чекаут
        findViewById(R.id.btnDoPurchase).setOnClickListener(v -> startProcessing());

        // --- СОСТОЯНИЕ 3: УСПЕХ ---
        findViewById(R.id.btnBackToApp).setOnClickListener(v -> {
            Intent intent = new Intent(this, StoreMainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // --- СОСТОЯНИЕ 4: ОШИБКА ---
        findViewById(R.id.btnBackToShop).setOnClickListener(v -> finish());
        findViewById(R.id.btnTryAgain).setOnClickListener(v -> startProcessing());

        // Клик по надписи "See details..." открывает окно логов
        View btnSeeDetails = findViewById(R.id.btnSeeDetails);
        if (btnSeeDetails != null) {
            btnSeeDetails.setOnClickListener(v -> {
                Intent intent = new Intent(PaymentProcessActivity.this, PaymentDetailsActivity.class);
                startActivity(intent);
            });
        }
    }

    // Тот самый метод проверки подключения к интернету (совместим с API 14+)
    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    private void startProcessing() {
        // Переключаем слои на загрузку
        stateConfirm.setVisibility(View.GONE);
        stateFailed.setVisibility(View.GONE);
        stateSuccess.setVisibility(View.GONE);
        stateProcessing.setVisibility(View.VISIBLE);

        if (dotsAnimation != null) {
            dotsAnimation.start();
        }

        // ПРОВЕРКА СЕТИ: Если интернета нет, сразу готовим сценарий ошибки
        if (!isOnline()) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (dotsAnimation != null) dotsAnimation.stop();
                stateProcessing.setVisibility(View.GONE);
                stateFailed.setVisibility(View.VISIBLE); // Выкидываем экран ошибки
            }, 1200); // 1 секунда анимации для плавности флоу
            return;
        }

        // Если интернет есть — запускаем стандартный запрос (задержка 2.5 секунды)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (dotsAnimation != null) dotsAnimation.stop();
            stateProcessing.setVisibility(View.GONE);

            // Запрос прошел успешно
            stateSuccess.setVisibility(View.VISIBLE);
        }, 2500);
    }

    @Override
    public void onBackPressed() {
        if (stateProcessing.getVisibility() == View.VISIBLE ||
                stateSuccess.getVisibility() == View.VISIBLE) {
            return;
        }
        super.onBackPressed();
    }
}