package com.farmo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.farmo.network.DashboardResponse;
import com.farmo.network.RetrofitClient;
import com.farmo.utils.SessionManager;

import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity {

    private boolean isBalanceVisible = true;
    private String userType = "Consumer";
    private String walletBalance = "0.00";
    private String fullName = "User";

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
        setContentView(R.layout.activity_dashboard);

        sessionManager = new SessionManager(this);
        userType = sessionManager.getUserType();

        fetchDashboardData();

        ImageView ivVisibility = findViewById(R.id.ivVisibility);
        TextView tvWalletBalance = findViewById(R.id.tvWalletBalance);
        TextView btnProfile = findViewById(R.id.btnProfile);

        ivVisibility.setOnClickListener(v -> {
            if (isBalanceVisible) {
                tvWalletBalance.setText("*****");
                ivVisibility.setImageResource(R.drawable.ic_visibility_off);
            } else {
                tvWalletBalance.setText(get_walletBalance());
                ivVisibility.setImageResource(R.drawable.ic_visibility);
            }
            isBalanceVisible = !isBalanceVisible;
        });

        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
    }

    private void fetchDashboardData() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        RetrofitClient.getApiService().getDashboard(userId).enqueue(new Callback<DashboardResponse>() {
            @Override
            public void onResponse(Call<DashboardResponse> call, Response<DashboardResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DashboardResponse data = response.body();
                    fullName = data.getName();
                    set_walletBalance(data.getWalletAmt());

                    updateGreeting();
                    refreshWalletUI(data);
                }
            }

            @Override
            public void onFailure(Call<DashboardResponse> call, Throwable t) {
                Toast.makeText(DashboardActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void updateGreeting() {
        TextView tvGreeting = findViewById(R.id.tvGreeting);
        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);
        String greeting;

        if (timeOfDay >= 0 && timeOfDay < 12) {
            greeting = getString(R.string.good_morning);
        } else if (timeOfDay >= 12 && timeOfDay < 16) {
            greeting = getString(R.string.good_afternoon);
        } else if (timeOfDay >= 16 && timeOfDay < 21) {
            greeting = getString(R.string.good_evening);
        } else {
            greeting = getString(R.string.good_night);
        }

        tvGreeting.setText(greeting + ", " + fullName);
    }

    private void refreshWalletUI(DashboardResponse data) {
        TextView tvTodaysLabel = findViewById(R.id.tvTodaysSalesLabel);
        TextView tvWalletBalance = findViewById(R.id.tvWalletBalance);
        TextView tvSalesAmount = findViewById(R.id.tvSalesAmount);
        
        String label = "Farmer".equals(userType) ? getString(R.string.todays_sales) : getString(R.string.todays_expenses);
        tvTodaysLabel.setText(label);
        tvWalletBalance.setText(data.getWalletAmt());
        tvSalesAmount.setText(data.getIncome());
        
        float ratingValue = data.getRating();
        String ratingText = (ratingValue <= 0) ? "No rating" : String.valueOf(ratingValue);
    }
}
