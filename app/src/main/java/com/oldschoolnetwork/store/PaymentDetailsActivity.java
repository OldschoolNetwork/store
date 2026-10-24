package com.oldschoolnetwork.store;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class PaymentDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_details);

        // Кнопка назад тупо закрывает этот слой
        findViewById(R.id.btnDetailsBack).setOnClickListener(v -> finish());

        // Оранжевая кнопка саппорта пока кидает тост
        findViewById(R.id.btnDetailsSupport).setOnClickListener(v ->
                Toast.makeText(this, "Opening ticket channel...", Toast.LENGTH_SHORT).show()
        );
    }
}