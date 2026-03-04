package com.farmo.activities.commonActivities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.farmo.R;
import com.farmo.activities.authActivities.LoginActivity;
import com.farmo.network.RetrofitClient;
import com.farmo.network.User.ProfileServices;
import com.farmo.network.CommonServices.DownloadService;
import com.farmo.utils.SessionManager;

import java.io.File;

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

        findViewById(R.id.btn_verification).setOnClickListener(v -> {
            Intent intent = new Intent(this, KYCVerificationActivity.class);
            startActivity(intent);
        });
    }

    private void fetchProfileData() {
        String userId = sessionManager.getUserId();

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        RetrofitClient.getApiService(this).getProfileData(sessionManager.getAuthToken(), userId).enqueue(new Callback<ProfileServices.ProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProfileServices.ProfileResponse> call, @NonNull Response<ProfileServices.ProfileResponse> response) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }

                if (response.isSuccessful() && response.body() != null) {
                    updateUI(response.body());
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
        DownloadService.Executor executor = new DownloadService.Executor(this, RetrofitClient.getApiService(this));
        
        executor.download(
            sessionManager.getAuthToken(),
            sessionManager.getUserId(),
            DownloadService.Req.profile(),
            new DownloadService.Executor.Callback() {
                @Override
                public void onSuccess(File file, DownloadService.Res raw) {
                    Glide.with(ProfileActivity.this)
                         .load(file)
                         .circleCrop()
                         .placeholder(R.drawable.pp__placeholder)
                         .error(R.drawable.pp__placeholder)
                         .into(ivProfileImage);
                }

                @Override
                public void onError(String error) {
                    Log.e("ProfileActivity", "Failed to load profile picture: " + error);
                }
            }
        );
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
}
