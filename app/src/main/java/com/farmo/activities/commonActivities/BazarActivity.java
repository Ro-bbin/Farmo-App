package com.farmo.activities.commonActivities;

import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.farmo.R;
import com.farmo.adapter.BazarProductAdapter;
import com.farmo.network.CommonServices.BazarModuleService;
import com.farmo.network.CommonServices.DownloadService;
import com.farmo.network.RetrofitClient;
import com.farmo.utils.SessionManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BazarActivity extends AppCompatActivity {

    private static final String TAG               = "BazarActivity";
    private static final int    PRODUCTS_PER_PAGE = 10;

    // ── Views ──────────────────────────────────────────────────────────────
    private RecyclerView recyclerView;
    private ImageView    btnBack, btnSearchAction;
    private Spinner      spinnerSort, spinnerFarmer;
    private ProgressBar  progressBar;
    private EditText     etSearch;
    private Button       btnShowMore;

    // ── State ──────────────────────────────────────────────────────────────
    private BazarProductAdapter adapter;
    private final List<BazarModuleService.BazarProduct> displayedProducts = new ArrayList<>();

    private int     currentChainId          = 0;
    private boolean hasMorePages             = false;
    private int     currentPage             = 1;

    private SessionManager sessionManager;

    // ══════════════════════════════════════════════════════════════════════
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bazar);

        sessionManager = new SessionManager(this);
        if (!initViews()) return;

        setupSpinners();
        setupSearch();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  View init
    // ══════════════════════════════════════════════════════════════════════
    private boolean initViews() {
        btnBack         = findViewById(R.id.btnBack);
        recyclerView    = findViewById(R.id.recyclerView);
        spinnerSort     = findViewById(R.id.spinnerSort);
        spinnerFarmer   = findViewById(R.id.spinnerFarmer);
        progressBar     = findViewById(R.id.progressBar);
        etSearch        = findViewById(R.id.etSearchBazar);
        btnSearchAction = findViewById(R.id.btnSearchAction);
        btnShowMore     = findViewById(R.id.btnShowMore);

        if (btnBack == null || recyclerView == null || spinnerFarmer == null
                || progressBar == null || spinnerSort == null
                || btnSearchAction == null || btnShowMore == null) {
            Toast.makeText(this, "UI init failed", Toast.LENGTH_SHORT).show();
            return false;
        }

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new BazarProductAdapter(this, displayedProducts);
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnSearchAction.setOnClickListener(v -> resetAndLoad());
        
        btnShowMore.setVisibility(View.GONE);
        btnShowMore.setOnClickListener(v -> {
            btnShowMore.setVisibility(View.GONE);
            startSequentialLoad(currentPage + 1);
        });

        return true;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Spinners
    // ══════════════════════════════════════════════════════════════════════
    private void setupSpinners() {
        String[] sortOptions   = {"Best Match", "Price: Low to High"};
        ArrayAdapter<String> sAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, sortOptions);
        sAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(sAdapter);

        String[] farmerOptions = {"all", "connectiononly"};
        ArrayAdapter<String> fAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, farmerOptions);
        fAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFarmer.setAdapter(fAdapter);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            private int lastSortPos = -1;
            private int lastFarmerPos = -1;
            private int initCount = 0;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                boolean changed = false;
                if (parent == spinnerSort) {
                    if (pos != lastSortPos) {
                        lastSortPos = pos;
                        changed = true;
                    }
                } else if (parent == spinnerFarmer) {
                    if (pos != lastFarmerPos) {
                        lastFarmerPos = pos;
                        changed = true;
                    }
                }

                if (!changed) return;

                initCount++;
                if (initCount == 2) { 
                    startSequentialLoad(1);
                } else if (initCount > 2) { 
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
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            resetAndLoad();
            return true;
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Reset
    // ══════════════════════════════════════════════════════════════════════
    private void resetAndLoad() {
        displayedProducts.clear();
        adapter.notifyDataSetChanged();
        btnShowMore.setVisibility(View.GONE);
        startSequentialLoad(1);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CORE SEQUENTIAL CHAIN
    // ══════════════════════════════════════════════════════════════════════

    private void startSequentialLoad(int page) {
        currentChainId++;
        int chainId = currentChainId;
        currentPage = page;
        progressBar.setVisibility(View.VISIBLE);
        fetchProduct(page, 1, chainId);
    }

    private void fetchProduct(int page, int serialNo, int chainId) {
        if (chainId != currentChainId) return;

        String filter     = spinnerFarmer.getSelectedItem().toString();
        String searchTerm = etSearch.getText() != null ? etSearch.getText().toString().trim() : "";

        BazarModuleService.BazarRequest request =
                new BazarModuleService.BazarRequest(page, filter, searchTerm, serialNo);

        RetrofitClient.getApiService(this)
                .getBazarProducts(sessionManager.getAuthToken(), sessionManager.getUserId(), request)
                .enqueue(new Callback<BazarModuleService.BazarResponse>() {

                    @Override
                    public void onResponse(@NonNull Call<BazarModuleService.BazarResponse> call,
                                           @NonNull Response<BazarModuleService.BazarResponse> response) {
                        if (chainId != currentChainId) return;

                        if (!response.isSuccessful() || response.body() == null) {
                            finishChain(chainId);
                            return;
                        }

                        BazarModuleService.BazarResponse res = response.body();
                        hasMorePages = res.hasNextPage();
                        List<BazarModuleService.BazarProduct> list = res.getProducts();

                        if (list == null || list.isEmpty()) {
                            finishChain(chainId);
                            return;
                        }

                        BazarModuleService.BazarProduct product = list.get(0);
                        
                        // 1. Immediately add to list with default photo
                        addToList(product, page, serialNo, chainId);
                        
                        // 2. Start background download for actual image
                        downloadActualImage(product);
                    }

                    @Override
                    public void onFailure(@NonNull Call<BazarModuleService.BazarResponse> call, @NonNull Throwable t) {
                        if (chainId != currentChainId) return;
                        finishChain(chainId);
                    }
                });
    }

    private void downloadActualImage(BazarModuleService.BazarProduct product) {
        String productId = product.getId();
        String imgUrl    = product.getImageUrl();
        int imageSeq     = product.getImageSerialNo();

        if (imgUrl == null || imgUrl.isEmpty() || imgUrl.equalsIgnoreCase("null")) return;

        File cachedFile = getLocalImageFile(productId);
        String savedUrl = sessionManager.getValue("product_img_" + productId, "");

        if (cachedFile.exists() && imgUrl.equals(savedUrl)) return;

        new DownloadService.Executor(this, RetrofitClient.getApiService(this))
                .download(
                        sessionManager.getAuthToken(),
                        sessionManager.getUserId(),
                        DownloadService.Req.product(productId, imageSeq),
                        new DownloadService.Executor.Callback() {
                            @Override
                            public void onSuccess(File file, DownloadService.Res raw) {
                                File dest = getLocalImageFile(productId);
                                if (copyFile(file, dest)) {
                                    sessionManager.saveValue("product_img_" + productId, imgUrl);
                                    runOnUiThread(() -> {
                                        int index = displayedProducts.indexOf(product);
                                        if (index != -1) adapter.notifyItemChanged(index);
                                    });
                                }
                            }
                            @Override public void onError(String error) {}
                        }
                );
    }

    private void addToList(BazarModuleService.BazarProduct product,
                           int page, int serialNo, int chainId) {
        runOnUiThread(() -> {
            if (chainId != currentChainId) return;

            displayedProducts.add(product);
            adapter.notifyItemInserted(displayedProducts.size() - 1);
            progressBar.setVisibility(View.GONE);

            // Logic: Continue fetching within same page (1-10) if more products exist
            if (hasMorePages) {
                int nextSerial = serialNo + 1;
                if (nextSerial <= PRODUCTS_PER_PAGE) {
                    // Call automatically for next serial in same page
                    fetchProduct(page, nextSerial, chainId);
                } else {
                    // End of page (10), but has more in next page. Show Load More.
                    finishChain(chainId);
                    btnShowMore.setVisibility(View.VISIBLE);
                }
            } else {
                // No more products at all
                finishChain(chainId);
                btnShowMore.setVisibility(View.GONE);
            }
        });
    }

    private void finishChain(int chainId) {
        if (chainId != currentChainId) return;
        runOnUiThread(() -> progressBar.setVisibility(View.GONE));
    }

    private File getLocalImageFile(String productId) {
        File dir = new File(getFilesDir(), "products");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "prod_" + (productId != null ? productId : "unknown") + ".jpg");
    }

    private boolean copyFile(File src, File dst) {
        try (InputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
