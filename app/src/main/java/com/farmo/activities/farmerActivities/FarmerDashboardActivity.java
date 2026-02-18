package com.farmo.activities.farmerActivities;

import android.annotation.SuppressLint;
import android.content.Intent;
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
import com.farmo.activities.commonActivities.ProfileActivity;
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

    private static final String TAG = "FarmerDashboard";

    private boolean isBalanceVisible = true;
    private String walletBalance = "0.00";
    private String fullName = "UserName";
    private String todaySales = "0.00";

    private SessionManager sessionManager;
    private TextView tvSalesAmount, tvWalletBalance;

    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean isManualRefresh = false;

    // --- NEW LOADING OVERLAY VARIABLES ---
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
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setupUI();
        setupSwipeRefresh();
        fetchDashboardData();
    }

    /**
     * Creates and toggles a Blur/Dim overlay programmatically.
     */
    private void showLoadingOverlay(boolean show) {
        if (show) {
            if (loadingOverlay == null) {
                // 1. Create the container (The Dim Layer)
                loadingOverlay = new RelativeLayout(this);
                loadingOverlay.setLayoutParams(new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
                loadingOverlay.setBackgroundColor(Color.parseColor("#99000000")); // Semi-transparent black
                loadingOverlay.setClickable(true);
                loadingOverlay.setFocusable(true);

                // 2. Create the ProgressRing
                ProgressBar progressBar = new ProgressBar(this);
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.addRule(RelativeLayout.CENTER_IN_PARENT);
                progressBar.setLayoutParams(lp);

                // Add color tint to match your theme
                progressBar.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));

                loadingOverlay.addView(progressBar);

                // 3. Attach to Activity Root
                ViewGroup rootView = findViewById(android.R.id.content);
                rootView.addView(loadingOverlay);
            }
            loadingOverlay.setVisibility(View.VISIBLE);

            // 4. Real Blur Effect for Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && mainScrollView != null) {
                mainScrollView.setRenderEffect(RenderEffect.createBlurEffect(15f, 15f, Shader.TileMode.CLAMP));
            }
        } else {
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(View.GONE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && mainScrollView != null) {
                    mainScrollView.setRenderEffect(null);
                }
            }
        }
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        if (swipeRefreshLayout == null) return;

        swipeRefreshLayout.setColorSchemeResources(R.color.topical_forest, android.R.color.holo_green_dark);

        if (mainScrollView != null) {
            mainScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                swipeRefreshLayout.setEnabled(scrollY == 0);
            });
        }

        swipeRefreshLayout.setOnRefreshListener(() -> {
            isManualRefresh = true;
            fetchDashboardData();
        });
    }

    private void fetchDashboardData() {
        if (!sessionManager.isLoggedIn()) return;

        // LOGIC: Show Blur/Overlay only if it's NOT a manual pull-to-refresh
        // AND if we haven't loaded data yet (walletBalance is "0.00")
        if (!isManualRefresh && walletBalance.equals("0.00")) {
            showLoadingOverlay(true);
        } else if (isManualRefresh && swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }

        RetrofitClient.getApiService(this).getDashboard(sessionManager.getAuthToken(), sessionManager.getUserId())
                .enqueue(new Callback<DashboardService.DashboardResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<DashboardService.DashboardResponse> call,
                                           @NonNull Response<DashboardService.DashboardResponse> response) {

                        showLoadingOverlay(false);
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null) {
                            DashboardService.DashboardResponse data = response.body();

                            walletBalance = data.getWallet_amt() != null ? data.getWallet_amt() : "0.00";
                            todaySales = data.getTodayIncome() != null ? data.getTodayIncome() : "0.00";
                            fullName = data.getUsername() != null ? data.getUsername() : "User";

                            runOnUiThread(() -> {
                                updateGreeting(fullName);
                                if (isBalanceVisible) {
                                    tvWalletBalance.setText(String.format("NRs. %s", walletBalance));
                                    tvSalesAmount.setText(String.format("NRs. %s", todaySales));
                                }
                            });

                            if (isManualRefresh) {
                                Toast.makeText(FarmerDashboardActivity.this, "Refreshed", Toast.LENGTH_SHORT).show();
                                isManualRefresh = false;
                            }
                        } else {
                            handleError(response);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<DashboardService.DashboardResponse> call, @NonNull Throwable t) {
                        showLoadingOverlay(false);
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(FarmerDashboardActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
                        isManualRefresh = false;
                    }
                });
    }

    private void handleError(Response<DashboardService.DashboardResponse> response) {
        isManualRefresh = false;
        if (response.code() == 401 || response.code() == 403) {
            sessionManager.clearSession();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupUI() {
        ImageView ivVisibility = findViewById(R.id.ivVisibility);
        tvWalletBalance = findViewById(R.id.tvWalletBalance);
        tvSalesAmount = findViewById(R.id.tvSalesAmount);

        // ... Keep all your existing click listeners as they were ...

        ivVisibility.setOnClickListener(v -> {
            if (isBalanceVisible) {
                tvWalletBalance.setText("*****");
                tvSalesAmount.setText("*****");
                ivVisibility.setImageResource(R.drawable.ic_visibility_off);
            } else {
                tvWalletBalance.setText(String.format("NRs. %s", walletBalance));
                tvSalesAmount.setText(String.format("NRs. %s", todaySales));
                ivVisibility.setImageResource(R.drawable.ic_visibility);
            }
            isBalanceVisible = !isBalanceVisible;
        });

        findViewById(R.id.btnProfile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        findViewById(R.id.ivRefresh).setOnClickListener(v -> refreshWalletUI());
    }

    @SuppressLint("SetTextI18n")
    public void updateGreeting(String name) {
        int timeOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting = (timeOfDay < 12) ? "Good Morning" : (timeOfDay < 16) ? "Good Afternoon" : (timeOfDay < 21) ? "Good Evening" : "Good Night";
        TextView tvGreeting = findViewById(R.id.tvGreeting);
        if (tvGreeting != null) tvGreeting.setText(greeting + ", " + name);
    }

    private void refreshWalletUI() {
        Log.d(TAG, "Refreshing wallet data...");

        RetrofitClient.getApiService(this).getRefreshWallet(sessionManager.getAuthToken(), sessionManager.getUserId())
                .enqueue(new Callback<RefreshWallet.refreshWalletResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<RefreshWallet.refreshWalletResponse> call,
                                           @NonNull Response<RefreshWallet.refreshWalletResponse> response) {

                        Log.d(TAG, "Wallet refresh response code: " + response.code());

                        if (response.isSuccessful() && response.body() != null) {
                            RefreshWallet.refreshWalletResponse data = response.body();

                            String balance = data.getBalance() != null ? data.getBalance() : "0.00";
                            String todayIncome = data.getTodaysIncome() != null ? data.getTodaysIncome() : "0.00";

                            // Update local variables
                            walletBalance = balance;
                            todaySales = todayIncome;

                            // Update UI only if balance is visible
                            if (isBalanceVisible) {
                                tvWalletBalance.setText(String.format("NRs. %s", balance));
                                tvSalesAmount.setText(String.format("NRs. %s", todayIncome));
                            }

                            Toast.makeText(FarmerDashboardActivity.this,
                                    "Wallet refreshed", Toast.LENGTH_SHORT).show();

                        } else {
                            String errorMessage = "Failed to load wallet data";

                            if (response.errorBody() != null) {
                                try {
                                    String errorBody = response.errorBody().string();
                                    Log.e(TAG, "Wallet error response: " + errorBody);

                                    RefreshWallet.refreshWalletResponse errorResponse =
                                            new Gson().fromJson(errorBody, RefreshWallet.refreshWalletResponse.class);

                                    if (errorResponse != null && errorResponse.getError() != null) {
                                        errorMessage = errorResponse.getError();
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing wallet error", e);
                                    errorMessage = "Error: " + response.code();
                                }
                            }

                            Toast.makeText(FarmerDashboardActivity.this,
                                    errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<RefreshWallet.refreshWalletResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "Wallet refresh failed", t);
                        Toast.makeText(FarmerDashboardActivity.this,
                                "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionManager.isLoggedIn()) {
            fetchDashboardData();
        }
    }
}