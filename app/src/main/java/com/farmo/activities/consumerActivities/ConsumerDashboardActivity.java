package com.farmo.activities.consumerActivities;

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
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.farmo.R;
import com.farmo.activities.authActivities.LoginActivity;
import com.farmo.activities.commonActivities.BazarActivity;
import com.farmo.activities.commonActivities.OrdersActivity;
import com.farmo.activities.commonActivities.ProfileActivity;
import com.farmo.activities.commonActivities.SettingsActivity;
import com.farmo.network.Dashboard.DashboardService;
import com.farmo.network.Dashboard.RefreshWallet;
import com.farmo.network.RetrofitClient;
import com.farmo.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;

import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConsumerDashboardActivity extends AppCompatActivity {

    private static final String TAG = "ConsumerDashboard";

    private TextView tvGreeting, tvWalletBalance, tvSalesAmount, tvViewAllCategories, tvTodaysSalesLabel, tvRatingValue;
    private ImageView ivVisibility;
    private TextView btnProfile;
    private LinearLayout layoutCategoriesHorizontal;
    private GridLayout layoutCategoriesExpanded;

    private boolean isBalanceVisible = true;
    private String walletBalance = "0.00";
    private String todayExpense = "0.00";
    private String fullName = "User";
    private String rating = "None";

    private SessionManager sessionManager;
    private SwipeRefreshLayout swipeRefreshLayout;
    private NestedScrollView mainScrollView;
    private RelativeLayout loadingOverlay;
    private boolean isManualRefresh = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consumer_dashboard);

        sessionManager = new SessionManager(this);
        mainScrollView = findViewById(R.id.nested_scroll_view);

        // Check login first
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        initViews();
        setupListeners();
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
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
        } else if (!walletBalance.equals("0.00")) {
            // Only re-fetch if data was already loaded once (not on cold start)
            fetchDashboardData();
        }
        
        // Ensure Home is selected when returning to dashboard
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        }
    }

    private void redirectToLogin() {
        sessionManager.clearSession();
        Intent intent = new Intent(ConsumerDashboardActivity.this, LoginActivity.class); // Explicit context
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ─── Loading Overlay (same pattern as FarmerDashboard) ───────────────────
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
                        android.content.res.ColorStateList.valueOf(Color.WHITE));

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

    // ─── SwipeRefresh Setup ──────────────────────────────────────────────────
    private void setupSwipeRefresh() {
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        if (swipeRefreshLayout == null) return;

        swipeRefreshLayout.setColorSchemeResources(
                R.color.topical_forest, android.R.color.holo_green_dark);

        if (mainScrollView != null) {
            mainScrollView.setOnScrollChangeListener(
                    (NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                            swipeRefreshLayout.setEnabled(scrollY == 0));
        }

        swipeRefreshLayout.setOnRefreshListener(() -> {
            isManualRefresh = true;
            fetchDashboardData();
        });
    }

    // ─── Fetch Dashboard Data ────────────────────────────────────────────────
    private void fetchDashboardData() {
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin();
            return;
        }

        // Show overlay only on first load
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
                                tvRatingValue.setText(rating);
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
                                Toast.makeText(ConsumerDashboardActivity.this,
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
                        Toast.makeText(ConsumerDashboardActivity.this,
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

    // ─── Refresh Wallet ──────────────────────────────────────────────────────
    private void refreshWalletUI() {
        Log.d(TAG, "Refreshing wallet data...");

        RetrofitClient.getApiService(this)
                .getRefreshWallet(sessionManager.getAuthToken(), sessionManager.getUserId())
                .enqueue(new Callback<RefreshWallet.refreshWalletResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<RefreshWallet.refreshWalletResponse> call,
                                           @NonNull Response<RefreshWallet.refreshWalletResponse> response) {

                        if (response.isSuccessful() && response.body() != null) {
                            RefreshWallet.refreshWalletResponse data = response.body();

                            String balance = data.getBalance() != null ? data.getBalance() : "0.00";
                            String todayAmt = data.getTodayExpense() != null ? data.getTodayExpense() : "0.00";

                            walletBalance = balance;
                            todayExpense = todayAmt;

                            if (isBalanceVisible) {
                                tvWalletBalance.setText(String.format("NRs. %s", balance));
                                tvSalesAmount.setText(String.format("NRs. %s", todayAmt));
                            } else {
                                tvWalletBalance.setText("*****");
                                tvSalesAmount.setText("*****");
                            }

                            Toast.makeText(ConsumerDashboardActivity.this,
                                    "Wallet refreshed", Toast.LENGTH_SHORT).show();

                        } else {
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
                                    Log.e(TAG, "Error parsing wallet error", e);
                                    errorMessage = "Error: " + response.code();
                                }
                            }
                            Toast.makeText(ConsumerDashboardActivity.this,
                                    errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<RefreshWallet.refreshWalletResponse> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "Wallet refresh failed", t);
                        Toast.makeText(ConsumerDashboardActivity.this,
                                "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ─── Greeting ────────────────────────────────────────────────────────────
    @SuppressLint("SetTextI18n")
    public void updateGreeting(String name) {
        if (tvGreeting == null) return;
        int timeOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting = (timeOfDay < 12) ? "Good Morning"
                : (timeOfDay < 16) ? "Good Afternoon"
                : (timeOfDay < 21) ? "Good Evening"
                : "Good Night";
        tvGreeting.setText(greeting + ",\n" + name);
    }

    // ─── Init Views ──────────────────────────────────────────────────────────
    private void initViews() {
        tvGreeting = findViewById(R.id.tvGreeting);
        tvWalletBalance = findViewById(R.id.tvWalletBalance);
        tvSalesAmount = findViewById(R.id.tvSalesAmount);
        tvViewAllCategories = findViewById(R.id.tvViewAllCategories);
        ivVisibility = findViewById(R.id.ivVisibility);
        btnProfile = findViewById(R.id.btnProfile);
        layoutCategoriesHorizontal = findViewById(R.id.layoutCategoriesHorizontal);
        layoutCategoriesExpanded = findViewById(R.id.layoutCategoriesExpanded);
        tvTodaysSalesLabel = findViewById(R.id.tvTodaysSalesLabel);
        tvRatingValue = findViewById(R.id.tvRatingValue);

        if (tvTodaysSalesLabel != null) {
            tvTodaysSalesLabel.setText("Today's Expense:");
        }

        if (tvWalletBalance == null || tvSalesAmount == null) {
            Log.e(TAG, "Critical wallet views not found — check XML IDs");
        }
    }

    // ─── Listeners ───────────────────────────────────────────────────────────
    private void setupListeners() {
        // Toggle Wallet Balance Visibility
        if (ivVisibility != null) {
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
        }

        // Toggle Categories View
        if (tvViewAllCategories != null && layoutCategoriesExpanded != null
                && layoutCategoriesHorizontal != null) {
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

        // Setup individual category clicks
        setupCategoryClickListeners(layoutCategoriesHorizontal);
        setupCategoryClickListeners(layoutCategoriesExpanded);

        // Profile Navigation
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileActivity.class)));
        }

        // Refresh Wallet — ivRefresh is a RelativeLayout in XML
        View ivRefresh = findViewById(R.id.ivRefresh);
        if (ivRefresh != null) {
            ivRefresh.setOnClickListener(v -> refreshWalletUI());
        }

        // Add Money & Withdraw
        findViewById(R.id.btnAddMoney).setOnClickListener(v -> 
                Toast.makeText(this, "Add Money feature coming soon", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnWithdrawMoney).setOnClickListener(v -> 
                Toast.makeText(this, "Withdraw feature coming soon", Toast.LENGTH_SHORT).show());

        // Bottom Navigation listener
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.navigation_home) {
                    fetchDashboardData();
                    return true;
                } else if (id == R.id.navigation_bazar) {
                    startActivity(new Intent(this, BazarActivity.class));
                    return true;
                } else if (id == R.id.navigation_orders) {
                    startActivity(new Intent(this, OrdersActivity.class));
                    return true;
                } else if (id == R.id.navigation_more) {
                    Intent intent = new Intent(ConsumerDashboardActivity.this, SettingsActivity.class);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        }
    }

    private void setupCategoryClickListeners(ViewGroup layout) {
        if (layout == null) return;
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof LinearLayout) {
                child.setOnClickListener(v -> {
                    // Assuming second child is the category text
                    if (((LinearLayout) v).getChildCount() > 1) {
                        View textChild = ((LinearLayout) v).getChildAt(1);
                        if (textChild instanceof TextView) {
                            Toast.makeText(ConsumerDashboardActivity.this,
                                    "Catagory clicked", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    }
}
