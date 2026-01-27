package com.farmo.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.farmo.activities.consumerActivities.ConsumerDashboardActivity;
import com.farmo.activities.farmerActivities.FarmerDashboardActivity;
import com.farmo.R;
import com.farmo.network.ApiService;
import com.farmo.network.auth.LoginRequest;
import com.farmo.network.auth.LoginResponse;
import com.farmo.network.RetrofitClient;
import com.farmo.network.auth.TokenLoginRequest;
import com.farmo.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etPassword;
    private CheckBox cbRememberMe;
    private ProgressDialog progressDialog;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sessionManager = new SessionManager(this);
        
        // Auto-login if session exists
        if (sessionManager.isLoggedIn()) {
            performTokenLogin();
            return; // Exit early to avoid showing login UI briefly
        }
        
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        cbRememberMe = findViewById(R.id.cb_remember_me);
        Button loginButton = findViewById(R.id.btn_login);
        TextView forgotPassword = findViewById(R.id.tv_forgot_password);
        TextView signUp = findViewById(R.id.tv_signup);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging in...");
        progressDialog.setCancelable(false);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        forgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, IdentifyUserActivity.class);
            startActivity(intent);
        });

        signUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    private void performTokenLogin() {
        String token = sessionManager.getAuthToken();
        String userId = sessionManager.getUserId();
        String deviceInfo = Build.MANUFACTURER + " " + Build.MODEL;

        TokenLoginRequest request = new TokenLoginRequest(token, "", userId, false, deviceInfo);

        RetrofitClient.getApiService(this).loginWithToken(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    sessionManager.saveSession(
                            loginResponse.getUserId(),
                            loginResponse.getUserType(),
                            loginResponse.getToken(),
                            loginResponse.getRefreshToken(),
                            true
                    );
                    goToDashboard(loginResponse.getUserId(), loginResponse.getUserType());
                } else {
                    sessionManager.clearSession();
                    // Re-show login UI
                    setContentView(R.layout.activity_login);
                    initViews();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Auto-login failed. Check connection.", Toast.LENGTH_SHORT).show();
                setContentView(R.layout.activity_login);
                initViews();
            }
        });
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        cbRememberMe = findViewById(R.id.cb_remember_me);
        findViewById(R.id.btn_login).setOnClickListener(v -> performLogin());
        findViewById(R.id.tv_forgot_password).setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, IdentifyUserActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.tv_signup).setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    private void performLogin() {
        String identifier = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        boolean rememberMe = cbRememberMe.isChecked();

        if (identifier.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter credentials", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();

        String deviceInfo = Build.MANUFACTURER + " " + Build.MODEL;
        LoginRequest loginRequest = new LoginRequest(identifier, password, false, deviceInfo);

        ApiService apiService = RetrofitClient.getApiService(this);
        apiService.login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    sessionManager.saveSession(
                            loginResponse.getUserId(),
                            loginResponse.getUserType(),
                            loginResponse.getToken(),
                            loginResponse.getRefreshToken(),
                            rememberMe
                    );
                    goToDashboard(loginResponse.getUserId(), loginResponse.getUserType());
                } else if (response.errorBody() != null) {
                    try {
                        String errorBody = response.errorBody().string();
                        LoginResponse errorResponse = new Gson().fromJson(errorBody, LoginResponse.class);
                        if (response.code() == 403 && errorResponse != null && "ACCOUNT_PENDING".equals(errorResponse.getErrorCode())) {
                            Intent intent = new Intent(LoginActivity.this, ActivateAccountActivity.class);
                            intent.putExtra("USER_ID", identifier);
                            intent.putExtra("CURRENT_PASSWORD", password);
                            startActivity(intent);
                        } else {
                            String msg = (errorResponse != null && errorResponse.getError() != null) ? errorResponse.getError() : "Error: " + response.code();
                            Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(LoginActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(LoginActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToDashboard(String userId, String userType) {
        if(userType.equalsIgnoreCase("farmer") || userType.equalsIgnoreCase("verifiedfarmer")){
            Intent intent = new Intent(LoginActivity.this, FarmerDashboardActivity.class);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("USER_TYPE", userType);
            startActivity(intent);
            finish();
        }
        else if (userType.equalsIgnoreCase("consumer") || userType.equalsIgnoreCase("verifiedconsumer")) {
            Intent intent = new Intent(LoginActivity.this, ConsumerDashboardActivity.class);
            intent.putExtra("USER_ID", userId);
            intent.putExtra("USER_TYPE", userType);
            startActivity(intent);
            finish();
        }
        else {
            Toast.makeText(LoginActivity.this, "Invalid user type", Toast.LENGTH_SHORT).show();
        }
    }
}
