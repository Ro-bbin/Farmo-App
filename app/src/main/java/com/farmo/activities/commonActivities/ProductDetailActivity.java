package com.farmo.activities.commonActivities;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.farmo.R;
import com.farmo.adapter.ImageSliderAdapter;
import com.farmo.network.RetrofitClient;
import com.farmo.network.CommonServices.AddressService;
import com.farmo.network.CommonServices.DownloadService;
import com.farmo.network.CommonServices.OrderRequestService;
import com.farmo.network.CommonServices.ProductDetailsServices;
import com.farmo.network.farmer.AddProductService;
import com.farmo.network.farmer.UpdateProduct;
import com.farmo.utils.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends AppCompatActivity {
    private static final String TAG = "ProductDetailActivity";
    private String productId;
    private SessionManager sessionManager;
    private ProgressBar progressBar;
    private View layoutContent, layoutBottomBar, layoutFarmerSection, layoutOrigin, layoutDeliveryReturns;
    private TextView tvProductName, tvCategory, tvDescription, tvFarmerName, tvFarmerLocation, tvPrice, tvOldPrice, tvUnit, tvDiscountBadge, tvDeliveryStatus, tvType, tvAvailableQty, tvHarvestDate, tvOrigin, tvExpiryDate, tvRatingNum, tvRatingCount, tvSoldCount, tvImageCounter;
    private Button btnReviewIt, btnReqestOrder, btnEditProduct;
    private RatingBar ratingBar;
    private ViewPager2 viewPagerImages;
    private ImageSliderAdapter imageAdapter;
    private List<File> productImages = new ArrayList<>();

    private double originalPriceVal = 0.0;
    private double loadedFinalPrice = 0.0;
    private String currentDiscountType = "None";
    private String currentDiscountValue = "0";
    private String loadedUnit = "unit";
    private double orderQty = 1.0;

    private List<AddProductService.ProductType> productTypeData = new ArrayList<>();
    private List<String> editSelectedKeywords = new ArrayList<>();
    private String currentExpiryDateEdit = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        productId = getIntent().getStringExtra("PRODUCT_ID");
        sessionManager = new SessionManager(this);

        initViews();
        fetchProductDetail();

        if (btnEditProduct != null) btnEditProduct.setOnClickListener(v -> showEditProductPopup());
        if (btnReqestOrder != null) btnReqestOrder.setOnClickListener(v -> showOrderPopup());
        if (btnReviewIt != null) btnReviewIt.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReviewActivity.class);
            intent.putExtra("PRODUCT_ID", productId);
            startActivity(intent);
        });
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        layoutContent = findViewById(R.id.layoutContent);
        layoutBottomBar = findViewById(R.id.layoutBottomBar);
        layoutFarmerSection = findViewById(R.id.layoutFarmerSection);
        layoutOrigin = findViewById(R.id.layoutOrigin);
        layoutDeliveryReturns = findViewById(R.id.layoutDeliveryReturns);

        tvProductName = findViewById(R.id.tvProductName);
        tvCategory = findViewById(R.id.tvCategory);
        tvDescription = findViewById(R.id.tvDescription);
        tvFarmerName = findViewById(R.id.tvFarmerName);
        tvFarmerLocation = findViewById(R.id.tvFarmerLocation);
        tvPrice = findViewById(R.id.tvPrice);
        tvOldPrice = findViewById(R.id.tvOldPrice);
        tvUnit = findViewById(R.id.tvUnit);
        tvDiscountBadge = findViewById(R.id.tvDiscountBadge);
        tvDeliveryStatus = findViewById(R.id.tvDeliveryStatus);
        tvType = findViewById(R.id.tvType);
        tvAvailableQty = findViewById(R.id.tvAvailableQty);
        tvHarvestDate = findViewById(R.id.tvHarvestDate);
        tvOrigin = findViewById(R.id.tvOrigin);
        tvExpiryDate = findViewById(R.id.tvExpiryDate);
        tvRatingNum = findViewById(R.id.tvRatingNum);
        tvRatingCount = findViewById(R.id.tvRatingCount);
        tvSoldCount = findViewById(R.id.tvSoldCount);
        tvImageCounter = findViewById(R.id.tvImageCounter);

        btnReviewIt = findViewById(R.id.btnReviewIt);
        btnReqestOrder = findViewById(R.id.btnReqestOrder);
        btnEditProduct = findViewById(R.id.btnEditProduct);

        ratingBar = findViewById(R.id.ratingBar);
        viewPagerImages = findViewById(R.id.viewPagerImages);

        imageAdapter = new ImageSliderAdapter(productImages);
        if (viewPagerImages != null) viewPagerImages.setAdapter(imageAdapter);
    }

    private void showEditProductPopup() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        RetrofitClient.getApiService(this)
                .getDetails_before_UpdateProduct(sessionManager.getAuthToken(), sessionManager.getUserId(), productId)
                .enqueue(new Callback<UpdateProduct.Request>() {
                    @Override
                    public void onResponse(@NonNull Call<UpdateProduct.Request> call, @NonNull Response<UpdateProduct.Request> response) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            openEditDialog(response.body());
                        } else {
                            Toast.makeText(ProductDetailActivity.this, "Failed to load current details for update", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<UpdateProduct.Request> call, @NonNull Throwable t) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(ProductDetailActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openEditDialog(UpdateProduct.Request reqData) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View popupView = inflater.inflate(R.layout.popup_edit_product, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.RoundedDialog).setView(popupView).create();

        EditText etName = popupView.findViewById(R.id.etEditProductName);
        AutoCompleteTextView actvCategory = popupView.findViewById(R.id.autoCompleteEditProductType);
        AutoCompleteTextView actvKeywordSearch = popupView.findViewById(R.id.autoCompleteEditKeywordSearch);
        ImageButton btnAddKeyword = popupView.findViewById(R.id.btnEditAddKeyword);
        ChipGroup cgKeywords = popupView.findViewById(R.id.chipGroupEditKeywords);
        CheckBox cbIsOrganic = popupView.findViewById(R.id.cbIsOrganic);
        TextView tvCurrentQty = popupView.findViewById(R.id.tvCurrentQty);
        EditText etAddQty = popupView.findViewById(R.id.etAddQty);
        EditText etPrice = popupView.findViewById(R.id.etEditPrice);
        Spinner spDiscountType = popupView.findViewById(R.id.spDiscountType);
        EditText etDiscountVal = popupView.findViewById(R.id.etEditDiscount);
        Spinner spDeliveryOption = popupView.findViewById(R.id.spEditDeliveryOption);
        TextView tvExpiryDateEdit = popupView.findViewById(R.id.tvEditExpiryDate);
        EditText etDescription = popupView.findViewById(R.id.etEditDescription);
        Button btnUpdate = popupView.findViewById(R.id.btnUpdateProduct);

        // Pre-fill fields from request data
        if (etName != null) etName.setText(reqData.getName());
        if (etDescription != null) etDescription.setText(reqData.getDescription());
        if (etPrice != null) etPrice.setText(String.valueOf(reqData.getCostPerUnit()));
        if (etDiscountVal != null) etDiscountVal.setText(String.valueOf(reqData.getDiscount()));
        if (tvCurrentQty != null) tvCurrentQty.setText(String.valueOf(reqData.getQuantityAvailable()));
        
        if (tvExpiryDateEdit != null) {
            tvExpiryDateEdit.setText(reqData.getExpiryDate() != null ? reqData.getExpiryDate() : "");
            currentExpiryDateEdit = reqData.getExpiryDate() != null ? reqData.getExpiryDate() : "";
            tvExpiryDateEdit.setOnClickListener(v -> showExpiryDatePicker(tvExpiryDateEdit));
        }
        if (cbIsOrganic != null) cbIsOrganic.setChecked(reqData.getOrganic() != null ? reqData.getOrganic() : false);

        // Setup Keywords chips
        editSelectedKeywords.clear();
        if (cgKeywords != null) {
            cgKeywords.removeAllViews();
            if (reqData.getKeywords() != null) {
                for (String kw : reqData.getKeywords()) {
                    addKeywordChip(kw, cgKeywords, editSelectedKeywords);
                }
            }
        }

        // Pre-fill Discount Type Spinner
        List<String> dTypes = Arrays.asList("None", "Fixed", "Percentage");
        ArrayAdapter<String> dAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dTypes);
        dAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spDiscountType != null) {
            spDiscountType.setAdapter(dAdapter);
            String currentDType = reqData.getDiscountType() != null ? reqData.getDiscountType() : "None";
            int idx = -1;
            for (int i = 0; i < dTypes.size(); i++) {
                if (dTypes.get(i).equalsIgnoreCase(currentDType)) { idx = i; break; }
            }
            if (idx >= 0) spDiscountType.setSelection(idx);
        }

        // Pre-fill Delivery Option Spinner
        List<String> deliveryOptions = Arrays.asList("Available", "Not Available");
        ArrayAdapter<String> delAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, deliveryOptions);
        delAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spDeliveryOption != null) {
            spDeliveryOption.setAdapter(delAdapter);
            String currentDel = reqData.getDeliveryOption() != null ? reqData.getDeliveryOption() : "Available";
            int idx = -1;
            for (int i = 0; i < deliveryOptions.size(); i++) {
                if (deliveryOptions.get(i).equalsIgnoreCase(currentDel)) { idx = i; break; }
            }
            if (idx >= 0) spDeliveryOption.setSelection(idx);
        }

        // Setup Category AutoComplete pre-fill
        NoFilterArrayAdapter categoryAdapter = new NoFilterArrayAdapter(this, new ArrayList<>());
        if (actvCategory != null) {
            actvCategory.setAdapter(categoryAdapter);
            actvCategory.setText(reqData.getProduct_type(), false);
            actvCategory.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) { fetchProductTypes(s.toString(), categoryAdapter, actvCategory); }
            });
        }

        // Setup Keyword Search AutoComplete
        NoFilterArrayAdapter keywordAdapter = new NoFilterArrayAdapter(this, new ArrayList<>());
        if (actvKeywordSearch != null) {
            actvKeywordSearch.setAdapter(keywordAdapter);
            actvKeywordSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    String cat = actvCategory != null ? actvCategory.getText().toString() : "";
                    fetchKeywords(cat, s.toString(), keywordAdapter, actvKeywordSearch);
                }
            });
            actvKeywordSearch.setOnItemClickListener((parent, view, position, id) -> {
                String kw = (String) parent.getItemAtPosition(position);
                addKeywordChip(kw, cgKeywords, editSelectedKeywords);
                actvKeywordSearch.setText("");
            });
        }

        if (btnAddKeyword != null) {
            btnAddKeyword.setOnClickListener(v -> {
                String kw = actvKeywordSearch.getText().toString().trim();
                if (!kw.isEmpty()) {
                    addKeywordChip(kw, cgKeywords, editSelectedKeywords);
                    actvKeywordSearch.setText("");
                }
            });
        }

        if (btnUpdate != null) {
            btnUpdate.setOnClickListener(v -> {
                UpdateProduct.Request updateReq = new UpdateProduct.Request();
                updateReq.setP_id(productId);
                updateReq.setName(etName.getText().toString());
                updateReq.setProduct_type(getProductTypeKey(actvCategory.getText().toString()));
                try {
                    updateReq.setCostPerUnit(Double.parseDouble(etPrice.getText().toString()));
                } catch (Exception ignored) {}
                updateReq.setDiscountType(spDiscountType.getSelectedItem().toString());
                try {
                    updateReq.setDiscount(Double.parseDouble(etDiscountVal.getText().toString()));
                } catch (Exception ignored) {}
                
                try {
                    String addQtyStr = etAddQty.getText().toString();
                    int currentVal = reqData.getQuantityAvailable() != null ? reqData.getQuantityAvailable() : 0;
                    if (!addQtyStr.isEmpty()) {
                        updateReq.setQuantityAvailable(currentVal + (int) Double.parseDouble(addQtyStr));
                    } else {
                        updateReq.setQuantityAvailable(currentVal);
                    }
                } catch (Exception ignored) {}

                updateReq.setKeywords(new ArrayList<>(editSelectedKeywords));
                updateReq.setDescription(etDescription.getText().toString());
                updateReq.setOrganic(cbIsOrganic.isChecked());
                updateReq.setDeliveryOption(spDeliveryOption.getSelectedItem().toString());
                updateReq.setExpiryDate(currentExpiryDateEdit);

                performUpdate(updateReq);
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void showExpiryDatePicker(TextView tv) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            currentExpiryDateEdit = String.format(Locale.US, "%02d-%02d-%d", day, month + 1, year);
            tv.setText(currentExpiryDateEdit);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void addKeywordChip(String keyword, ChipGroup group, List<String> list) {
        String kw = keyword.trim().toLowerCase();
        if (kw.isEmpty() || list.contains(kw)) return;
        list.add(kw);
        Chip chip = new Chip(this);
        chip.setText(kw);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            group.removeView(chip);
            list.remove(kw);
        });
        group.addView(chip);
    }

    private void fetchProductTypes(String query, NoFilterArrayAdapter adapter, AutoCompleteTextView view) {
        AddProductService.ProductTypeRequest req = new AddProductService.ProductTypeRequest();
        req.search = query;
        RetrofitClient.getApiService(this).getProductTypes(sessionManager.getAuthToken(), sessionManager.getUserId(), req).enqueue(new Callback<AddProductService.ProductTypeResponse>() {
            @Override public void onResponse(@NonNull Call<AddProductService.ProductTypeResponse> call, @NonNull Response<AddProductService.ProductTypeResponse> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                List<AddProductService.ProductType> types = response.body().getCategories();
                if (types == null) return;
                productTypeData = types;
                List<String> labels = new ArrayList<>();
                for (AddProductService.ProductType t : types) labels.add(t.getLabel());
                adapter.updateItems(labels);
                if (!labels.isEmpty()) view.showDropDown();
            }
            @Override public void onFailure(@NonNull Call<AddProductService.ProductTypeResponse> call, @NonNull Throwable t) { Log.e(TAG, "fetchProductTypes failure: " + t.getMessage()); }
        });
    }

    private void fetchKeywords(String categoryLabel, String query, NoFilterArrayAdapter adapter, AutoCompleteTextView view) {
        if (query.isEmpty()) return;
        AddProductService.keywordRequest req = new AddProductService.keywordRequest();
        req.category = getProductTypeKey(categoryLabel);
        req.keyword = query;

        RetrofitClient.getApiService(this).getKeyword(sessionManager.getAuthToken(), sessionManager.getUserId(), req)
                .enqueue(new Callback<AddProductService.KeywordResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AddProductService.KeywordResponse> call, @NonNull Response<AddProductService.KeywordResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<AddProductService.FarmProduct> products = response.body().getFarm_products();
                            if (products != null) {
                                List<String> suggestions = new ArrayList<>();
                                for (AddProductService.FarmProduct p : products) suggestions.add(p.getEnglish_name());
                                adapter.updateItems(suggestions);
                                if (!suggestions.isEmpty()) view.showDropDown();
                            }
                        }
                    }
                    @Override public void onFailure(@NonNull Call<AddProductService.KeywordResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "fetchKeywords failure: " + t.getMessage());
                    }
                });
    }

    private String getProductTypeKey(String label) {
        for (AddProductService.ProductType t : productTypeData) {
            if (t.getLabel().equalsIgnoreCase(label)) return t.getKey();
        }
        return label.toLowerCase().replace(" ", "-");
    }

    private void performUpdate(UpdateProduct.Request updateReq) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        RetrofitClient.getApiService(this)
                .updateProduct(sessionManager.getAuthToken(), sessionManager.getUserId(), updateReq)
                .enqueue(new Callback<UpdateProduct.Response>() {
                    @Override
                    public void onResponse(@NonNull Call<UpdateProduct.Response> call, @NonNull Response<UpdateProduct.Response> response) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            Toast.makeText(ProductDetailActivity.this, "Product updated successfully", Toast.LENGTH_SHORT).show();
                            fetchProductDetail(); // Refresh the screen
                        } else {
                            Toast.makeText(ProductDetailActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<UpdateProduct.Response> call, @NonNull Throwable t) {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(ProductDetailActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
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
        
        // Handle Visibility based on Ownership
        boolean isOwnProduct = p.getUserId() != null && p.getUserId().equals(sessionManager.getUserId());
        
        if (layoutBottomBar != null) layoutBottomBar.setVisibility(View.VISIBLE);
        if (isOwnProduct) {
            if (layoutFarmerSection != null) layoutFarmerSection.setVisibility(View.GONE);
            if (btnReviewIt != null) btnReviewIt.setVisibility(View.GONE);
            if (btnReqestOrder != null) btnReqestOrder.setVisibility(View.GONE);
            if (btnEditProduct != null) btnEditProduct.setVisibility(View.VISIBLE);
            if (layoutOrigin != null) layoutOrigin.setVisibility(View.GONE);
        } else {
            if (layoutFarmerSection != null) layoutFarmerSection.setVisibility(View.VISIBLE);
            if (tvFarmerName != null) tvFarmerName.setText(p.getFarmerName());
            if (tvFarmerLocation != null) tvFarmerLocation.setText("📍 " + p.getFarmerLocation());
            if (btnReviewIt != null) btnReviewIt.setVisibility(View.VISIBLE);
            if (btnReqestOrder != null) btnReqestOrder.setVisibility(View.VISIBLE);
            if (btnEditProduct != null) btnEditProduct.setVisibility(View.GONE);
            if (layoutOrigin != null) layoutOrigin.setVisibility(View.VISIBLE);
        }

        // Delivery Availability logic fixed
        if (tvDeliveryStatus != null) {
            String delOption = p.getDeliveryOption();
            if (delOption != null) {
                if (delOption.equalsIgnoreCase("Available")) {
                    tvDeliveryStatus.setText("Delivery Available");
                } else if (delOption.equalsIgnoreCase("Not Available")) {
                    tvDeliveryStatus.setText("Delivery Not Available");
                } else {
                    tvDeliveryStatus.setText(delOption);
                }
            } else {
                tvDeliveryStatus.setText("Delivery Not Available");
            }
        }
        if (layoutDeliveryReturns != null) {
            layoutDeliveryReturns.setVisibility(p.isInStock() ? View.VISIBLE : View.GONE);
        }

        try {
            double cost = Double.parseDouble(p.getCostPerUnit() != null ? p.getCostPerUnit() : "0");
            double discountVal = Double.parseDouble(p.getDiscountValue() != null ? p.getDiscountValue() : "0");
            String discountType = p.getDiscountType() != null ? p.getDiscountType() : "None";

            originalPriceVal = cost;
            currentDiscountType = discountType;
            currentDiscountValue = p.getDiscountValue();

            double finalPrice = cost;
            if (discountType.equalsIgnoreCase("Fixed") || discountType.equalsIgnoreCase("Flat")) {
                finalPrice = cost - discountVal;
                if (tvDiscountBadge != null) {
                    tvDiscountBadge.setVisibility(discountVal > 0 ? View.VISIBLE : View.GONE);
                    tvDiscountBadge.setText("Rs. " + (int)discountVal + " OFF");
                }
            } else if (discountType.equalsIgnoreCase("Percentage")) {
                finalPrice = cost - (cost * discountVal / 100.0);
                if (tvDiscountBadge != null) {
                    tvDiscountBadge.setVisibility(discountVal > 0 ? View.VISIBLE : View.GONE);
                    tvDiscountBadge.setText((int)discountVal + "% OFF");
                }
            } else {
                if (tvDiscountBadge != null) tvDiscountBadge.setVisibility(View.GONE);
            }
            finalPrice = Math.max(finalPrice, 0);

            loadedFinalPrice = finalPrice;
            loadedUnit = p.getUnit() != null ? p.getUnit() : "unit";

            if (tvPrice != null) tvPrice.setText(String.format(Locale.US, "Rs. %.0f", finalPrice));
            if (tvOldPrice != null) {
                tvOldPrice.setText(String.format(Locale.US, "Rs. %.0f", cost));
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
        if (tvExpiryDate != null) tvExpiryDate.setText(p.getExpiryDate() != null ? p.getExpiryDate() : "N/A");

        if (ratingBar != null) ratingBar.setRating(p.getRating());
        if (tvRatingNum != null) tvRatingNum.setText(String.valueOf(p.getRating()));
        if (tvRatingCount != null) tvRatingCount.setText("(" + p.getRatingCount() + " reviews)");
        if (tvSoldCount != null) tvSoldCount.setText(p.getSoldCount() + " sold");

        // Media loading logic
        int noOfMedia = p.getNoOfMedia();
        if (noOfMedia > 0) {
            downloadAllMedia(noOfMedia);
        } else {
            productImages.clear();
            productImages.add(null);
            imageAdapter.notifyDataSetChanged();
            if (tvImageCounter != null) tvImageCounter.setText("0 / 0");
        }

        if (viewPagerImages != null) {
            viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override public void onPageSelected(int position) {
                    if (tvImageCounter != null) tvImageCounter.setText((position + 1) + " / " + productImages.size());
                }
            });
        }
    }

    private void downloadAllMedia(int count) {
        productImages.clear();
        for (int i = 0; i < count; i++) productImages.add(null);
        imageAdapter.notifyDataSetChanged();
        for (int i = 1; i <= count; i++) downloadSingleMedia(i);
    }

    private void downloadSingleMedia(int seq) {
        File cachedFile = getLocalProductMediaFile(productId, seq);
        if (cachedFile.exists()) {
            updateImageInSlider(cachedFile, seq - 1);
            return;
        }
        DownloadService.Executor executor = new DownloadService.Executor(this, RetrofitClient.getApiService(this));
        executor.download(sessionManager.getAuthToken(), sessionManager.getUserId(), DownloadService.Req.product(productId, seq),
                new DownloadService.Executor.Callback() {
                    @Override
                    public void onSuccess(File file, DownloadService.Res raw) {
                        File dest = getLocalProductMediaFile(productId, seq);
                        if (copyFile(file, dest)) updateImageInSlider(dest, seq - 1);
                    }
                    @Override public void onError(String error) {}
                });
    }

    private void updateImageInSlider(File file, int index) {
        runOnUiThread(() -> {
            if (index < productImages.size()) {
                productImages.set(index, file);
                imageAdapter.notifyItemChanged(index);
                if (tvImageCounter != null) tvImageCounter.setText((viewPagerImages.getCurrentItem() + 1) + " / " + productImages.size());
            }
        });
    }

    private File getLocalProductMediaFile(String productId, int seq) {
        File dir = new File(getFilesDir(), "product_media/" + productId);
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "media_" + seq + ".jpg");
    }

    private boolean copyFile(File src, File dst) {
        try (InputStream in = new FileInputStream(src); FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192]; int len;
            while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
            return true;
        } catch (IOException e) { return false; }
    }

    private void showOrderPopup() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View popupView = inflater.inflate(R.layout.popup_order_request, null);
        AlertDialog orderDialog = new AlertDialog.Builder(this, R.style.RoundedDialog).setView(popupView).create();

        ImageButton btnClose = popupView.findViewById(R.id.btnClosePopup);
        View btnIncrease = popupView.findViewById(R.id.btnIncrease);
        View btnDecrease = popupView.findViewById(R.id.btnDecrease);
        EditText etQuantity = popupView.findViewById(R.id.etQuantity);
        TextView tvQuantityUnit = popupView.findViewById(R.id.tvQuantityUnit);
        TextView tvTotalCost = popupView.findViewById(R.id.tvTotalCost);
        TextView tvDiscountAmount = popupView.findViewById(R.id.tvDiscountAmount);
        View layoutDiscount = popupView.findViewById(R.id.layoutDiscount);
        Button btnOrderRequest = popupView.findViewById(R.id.btnOrderRequest);
        
        EditText etProvince = popupView.findViewById(R.id.etProvince);
        EditText etDistrict = popupView.findViewById(R.id.etDistrict);
        EditText etMunicipal = popupView.findViewById(R.id.etMunicipal);
        EditText etWard = popupView.findViewById(R.id.etWard);
        EditText etTole = popupView.findViewById(R.id.etTole);
        EditText etOrderMessage = popupView.findViewById(R.id.etOrderMessage);
        EditText etExpectedDelivery = popupView.findViewById(R.id.tvExpectedDelivery);
        RadioGroup rgPaymentMethod = popupView.findViewById(R.id.rgPaymentMethod);

        if (tvQuantityUnit != null) tvQuantityUnit.setText(" " + loadedUnit);

        orderQty = 1.0;
        if (etQuantity != null) {
            etQuantity.setText(String.valueOf(orderQty));
            etQuantity.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    try {
                        String val = s.toString();
                        orderQty = val.isEmpty() ? 0.0 : Double.parseDouble(val);
                        updateOrderPopupCalculation(tvTotalCost, tvDiscountAmount, layoutDiscount);
                    } catch (Exception ignored) {}
                }
            });
        }
        updateOrderPopupCalculation(tvTotalCost, tvDiscountAmount, layoutDiscount);

        RetrofitClient.getApiService(this)
                .getOWNAddress(sessionManager.getAuthToken(), sessionManager.getUserId())
                .enqueue(new Callback<AddressService.AddressResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AddressService.AddressResponse> call, @NonNull Response<AddressService.AddressResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            AddressService.AddressResponse addr = response.body();
                            if (etProvince != null) etProvince.setText(addr.getProvince());
                            if (etDistrict != null) etDistrict.setText(addr.getDistrict());
                            if (etMunicipal != null) etMunicipal.setText(addr.getMunicipal());
                            if (etWard != null) etWard.setText(addr.getWard());
                            if (etTole != null) etTole.setText(addr.getTole());
                        }
                    }
                    @Override public void onFailure(@NonNull Call<AddressService.AddressResponse> call, @NonNull Throwable t) {}
                });

        if (btnIncrease != null) btnIncrease.setOnClickListener(v -> { 
            orderQty++; 
            if (etQuantity != null) etQuantity.setText(String.format(Locale.US, "%.1f", orderQty)); 
            updateOrderPopupCalculation(tvTotalCost, tvDiscountAmount, layoutDiscount);
        });
        if (btnDecrease != null) btnDecrease.setOnClickListener(v -> { 
            if (orderQty > 0.5) { 
                orderQty--; 
                if (orderQty < 0) orderQty = 0;
                if (etQuantity != null) etQuantity.setText(String.format(Locale.US, "%.1f", orderQty)); 
                updateOrderPopupCalculation(tvTotalCost, tvDiscountAmount, layoutDiscount);
            } 
        });
        if (btnClose != null) btnClose.setOnClickListener(v -> orderDialog.dismiss());
        
        if (btnOrderRequest != null) btnOrderRequest.setOnClickListener(v -> { 
            String province = etProvince != null ? etProvince.getText().toString() : "";
            String district = etDistrict != null ? etDistrict.getText().toString() : "";
            String municipal = etMunicipal != null ? etMunicipal.getText().toString() : "";
            String ward = etWard != null ? etWard.getText().toString() : "";
            String tole = etTole != null ? etTole.getText().toString() : "";
            String message = etOrderMessage != null ? etOrderMessage.getText().toString() : "";
            String expectedDeliveryStr = etExpectedDelivery != null ? etExpectedDelivery.getText().toString() : "5";
            int expectedDelivery = expectedDeliveryStr.isEmpty() ? 5 : Integer.parseInt(expectedDeliveryStr);

            String payment = "WALLET";
            if (rgPaymentMethod != null) {
                int selectedId = rgPaymentMethod.getCheckedRadioButtonId();
                if (selectedId == R.id.rbCOD) payment = "COD";
            }

            if (orderQty <= 0) {
                Toast.makeText(this, "Quantity must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }

            orderDialog.dismiss();
            showConfirmOrderDialog(expectedDelivery, province, district, municipal, ward, tole, message, payment);
        });

        orderDialog.show();
    }

    private void updateOrderPopupCalculation(TextView tvTotalCost, TextView tvDiscountAmount, View layoutDiscount) {
        double totalOriginal = originalPriceVal * orderQty;
        double totalFinal = loadedFinalPrice * orderQty;
        double totalDiscount = totalOriginal - totalFinal;

        if (tvTotalCost != null) tvTotalCost.setText(String.format(Locale.US, "Rs. %.0f", totalFinal));
        
        if (totalDiscount > 0) {
            if (layoutDiscount != null) layoutDiscount.setVisibility(View.VISIBLE);
            if (tvDiscountAmount != null) tvDiscountAmount.setText(String.format(Locale.US, "-Rs. %.0f", totalDiscount));
        } else {
            if (layoutDiscount != null) layoutDiscount.setVisibility(View.GONE);
        }
    }

    private void showConfirmOrderDialog(int expectedDelivery, String province, String district, String municipal, String ward, String tole, String message, String payment) {
        View confirmView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_order, null);
        AlertDialog confirmDialog = new AlertDialog.Builder(this, R.style.RoundedDialog).setView(confirmView).create();
        confirmView.findViewById(R.id.btnCancel).setOnClickListener(v -> confirmDialog.dismiss());
        confirmView.findViewById(R.id.btnProceed).setOnClickListener(v -> {
            confirmDialog.dismiss();
            placeOrder(expectedDelivery, province, district, municipal, ward, tole, message, payment);
        });
        confirmDialog.show();
    }

    private void placeOrder(int expectedDelivery, String province, String district, String municipal, String ward, String tole, String message, String payment) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        OrderRequestService.OrderRequest request = new OrderRequestService.OrderRequest(
                expectedDelivery, String.valueOf(loadedFinalPrice * orderQty), productId, orderQty, message, payment,
                province, district, municipal, ward, tole, currentDiscountType, currentDiscountValue
        );
        RetrofitClient.getApiService(this).getOrderRequest(sessionManager.getAuthToken(), sessionManager.getUserId(), request)
                .enqueue(new Callback<OrderRequestService.OrderRequestResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<OrderRequestService.OrderRequestResponse> call, @NonNull Response<OrderRequestService.OrderRequestResponse> response) {
                        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                        if (response.isSuccessful() && response.body() != null) showOrderSuccessPopup(response.body());
                        else Toast.makeText(ProductDetailActivity.this, "Order request failed", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onFailure(@NonNull Call<OrderRequestService.OrderRequestResponse> call, @NonNull Throwable t) {
                        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                        Toast.makeText(ProductDetailActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showOrderSuccessPopup(OrderRequestService.OrderRequestResponse orderData) {
        View successView = LayoutInflater.from(this).inflate(R.layout.popup_order_success, null);
        AlertDialog successDialog = new AlertDialog.Builder(this, R.style.RoundedDialog).setView(successView).create();
        TextView tvOrderId = successView.findViewById(R.id.tvOrderId);
        TextView tvOtp = successView.findViewById(R.id.tvOtp);
        TextView tvOrderDate = successView.findViewById(R.id.tvOrderDate);
        Button btnClose = successView.findViewById(R.id.btnCloseSuccess);
        if (tvOrderId != null) tvOrderId.setText("Order ID: #" + orderData.getOrder_id());
        if (tvOtp != null) tvOtp.setText("OTP: " + orderData.getOtp());
        if (tvOrderDate != null) tvOrderDate.setText("Date: " + orderData.getOrdered_date());
        if (btnClose != null) btnClose.setOnClickListener(v -> successDialog.dismiss());
        successDialog.show();
    }

    // --- Helper Adapter for AutoComplete ---
    private static class NoFilterArrayAdapter extends ArrayAdapter<String> {
        private final List<String> items;
        private final List<String> filteredItems;

        NoFilterArrayAdapter(Context context, List<String> items) {
            super(context, android.R.layout.simple_dropdown_item_1line, items);
            this.items = items;
            this.filteredItems = new ArrayList<>(items);
        }

        public void updateItems(List<String> newItems) {
            this.items.clear();
            this.items.addAll(newItems);
            this.filteredItems.clear();
            this.filteredItems.addAll(newItems);
            notifyDataSetChanged();
        }

        @Override public int getCount() { return filteredItems.size(); }
        @Override public String getItem(int position) { return filteredItems.get(position); }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    results.values = items;
                    results.count = items.size();
                    return results;
                }
                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    filteredItems.clear();
                    if (results.values != null) filteredItems.addAll((List<String>) results.values);
                    notifyDataSetChanged();
                }
            };
        }
    }
}
