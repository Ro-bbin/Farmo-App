package com.farmo.activities.farmerActivities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.core.widget.NestedScrollView;

import com.farmo.activities.authActivities.LoginActivity;
import com.farmo.R;
import com.farmo.activities.commonActivities.BazarActivity;
import com.farmo.activities.commonActivities.OrdersActivity;
import com.farmo.activities.commonActivities.ProfileActivity;
import com.farmo.activities.commonActivities.SettingsActivity;
import com.farmo.activities.commonActivities.ShowConnectionActivity;
import com.farmo.activities.commonActivities.WalletActivity;
import com.farmo.network.Dashboard.DashboardService;
import com.farmo.network.Dashboard.RefreshWallet;
import com.farmo.network.RetrofitClient;
import com.farmo.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FarmerDashboardActivity extends AppCompatActivity {

    private static final String TAG = "FarmerDashboard";

    private boolean isBalanceVisible = true;
    private String walletBalance = "0.00";
    private String fullName = "UserName";
    private String todayExpense = "0.00";
    private String rating = "None";

    private SessionManager sessionManager;
    private TextView tvSalesAmount, tvWalletBalance, tvTodaysSalesLabel, tvRatingValue;
    private MaterialButton btn7, btn14, btnMonth;

    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean isManualRefresh = false;

    private RelativeLayout loadingOverlay;
    private NestedScrollView mainScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_dashboard);

        sessionManager = new SessionManager(this);
        mainScrollView = findViewById(R.id.nested_scroll_view);

        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            redirectToLogin();
            return;
        }

        setupUI();
        setupSwipeRefresh();
        fetchDashboardData();

        // Ensure Home is selected in bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn()) {
            fetchDashboardData();
        } else {
            redirectToLogin();
        }
        
        // Ensure Home is selected when returning to dashboard
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        }
    }

    private void redirectToLogin() {
        sessionManager.clearSession();
        Intent intent = new Intent(FarmerDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoadingOverlay(boolean show) {
        if (show) {
            if (loadingOverlay == null) {
                loadingOverlay = new RelativeLayout(this);
                loadingOverlay.setLayoutParams(new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                loadingOverlay.setBackgroundColor(Color.parseColor("#99000000"));
                loadingOverlay.setClickable(true);
                loadingOverlay.setFocusable(true);

                ProgressBar progressBar = new ProgressBar(this);
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.addRule(RelativeLayout.CENTER_IN_PARENT);
                progressBar.setLayoutParams(lp);
                progressBar.setIndeterminateTintList(
                        ColorStateList.valueOf(Color.WHITE));

                loadingOverlay.addView(progressBar);

                ViewGroup rootView = findViewById(android.R.id.content);
                rootView.addView(loadingOverlay);
            }
            loadingOverlay.setVisibility(View.VISIBLE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && mainScrollView != null) {
                mainScrollView.setRenderEffect(
                        RenderEffect.createBlurEffect(15f, 15f, Shader.TileMode.CLAMP));
            }
        } else {
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(View.GONE);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && mainScrollView != null) {
                mainScrollView.setRenderEffect(null);
            }
        }
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        if (swipeRefreshLayout == null) return;

        swipeRefreshLayout.setColorSchemeResources(
                R.color.topical_forest, android.R.color.holo_green_dark);

        if (mainScrollView != null) {
            mainScrollView.setOnScrollChangeListener(
                    (NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                        swipeRefreshLayout.setEnabled(scrollY == 0);
                    });
        }

        swipeRefreshLayout.setOnRefreshListener(() -> {
            isManualRefresh = true;
            fetchDashboardData();
        });
    }

    private void fetchDashboardData() {
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        if (!isManualRefresh && walletBalance.equals("0.00")) {
            showLoadingOverlay(true);
        } else if (isManualRefresh && swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }

        RetrofitClient.getApiService(this)
                .getDashboard(sessionManager.getAuthToken(), sessionManager.getUserId())
                .enqueue(new Callback<DashboardService.DashboardResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<DashboardService.DashboardResponse> call,
                                           @NonNull Response<DashboardService.DashboardResponse> response) {

                        showLoadingOverlay(false);
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null) {
                            DashboardService.DashboardResponse data = response.body();

                            walletBalance = data.getWallet_amt() != null ? data.getWallet_amt() : "0.00";
                            todayExpense = data.getTodayExpense() != null ? data.getTodayExpense() : "0.00";
                            fullName = data.getUsername() != null ? data.getUsername() : "User";
                            rating = data.getRating() != null ? data.getRating() : "None";

                            runOnUiThread(() -> {
                                updateGreeting(fullName);
                                if (tvRatingValue != null) tvRatingValue.setText(rating);
                                if (isBalanceVisible) {
                                    tvWalletBalance.setText(String.format("NRs. %s", walletBalance));
                                    tvSalesAmount.setText(String.format("NRs. %s", todayExpense));
                                } else {
                                    tvWalletBalance.setText("*****");
                                    tvSalesAmount.setText("*****");
                                }
                            });

                            if (isManualRefresh) {
                                isManualRefresh = false;
                                Toast.makeText(FarmerDashboardActivity.this,
                                        "Refreshed", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            handleError(response);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<DashboardService.DashboardResponse> call,
                                          @NonNull Throwable t) {
                        showLoadingOverlay(false);
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                        isManualRefresh = false;
                        Log.e(TAG, "Dashboard fetch failed", t);
                        Toast.makeText(FarmerDashboardActivity.this,
                                "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleError(Response<DashboardService.DashboardResponse> response) {
        isManualRefresh = false;
        if (response.code() == 401 || response.code() == 403) {
            redirectToLogin();
        } else {
            Toast.makeText(this, "Failed to load data (Error " + response.code() + ")",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("NonConstantResourceId")
    private void setupUI() {
        ImageView ivVisibility = findViewById(R.id.ivVisibility);
        tvWalletBalance = findViewById(R.id.tvWalletBalance);
        tvSalesAmount = findViewById(R.id.tvSalesAmount);
        tvTodaysSalesLabel = findViewById(R.id.tvTodaysSalesLabel);
        tvRatingValue = findViewById(R.id.tvRatingValue);
        btn7 = findViewById(R.id.btn7Days);
        btn14 = findViewById(R.id.btn14Days);
        btnMonth = findViewById(R.id.btn1Month);

        if (tvTodaysSalesLabel != null) {
            tvTodaysSalesLabel.setText("Today's Expense:");
        }

        if (tvWalletBalance == null || tvSalesAmount == null || ivVisibility == null) {
            Log.e(TAG, "Critical views not found in layout — check your XML IDs");
            return;
        }

        ivVisibility.setOnClickListener(v -> {
            if (isBalanceVisible) {
                tvWalletBalance.setText("*****");
                tvSalesAmount.setText("*****");
                ivVisibility.setImageResource(R.drawable.ic_visibility_off);
            } else {
                tvWalletBalance.setText(String.format("NRs. %s", walletBalance));
                tvSalesAmount.setText(String.format("NRs. %s", todayExpense));
                ivVisibility.setImageResource(R.drawable.ic_visibility);
            }
            isBalanceVisible = !isBalanceVisible;
        });

        // Time Filters
        if (btn7 != null) btn7.setOnClickListener(v -> selectTimeFilter(7, btn7));
        if (btn14 != null) btn14.setOnClickListener(v -> selectTimeFilter(14, btn14));
        if (btnMonth != null) btnMonth.setOnClickListener(v -> selectTimeFilter(30, btnMonth));

        View btnProfile = findViewById(R.id.btnProfile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileActivity.class)));
        }

        View ivRefresh = findViewById(R.id.ivRefresh);
        if (ivRefresh != null) {
            ivRefresh.setOnClickListener(v -> refreshWalletUI());
        }

        findViewById(R.id.idOrderAnalystics).setOnClickListener(v -> 
            gotoOrderManagement());

        // Add Money & Withdraw
        findViewById(R.id.btnAddMoney).setOnClickListener(v -> 
                Toast.makeText(this, "Add Money feature coming soon", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnWithdrawMoney).setOnClickListener(v -> 
                Toast.makeText(this, "Withdraw feature coming soon", Toast.LENGTH_SHORT).show());

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.navigation_orders) {
                gotoOrderManagement();
                return true;
            } else if (id == R.id.navigation_home) {
                fetchDashboardData();
                return true;
            } else if (id == R.id.navigation_products) {
                Intent intent = new Intent(FarmerDashboardActivity.this, MyProductsActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.navigation_bazar) {
                try {
                    Intent intent = new Intent(FarmerDashboardActivity.this, BazarActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error navigating to Bazar", e);
                    Toast.makeText(this, "Bazar is under development", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (id == R.id.navigation_more) {
                try {
                    Intent intent = new Intent(FarmerDashboardActivity.this, SettingsActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error navigating to Settings", e);
                    Toast.makeText(this, "Settings is under development", Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            return false;
        });

        findViewById(R.id.tvMyProduct).setOnClickListener(v -> {
            Intent intent = new Intent(FarmerDashboardActivity.this, MyProductsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.Walletbox).setOnClickListener(v ->{
            Intent intent = new Intent(FarmerDashboardActivity.this, WalletActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnconnection).setOnClickListener(v -> {
            Intent intent = new Intent(FarmerDashboardActivity.this, ShowConnectionActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.addProduct).setOnClickListener(v ->{
            Intent intent = new Intent(FarmerDashboardActivity.this, AddProductActivity.class);
            startActivity(intent);
        });


    }

    private void selectTimeFilter(int days, MaterialButton selectedBtn) {
        updateTimeFilterButtonUI(selectedBtn);
        String label = (days == 7) ? "7 Days Income:" : (days == 14) ? "14 Days Income:" : "1 Month Income:";
        if (tvTodaysSalesLabel != null) tvTodaysSalesLabel.setText(label);
        Toast.makeText(this, "Filtering for " + days + " days", Toast.LENGTH_SHORT).show();
        // Here you would typically call an API with the date range
    }

    private void updateTimeFilterButtonUI(MaterialButton selectedBtn) {
        MaterialButton[] buttons = {btn7, btn14, btnMonth};
        for (MaterialButton btn : buttons) {
            if (btn == null) continue;
            if (btn == selectedBtn) {
                btn.setBackgroundColor(getResources().getColor(R.color.topical_forest));
                btn.setTextColor(Color.WHITE);
            } else {
                btn.setBackgroundColor(Color.TRANSPARENT);
                btn.setTextColor(getResources().getColor(R.color.topical_forest));
            }
        }
    }
    
    private void gotoOrderManagement(){
        Intent intent = new Intent(this, OrdersActivity.class);
        startActivity(intent);
    }
    @SuppressLint("SetTextI18n")
    public void updateGreeting(String name) {
        int timeOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting = (timeOfDay < 12) ? "Good Morning"
                : (timeOfDay < 16) ? "Good Afternoon"
                : (timeOfDay < 21) ? "Good Evening"
                : "Good Night";
        TextView tvGreeting = findViewById(R.id.tvGreeting);
        if (tvGreeting != null) {
            tvGreeting.setText(greeting + ",\n" + name);
        }
    }

    private void refreshWalletUI() {
        RetrofitClient.getApiService(this)
                .getRefreshWallet(sessionManager.getAuthToken(), sessionManager.getUserId())
                .enqueue(new Callback<RefreshWallet.refreshWalletResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<RefreshWallet.refreshWalletResponse> call,
                                           @NonNull Response<RefreshWallet.refreshWalletResponse> response) {

                        if (response.isSuccessful() && response.body() != null) {
                            RefreshWallet.refreshWalletResponse data = response.body();

                            String balance = data.getBalance() != null ? data.getBalance() : "0.00";
                            String todayExpenseVal = data.getTodayExpense() != null
                                    ? data.getTodayExpense() : "0.00";

                            walletBalance = balance;
                            todayExpense = todayExpenseVal;

                            if (isBalanceVisible) {
                                tvWalletBalance.setText(String.format("NRs. %s", balance));
                                tvSalesAmount.setText(String.format("NRs. %s", todayExpenseVal));
                            } else {
                                tvWalletBalance.setText("*****");
                                tvSalesAmount.setText("*****");
                            }

                            Toast.makeText(FarmerDashboardActivity.this,
                                    "Wallet refreshed", Toast.LENGTH_SHORT).show();

                        } else {
                            Toast.makeText(FarmerDashboardActivity.this,
                                    "Failed to load wallet data", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<RefreshWallet.refreshWalletResponse> call,
                                          @NonNull Throwable t) {
                        Toast.makeText(FarmerDashboardActivity.this,
                                "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
