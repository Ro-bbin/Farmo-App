package com.farmo.activities.authActivities;

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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.farmo.R;
import com.farmo.network.MessageResponse;
import com.farmo.network.RetrofitClient;
import com.farmo.network.auth.VerifyOtpRequest;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FP_VerifyOtpActivity extends AppCompatActivity {

    private TextView tvTimer, tvResendCode;
    private CountDownTimer countDownTimer;
    private EditText etOtp;
    private ProgressDialog progressDialog;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        userId = getIntent().getStringExtra("USER_ID");

        tvTimer = findViewById(R.id.tvTimer);
        tvResendCode = findViewById(R.id.tvResendCode);
        Button btnVerify = findViewById(R.id.btnVerify);
        etOtp = findViewById(R.id.etOtp);
        LinearLayout btnBack = findViewById(R.id.btnBack);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Verifying OTP...");
        progressDialog.setCancelable(false);

        btnBack.setOnClickListener(v -> onBackPressed());

        startTimer(5);

        tvResendCode.setOnClickListener(v -> {
            Toast.makeText(this, "OTP Resent!", Toast.LENGTH_SHORT).show();
            startTimer(5);
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
        
        RetrofitClient.getApiService(this).verifyOtp(request).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(@NonNull Call<MessageResponse> call, @NonNull Response<MessageResponse> response) {
                progressDialog.dismiss();
                if (response.code() == 202 || response.isSuccessful()) {
                    Intent intent = new Intent(FP_VerifyOtpActivity.this, FP_ResetPasswordActivity.class);
                    intent.putExtra("USER_ID", userId);
                    startActivity(intent);
                    finish();
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<MessageResponse> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(FP_VerifyOtpActivity.this, "Network error", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(FP_VerifyOtpActivity.this, msg, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(FP_VerifyOtpActivity.this, "Invalid OTP", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(FP_VerifyOtpActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startTimer(int minutes) {
        if (countDownTimer != null) countDownTimer.cancel();

        long durationInMillis = (long) minutes * 60 * 1000; // convert minutes to milliseconds

        countDownTimer = new CountDownTimer(durationInMillis, 1000) {
            public void onTick(long millisUntilFinished) {
                int min = (int) (millisUntilFinished / 1000) / 60;
                int sec = (int) (millisUntilFinished / 1000) % 60;
                tvTimer.setText(String.format("Code expires in %02d:%02d", min, sec));
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
