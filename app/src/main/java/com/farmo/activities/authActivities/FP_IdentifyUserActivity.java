package com.farmo.activities.authActivities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.farmo.R;
import com.farmo.network.auth.ForgotPasswordRequest;
import com.farmo.network.auth.ForgotPasswordResponse;
import com.farmo.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FP_IdentifyUserActivity extends AppCompatActivity {

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identify_user);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Identifying user...");
        progressDialog.setCancelable(false);

        LinearLayout btnBack = findViewById(R.id.btnBack);
        EditText etUsernameOrPhone = findViewById(R.id.etUsernameOrPhone);
        Button btnIdentifyUser = findViewById(R.id.btnIdentifyUser);
        TextView tvContactSupport = findViewById(R.id.tvContactSupport);

        // Back button logic
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Identify user logic
        if (btnIdentifyUser != null) {
            btnIdentifyUser.setOnClickListener(v -> {
                if (etUsernameOrPhone != null) {
                    String input = etUsernameOrPhone.getText().toString().trim();
                    if (input.isEmpty()) {
                        etUsernameOrPhone.setError("Please enter username or phone");
                    } else {
                        performIdentification(input);
                    }
                }
            });
        }

        // Clickable and bold "Contact Farmo Support"
        if (tvContactSupport != null) {
            String baseText = getString(R.string.cant_remember_details, "");
            String clickablePart = getString(R.string.contact_farmo_support);
            String fullText = baseText.replace("%1$s", clickablePart);

            SpannableString ss = new SpannableString(fullText);
            int startIndex = fullText.indexOf(clickablePart);
            if (startIndex != -1) {
                int endIndex = startIndex + clickablePart.length();

                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        Toast.makeText(FP_IdentifyUserActivity.this, "Opening support...", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setUnderlineText(false);
                        ds.setColor(getResources().getColor(R.color.topical_forest));
                    }
                };

                ss.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ss.setSpan(new StyleSpan(Typeface.BOLD), startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                tvContactSupport.setText(ss);
                tvContactSupport.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }

    private void performIdentification(String identifier) {
        progressDialog.show();
        ForgotPasswordRequest request = new ForgotPasswordRequest(identifier);
        
        RetrofitClient.getApiService(this).forgotPassword(request).enqueue(new Callback<ForgotPasswordResponse>() {
            @Override
            public void onResponse(@NonNull Call<ForgotPasswordResponse> call, @NonNull Response<ForgotPasswordResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    ForgotPasswordResponse res = response.body();
                    
                    Intent intent = new Intent(FP_IdentifyUserActivity.this, FP_ForgotPasswordActivity.class);
                    intent.putExtra("USER_ID", res.getUserId());
                    intent.putExtra("HALF_EMAIL", res.getHalfEmail());
                    startActivity(intent);
                } else {
                    Toast.makeText(FP_IdentifyUserActivity.this, "User not found or inactive", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ForgotPasswordResponse> call, @NonNull Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(FP_IdentifyUserActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
