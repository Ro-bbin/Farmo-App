package com.farmo.activities.commonActivities;

import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.viewpager2.widget.ViewPager2;

import com.farmo.R;
import com.farmo.adapter.ImageSliderAdapter;
import com.farmo.network.CommonServices.ProductDetailsServices;
import com.farmo.network.RetrofitClient;
import com.farmo.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends AppCompatActivity {

    private static final String TAG = "ProductDetail";

    // ── Main screen views ─────────────────────────────────────────────────────
    private ProgressBar      progressBar;
    private NestedScrollView layoutContent;
    private TextView         tvProductName, tvPrice, tvOldPrice, tvUnit, tvDescription;
    private TextView         tvSoldCount, tvAvailableQty, tvRatingNum, tvRatingCount, tvImageCounter;
    private TextView         tvCategory, tvMinOrder, tvType, tvHarvestDate, tvOrigin;
    private TextView         tvFarmerName, tvFarmerLocation;
    private TextView         tvReadMore;
    private ImageView        btnBack;
    private ViewPager2       viewPagerImages;
    private Button           btnReqestOrder;
    private RatingBar        ratingBar;

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean isDescriptionExpanded = false;
    private int     orderQty              = 1;
    private double  loadedFinalPrice      = 0.0;
    private String  loadedUnit            = "unit";
    private String  productId;
    private SessionManager sessionManager;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);
        
        productId = getIntent().getStringExtra("PRODUCT_ID");
        sessionManager = new SessionManager(this);

        if (productId == null) {
            Toast.makeText(this, "Product ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupClickListeners();
        fetchProductDetail();
    }

    private void initViews() {
        progressBar      = findViewById(R.id.progressBar);
        layoutContent    = findViewById(R.id.layoutContent);
        tvProductName    = findViewById(R.id.tvProductName);
        tvPrice          = findViewById(R.id.tvPrice);
        tvOldPrice       = findViewById(R.id.tvOldPrice);
        tvUnit           = findViewById(R.id.tvUnit);
        tvDescription    = findViewById(R.id.tvDescription);
        tvSoldCount      = findViewById(R.id.tvSoldCount);
        tvAvailableQty   = findViewById(R.id.tvAvailableQty);
        tvRatingNum      = findViewById(R.id.tvRatingNum);
        tvRatingCount    = findViewById(R.id.tvRatingCount);
        tvCategory       = findViewById(R.id.tvCategory);
        tvMinOrder       = findViewById(R.id.tvMinOrder);
        tvType           = findViewById(R.id.tvType);
        tvHarvestDate    = findViewById(R.id.tvHarvestDate);
        tvOrigin         = findViewById(R.id.tvOrigin);
        tvFarmerName     = findViewById(R.id.tvFarmerName);
        tvFarmerLocation = findViewById(R.id.tvFarmerLocation);
        viewPagerImages  = findViewById(R.id.viewPagerImages);
        btnBack          = findViewById(R.id.btnBack);
        btnReqestOrder   = findViewById(R.id.btnReqestOrder);
        ratingBar        = findViewById(R.id.ratingBar);
        tvReadMore       = findViewById(R.id.tvReadMore);
        tvImageCounter   = findViewById(R.id.tvImageCounter);

        if (tvOldPrice != null) {
            tvOldPrice.setPaintFlags(tvOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
    }

    private void setupClickListeners() {
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (tvReadMore != null) {
            tvReadMore.setOnClickListener(v -> {
                if (!isDescriptionExpanded) {
                    tvDescription.setMaxLines(Integer.MAX_VALUE);
                    tvReadMore.setText("Read less ▲");
                    isDescriptionExpanded = true;
                } else {
                    tvDescription.setMaxLines(4);
                    tvReadMore.setText("Read more ▼");
                    isDescriptionExpanded = false;
                }
            });
        }
        if (btnReqestOrder != null) btnReqestOrder.setOnClickListener(v -> showOrderPopup());
    }

    private void fetchProductDetail() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (layoutContent != null) layoutContent.setVisibility(View.GONE);

        ProductDetailsServices.Request request = new ProductDetailsServices.Request(productId);

        RetrofitClient.getApiService(this)
                .getProductDetails(sessionManager.getAuthToken(), sessionManager.getUserId(), request)
                .enqueue(new Callback<ProductDetailsServices.Response>() {
                    @Override
                    public void onResponse(@NonNull Call<ProductDetailsServices.Response> call, @NonNull Response<ProductDetailsServices.Response> response) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            if (layoutContent != null) layoutContent.setVisibility(View.VISIBLE);
                            updateUI(response.body());
                        } else {
                            Toast.makeText(ProductDetailActivity.this, "Failed to load details", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ProductDetailsServices.Response> call, @NonNull Throwable t) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(ProductDetailActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUI(ProductDetailsServices.Response p) {
        if (tvProductName != null) tvProductName.setText(p.getName());
        if (tvCategory != null) tvCategory.setText(p.getProductType());
        if (tvDescription != null) tvDescription.setText(p.getDescription());
        if (tvFarmerName != null) tvFarmerName.setText(p.getFarmerName());
        if (tvFarmerLocation != null) tvFarmerLocation.setText("📍 " + p.getFarmerLocation());

        try {
            double cost = Double.parseDouble(p.getCostPerUnit() != null ? p.getCostPerUnit() : "0");
            double discountVal = Double.parseDouble(p.getDiscountValue() != null ? p.getDiscountValue() : "0");
            String discountType = p.getDiscountType() != null ? p.getDiscountType() : "Percentage";

            double finalPrice = discountType.equalsIgnoreCase("Fixed") || discountType.equalsIgnoreCase("Flat")
                    ? cost - discountVal
                    : cost - (cost * discountVal / 100.0);
            finalPrice = Math.max(finalPrice, 0);

            loadedFinalPrice = finalPrice;
            loadedUnit = p.getUnit() != null ? p.getUnit() : "unit";

            if (tvPrice != null) tvPrice.setText(String.format("Rs. %.0f", finalPrice));
            if (tvOldPrice != null) {
                tvOldPrice.setText(String.format("Rs. %.0f", cost));
                tvOldPrice.setVisibility(discountVal > 0 ? View.VISIBLE : View.GONE);
            }
            if (tvUnit != null) tvUnit.setText("/ " + loadedUnit);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing price: " + e.getMessage());
        }

        if (tvType != null) tvType.setText(p.isOrganic() ? "Organic" : "Conventional");
        if (tvAvailableQty != null) tvAvailableQty.setText(p.isInStock() ? "In Stock" : "Out of Stock");
        if (tvHarvestDate != null) tvHarvestDate.setText(p.getProducedDate());
        if (tvOrigin != null) tvOrigin.setText(p.getFarmerLocation());

        if (ratingBar != null) ratingBar.setRating(p.getRating());
        if (tvRatingNum != null) tvRatingNum.setText(String.valueOf(p.getRating()));
        if (tvRatingCount != null) tvRatingCount.setText("(" + p.getRatingCount() + " reviews)");
        if (tvSoldCount != null) tvSoldCount.setText(p.getSoldCount() + " sold");

        // Use a placeholder if media is not provided in ProductDetailsServices.Response
        List<String> images = new ArrayList<>();
        images.add("");
        ImageSliderAdapter adapter = new ImageSliderAdapter(images);
        if (viewPagerImages != null) {
            viewPagerImages.setAdapter(adapter);
            if (tvImageCounter != null) tvImageCounter.setText("1 / " + images.size());
            viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override public void onPageSelected(int position) {
                    if (tvImageCounter != null) tvImageCounter.setText((position + 1) + " / " + images.size());
                }
            });
        }
    }

    private void showOrderPopup() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View popupView = inflater.inflate(R.layout.popup_order_request, null);
        AlertDialog orderDialog = new AlertDialog.Builder(this, R.style.RoundedDialog).setView(popupView).create();

        ImageButton btnClose = popupView.findViewById(R.id.btnClosePopup);
        View btnIncrease = popupView.findViewById(R.id.btnIncrease);
        View btnDecrease = popupView.findViewById(R.id.btnDecrease);
        TextView tvQuantity = popupView.findViewById(R.id.tvQuantity);
        TextView tvTotalCost = popupView.findViewById(R.id.tvTotalCost);
        Button btnOrderRequest = popupView.findViewById(R.id.btnOrderRequest);

        orderQty = 1;
        if (tvQuantity != null) tvQuantity.setText(String.valueOf(orderQty));
        if (tvTotalCost != null) tvTotalCost.setText(String.format("Rs. %.0f", loadedFinalPrice * orderQty));

        if (btnIncrease != null) btnIncrease.setOnClickListener(v -> { 
            orderQty++; 
            if (tvQuantity != null) tvQuantity.setText(String.valueOf(orderQty)); 
            if (tvTotalCost != null) tvTotalCost.setText(String.format("Rs. %.0f", loadedFinalPrice * orderQty)); 
        });
        if (btnDecrease != null) btnDecrease.setOnClickListener(v -> { 
            if (orderQty > 1) { 
                orderQty--; 
                if (tvQuantity != null) tvQuantity.setText(String.valueOf(orderQty)); 
                if (tvTotalCost != null) tvTotalCost.setText(String.format("Rs. %.0f", loadedFinalPrice * orderQty)); 
            } 
        });
        if (btnClose != null) btnClose.setOnClickListener(v -> orderDialog.dismiss());
        if (btnOrderRequest != null) btnOrderRequest.setOnClickListener(v -> { 
            Toast.makeText(this, "Order requested!", Toast.LENGTH_SHORT).show(); 
            orderDialog.dismiss(); 
        });

        orderDialog.show();
    }
}
