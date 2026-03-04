package com.farmo.activities.farmerActivities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.farmo.R;
import com.farmo.network.CommonServices.UploadService;
import com.farmo.network.RetrofitClient;
import com.farmo.network.farmer.AddProductService;
import com.farmo.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddProductActivity extends AppCompatActivity {

    private static final String TAG = "AddProductActivity";

    private static final String NOTIF_CHANNEL_ID  = "upload_channel";
    private static final int    NOTIF_ID_UPLOAD   = 1001;
    private static final long   DEBOUNCE_DELAY_MS = 300;

    private static final int  MAX_IMAGES      = 5;
    private static final long MAX_IMAGE_BYTES = 20L * 1024 * 1024;

    private static final String[] UNIT_LABELS = {
            "Kilogram", "Gram", "Litre", "Piece", "Dozen", "Quintal", "Ton"
    };
    private static final String[] UNIT_VALUES = {
            "kg", "g", "l", "piece", "dozen", "quintal", "ton"
    };

    private AutoCompleteTextView autoCompleteProductType;
    private AutoCompleteTextView autoCompleteKeywordSearch;
    private MaterialButton       btnAddKeyword;
    private TextInputEditText    etProductName;
    private AutoCompleteTextView spinnerUnit;
    private TextInputEditText    etQuantity;
    private TextInputEditText    etPricePerUnit;
    private TextInputEditText    etProducedDate;
    private TextInputEditText    etExpiryDate;
    private AutoCompleteTextView spinnerDiscountType;
    private TextInputEditText    etDiscount;
    private TextInputEditText    etDescription;
    private RadioGroup           rgOrganic;
    private MaterialCheckBox     cbAvailable;
    private MaterialCheckBox     cbNotAvailable;
    private MaterialButton       btnUploadMedia;
    private MaterialButton       btnAddProduct;
    private MaterialButton       btnCancel;
    private RecyclerView         rvMediaPreview;
    private ChipGroup            chipGroupKeywords;

    private final List<Uri>    selectedImages   = new ArrayList<>();
    private final List<String> selectedKeywords = new ArrayList<>();
    private String             resolvedProductId;
    private Timer              debounceTimer        = new Timer();
    private Timer              keywordDebounceTimer = new Timer();

    private MediaPreviewAdapter mediaPreviewAdapter;
    private NoFilterArrayAdapter productTypeAdapter;
    private NoFilterArrayAdapter keywordAdapter;
    private List<AddProductService.ProductType> productTypeData = new ArrayList<>();

    private String         authToken;
    private String         userId;
    private SessionManager sessionManager;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private boolean isSelectingFromDropdown = false;

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
                    if (results.values != null) {
                        filteredItems.addAll((List<String>) results.values);
                    }
                    notifyDataSetChanged();
                }
            };
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        sessionManager = new SessionManager(this);
        authToken = sessionManager.getAuthToken();
        userId    = sessionManager.getUserId();

        createNotificationChannel();
        bindViews();
        registerImagePicker();
        setupUnitDropdown();
        setupDiscountTypeDropdown();
        setupProductTypeAutoComplete();
        setupKeywordAutoComplete();
        setupDatePickers();
        setupMediaPicker();
        setupSubmitButton();
    }

    private void bindViews() {
        autoCompleteProductType   = findViewById(R.id.autoCompleteProductType);
        autoCompleteKeywordSearch = findViewById(R.id.autoCompleteKeywordSearch);
        btnAddKeyword             = findViewById(R.id.btnAddKeyword);
        etProductName             = findViewById(R.id.etProductName);
        spinnerUnit               = findViewById(R.id.spinnerUnit);
        etQuantity                = findViewById(R.id.etQuantity);
        etPricePerUnit            = findViewById(R.id.etPricePerUnit);
        etProducedDate            = findViewById(R.id.etProducedDate);
        etExpiryDate              = findViewById(R.id.etExpiryDate);
        spinnerDiscountType       = findViewById(R.id.spinnerDiscountType);
        etDiscount                = findViewById(R.id.etDiscount);
        etDescription             = findViewById(R.id.etDescription);
        rgOrganic                 = findViewById(R.id.rgOrganic);
        cbAvailable               = findViewById(R.id.cbAvailable);
        cbNotAvailable            = findViewById(R.id.cbNotAvailable);
        btnUploadMedia            = findViewById(R.id.btnUploadMedia);
        btnAddProduct             = findViewById(R.id.btnAddProduct);
        btnCancel                 = findViewById(R.id.btnCancel);
        rvMediaPreview            = findViewById(R.id.rvMediaPreview);
        chipGroupKeywords         = findViewById(R.id.chipGroupKeywords);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());

        cbAvailable.setOnCheckedChangeListener((b, checked) -> { if (checked) cbNotAvailable.setChecked(false); });
        cbNotAvailable.setOnCheckedChangeListener((b, checked) -> { if (checked) cbAvailable.setChecked(false); });

        btnAddKeyword.setOnClickListener(v -> {
            String kw = autoCompleteKeywordSearch.getText().toString().trim();
            if (!kw.isEmpty()) { addKeywordChip(kw); autoCompleteKeywordSearch.setText("", false); }
        });

        mediaPreviewAdapter = new MediaPreviewAdapter(selectedImages, this::onImageClicked, this::onImageRemoved);
        rvMediaPreview.setLayoutManager(new GridLayoutManager(this, 1, GridLayoutManager.HORIZONTAL, false));
        rvMediaPreview.setAdapter(mediaPreviewAdapter);
    }

    private void setupUnitDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, UNIT_LABELS);
        spinnerUnit.setAdapter(adapter);
    }

    private String resolveUnitValue(String label) {
        for (int i = 0; i < UNIT_LABELS.length; i++) { if (UNIT_LABELS[i].equalsIgnoreCase(label)) return UNIT_VALUES[i]; }
        return label.toLowerCase();
    }

    private void setupDiscountTypeDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new String[]{"Percentage", "Flat"});
        spinnerDiscountType.setAdapter(adapter);
        spinnerDiscountType.setText("Percentage", false);
    }

    private void setupDatePickers() {
        etProducedDate.setOnClickListener(v -> showDatePicker(etProducedDate));
        etExpiryDate.setOnClickListener(v   -> showDatePicker(etExpiryDate));
    }

    private void showDatePicker(TextInputEditText target) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) ->
                target.setText(String.format("%04d-%02d-%02d", year, month + 1, day)),
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setupProductTypeAutoComplete() {
        autoCompleteProductType.setThreshold(1);
        productTypeAdapter = new NoFilterArrayAdapter(this, new ArrayList<>());
        autoCompleteProductType.setAdapter(productTypeAdapter);
        autoCompleteProductType.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isSelectingFromDropdown) return;
                String query = s.toString().trim();
                if (query.isEmpty()) { autoCompleteProductType.dismissDropDown(); return; }
                debounceTimer.cancel();
                debounceTimer = new Timer();
                debounceTimer.schedule(new TimerTask() {
                    @Override public void run() { runOnUiThread(() -> fetchProductTypes(query)); }
                }, DEBOUNCE_DELAY_MS);
            }
        });
        autoCompleteProductType.setOnItemClickListener((parent, view, position, id) -> {
            isSelectingFromDropdown = true;
            String selected = (String) parent.getItemAtPosition(position);
            autoCompleteProductType.setText(selected, false);
            autoCompleteProductType.setSelection(selected.length());
            isSelectingFromDropdown = false;
        });
    }

    private void fetchProductTypes(String query) {
        if (query.isEmpty()) return;
        AddProductService.ProductTypeRequest req = new AddProductService.ProductTypeRequest();
        req.search = query;
        RetrofitClient.getApiService(this).getProductTypes(authToken, userId, req).enqueue(new Callback<AddProductService.ProductTypeResponse>() {
            @Override public void onResponse(@NonNull Call<AddProductService.ProductTypeResponse> call, @NonNull Response<AddProductService.ProductTypeResponse> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                List<AddProductService.ProductType> types = response.body().getCategories();
                if (types == null) return;
                productTypeData = types;
                List<String> labels = new ArrayList<>();
                for (AddProductService.ProductType t : types) labels.add(t.getLabel());
                productTypeAdapter.updateItems(labels);
                if (labels.isEmpty()) autoCompleteProductType.dismissDropDown();
                else { autoCompleteProductType.showDropDown(); }
            }
            @Override public void onFailure(@NonNull Call<AddProductService.ProductTypeResponse> call, @NonNull Throwable t) { Log.e(TAG, "fetchProductTypes failure: " + t.getMessage()); }
        });
    }

    private String getProductTypeKey(String label) {
        for (AddProductService.ProductType t : productTypeData) { if (t.getLabel().equalsIgnoreCase(label)) return t.getKey(); }
        return label.toLowerCase().replace(" ", "-");
    }

    private void setupKeywordAutoComplete() {
        autoCompleteKeywordSearch.setThreshold(1);
        keywordAdapter = new NoFilterArrayAdapter(this, new ArrayList<>());
        autoCompleteKeywordSearch.setAdapter(keywordAdapter);
        autoCompleteKeywordSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isSelectingFromDropdown) return;
                String text = s.toString();
                if (text.isEmpty()) { autoCompleteKeywordSearch.dismissDropDown(); return; }
                if (text.endsWith(",") || text.endsWith(" ")) {
                    String kw = text.replace(",", "").trim();
                    if (!kw.isEmpty()) addKeywordChip(kw);
                    isSelectingFromDropdown = true;
                    autoCompleteKeywordSearch.setText("", false);
                    isSelectingFromDropdown = false;
                    return;
                }
                keywordDebounceTimer.cancel();
                keywordDebounceTimer = new Timer();
                keywordDebounceTimer.schedule(new TimerTask() {
                    @Override public void run() { runOnUiThread(() -> fetchKeywordSuggestions(autoCompleteProductType.getText().toString().trim(), text.trim())); }
                }, DEBOUNCE_DELAY_MS);
            }
        });
        autoCompleteKeywordSearch.setOnItemClickListener((parent, view, position, id) -> {
            isSelectingFromDropdown = true;
            String selected = (String) parent.getItemAtPosition(position);
            addKeywordChip(selected);
            autoCompleteKeywordSearch.setText("", false);
            isSelectingFromDropdown = false;
        });
    }

    private void fetchKeywordSuggestions(String categoryLabel, String keyword) {
        if (keyword.isEmpty()) return;
        AddProductService.keywordRequest req = new AddProductService.keywordRequest();
        req.category = getProductTypeKey(categoryLabel);
        req.keyword  = keyword;
        RetrofitClient.getApiService(this).getKeyword(authToken, userId, req).enqueue(new Callback<AddProductService.KeywordResponse>() {
            @Override public void onResponse(@NonNull Call<AddProductService.KeywordResponse> call, @NonNull Response<AddProductService.KeywordResponse> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                List<AddProductService.FarmProduct> products = response.body().getFarm_products();
                if (products == null) return;
                List<String> suggestions = new ArrayList<>();
                for (AddProductService.FarmProduct p : products) suggestions.add(p.getEnglish_name());
                keywordAdapter.updateItems(suggestions);
                if (suggestions.isEmpty()) autoCompleteKeywordSearch.dismissDropDown();
                else { autoCompleteKeywordSearch.showDropDown(); }
            }
            @Override public void onFailure(@NonNull Call<AddProductService.KeywordResponse> call, @NonNull Throwable t) { Log.e(TAG, "fetchKeywordSuggestions failure: " + t.getMessage()); }
        });
    }

    private void addKeywordChip(String keyword) {
        String kw = keyword.trim().toLowerCase();
        if (kw.isEmpty() || selectedKeywords.contains(kw)) return;
        selectedKeywords.add(kw);
        Chip chip = new Chip(this);
        chip.setText(kw);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> { selectedKeywords.remove(kw); chipGroupKeywords.removeView(chip); });
        chipGroupKeywords.addView(chip);
    }

    private void registerImagePicker() {
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
            Intent data = result.getData();
            int spotsLeft = MAX_IMAGES - selectedImages.size();
            int added = 0;
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count && added < spotsLeft; i++) { if (validateAndAddImage(data.getClipData().getItemAt(i).getUri())) added++; }
            } else if (data.getData() != null) { if (validateAndAddImage(data.getData())) added++; }
            if (added > 0) mediaPreviewAdapter.notifyDataSetChanged();
            updateImageButtonState();
        });
    }

    private void setupMediaPicker() {
        btnUploadMedia.setOnClickListener(v -> {
            if (selectedImages.size() >= MAX_IMAGES) { Toast.makeText(this, "Max " + MAX_IMAGES + " images.", Toast.LENGTH_SHORT).show(); return; }
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            imagePickerLauncher.launch(intent);
        });
    }

    private boolean validateAndAddImage(Uri uri) {
        if (getFileSize(this, uri) > MAX_IMAGE_BYTES) { Toast.makeText(this, "Exceeds 20MB. Skipped.", Toast.LENGTH_SHORT).show(); return false; }
        selectedImages.add(uri); return true;
    }

    private long getFileSize(Context c, Uri uri) {
        try (android.database.Cursor cursor = c.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (idx >= 0) return cursor.getLong(idx);
            }
        } catch (Exception e) { Log.e(TAG, "getFileSize: " + e.getMessage()); }
        return 0;
    }

    private void onImageClicked(Uri uri) {
        ImageView iv = new ImageView(this);
        iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        iv.setAdjustViewBounds(true);
        Glide.with(this).load(uri).into(iv);
        new AlertDialog.Builder(this).setView(iv).setNegativeButton("Close", null).show();
    }

    private void onImageRemoved(int position) { selectedImages.remove(position); mediaPreviewAdapter.notifyDataSetChanged(); updateImageButtonState(); }

    private void updateImageButtonState() { boolean full = selectedImages.size() >= MAX_IMAGES; btnUploadMedia.setAlpha(full ? 0.5f : 1.0f); btnUploadMedia.setEnabled(!full); }

    private void setupSubmitButton() {
        btnAddProduct.setOnClickListener(v -> {
            if (!validateForm()) return;
            btnAddProduct.setEnabled(false);
            btnAddProduct.setText("Adding…");
            addProductToServer();
        });
    }

    private boolean validateForm() {
        if (isEmpty(etProductName)) { etProductName.setError("Required"); return false; }
        if (autoCompleteProductType.getText().toString().trim().isEmpty()) { autoCompleteProductType.setError("Required"); return false; }
        if (spinnerUnit.getText().toString().trim().isEmpty()) { spinnerUnit.setError("Required"); return false; }
        if (isEmpty(etQuantity))     { etQuantity.setError("Required");     return false; }
        if (isEmpty(etPricePerUnit)) { etPricePerUnit.setError("Required"); return false; }
        if (isEmpty(etProducedDate)) { etProducedDate.setError("Required"); return false; }
        if (isEmpty(etExpiryDate))   { etExpiryDate.setError("Required");   return false; }
        if (!cbAvailable.isChecked() && !cbNotAvailable.isChecked()) { Toast.makeText(this, "Select delivery option.", Toast.LENGTH_SHORT).show(); return false; }
        return true;
    }

    private boolean isEmpty(TextInputEditText et) { return et.getText() == null || et.getText().toString().trim().isEmpty(); }

    private String parseError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String json = response.errorBody().string();
                Log.e(TAG, "Server Error Body: " + json);
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                if (obj.has("error")) return obj.get("error").getAsString();
                if (obj.has("message")) return obj.get("message").getAsString();
            }
        } catch (Exception e) { Log.e(TAG, "parseError: " + e.getMessage()); }
        return "Error: " + response.code();
    }

    private void addProductToServer() {
        AddProductService.AddProductRequest req = buildAddProductRequest();
        Log.d(TAG, "Sending Request JSON: " + new Gson().toJson(req));
        RetrofitClient.getApiService(this).addProduct(authToken, userId, req).enqueue(new Callback<AddProductService.AddProductResponse>() {
            @Override public void onResponse(@NonNull Call<AddProductService.AddProductResponse> call, @NonNull Response<AddProductService.AddProductResponse> response) {
                btnAddProduct.setEnabled(true); btnAddProduct.setText("Add Product");
                if (!response.isSuccessful() || response.body() == null) { Toast.makeText(AddProductActivity.this, parseError(response), Toast.LENGTH_LONG).show(); return; }
                resolvedProductId = response.body().product_id;
                Toast.makeText(AddProductActivity.this, "Product created!", Toast.LENGTH_SHORT).show();
                if (!selectedImages.isEmpty()) startImageUploadInBackground(); else finish();
            }
            @Override public void onFailure(@NonNull Call<AddProductService.AddProductResponse> call, @NonNull Throwable t) {
                btnAddProduct.setEnabled(true); btnAddProduct.setText("Add Product");
                Toast.makeText(AddProductActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private AddProductService.AddProductRequest buildAddProductRequest() {
        AddProductService.AddProductRequest req = new AddProductService.AddProductRequest();
        req.product_name     = etProductName.getText().toString().trim();
        req.product_type     = getProductTypeKey(autoCompleteProductType.getText().toString().trim());
        req.unit             = resolveUnitValue(spinnerUnit.getText().toString().trim());
        req.quantity         = Double.parseDouble(etQuantity.getText().toString().trim());
        req.is_organic       = rgOrganic.getCheckedRadioButtonId() == R.id.rbOrganic;
        req.cost_per_unit    = Double.parseDouble(etPricePerUnit.getText().toString().trim());
        req.produced_date    = etProducedDate.getText().toString().trim();
        req.expiry_date      = etExpiryDate.getText().toString().trim();
        req.description      = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        req.discount_type    = spinnerDiscountType.getText().toString().trim().toLowerCase();
        req.delivery_options = cbAvailable.isChecked() ? "Available" : "Not Available";
        req.keywords         = new ArrayList<>(selectedKeywords);
        String d = etDiscount.getText() != null ? etDiscount.getText().toString().trim() : "";
        req.discount = d.isEmpty() ? 0 : Integer.parseInt(d);
        return req;
    }

    private void startImageUploadInBackground() {
        List<Uri> images = new ArrayList<>(selectedImages);
        int total = images.size();
        Context appContext = getApplicationContext();
        UploadService.ChunkedFileUploader uploader = new UploadService.ChunkedFileUploader(RetrofitClient.getApiService(appContext), userId);
        finish();
        AtomicInteger uploadedCount = new AtomicInteger(0);
        for (int i = 0; i < images.size(); i++) {
            final int seq = i + 1; Uri uri = images.get(i);
            File tempFile = copyUriToTempFile(appContext, uri, "img_" + seq);
            if (tempFile != null) {
                uploader.uploadFile(tempFile, "PRODUCT_MEDIA", resolvedProductId, seq, new UploadService.ChunkedFileUploader.UploadCallback() {
                    @Override public void onSuccess(UploadService.UploadResponse r) { tempFile.delete(); if (uploadedCount.incrementAndGet() == total) showUploadCompleteNotification(appContext, total, total); }
                    @Override public void onError(String e) { tempFile.delete(); if (uploadedCount.incrementAndGet() == total) showUploadCompleteNotification(appContext, uploadedCount.get(), total); }
                });
            }
        }
    }

    private File copyUriToTempFile(Context context, Uri uri, String name) {
        try {
            File dir = new File(context.getCacheDir(), "upload_tmp"); if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, System.currentTimeMillis() + "_" + name);
            try (InputStream in = context.getContentResolver().openInputStream(uri); FileOutputStream out = new FileOutputStream(file)) {
                if (in == null) return null; byte[] buf = new byte[8192]; int len;
                while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
            }
            return file;
        } catch (IOException e) { return null; }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(NOTIF_CHANNEL_ID, "Upload", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private static void showUploadCompleteNotification(Context c, int up, int total) {
        if (ActivityCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        NotificationManagerCompat.from(c).notify(NOTIF_ID_UPLOAD, new NotificationCompat.Builder(c, NOTIF_CHANNEL_ID).setSmallIcon(android.R.drawable.stat_sys_upload_done).setContentTitle("Upload Complete").setContentText(up + "/" + total + " uploaded.").setAutoCancel(true).build());
    }

    public static class MediaPreviewAdapter extends RecyclerView.Adapter<MediaPreviewAdapter.VH> {
        public interface OnImageClickListener { void onImageClicked(Uri uri); }
        public interface OnImageRemoveListener { void onImageRemoved(int position); }
        private final List<Uri> images; private final OnImageClickListener onClick; private final OnImageRemoveListener onRemove;
        public MediaPreviewAdapter(List<Uri> images, OnImageClickListener onClick, OnImageRemoveListener onRemove) { this.images = images; this.onClick = onClick; this.onRemove = onRemove; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) { return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_image_preview, p, false)); }
        @Override public void onBindViewHolder(@NonNull VH h, int p) { Uri u = images.get(p); Glide.with(h.imageView.getContext()).load(u).centerCrop().into(h.imageView); h.imageView.setOnClickListener(v -> onClick.onImageClicked(u)); h.btnRemove.setOnClickListener(v -> onRemove.onImageRemoved(h.getAdapterPosition())); }
        @Override public int getItemCount() { return images.size(); }
        static class VH extends RecyclerView.ViewHolder { ImageView imageView; View btnRemove; VH(@NonNull View v) { super(v); imageView = v.findViewById(R.id.ivPreview); btnRemove = v.findViewById(R.id.btnRemoveImage); } }
    }
}
