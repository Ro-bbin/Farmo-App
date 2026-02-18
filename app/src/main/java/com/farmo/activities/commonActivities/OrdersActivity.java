package com.farmo.activities.commonActivities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.farmo.R;

public class OrdersActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}