package com.oldschoolnetwork.store;

import android.content.Intent; // Добавлен импорт для переходов
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CheckoutActivity extends AppCompatActivity {

    private int userPoints = 900; // Стартовый баланс юзера
    private int finalTotal = 0;   // Общая сумма заказа

    // Цены предметов (должны быть синхронны с шопом)
    private final int PRICE_ITEM_1 = 350;
    private final int PRICE_ITEM_2 = 560;

    // Переменные количества
    private int qtyItem1 = 0;
    private int qtyItem2 = 0;
    private int qtyCurrency = 0;

    // Элементы UI
    private TextView tvTotalCheckout;
    private TextView tvUserPointsCheckout;

    private View containerItem1;
    private TextView tvCheckoutQty1;
    private TextView tvPriceInfoItem1;

    private View containerItem2;
    private TextView tvCheckoutQty2;
    private TextView tvPriceInfoItem2;

    private View containerCurrency;
    private TextView tvCheckoutCurrencyQty;
    private TextView tvCheckoutCurrencyPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        // Кнопка закрытия
        findViewById(R.id.btnBackCheckout).setOnClickListener(v -> finish());

        // Получаем точные данные из интента PointsShopActivity
        qtyItem1 = getIntent().getIntExtra("QTY_ITEM_1", 0);
        qtyItem2 = getIntent().getIntExtra("QTY_ITEM_2", 0);
        qtyCurrency = getIntent().getIntExtra("QTY_CURRENCY", 0);

        // Связываем элементы Товар 1 (Frame)
        containerItem1 = findViewById(R.id.containerItem1Checkout);
        tvCheckoutQty1 = findViewById(R.id.tvCheckoutQty1);
        tvPriceInfoItem1 = findViewById(R.id.tvPriceInfoItem1);
        View btnCheckoutMinus1 = findViewById(R.id.btnCheckoutMinus1);
        View btnCheckoutPlus1 = findViewById(R.id.btnCheckoutPlus1);

        // Связываем элементы Товар 2 (Artifact)
        containerItem2 = findViewById(R.id.containerItem2Checkout);
        tvCheckoutQty2 = findViewById(R.id.tvCheckoutQty2);
        tvPriceInfoItem2 = findViewById(R.id.tvPriceInfoItem2);
        View btnCheckoutMinus2 = findViewById(R.id.btnCheckoutMinus2);
        View btnCheckoutPlus2 = findViewById(R.id.btnCheckoutPlus2);

        // Связываем элементы Валюты (Coins)
        containerCurrency = findViewById(R.id.containerCurrencyCheckout);
        tvCheckoutCurrencyQty = findViewById(R.id.tvCheckoutCurrencyQty);
        tvCheckoutCurrencyPrice = findViewById(R.id.tvCheckoutCurrencyPrice);

        tvTotalCheckout = findViewById(R.id.tvTotalCheckout);
        tvUserPointsCheckout = findViewById(R.id.tvUserPointsCheckout);
        CheckBox cbCheckoutTerms = findViewById(R.id.cbCheckoutTerms);
        TextView btnFinalPurchase = findViewById(R.id.btnFinalPurchase);

        // --- ОБРАБОТЧИКИ ДЛЯ ИЗМЕНЕНИЯ КОЛИЧЕСТВА ВНУТРИ ЧЕКАУТА ---
        if (btnCheckoutPlus1 != null) {
            btnCheckoutPlus1.setOnClickListener(v -> {
                if (checkBudget(PRICE_ITEM_1)) {
                    qtyItem1++;
                    calculateCheckout();
                }
            });
        }
        if (btnCheckoutMinus1 != null) {
            btnCheckoutMinus1.setOnClickListener(v -> {
                if (qtyItem1 > 0) {
                    qtyItem1--;
                    calculateCheckout();
                }
            });
        }

        if (btnCheckoutPlus2 != null) {
            btnCheckoutPlus2.setOnClickListener(v -> {
                if (checkBudget(PRICE_ITEM_2)) {
                    qtyItem2++;
                    calculateCheckout();
                }
            });
        }
        if (btnCheckoutMinus2 != null) {
            btnCheckoutMinus2.setOnClickListener(v -> {
                if (qtyItem2 > 0) {
                    qtyItem2--;
                    calculateCheckout();
                }
            });
        }

        // Запуск первоначального расчета корзины
        calculateCheckout();

        // --- ЛОГИКА ОПЛАТЫ (ИСПРАВЛЕНО: Теперь запускает флоу подтверждения и процессинга) ---
        btnFinalPurchase.setOnClickListener(v -> {
            if (!cbCheckoutTerms.isChecked()) {
                Toast.makeText(this, "Please agree to the Terms and Conditions.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (finalTotal > userPoints) {
                Toast.makeText(this, "Transaction failed: Not enough ON Points!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Передаем управление в PaymentProcessActivity, которая по цепочке покажет нужные стейты
            Intent intent = new Intent(CheckoutActivity.this, PaymentProcessActivity.class);
            startActivity(intent);
        });
    }

    private boolean checkBudget(int price) {
        if (finalTotal + price > userPoints) {
            Toast.makeText(this, "Not enough ON Points to add more!", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // Динамический пересчет видимости слоев и итоговой суммы
    private void calculateCheckout() {
        finalTotal = (qtyItem1 * PRICE_ITEM_1) + (qtyItem2 * PRICE_ITEM_2) + qtyCurrency;

        tvTotalCheckout.setText(finalTotal + ".00");
        tvUserPointsCheckout.setText(userPoints + ".00");

        // Управляем видимостью товара 1
        if (qtyItem1 > 0 && containerItem1 != null) {
            containerItem1.setVisibility(View.VISIBLE);
            tvCheckoutQty1.setText(String.valueOf(qtyItem1));
            tvPriceInfoItem1.setText(qtyItem1 + " x " + PRICE_ITEM_1 + " ONP");
        } else if (containerItem1 != null) {
            containerItem1.setVisibility(View.GONE);
        }

        // Управляем видимостью товара 2
        if (qtyItem2 > 0 && containerItem2 != null) {
            containerItem2.setVisibility(View.VISIBLE);
            tvCheckoutQty2.setText(String.valueOf(qtyItem2));
            tvPriceInfoItem2.setText(qtyItem2 + " x " + PRICE_ITEM_2 + " ONP");
        } else if (containerItem2 != null) {
            containerItem2.setVisibility(View.GONE);
        }

        // Управляем видимостью Валюты (изменяется только в диалоге шопа)
        if (qtyCurrency > 0 && containerCurrency != null) {
            containerCurrency.setVisibility(View.VISIBLE);
            // 1 ON Point = 2 Coins, значит выводим чистые коины
            tvCheckoutCurrencyQty.setText(String.valueOf(qtyCurrency * 2));
            tvCheckoutCurrencyPrice.setText(String.valueOf(qtyCurrency));
        } else if (containerCurrency != null) {
            containerCurrency.setVisibility(View.GONE);
        }

        // Если из чекаута удалили вообще все товары — закрываем экран и возвращаем в шоп
        if (qtyItem1 == 0 && qtyItem2 == 0 && qtyCurrency == 0) {
            Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}