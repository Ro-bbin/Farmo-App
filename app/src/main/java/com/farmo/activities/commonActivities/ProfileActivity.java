package com.farmo.activities.commonActivities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.farmo.R;
import com.farmo.activities.authActivities.LoginActivity;
import com.farmo.network.RetrofitClient;
import com.farmo.network.User.ProfileResponse;
import com.farmo.utils.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private ImageView ivProfileImage;
    private TextView tvProfileName, tvProfileUserType, tvAbout;
    private SessionManager sessionManager;

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

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            sessionManager.clearSession();
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
        });
    }

    private void fetchProfileData() {
        String token = "Bearer " + sessionManager.getAuthToken();
        String userId = sessionManager.getUserId();

        RetrofitClient.getApiService(this).getProfileData(token, userId).enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<ProfileResponse> call, @NonNull Response<ProfileResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateUI(response.body());
                } else {
                    Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ProfileResponse> call, @NonNull Throwable t) {
                Toast.makeText(ProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(ProfileResponse data) {
        tvProfileName.setText(data.getFullName());
        tvProfileUserType.setText(data.getUserType());
        tvAbout.setText(data.getAbout() != null ? data.getAbout() : "No info provided.");

        // Decode Base64 Image
        if (data.getProfilePicture() != null) {
            byte[] decodedString = Base64.decode(data.getProfilePicture(), Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            ivProfileImage.setImageBitmap(decodedByte);
        }

        // Setup Details
        setupInfoItem(R.id.itemEmail, "Email", data.getEmail());
        setupInfoItem(R.id.itemPhone, "Phone", data.getPhone());
        setupInfoItem(R.id.itemAddress, "Address", data.getAddress());
        setupInfoItem(R.id.itemSex, "Gender", data.getSex());
        setupInfoItem(R.id.itemDob, "DOB", data.getDob());
        setupInfoItem(R.id.itemWhatsapp, "WhatsApp", data.getWhatsapp());
        setupInfoItem(R.id.itemFacebook, "Facebook", data.getFacebook());
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
