package com.farmo.activities.commonActivities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.farmo.R;
import com.farmo.activities.authActivities.LoginActivity;
import com.farmo.network.RetrofitClient;
import com.farmo.network.User.ProfileServices;
import com.farmo.utils.SessionManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private ImageView ivProfileImage;
    private TextView tvProfileName, tvProfileUserType, tvAbout;
    private SessionManager sessionManager;
    private ProgressBar progressBar;

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
        //progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            sessionManager.clearSession();
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
        });
    }

    private void fetchProfileData() {
        String userId = sessionManager.getUserId();

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // No need for Bearer token, just pass user-id in header
        RetrofitClient.getApiService(this).getProfileData(sessionManager.getAuthToken(),userId).enqueue(new Callback<ProfileServices.ProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProfileServices.ProfileResponse> call, @NonNull Response<ProfileServices.ProfileResponse> response) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }

                if (response.isSuccessful() && response.body() != null) {
                    updateUI(response.body());
                    // Fetch profile picture in background after text data is loaded
                    fetchProfilePicture();
                } else {
                    Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProfileServices.ProfileResponse> call, @NonNull Throwable t) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                Toast.makeText(ProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchProfilePicture() {
        String userId = sessionManager.getUserId();

        // Create request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("subject", "PROFILE_PICTURE");

        // Make API call in background
        RetrofitClient.getApiService(this).downloadFile(userId, sessionManager.getAuthToken(), requestBody).enqueue(new Callback<ProfileServices.FileDownloadResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProfileServices.FileDownloadResponse> call, @NonNull Response<ProfileServices.FileDownloadResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ProfileServices.FileDownloadResponse fileData = response.body();
                    if (fileData.getFile() != null && !fileData.getFile().isEmpty()) {
                        // Decode Base64 Image
                        try {
                            byte[] decodedString = Base64.decode(fileData.getFile(), Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            if (decodedByte != null) {
                                ivProfileImage.setImageBitmap(decodedByte);
                            }
                        } catch (Exception e) {
                            // Keep default/placeholder image
                            ivProfileImage.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pp__placeholder));
                        }
                    }
                }
                ivProfileImage.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pp__placeholder));
                // If failed or no image, keep the default/placeholder image
            }

            @Override
            public void onFailure(@NonNull Call<ProfileServices.FileDownloadResponse> call, @NonNull Throwable t) {
                // Silently fail - keep default/placeholder image
                Log.e("ProfileActivity", "Failed to load profile picture: " + t.getMessage());
            }
        });
    }

    private void updateUI(ProfileServices.ProfileResponse data) {
        tvProfileName.setText(data.getFullName());
        tvProfileUserType.setText(data.getUserType());
        tvAbout.setText(data.getAbout() != null && !data.getAbout().isEmpty() ? data.getAbout() : "No info provided.");

        // Set join date
        TextView tvJoinDate = findViewById(R.id.tvJoinDate);
        if (tvJoinDate != null && data.getJoinDate() != null && !data.getJoinDate().isEmpty()) {
            tvJoinDate.setText("Member since: " + data.getJoinDate());
        }

        // Setup Details
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
}
