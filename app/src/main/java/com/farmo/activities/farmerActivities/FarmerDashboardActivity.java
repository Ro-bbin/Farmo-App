package com.farmo.activities.farmerActivities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.farmo.activities.OrdersActivity;
import com.farmo.activities.ProfileActivity;
import com.farmo.R;
import com.farmo.activities.ReviewsActivity;
import com.farmo.network.Farmer.FarmerDashboardService;
import com.farmo.network.RetrofitClient;
import com.farmo.utils.SessionManager;

import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FarmerDashboardActivity extends AppCompatActivity {

    private boolean isBalanceVisible = true;
    private String userType = "Farmer";
    private String walletBalance = "0.00";
    private String fullName = "UserName";
    private String todaySales = "0.00";

    private SessionManager sessionManager;

    private void set_walletBalance(String walletBalance) {
        this.walletBalance = walletBalance;
    }
    private String get_walletBalance() {
        return walletBalance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_dashboard);

        sessionManager = new SessionManager(this);
        userType = sessionManager.getUserType();

        fetchDashboardData();
        setupUI();
    }

    private void setupUI() {
        ImageView ivVisibility = findViewById(R.id.ivVisibility);
        TextView tvWalletBalance = findViewById(R.id.tvWalletBalance);
        TextView btnProfile = findViewById(R.id.btnProfile);
        TextView tvSalesAmount = findViewById(R.id.tvSalesAmount);

        // Visibility Toggle
        ivVisibility.setOnClickListener(v -> {
            if (isBalanceVisible) {
                tvWalletBalance.setText("*****");
                tvSalesAmount.setText("*****");
                ivVisibility.setImageResource(R.drawable.ic_visibility_off);
            } else {
                tvWalletBalance.setText(String.format("NRs. %s", get_walletBalance()));
                tvSalesAmount.setText(String.format("NRs. %s", todaySales));
                ivVisibility.setImageResource(R.drawable.ic_visibility);
            }
            isBalanceVisible = !isBalanceVisible;
        });

        // Navigation to Profile
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(FarmerDashboardActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // Quick Actions Grid
        findViewById(R.id.cardAddProduct).setOnClickListener(v -> {
            Intent intent = new Intent(FarmerDashboardActivity.this, AddProductActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.cardOrders).setOnClickListener(v -> {
            Intent intent = new Intent(FarmerDashboardActivity.this, OrdersActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.cardMyProducts).setOnClickListener(v -> {
            Intent intent = new Intent(FarmerDashboardActivity.this, MyProductsActivity.class);
            startActivity(intent);
        });

        // Stats Row
        findViewById(R.id.cardOrderAnalytics).setOnClickListener(v -> {
            Intent intent = new Intent(FarmerDashboardActivity.this, OrdersActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.cardReviews).setOnClickListener(v -> {
            Intent intent = new Intent(FarmerDashboardActivity.this, ReviewsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.cardReviewsBottom).setOnClickListener(v -> {
            Intent intent = new Intent(FarmerDashboardActivity.this, ReviewsActivity.class);
            startActivity(intent);
        });
    }

    private void fetchDashboardData() {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) return;

        RetrofitClient.getApiService(this).getDashboard(userId).enqueue(new Callback<FarmerDashboardService.DashboardResponse>() {
            @Override
            public void onResponse(Call<FarmerDashboardService.DashboardResponse> call,
                                   Response<FarmerDashboardService.DashboardResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    FarmerDashboardService.DashboardResponse data = response.body();

                    // Save values safely
                    fullName = data.getUsername() != null ? data.getUsername() : "User";
                    set_walletBalance(data.getWallet_amt() != null ? data.getWallet_amt() : "0.00");
                    todaySales = data.getTodayIncome() != null ? data.getTodayIncome() : "0.00";

                    // Update greeting and wallet UI
                    updateGreeting();
                    refreshWalletUI(data);
                }
            }

            @Override
            public void onFailure(Call<FarmerDashboardService.DashboardResponse> call, Throwable t) {
                Toast.makeText(FarmerDashboardActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    public void updateGreeting() {
        int timeOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;

        if (timeOfDay < 12) {
            greeting = getString(R.string.good_morning);
        } else if (timeOfDay < 16) {
            greeting = getString(R.string.good_afternoon);
        } else if (timeOfDay < 21) {
            greeting = getString(R.string.good_evening);
        } else {
            greeting = getString(R.string.good_night);
        }

        TextView tvGreeting = findViewById(R.id.tvGreeting);
        tvGreeting.setText(String.format("%s, %s", greeting, fullName));
    }

    private void refreshWalletUI(FarmerDashboardService.DashboardResponse data) {
        TextView tvTodaysLabel = findViewById(R.id.tvTodaysSalesLabel);
        TextView tvWalletBalance = findViewById(R.id.tvWalletBalance);
        TextView tvSalesAmount = findViewById(R.id.tvSalesAmount);

        String label = "Farmer".equals(userType) ? getString(R.string.todays_sales) : getString(R.string.todays_expenses);
        tvTodaysLabel.setText(label);
        tvWalletBalance.setText(String.format("NRs. %s", get_walletBalance()));
        tvSalesAmount.setText(String.format("NRs. %s", todaySales));
    }
}