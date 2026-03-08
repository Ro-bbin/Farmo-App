package com.farmo.activities.farmerActivities;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.farmo.R;
import com.farmo.activities.commonActivities.ProductDetailActivity;
import com.farmo.network.ApiService;
import com.farmo.network.RetrofitClient;
import com.farmo.network.farmer.MyProductListService;
import com.farmo.network.farmer.UpdateProduct;
import com.farmo.utils.SessionManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyProductsActivity extends AppCompatActivity {

    private static final String TAG = "MyProductsActivity";
    private static final String DATE_FORMAT = "dd-MM-yyyy";

    private LinearLayout productContainer;
    private LinearLayout noDataView;
    private EditText searchEditText;
    private View btnFilter;
    private TextView tvProductCount;

    private SessionManager sessionManager;
    private ApiService apiService;

    private List<MyProductListService.Response.Product> productList = new ArrayList<>();
    
    // Request parameters
    private String currentFilter = "all";
    private int currentPage = 1;
    private String dateFrom = "";
    private String dateTo = "";
    private String searchQuery = "";
    
    private int totalPages = 1;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_products);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getApiService(this);

        initViews();
        setupSearch();
        setupFilter();
        
        fetchProducts(false);
    }

    private void initViews() {
        productContainer = findViewById(R.id.productContainer);
        noDataView = findViewById(R.id.noDataView);
        searchEditText = findViewById(R.id.dashboard_search);
        btnFilter = findViewById(R.id.btnFilter);
        tvProductCount = findViewById(R.id.tvProductCount);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabAddProduct).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddProductActivity.class);
            startActivity(intent);
        });
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim();
                currentPage = 1;
                fetchProducts(false);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilter() {
        btnFilter.setOnClickListener(v -> showFilterDialog());
    }

    private void showFilterDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_filter);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        RadioGroup rgFilter = dialog.findViewById(R.id.rgFilter);
        TextView tvDateFrom = dialog.findViewById(R.id.tvDateFrom);
        TextView tvDateTo = dialog.findViewById(R.id.tvDateTo);
        Button btnApply = dialog.findViewById(R.id.btnApplyFilter);

        // Set current values
        switch (currentFilter) {
            case "available": rgFilter.check(R.id.rbAvailable); break;
            case "not-available": rgFilter.check(R.id.rbNotAvailable); break;
            case "expired": rgFilter.check(R.id.rbExpired); break;
            case "deleted": rgFilter.check(R.id.rbDeleted); break;
            default: rgFilter.check(R.id.rbAll); break;
        }
        
        if (!dateFrom.isEmpty()) tvDateFrom.setText(dateFrom);
        if (!dateTo.isEmpty()) tvDateTo.setText(dateTo);

        tvDateFrom.setOnClickListener(v -> showDatePicker(tvDateFrom));
        tvDateTo.setOnClickListener(v -> showDatePicker(tvDateTo));

        btnApply.setOnClickListener(v -> {
            int checkedId = rgFilter.getCheckedRadioButtonId();
            if (checkedId == R.id.rbAvailable) currentFilter = "available";
            else if (checkedId == R.id.rbNotAvailable) currentFilter = "not-available";
            else if (checkedId == R.id.rbExpired) currentFilter = "expired";
            else if (checkedId == R.id.rbDeleted) currentFilter = "deleted";
            else currentFilter = "all";

            String dFrom = tvDateFrom.getText().toString().trim();
            String dTo = tvDateTo.getText().toString().trim();

            if (!dFrom.isEmpty() && !dTo.isEmpty()) {
                if (!validateDateRange(dFrom, dTo)) {
                    Toast.makeText(this, "Date range cannot exceed 125 days", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            dateFrom = dFrom.contains("dd-mm-yyyy") ? "" : dFrom;
            dateTo = dTo.contains("dd-mm-yyyy") ? "" : dTo;

            currentPage = 1;
            fetchProducts(false);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showDatePicker(TextView target) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.US, "%02d-%02d-%04d", dayOfMonth, month + 1, year);
            target.setText(date);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private boolean validateDateRange(String start, String end) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT, Locale.US);
        try {
            Date d1 = sdf.parse(start);
            Date d2 = sdf.parse(end);
            if (d1 == null || d2 == null) return true;
            long diff = d2.getTime() - d1.getTime();
            long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
            return days <= 125 && days >= 0;
        } catch (ParseException e) {
            return false;
        }
    }

    private void fetchProducts(boolean isLoadMore) {
        if (isLoading) return;
        isLoading = true;

        String token = sessionManager.getAuthToken();
        String userId = sessionManager.getUserId();

        MyProductListService.Request request = new MyProductListService.Request(
                currentFilter,
                currentPage,
                searchQuery.isEmpty() ? null : searchQuery,
                dateFrom.isEmpty() ? null : dateFrom,
                dateTo.isEmpty() ? null : dateTo,
                null // sortBy
        );

        apiService.getMyProductList(token, userId, request).enqueue(new Callback<MyProductListService.Response>() {
            @Override
            public void onResponse(@NonNull Call<MyProductListService.Response> call, @NonNull Response<MyProductListService.Response> response) {
                isLoading = false;
                if (response.isSuccessful() && response.body() != null) {
                    MyProductListService.Response res = response.body();
                    totalPages = res.getTotalPages();
                    currentPage = res.getCurrentPage();
                    
                    if (tvProductCount != null) {
                        tvProductCount.setText("Products " + res.getTotalProducts() + " are in list.");
                    }

                    if (!isLoadMore) {
                        productList.clear();
                        productContainer.removeAllViews();
                    }
                    
                    List<MyProductListService.Response.Product> newProducts = res.getProducts();
                    if (newProducts != null) {
                        productList.addAll(newProducts);
                        for (MyProductListService.Response.Product p : newProducts) {
                            addProductCard(p);
                        }
                    }
                    
                    updateUI();
                    
                    if (currentPage < totalPages) {
                        addLoadMoreButton();
                    }
                } else {
                    Toast.makeText(MyProductsActivity.this, "Failed to load products", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<MyProductListService.Response> call, @NonNull Throwable t) {
                isLoading = false;
                Toast.makeText(MyProductsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        if (productList.isEmpty()) {
            productContainer.setVisibility(View.GONE);
            noDataView.setVisibility(View.VISIBLE);
        } else {
            productContainer.setVisibility(View.VISIBLE);
            noDataView.setVisibility(View.GONE);
        }
    }

    private void addProductCard(MyProductListService.Response.Product product) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_product_card, productContainer, false);

        ((TextView) v.findViewById(R.id.tvProductName)).setText(product.getName());
        ((TextView) v.findViewById(R.id.tvStock)).setText(product.getQuantityAvailable() + " remaining");
        ((TextView) v.findViewById(R.id.tvPrice)).setText("Rs. " + product.getCostPerUnit() + " / unit");

        TextView tvStatus = v.findViewById(R.id.tvStatus);
        applyBadgeStyle(tvStatus, product.getProductStatus());

        SwitchCompat switchAvailability = v.findViewById(R.id.switchAvailability);
        String status = product.getProductStatus() != null ? product.getProductStatus().toLowerCase() : "";

        if (status.equals("available") || status.equals("not-available")) {
            switchAvailability.setVisibility(View.VISIBLE);
            switchAvailability.setChecked(status.equals("available"));
            switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
                String action = isChecked ? "available" : "not-available";
                toggleProductAvailability(product.getPId(), action);
            });
        } else {
            switchAvailability.setVisibility(View.GONE);
        }

        // Hide Edit and View buttons as requested in previous turn
        v.findViewById(R.id.ivEdit).setVisibility(View.GONE);
        v.findViewById(R.id.ivArrow).setVisibility(View.GONE);

        // Make whole card clickable
        v.setOnClickListener(b -> {
            Intent intent = new Intent(MyProductsActivity.this, ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.getPId());
            startActivity(intent);
        });

        productContainer.addView(v);
    }

    private void toggleProductAvailability(String productId, String action) {
        apiService.toggleAvailability(sessionManager.getAuthToken(), sessionManager.getUserId(), 
                new UpdateProduct.ToggleAvailabilityRequest(productId, action))
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(MyProductsActivity.this, "Status updated to " + action, Toast.LENGTH_SHORT).show();
                            currentPage = 1;
                            fetchProducts(false); // Refresh list
                        } else {
                            Toast.makeText(MyProductsActivity.this, "Failed to update status", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        Toast.makeText(MyProductsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void applyBadgeStyle(TextView badge, String status) {
        if (status == null) status = "";
        switch (status.toLowerCase()) {
            case "available":
                badge.setText("Available");
                badge.setBackgroundResource(R.drawable.bg_status_active);
                break;
            case "not-available":
                badge.setText("Not Available");
                badge.setBackgroundResource(R.drawable.bg_status_inactive);
                break;
            case "sold":
                badge.setText("Sold");
                badge.setBackgroundResource(R.drawable.bg_status_out_of_stock);
                break;
            default:
                badge.setText(status);
                badge.setBackgroundResource(R.drawable.bg_status_inactive);
                break;
        }
        badge.setTextColor(Color.WHITE);
    }

    private void addLoadMoreButton() {
        // Remove existing load more if any
        View oldBtn = productContainer.findViewWithTag("load_more");
        if (oldBtn != null) productContainer.removeView(oldBtn);

        Button btnLoadMore = new Button(this);
        btnLoadMore.setText("Load More");
        btnLoadMore.setTag("load_more");
        btnLoadMore.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#00473B")));
        btnLoadMore.setTextColor(Color.WHITE);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(40, 20, 40, 40);
        btnLoadMore.setLayoutParams(params);

        btnLoadMore.setOnClickListener(v -> {
            currentPage++;
            productContainer.removeView(btnLoadMore);
            fetchProducts(true);
        });

        productContainer.addView(btnLoadMore);
    }
}
