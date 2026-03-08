package com.farmo.activities.commonActivities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.signature.ObjectKey;
import com.farmo.R;
import com.farmo.activities.authActivities.LoginActivity;
import com.farmo.network.CommonServices.UploadService;
import com.farmo.network.RetrofitClient;
import com.farmo.network.User.ProfileServices;
import com.farmo.network.CommonServices.DownloadService;
import com.farmo.utils.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private ImageView ivProfileImage;
    private TextView tvProfileName, tvProfileUserType, tvAbout;
    private SessionManager sessionManager;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;

    private Uri selectedImageUri;
    private ImageView dialogImageView;
    private AlertDialog uploadDialog;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null && dialogImageView != null) {
                        Glide.with(this)
                                .load(selectedImageUri)
                                .centerCrop()
                                .into(dialogImageView);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);
        initViews();
        fetchProfileData();
    }

    private void initViews() {
        ivProfileImage = findViewById(R.id.ivProfileImage);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileUserType = findViewById(R.id.tvProfileUserType);
        tvAbout = findViewById(R.id.tvAbout);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            performLogOUT(sessionManager);
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            startActivity(intent);
            finishAffinity();
        });

        ivProfileImage.setOnClickListener(v -> showUploadDialog());

        swipeRefreshLayout.setOnRefreshListener(this::fetchProfileData);
        swipeRefreshLayout.setColorSchemeResources(R.color.topical_forest);
    }

    private void showUploadDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_upload_profile_pic, null);
        dialogImageView = dialogView.findViewById(R.id.ivSelectedImage);
        View btnSelect = dialogView.findViewById(R.id.btnSelectFile);
        View btnSave = dialogView.findViewById(R.id.btnSave);
        View btnCancel = dialogView.findViewById(R.id.btnCancel);

        uploadDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnSelect.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            imagePickerLauncher.launch(intent);
        });

        btnCancel.setOnClickListener(v -> uploadDialog.dismiss());

        btnSave.setOnClickListener(v -> {
            if (selectedImageUri == null) {
                Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
                return;
            }
            startProfilePictureUpload();
            uploadDialog.dismiss();
        });

        uploadDialog.show();
    }

    private void startProfilePictureUpload() {
        Context context = getApplicationContext();
        File tempFile = copyUriToTempFile(context, selectedImageUri, "profile_pic.jpg");

        if (tempFile == null) {
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Uploading profile picture...", Toast.LENGTH_SHORT).show();

        new UploadService.ProfilePictureUploader(
                context,
                tempFile,
                new UploadService.UploadCallback() {
                    @Override
                    public void onSuccess(UploadService.Finish.Res result) {
                        tempFile.delete();
                        Log.d(TAG, "Upload success. Clearing saved filename to force re-download.");
                        sessionManager.setProfilePicName(""); // Clear to force re-download
                        fetchProfileData(); 
                        Toast.makeText(ProfileActivity.this, "Profile picture updated!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        tempFile.delete();
                        Toast.makeText(ProfileActivity.this, "Upload failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProgress(int percent) {
                    }
                }
        ).start();
    }

    private File copyUriToTempFile(Context context, Uri uri, String name) {
        try {
            File dir = new File(context.getCacheDir(), "upload_tmp");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, System.currentTimeMillis() + "_" + name);
            try (InputStream in = context.getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(file)) {
                if (in == null) return null;
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
            }
            return file;
        } catch (IOException e) {
            Log.e(TAG, "copyUriToTempFile error: " + e.getMessage());
            return null;
        }
    }

    private void fetchProfileData() {
        String userId = sessionManager.getUserId();

        if (progressBar != null && !swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        RetrofitClient.getApiService(this).getProfileData(sessionManager.getAuthToken(), userId).enqueue(new Callback<ProfileServices.ProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProfileServices.ProfileResponse> call, @NonNull Response<ProfileServices.ProfileResponse> response) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    ProfileServices.ProfileResponse data = response.body();
                    updateUI(data);
                    
                    String newPicName = data.getProfilePicture();
                    String savedPicName = sessionManager.getProfilePicName();
                    File cachedFile = getLocalProfilePicFile();

                    Log.d(TAG, "Profile Data - Server Filename: " + newPicName + ", Saved Filename: " + savedPicName);

                    if (newPicName != null && !newPicName.isEmpty() && !newPicName.equalsIgnoreCase("null")) {
                        if (!newPicName.equals(savedPicName) || !cachedFile.exists()) {
                            Log.d(TAG, "Triggering download: name mismatch or file missing.");
                            fetchProfilePicture(newPicName);
                        } else {
                            Log.d(TAG, "Loading from local cache.");
                            loadLocalProfilePicture(cachedFile, newPicName);
                        }
                    } else {
                        Log.d(TAG, "No profile picture found on server.");
                        ivProfileImage.setImageResource(R.drawable.pp__placeholder);
                        sessionManager.setProfilePicName("");
                    }

                } else {
                    Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProfileServices.ProfileResponse> call, @NonNull Throwable t) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(ProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private File getLocalProfilePicFile() {
        File dir = new File(getFilesDir(), "profile");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "current_profile_pic.jpg");
    }

    private void loadLocalProfilePicture(File file, String signature) {
        Glide.with(ProfileActivity.this)
             .load(file)
             .circleCrop()
             .signature(new ObjectKey(signature)) // Force Glide to refresh if signature (filename) changes
             .placeholder(R.drawable.pp__placeholder)
             .error(R.drawable.pp__placeholder)
             .into(ivProfileImage);
    }

    private void fetchProfilePicture(String newPicName) {
        DownloadService.Executor executor = new DownloadService.Executor(this, RetrofitClient.getApiService(this));
        
        executor.download(
            sessionManager.getAuthToken(),
            sessionManager.getUserId(),
            DownloadService.Req.profile(),
            new DownloadService.Executor.Callback() {
                @Override
                public void onSuccess(File file, DownloadService.Res raw) {
                    File dest = getLocalProfilePicFile();
                    if (copyFile(file, dest)) {
                        Log.d(TAG, "Download success. Saved to internal storage: " + newPicName);
                        sessionManager.setProfilePicName(newPicName);
                        loadLocalProfilePicture(dest, newPicName);
                    } else {
                        Log.e(TAG, "Failed to copy downloaded file to internal storage.");
                        loadLocalProfilePicture(file, newPicName); 
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to download profile picture: " + error);
                }
            }
        );
    }

    private boolean copyFile(File src, File dst) {
        try (InputStream in = new java.io.FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void updateUI(ProfileServices.ProfileResponse data) {
        tvProfileName.setText(data.getFullName());
        tvProfileUserType.setText(data.getUserType());
        tvAbout.setText(data.getAbout() != null && !data.getAbout().isEmpty() ? data.getAbout() : "No info provided.");

        TextView tvJoinDate = findViewById(R.id.tvJoinDate);
        if (tvJoinDate != null && data.getJoinDate() != null && !data.getJoinDate().isEmpty()) {
            tvJoinDate.setText("Member since: " + data.getJoinDate());
        }

        setupInfoItem(R.id.itemEmail, "Email", data.getEmail());
        setupInfoItem(R.id.itemPhone, "Phone", data.getPhone());
        setupInfoItem(R.id.itemPhone2, "Phone 2", data.getPhone2());
        setupInfoItem(R.id.itemWhatsapp, "WhatsApp", data.getWhatsapp());
        setupInfoItem(R.id.itemFacebook, "Facebook", data.getFacebook());
        setupInfoItem(R.id.itemSex, "Gender", data.getSex());
        setupInfoItem(R.id.itemDob, "Date of Birth", data.getDob());
        setupInfoItem(R.id.itemAddress, "Address", data.getAddress());
    }

    private void setupInfoItem(int viewId, String label, String value) {
        View view = findViewById(viewId);
        if (view != null) {
            TextView tvLabel = view.findViewById(R.id.tvLabel);
            TextView tvValue = view.findViewById(R.id.tvValue);
            tvLabel.setText(label);
            tvValue.setText(value != null && !value.equals("null") ? value : "---");
        }
    }

    public void performLogOUT(SessionManager sessionManager){
        RetrofitClient.getApiService(this).logout(sessionManager.getAuthToken(), sessionManager.getUserId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    sessionManager.clearSession();
                    Toast.makeText(ProfileActivity.this, "Logout successful", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable throwable) {

            }
        });
    }
}
