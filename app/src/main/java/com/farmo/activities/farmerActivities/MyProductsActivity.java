package com.farmo.activities.farmerActivities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.farmo.R;
import com.farmo.network.CommonServices.MyProductService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MyProductsActivity extends AppCompatActivity {

    private static final String TAG       = "MyProductsActivity";
    private static final String JSON_FILE = "products.json";

    // ── UI ────────────────────────────────────────────────────────────────────
    private LinearLayout productContainer;
    private LinearLayout noDataView;
    private TextView     tabAllProducts;
    private TextView     tabVegetables;
    private TextView     tabGrains;


    // ── DATA ──────────────────────────────────────────────────────────────────
    private final List<MyProductService> allProducts = new ArrayList<>();

    // =========================================================================
    //  LIFECYCLE
    // =========================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_products);

        initViews();
        setupTopBar();
        setupFab();
        loadFromJson();
    }

    // =========================================================================
    //  INIT
    // =========================================================================
    private void initViews() {
        productContainer = findViewById(R.id.productContainer);
        noDataView       = findViewById(R.id.noDataView);


        findViewById(R.id.fabAddProduct).setOnClickListener(v ->{
            Intent intent = new Intent(MyProductsActivity.this, AddProductActivity.class);
            startActivity(intent);
        });

    }

    private void setupTopBar() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }



    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fabAddProduct);
        if (fab != null) fab.setOnClickListener(v ->
                Toast.makeText(this, "Add Product — coming soon", Toast.LENGTH_SHORT).show());
    }

    private void selectTab(TextView selected, String category) {
        for (TextView t : new TextView[]{tabAllProducts, tabVegetables, tabGrains}) {
            if (t == null) continue;
            if (t == selected) {
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(Color.parseColor("#C8E6C9"));
                bg.setCornerRadius(16f);
                t.setBackground(bg);
                t.setTextColor(Color.parseColor("#00473B"));
                t.setTypeface(null, Typeface.BOLD);
            } else {
                t.setBackgroundColor(Color.TRANSPARENT);
                t.setTextColor(Color.parseColor("#546E7A"));
                t.setTypeface(null, Typeface.NORMAL);
            }
        }
        applyFilter(category);
    }

    // =========================================================================
    //  LOAD FROM JSON
    // =========================================================================
    private void loadFromJson() {
        showLoading(true);

        try {
            String[] list = getAssets().list("");
            Log.d(TAG, "=== ASSETS ===");
            for (String f : list) Log.d(TAG, "  [" + f + "]");
            Log.d(TAG, "==============");
        } catch (Exception e) {
            Log.e(TAG, "Cannot list assets: " + e.getMessage());
        }

        try {
            InputStream    is = getAssets().open(JSON_FILE);
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder  sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            br.close();
            is.close();

            String json = sb.toString().trim();
            if (json.isEmpty()) { onDataError("products.json is empty"); return; }

            Log.d(TAG, "Read OK — length=" + json.length());
            onDataLoaded(json);

        } catch (Exception e) {
            Log.e(TAG, "Load failed: " + e.getMessage());
            onDataError("Cannot open " + JSON_FILE + "\n" + e.getMessage());
        }
    }

    // =========================================================================
    //  SHARED PIPELINE
    // =========================================================================
    private void onDataLoaded(String json) {
        showLoading(false);
        allProducts.clear();
        allProducts.addAll(parseProducts(json));

        if (allProducts.isEmpty()) {
            Log.w(TAG, "0 products parsed");
            showNoData();
        } else {
            Log.d(TAG, "Loaded " + allProducts.size() + " products");
            if (tabAllProducts != null) selectTab(tabAllProducts, "All Products");
            else applyFilter("All Products");
        }
    }

    private void onDataError(String msg) {
        showLoading(false);
        Log.e(TAG, msg);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        showNoData();
    }

    // =========================================================================
    //  PARSE JSON → List<Product>
    // =========================================================================
    private List<MyProductService> parseProducts(String json) {
        List<MyProductService> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            Log.d(TAG, "JSON count: " + arr.length());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                MyProductService p    = new MyProductService();
                p.id         = o.optString("id",        String.valueOf(i));
                p.name       = o.optString("name",      "Unnamed");
                p.category   = o.optString("category",  "Other");
                p.status     = o.optString("status",    "inactive");
                p.price      = o.optString("price",     "0");
                p.priceUnit  = o.optString("priceUnit", "kg");
                p.stock      = o.optString("stock",     "0");
                p.stockUnit  = o.optString("stockUnit", "kg");
                p.image      = o.optString("image",     "");
                p.isActive   = p.status.equalsIgnoreCase("active");
                Log.d(TAG, "  [" + i + "] " + p.name + " | " + p.category + " | " + p.status);
                out.add(p);
            }
        } catch (Exception e) {
            Log.e(TAG, "Parse error: " + e.getMessage());
        }
        return out;
    }

    // =========================================================================
    //  FILTER → RENDER (no headers — cards only)
    // =========================================================================
    private void applyFilter(String category) {
        if (productContainer == null) return;
        productContainer.removeAllViews();

        boolean hasData = false;
        for (MyProductService p : allProducts) {
            boolean match = category.equalsIgnoreCase("All Products")
                    || p.category.equalsIgnoreCase(category);
            if (!match) continue;
            addProductCard(p);
            hasData = true;
        }

        if (hasData) {
            productContainer.setVisibility(View.VISIBLE);
            if (noDataView != null) noDataView.setVisibility(View.GONE);
        } else {
            showNoData();
        }
    }

    // =========================================================================
    //  PRODUCT CARD  →  res/layout/item_product_card.xml
    //
    //  ┌──────────────────────────────────────────────────────┐
    //  │  [IMG]  Organic Red Tomatoes       [✏]    [>>]      │
    //  │         [45 kg remaining] [Active]                   │
    //  │         Rs. 120 / kg                                │
    //  └──────────────────────────────────────────────────────┘
    // =========================================================================
    private void addProductCard(MyProductService product) {
        View v = LayoutInflater.from(this)
                .inflate(R.layout.item_product_card, productContainer, false);

        ((TextView) v.findViewById(R.id.tvProductName))
                .setText(product.name);
        ((TextView) v.findViewById(R.id.tvStock))
                .setText(product.stock + " " + product.stockUnit + " remaining");
        ((TextView) v.findViewById(R.id.tvPrice))
                .setText("Rs. " + product.price + " / " + product.priceUnit);

        TextView tvStatus = v.findViewById(R.id.tvStatus);
        applyBadgeStyle(tvStatus, product.status);

        // Tap to toggle Active ↔ Off  (Out of Stock is locked)
        tvStatus.setOnClickListener(badge -> {
            if (product.status.toLowerCase().contains("out")) {
                Toast.makeText(this,
                        product.name + " is Out of Stock — update stock first",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            product.isActive = !product.isActive;
            product.status   = product.isActive ? "active" : "inactive";
            applyBadgeStyle(tvStatus, product.status);
            Log.d(TAG, "Toggle: " + product.name + " → " + product.status);
        });

        v.findViewById(R.id.ivEdit).setOnClickListener(b ->
                Toast.makeText(this, "Edit: " + product.name, Toast.LENGTH_SHORT).show());

        v.findViewById(R.id.ivArrow).setOnClickListener(b ->
                Toast.makeText(this, "View: " + product.name, Toast.LENGTH_SHORT).show());

        productContainer.addView(v);
    }

    // =========================================================================
    //  STATUS BADGE
    // =========================================================================
    private void applyBadgeStyle(TextView badge, String status) {
        switch (status.toLowerCase()) {
            case "active":
                badge.setText("Active");
                badge.setTextColor(Color.WHITE);
                badge.setBackgroundResource(R.drawable.bg_status_active);
                break;
            case "out_of_stock":
            case "out of stock":
                badge.setText("Out of Stock");
                badge.setTextColor(Color.WHITE);
                badge.setBackgroundResource(R.drawable.bg_status_out_of_stock);
                break;
            default:
                badge.setText("Off");
                badge.setTextColor(Color.WHITE);
                badge.setBackgroundResource(R.drawable.bg_status_inactive);
                break;
        }
    }

    // =========================================================================
    //  HELPERS
    // =========================================================================
    private void showLoading(boolean show) { }

    private void showNoData() {
        if (productContainer != null) productContainer.setVisibility(View.GONE);
        if (noDataView != null)       noDataView.setVisibility(View.VISIBLE);
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
