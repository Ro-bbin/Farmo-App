package com.farmo.activities.consumerActivities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.farmo.R;
import com.farmo.activities.commonActivities.ProfileActivity;
import com.farmo.utils.SessionManager;

public class ConsumerDashboardActivity extends AppCompatActivity {

    private TextView tvGreeting, tvWalletBalance, tvSalesAmount, tvViewAllCategories;
    private ImageView ivVisibility, ivRefresh, btnNotification;
    private TextView btnProfile;
    private LinearLayout layoutCategoriesHorizontal;
    private GridLayout layoutCategoriesExpanded;
    
    private boolean isBalanceVisible = true;
    private String walletBalance = "0.00";
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consumer_dashboard);

        sessionManager = new SessionManager(this);
        
        initViews();
        setupListeners();
        fetchDashboardData();
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tvGreeting);
        tvWalletBalance = findViewById(R.id.tvWalletBalance);
        tvSalesAmount = findViewById(R.id.tvSalesAmount);
        tvViewAllCategories = findViewById(R.id.tvViewAllCategories);
        ivVisibility = findViewById(R.id.ivVisibility);
        ivRefresh = findViewById(R.id.ivRefresh);
        btnNotification = findViewById(R.id.btnNotification);
        btnProfile = findViewById(R.id.btnProfile);
        layoutCategoriesHorizontal = findViewById(R.id.layoutCategoriesHorizontal);
        layoutCategoriesExpanded = findViewById(R.id.layoutCategoriesExpanded);
    }

    private void setupListeners() {
        // Toggle Wallet Balance Visibility
        if (ivVisibility != null) {
            ivVisibility.setOnClickListener(v -> {
                if (isBalanceVisible) {
                    tvWalletBalance.setText("*****");
                    ivVisibility.setImageResource(R.drawable.ic_visibility_off);
                } else {
                    tvWalletBalance.setText(walletBalance);
                    ivVisibility.setImageResource(R.drawable.ic_visibility);
                }
                isBalanceVisible = !isBalanceVisible;
            });
        }

        // Toggle Categories View
        if (tvViewAllCategories != null && layoutCategoriesExpanded != null && layoutCategoriesHorizontal != null) {
            tvViewAllCategories.setOnClickListener(v -> {
                if (layoutCategoriesExpanded.getVisibility() == View.GONE) {
                    layoutCategoriesHorizontal.setVisibility(View.GONE);
                    layoutCategoriesExpanded.setVisibility(View.VISIBLE);
                    tvViewAllCategories.setText("Show Less <");
                } else {
                    layoutCategoriesHorizontal.setVisibility(View.VISIBLE);
                    layoutCategoriesExpanded.setVisibility(View.GONE);
                    tvViewAllCategories.setText("View All >");
                }
            });
        }

        // Profile Navigation
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                startActivity(new Intent(this, ProfileActivity.class));
            });
        }

        // Refresh Data
        if (ivRefresh != null) {
            ivRefresh.setOnClickListener(v -> fetchDashboardData());
        }
    }

    private void fetchDashboardData() {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) return;

//        RetrofitClient.getApiService(this).getDashboard(userId).enqueue(new Callback<DashboardService.DashboardResponse>() {
//            @Override
//            public void onResponse(Call<DashboardService.DashboardResponse> call, Response<DashboardResponse> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    DashboardResponse data = response.body();
//                    walletBalance = data.getWalletAmt();
//
//                    if (tvWalletBalance != null && isBalanceVisible) {
//                        tvWalletBalance.setText(walletBalance);
//                    }
//                    if (tvSalesAmount != null) {
//                        tvSalesAmount.setText(data.getIncome());
//                    }
//                    updateGreeting(data.getName());
//                }
//            }
//
//            @Override
//            public void onFailure(Call<DashboardResponse> call, Throwable t) {
//                Toast.makeText(ConsumerDashboardActivity.this, "Failed to sync data", Toast.LENGTH_SHORT).show();
//            }
//        });
    }

//    private void updateGreeting(String userName) {
//        if (tvGreeting == null) return;
//
//        Calendar c = Calendar.getInstance();
//        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);
//        String greeting;
//
//        if (timeOfDay < 12) greeting = getString(R.string.good_morning);
//        else if (timeOfDay < 16) greeting = getString(R.string.good_afternoon);
//        else if (timeOfDay < 21) greeting = getString(R.string.good_evening);
//        else greeting = getString(R.string.good_night);
//
//        tvGreeting.setText(greeting + ", " + (userName != null ? userName : "User"));
//    }
}
