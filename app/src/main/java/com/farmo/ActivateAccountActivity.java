package com.farmo;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.farmo.network.ActivateAccountRequest;
import com.farmo.network.MessageResponse;
import com.farmo.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivateAccountActivity extends AppCompatActivity {

    private TextInputEditText etOldPassword, etNewPassword, etConfirmNewPassword;
    private MaterialButton btnActivate;
    private String userId;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activate_account);

        userId = getIntent().getStringExtra("USER_ID");
        String currentPassword = getIntent().getStringExtra("CURRENT_PASSWORD");

        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);
        btnActivate = findViewById(R.id.btnActivate);

        if (currentPassword != null) {
            etOldPassword.setText(currentPassword);
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Activating account...");
        progressDialog.setCancelable(false);

        btnActivate.setOnClickListener(v -> {
            String oldPass = etOldPassword.getText().toString();
            String newPass = etNewPassword.getText().toString();
            String confirmPass = etConfirmNewPassword.getText().toString();

            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                etConfirmNewPassword.setError("Passwords do not match");
                return;
            }

            performActivation(oldPass, newPass);
        });
    }

    private void performActivation(String oldPass, String newPass) {
        progressDialog.show();
        ActivateAccountRequest request = new ActivateAccountRequest(userId, oldPass, newPass);

        RetrofitClient.getApiService().activateAccount(request).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful()) {
                    Toast.makeText(ActivateAccountActivity.this, "Account Activated Successfully!", Toast.LENGTH_LONG).show();
                    finish(); // Go back to login
                } else {
                    Toast.makeText(ActivateAccountActivity.this, "Activation failed. Check your current password.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(ActivateAccountActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
