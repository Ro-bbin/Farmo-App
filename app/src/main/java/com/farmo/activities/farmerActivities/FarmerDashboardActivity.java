package com.farmo.activities.farmerActivities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.farmo.activities.OrdersActivity;
import com.farmo.activities.ProfileActivity;
import com.farmo.R;
import com.farmo.activities.ReviewsActivity;
import com.farmo.activities.wallet.WalletActivity;
import com.farmo.network.Dashboard.DashboardService;
import com.farmo.network.Dashboard.RefreshWallet;
import com.farmo.network.RetrofitClient;
import com.farmo.utils.SessionManager;
import com.google.gson.Gson;

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

    private RelativeLayout walletArea ;

    private TextView tvSalesAmount, tvWalletBalance;

    private ImageView RefreshWalletbyImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_dashboard);

        sessionManager = new SessionManager(this);
        userType = sessionManager.getUserType();
        setupUI();


        fetchDashboardData();


    }

    private void setupUI() {
        ImageView ivVisibility = findViewById(R.id.ivVisibility);
        tvWalletBalance = findViewById(R.id.tvWalletBalance);
        TextView btnProfile = findViewById(R.id.btnProfile);
        tvSalesAmount = findViewById(R.id.tvSalesAmount);
        walletArea = findViewById(R.id.Walletbox);
        RefreshWalletbyImage = findViewById(R.id.ivRefresh);


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

        walletArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create an Intent to switch activities
                Intent intent = new Intent(FarmerDashboardActivity.this, WalletActivity.class);
                startActivity(intent);
                finish();
            }
        });

        RefreshWalletbyImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshWalletUI();
            }
        });
    }

    private void fetchDashboardData() {

        RetrofitClient.getApiService(this).getDashboard().enqueue(new Callback<DashboardService.DashboardResponse>() {
            @Override
            public void onResponse(Call<DashboardService.DashboardResponse> call,
                                   Response<DashboardService.DashboardResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    DashboardService.DashboardResponse data = response.body();

                    // Save values safely
                    fullName = data.getUsername() != null ? data.getUsername() : "User";
                    set_walletBalance(data.getWallet_amt() != null ? data.getWallet_amt() : "0.00");
                    todaySales = data.getTodayIncome() != null ? data.getTodayIncome() : "0.00";

                    // Update UI on main thread - REMOVED refreshWalletUI()
                    runOnUiThread(() -> {
                        updateGreeting(fullName);
                        // Update wallet display directly
                        tvWalletBalance.setText(String.format("NRs. %s", walletBalance));
                        tvSalesAmount.setText(String.format("NRs. %s", todaySales));
                    });
                }
            }

            @Override
            public void onFailure(Call<DashboardService.DashboardResponse> call, Throwable t) {
                Toast.makeText(FarmerDashboardActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    public void updateGreeting(String name) {
        int timeOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;

        if (timeOfDay < 12) {
            greeting = "Good Morning";
        } else if (timeOfDay < 16) {
            greeting = "Good Afternoon";
        } else if (timeOfDay < 21) {
            greeting = "Good Evening";
        } else {
            greeting = "Good Night";
        }
        //greeting = greeting + ", " + name;

        TextView tvGreeting = findViewById(R.id.tvGreeting);
        tvGreeting.setText(String.format("%s, %s", greeting, name));
    }

    private void refreshWalletUI() {
        RetrofitClient.getApiService(this).getRefreshWallet()
                .enqueue(new Callback<RefreshWallet.refreshWalletResponse>() {
                    @Override
                    public void onResponse(Call<RefreshWallet.refreshWalletResponse> call,
                                           Response<RefreshWallet.refreshWalletResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            RefreshWallet.refreshWalletResponse data = response.body();

                            String balance = data.getBalance() != null ? data.getBalance() : "0.00";
                            String todaysIncome = data.getTodaysIncome() != null ? data.getTodaysIncome() : "0.00";

                            // Update local variables too!
                            set_walletBalance(balance);
                            todaySales = todaysIncome;

                            // Update UI
                            tvWalletBalance.setText(String.format("NRs. %s", balance));
                            tvSalesAmount.setText(String.format("NRs. %s", todaysIncome));

                            Toast.makeText(FarmerDashboardActivity.this,
                                    "Wallet refreshed",
                                    Toast.LENGTH_SHORT).show();

                        } else {
                            // Parse error from response body
                            String errorMessage = "Failed to load wallet data";

                            if (response.errorBody() != null) {
                                try {
                                    String errorBody = response.errorBody().string();
                                    RefreshWallet.refreshWalletResponse errorResponse =
                                            new Gson().fromJson(errorBody, RefreshWallet.refreshWalletResponse.class);

                                    if (errorResponse != null && errorResponse.getError() != null) {
                                        errorMessage = errorResponse.getError();
                                    }
                                } catch (Exception e) {
                                    errorMessage = "Error: " + response.code();
                                }
                            }

                            Toast.makeText(FarmerDashboardActivity.this,
                                    errorMessage,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<RefreshWallet.refreshWalletResponse> call, Throwable t) {
                        Toast.makeText(FarmerDashboardActivity.this,
                                "Network Error: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }


}