package com.example.farmo_app;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.farmo_app.network.RetrofitClient;
import com.example.farmo_app.network.UserProfileResponse;
import com.example.farmo_app.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvProfileName, tvProfileUserType;
    private View itemUserId, itemPhone, itemEmail, itemSex, itemDob, itemAddress;
    private ProgressDialog progressDialog;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);
        String userId = sessionManager.getUserId();

        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileUserType = findViewById(R.id.tvProfileUserType);
        
        itemUserId = findViewById(R.id.itemUserId);
        itemPhone = findViewById(R.id.itemPhone);
        itemEmail = findViewById(R.id.itemEmail);
        itemSex = findViewById(R.id.itemSex);
        itemDob = findViewById(R.id.itemDob);
        itemAddress = findViewById(R.id.itemAddress);
        
        setupItem(itemUserId, "User ID");
        setupItem(itemPhone, "Phone Number");
        setupItem(itemEmail, "Email Address");
        setupItem(itemSex, "Sex");
        setupItem(itemDob, "Date of Birth");
        setupItem(itemAddress, "Address");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnLogout).setOnClickListener(v -> performLogout());

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading Profile...");
        progressDialog.setCancelable(false);

        if (userId != null) {
            fetchUserProfile(userId);
        } else {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupItem(View view, String label) {
        TextView tvLabel = view.findViewById(R.id.tvLabel);
        tvLabel.setText(label);
    }

    private void setItemValue(View view, String value) {
        TextView tvValue = view.findViewById(R.id.tvValue);
        tvValue.setText(value != null ? value : "N/A");
    }

    private void fetchUserProfile(String userId) {
        progressDialog.show();
        RetrofitClient.getApiService().getUserProfile(userId).enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(Call<UserProfileResponse> call, Response<UserProfileResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    displayProfile(response.body());
                } else {
                    Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UserProfileResponse> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(ProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayProfile(UserProfileResponse profile) {
        tvProfileName.setText(profile.getFullName());
        tvProfileUserType.setText(profile.getUserType());
        
        setItemValue(itemUserId, profile.getUserId());
        setItemValue(itemPhone, profile.getPhone());
        setItemValue(itemEmail, profile.getEmail());
        setItemValue(itemSex, profile.getSex());
        setItemValue(itemDob, profile.getDob());
        
        String fullAddress = profile.getTole() + ", Ward " + profile.getWard() + "\n" +
                profile.getMunicipality() + ", " + profile.getDistrict() + "\n" +
                profile.getProvince();
        setItemValue(itemAddress, fullAddress);
    }

    private void performLogout() {
        sessionManager.clearSession();
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
