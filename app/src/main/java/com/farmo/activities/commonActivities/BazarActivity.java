package com.farmo.activities.commonActivities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.farmo.R;
import com.farmo.adapter.OrderAdapter;
import com.farmo.network.CommonServices.BazarModuleService;
import com.farmo.network.RetrofitClient;
import com.farmo.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BazarActivity extends AppCompatActivity {

    private static final String TAG       = "BazarActivity";

    // ── Views ─────────────────────────────────────────────────────────────
    private RecyclerView  recyclerView;
    private Button        btnShowMore;
    private ImageView     btnBack, btnSearchAction;
    private Spinner       spinnerSort, spinnerFarmer;
    private ProgressBar   progressBar;
    private EditText      etSearch;

    // ── Data ──────────────────────────────────────────────────────────────
    private OrderAdapter.BazarProductAdapter adapter;
    private final List<BazarModuleService.BazarProduct> displayedProducts = new ArrayList<>();

    private int     currentPage   = 1;
    private boolean spinnersReady = false;
    private boolean hasNextPage   = false;
    private boolean isLoading     = false;
    private SessionManager sessionManager;

    // ─────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bazar);

        sessionManager = new SessionManager(this);

        if (!initViews()) return;

        setupSpinners();
        setupSearch();
        spinnersReady = true;

        // Load first page
        loadBazarProducts(1, getSelectedFilter(), "");
    }

    private boolean initViews() {
        btnBack          = findViewById(R.id.btnBack);
        btnShowMore      = findViewById(R.id.btnShowMore);
        recyclerView     = findViewById(R.id.recyclerView);
        spinnerSort      = findViewById(R.id.spinnerSort);
        spinnerFarmer    = findViewById(R.id.spinnerFarmer);
        progressBar      = findViewById(R.id.progressBar);
        etSearch         = findViewById(R.id.etSearchBazar);
        btnSearchAction  = findViewById(R.id.btnSearchAction);

        if (btnBack == null || btnShowMore == null || recyclerView == null || spinnerFarmer == null || progressBar == null || spinnerSort == null || btnSearchAction == null) {
            showErrorAndFinish("Required views not found in layout");
            return false;
        }

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new OrderAdapter.BazarProductAdapter(this, displayedProducts);
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnShowMore.setOnClickListener(v -> {
            if (hasNextPage && !isLoading) {
                loadBazarProducts(currentPage + 1, getSelectedFilter(), etSearch.getText().toString());
            }
        });

        btnSearchAction.setOnClickListener(v -> resetAndLoad());

        return true;
    }

    private void setupSpinners() {
        String[] sortOptions   = {"Best Match", "Price: Low to High"};
        ArrayAdapter<String> sAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, sortOptions);
        sAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(sAdapter);

        String[] farmerOptions = {"all", "conectiononly"};
        ArrayAdapter<String> fAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, farmerOptions);
        fAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFarmer.setAdapter(fAdapter);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (spinnersReady) {
                    resetAndLoad();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerSort.setOnItemSelectedListener(listener);
        spinnerFarmer.setOnItemSelectedListener(listener);
    }

    private void setupSearch() {
        if (etSearch == null) return;
        
        // Optional: Trigger search on keyboard 'Search' action
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            resetAndLoad();
            return true;
        });
    }

    private void resetAndLoad() {
        currentPage = 1;
        displayedProducts.clear();
        adapter.notifyDataSetChanged();
        loadBazarProducts(1, getSelectedFilter(), etSearch.getText().toString());
    }

    private String getSelectedFilter() {
        return spinnerFarmer.getSelectedItem().toString();
    }

    private void loadBazarProducts(int page, String filter, String searchTerm) {
        if (isLoading) return;
        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        
        String token = sessionManager.getAuthToken();
        String userId = sessionManager.getUserId();

        BazarModuleService.BazarRequest request = new BazarModuleService.BazarRequest(page, filter, searchTerm);
        
        RetrofitClient.getApiService(this).getBazarProducts(token, userId, request).enqueue(new Callback<BazarModuleService.BazarResponse>() {
            @Override
            public void onResponse(Call<BazarModuleService.BazarResponse> call, Response<BazarModuleService.BazarResponse> response) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    BazarModuleService.BazarResponse res = response.body();
                    currentPage = res.getPage();
                    hasNextPage = res.hasNextPage();
                    
                    List<BazarModuleService.BazarProduct> newProducts = res.getProducts();
                    if (newProducts != null && !newProducts.isEmpty()) {
                        int start = displayedProducts.size();
                        displayedProducts.addAll(newProducts);
                        adapter.notifyItemRangeInserted(start, newProducts.size());
                    } else if (page == 1) {
                         Toast.makeText(BazarActivity.this, "No products found", Toast.LENGTH_SHORT).show();
                    }
                    
                    btnShowMore.setVisibility(hasNextPage ? View.VISIBLE : View.GONE);
                } else {
                    Log.e(TAG, "Response Error: " + response.code() + " " + response.message());
                    Toast.makeText(BazarActivity.this, "Failed to load products", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BazarModuleService.BazarResponse> call, Throwable t) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "API Error: " + t.getMessage());
                Toast.makeText(BazarActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showErrorAndFinish(String message) {
        Log.e(TAG, message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }
}
