package com.farmo.activities.authActivities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.farmo.R;
import com.farmo.network.auth.ForgotPasswordChangePasswordRequest;
import com.farmo.network.MessageResponse;
import com.farmo.network.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FP_ResetPasswordActivity extends AppCompatActivity {

    private TextInputEditText etNewPassword, etConfirmPassword;
    private String userId;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reset_password);

        userId = getIntent().getStringExtra("USER_ID");

        // Reference the correct root ID from activity_reset_password.xml
        View mainView = findViewById(R.id.forgotresetpassword);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                // We keep the left/right padding from XML (24dp) and only handle top/bottom system bars
                v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), systemBars.bottom);
                return insets;
            });
        }

        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        Button btnResetPassword = findViewById(R.id.btnResetPassword);
        LinearLayout btnBack = findViewById(R.id.btnBack);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Updating password...");
        progressDialog.setCancelable(false);

        // Back button logic
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        btnResetPassword.setOnClickListener(v -> {
            String newPass = etNewPassword.getText().toString();
            String confirmPass = etConfirmPassword.getText().toString();

            if (newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else if (!newPass.equals(confirmPass)) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
            } else {
                performPasswordChange(newPass);
            }
        });
    }

    private void performPasswordChange(String password) {
        progressDialog.show();
        ForgotPasswordChangePasswordRequest request = new ForgotPasswordChangePasswordRequest(userId, password);
        
        RetrofitClient.getApiService(this).changePassword(request).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(@NonNull Call<MessageResponse> call, @NonNull Response<MessageResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    String successMsg = response.body().getMessage();
                    if (successMsg == null || successMsg.isEmpty()) {
                        successMsg = "Password changed successfully!";
                    }
                    Toast.makeText(FP_ResetPasswordActivity.this, successMsg, Toast.LENGTH_LONG).show();
                    
                    Intent intent = new Intent(FP_ResetPasswordActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<MessageResponse> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(FP_ResetPasswordActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleErrorResponse(Response<?> response) {
        if (response.errorBody() != null) {
            try {
                String errorBody = response.errorBody().string();
                MessageResponse errorResponse = new Gson().fromJson(errorBody, MessageResponse.class);
                String msg = (errorResponse != null && errorResponse.getError() != null) 
                        ? errorResponse.getError() 
                        : "Error: " + response.code();
                Toast.makeText(FP_ResetPasswordActivity.this, msg, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(FP_ResetPasswordActivity.this, "Failed to reset password", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(FP_ResetPasswordActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
        }
    }
}
