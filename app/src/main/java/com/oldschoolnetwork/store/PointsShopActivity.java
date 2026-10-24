package com.oldschoolnetwork.store;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PointsShopActivity extends AppCompatActivity {

    private final int userPoints = 900; // Баланс юзера
    private int cartTotal = 0;    // Сумма корзины
    private int cartItems = 0;    // Общее кол-во предметов

    // Цены товаров
    private final int PRICE_ITEM_1 = 350;
    private final int PRICE_ITEM_2 = 560;

    // Количество каждого товара в корзине
    private int qtyItem1 = 0;
    private int qtyItem2 = 0;
    private int qtyCurrency = 0; // Для диалога с ползунком

    private TextView tvCartItemsCount;
    private TextView tvCartTotalCost;

    // Нижняя корзина
    private LinearLayout btnMyCart;
    private TextView tvMyCartText;
    private ImageView iconMyCart;

    // Элементы переключателей Товар 1
    private LinearLayout btnAddToCart1;
    private LinearLayout layoutCounter1;
    private TextView tvQty1;

    // Элементы переключателей Товар 2
    private LinearLayout btnAddToCart2;
    private LinearLayout layoutCounter2;
    private TextView tvQty2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_points_shop);

        // Инициализация общего UI
        tvCartItemsCount = findViewById(R.id.tvCartItemsCount);
        tvCartTotalCost = findViewById(R.id.tvCartTotalCost);
        btnMyCart = findViewById(R.id.btnMyCart);
        tvMyCartText = findViewById(R.id.tvMyCartText);
        iconMyCart = findViewById(R.id.iconMyCart);

        findViewById(R.id.btnBackShop).setOnClickListener(v -> finish());

        // Связываем элементы Товар 1
        btnAddToCart1 = findViewById(R.id.btnAddToCart1);
        layoutCounter1 = findViewById(R.id.layoutCounter1);
        tvQty1 = findViewById(R.id.tvQty1);
        TextView btnMinus1 = findViewById(R.id.btnMinus1);
        TextView btnPlus1 = findViewById(R.id.btnPlus1);

        // Связываем элементы Товар 2
        btnAddToCart2 = findViewById(R.id.btnAddToCart2);
        layoutCounter2 = findViewById(R.id.layoutCounter2);
        tvQty2 = findViewById(R.id.tvQty2);
        TextView btnMinus2 = findViewById(R.id.btnMinus2);
        TextView btnPlus2 = findViewById(R.id.btnPlus2);

        // --- ЛОГИКА ТОВАРА 1 ---
        btnAddToCart1.setOnClickListener(v -> {
            if (checkBudget(PRICE_ITEM_1)) {
                qtyItem1 = 1;
                updateShopState();
            }
        });
        btnPlus1.setOnClickListener(v -> {
            if (checkBudget(PRICE_ITEM_1)) {
                qtyItem1++;
                updateShopState();
            }
        });
        btnMinus1.setOnClickListener(v -> {
            if (qtyItem1 > 0) {
                qtyItem1--;
                updateShopState();
            }
        });

        // --- ЛОГИКА ТОВАРА 2 ---
        btnAddToCart2.setOnClickListener(v -> {
            if (checkBudget(PRICE_ITEM_2)) {
                qtyItem2 = 1;
                updateShopState();
            }
        });
        btnPlus2.setOnClickListener(v -> {
            if (checkBudget(PRICE_ITEM_2)) {
                qtyItem2++;
                updateShopState();
            }
        });
        btnMinus2.setOnClickListener(v -> {
            if (qtyItem2 > 0) {
                qtyItem2--;
                updateShopState();
            }
        });

        // Валюта (Диалог с ползунком)
        findViewById(R.id.btnSetQuantityCurrency).setOnClickListener(v -> openQuantityDialog());

        // Первоначальное состояние корзины (Выключено)
        btnMyCart.setClickable(false);
        btnMyCart.setEnabled(false);
    }

    // Проверка, хватает ли денег на добавление предмета
    private boolean checkBudget(int price) {
        if (cartTotal + price > userPoints) {
            Toast.makeText(this, "Not enough ON Points!", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // Глобальное обновление цен, счетчиков и видимости кнопок на экране
    private void updateShopState() {
        // Пересчитываем итоги
        cartTotal = (qtyItem1 * PRICE_ITEM_1) + (qtyItem2 * PRICE_ITEM_2) + qtyCurrency;
        cartItems = qtyItem1 + qtyItem2 + (qtyCurrency > 0 ? 1 : 0);

        // Обновляем Товар 1
        if (qtyItem1 > 0) {
            btnAddToCart1.setVisibility(View.GONE);
            layoutCounter1.setVisibility(View.VISIBLE);
            tvQty1.setText(String.valueOf(qtyItem1));
        } else {
            btnAddToCart1.setVisibility(View.VISIBLE);
            layoutCounter1.setVisibility(View.GONE);
        }

        // Обновляем Товар 2
        if (qtyItem2 > 0) {
            btnAddToCart2.setVisibility(View.GONE);
            layoutCounter2.setVisibility(View.VISIBLE);
            tvQty2.setText(String.valueOf(qtyItem2));
        } else {
            btnAddToCart2.setVisibility(View.VISIBLE);
            layoutCounter2.setVisibility(View.GONE);
        }

        // Обновляем нижнюю плашку корзины
        tvCartItemsCount.setText(cartItems + " items");
        tvCartTotalCost.setText(String.valueOf(cartTotal));

        if (cartItems > 0) {
            // Активируем корзину
            btnMyCart.setEnabled(true);
            btnMyCart.setClickable(true);
            btnMyCart.setBackgroundResource(R.drawable.btn_outline_blue);
            tvMyCartText.setTextColor(Color.WHITE);
            iconMyCart.setColorFilter(Color.WHITE);

            // ИСПРАВЛЕНО: Теперь интент передает все QTY переменные на 100% правильно!
            btnMyCart.setOnClickListener(v -> {
                Intent intent = new Intent(PointsShopActivity.this, CheckoutActivity.class);
                intent.putExtra("QTY_ITEM_1", qtyItem1);
                intent.putExtra("QTY_ITEM_2", qtyItem2);
                intent.putExtra("QTY_CURRENCY", qtyCurrency);
                startActivity(intent);
            });
        } else {
            // Выключаем и делаем серой
            btnMyCart.setEnabled(false);
            btnMyCart.setClickable(false);
            btnMyCart.setBackgroundResource(R.drawable.btn_outline_disabled);
            tvMyCartText.setTextColor(Color.parseColor("#777777"));
            iconMyCart.setColorFilter(Color.parseColor("#777777"));
            btnMyCart.setOnClickListener(null);
        }
    }

    private void openQuantityDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_set_quantity, null);
        AlertDialog dialog = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK)
                .setView(dialogView)
                .create();

        int rate = 2;
        int maxPointsToSpend = userPoints - (cartTotal - qtyCurrency);

        SeekBar sbQuantity = dialogView.findViewById(R.id.sbQuantity);
        TextView tvDialogLargeQuantity = dialogView.findViewById(R.id.tvDialogLargeQuantity);
        TextView tvDialogMaxLimit = dialogView.findViewById(R.id.tvDialogMaxLimit);

        sbQuantity.setMax(maxPointsToSpend);
        tvDialogMaxLimit.setText("Max: " + maxPointsToSpend + " ONP");

        sbQuantity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int coins = progress * rate;
                tvDialogLargeQuantity.setText(coins + " Coins\n(" + progress + " ONP)");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        View.OnClickListener adjustListener = v -> {
            int current = sbQuantity.getProgress();
            int amount = 0;
            if (v.getId() == R.id.btnAdd1) amount = 1;
            else if (v.getId() == R.id.btnAdd10) amount = 10;
            else if (v.getId() == R.id.btnAdd100) amount = 100;
            else if (v.getId() == R.id.btnSub1) amount = -1;
            else if (v.getId() == R.id.btnSub10) amount = -10;
            else if (v.getId() == R.id.btnSub100) amount = -100;

            int newProgress = current + amount;
            if (newProgress < 0) newProgress = 0;
            if (newProgress > maxPointsToSpend) newProgress = maxPointsToSpend;
            sbQuantity.setProgress(newProgress);
        };

        dialogView.findViewById(R.id.btnAdd1).setOnClickListener(adjustListener);
        dialogView.findViewById(R.id.btnAdd10).setOnClickListener(adjustListener);
        dialogView.findViewById(R.id.btnAdd100).setOnClickListener(adjustListener);
        dialogView.findViewById(R.id.btnSub1).setOnClickListener(adjustListener);
        dialogView.findViewById(R.id.btnSub10).setOnClickListener(adjustListener);
        dialogView.findViewById(R.id.btnSub100).setOnClickListener(adjustListener);

        dialogView.findViewById(R.id.btnDialogAddToCart).setOnClickListener(v -> {
            int selectedPoints = sbQuantity.getProgress();
            if (selectedPoints > 0) {
                qtyCurrency = selectedPoints;
                updateShopState();
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Select quantity > 0", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}