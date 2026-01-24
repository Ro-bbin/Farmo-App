package com.example.farmo_app;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.farmo_app.network.MessageResponse;
import com.example.farmo_app.network.RetrofitClient;
import com.example.farmo_app.network.VerifyOtpRequest;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyOtpActivity extends AppCompatActivity {

    private TextView tvTimer, tvResendCode;
    private CountDownTimer countDownTimer;
    private Button btnVerify;
    private EditText etOtp;
    private LinearLayout btnBack;
    private ProgressDialog progressDialog;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        userId = getIntent().getStringExtra("USER_ID");

        tvTimer = findViewById(R.id.tvTimer);
        tvResendCode = findViewById(R.id.tvResendCode);
        btnVerify = findViewById(R.id.btnVerify);
        etOtp = findViewById(R.id.etOtp);
        btnBack = findViewById(R.id.btnBack);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Verifying OTP...");
        progressDialog.setCancelable(false);

        btnBack.setOnClickListener(v -> onBackPressed());

        startTimer();

        tvResendCode.setOnClickListener(v -> {
            Toast.makeText(this, "OTP Resent!", Toast.LENGTH_SHORT).show();
            startTimer();
        });

        btnVerify.setOnClickListener(v -> {
            String otp = etOtp.getText().toString().trim();
            
            if (otp.length() < 6) {
                Toast.makeText(this, "Please enter 6-digit OTP", Toast.LENGTH_SHORT).show();
            } else {
                verifyOtp(otp);
            }
        });
    }

    private void verifyOtp(String otp) {
        progressDialog.show();
        VerifyOtpRequest request = new VerifyOtpRequest(userId, otp);
        
        RetrofitClient.getApiService().verifyOtp(request).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                progressDialog.dismiss();
                if (response.code() == 202 || response.isSuccessful()) {
                    Intent intent = new Intent(VerifyOtpActivity.this, ResetPasswordActivity.class);
                    intent.putExtra("USER_ID", userId);
                    startActivity(intent);
                    finish();
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(VerifyOtpActivity.this, "Network error", Toast.LENGTH_SHORT).show();
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
                        : "Invalid or expired OTP";
                Toast.makeText(VerifyOtpActivity.this, msg, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(VerifyOtpActivity.this, "Invalid OTP", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(VerifyOtpActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        
        countDownTimer = new CountDownTimer(600000, 1000) {
            public void onTick(long millisUntilFinished) {
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                tvTimer.setText(String.format("Code expires in %02d:%02d", minutes, seconds));
                tvResendCode.setEnabled(false);
                tvResendCode.setTextColor(Color.GRAY);
            }

            public void onFinish() {
                tvTimer.setText("Code expired");
                tvResendCode.setEnabled(true);
                tvResendCode.setTextColor(Color.parseColor("#00473B"));
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
