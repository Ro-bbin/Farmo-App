package com.farmo.activities.commonActivities;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.farmo.R;
import com.farmo.network.CommonServices.AddressService;
import com.farmo.network.CommonServices.WalletService;
import com.farmo.network.MessageResponse;
import com.farmo.network.RetrofitClient;
import com.farmo.network.auth.ChangePassword;
import com.farmo.network.CommonServices.UpdateProfile;
import com.farmo.network.User.ProfileServices;
import com.farmo.network.consumer.ChangeToFarmer;
import com.farmo.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private UpdateProfile.Request pendingProfileRequest;
    private ProfileServices.ProfileResponse currentProfile;
    private AddressService.AddressResponse currentAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        sessionManager = new SessionManager(this);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Setup Change Password click listener
        View btnChangePassword = findViewById(R.id.setting_change_password);
        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        }

        // Setup Change PIN click listener
        View btnChangePin = findViewById(R.id.setting_change_pin);
        if (btnChangePin != null) {
            btnChangePin.setOnClickListener(v -> showChangePinDialog());
        }

        // Setup Contact Us click listener
        View btnContactUs = findViewById(R.id.setting_contact_us);
        if (btnContactUs != null) {
            btnContactUs.setOnClickListener(v -> openEmail());
        }

        // Setup About Us click listener
        View btnAboutUs = findViewById(R.id.setting_about_us);
        if (btnAboutUs != null) {
            btnAboutUs.setOnClickListener(v -> {
                Intent intent = new Intent(SettingsActivity.this, AboutUsActivity.class);
                startActivity(intent);
            });
        }

        // Check for update just Toaste Message:
        findViewById(R.id.setting_update).setOnClickListener(v -> {
            Toast.makeText(this, "Update is under development", Toast.LENGTH_SHORT).show();
        });

        // Edit Profile
        findViewById(R.id.setting_profile).setOnClickListener(v -> {
            fetchDataAndShowEdit();
        });

        setupChangeToFarmer();
    }

    private void setupChangeToFarmer() {
        String userType = sessionManager.getUserType();
        View btnChangeToFarmer = findViewById(R.id.setting_change_farmer);
        View divider = findViewById(R.id.divider_change_farmer);

        if ("Consumer".equalsIgnoreCase(userType) || "VerifiedConsumer".equalsIgnoreCase(userType)) {
            if (btnChangeToFarmer != null) {
                btnChangeToFarmer.setVisibility(View.VISIBLE);
                btnChangeToFarmer.setOnClickListener(v -> showChangeToFarmerInfoDialog());
            }
            if (divider != null) divider.setVisibility(View.VISIBLE);
        } else {
            if (btnChangeToFarmer != null) btnChangeToFarmer.setVisibility(View.GONE);
            if (divider != null) divider.setVisibility(View.GONE);
        }
    }

    private void showChangeToFarmerInfoDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_to_farmer, null);
        dialog.setContentView(dialogView);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
        }

        ImageView btnClose = dialogView.findViewById(R.id.btnClose);
        MaterialButton btnProceed = dialogView.findViewById(R.id.btnProceed);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (btnProceed != null) {
            btnProceed.setOnClickListener(v -> {
                dialog.dismiss();
                showChangeToFarmerPasswordDialog();
            });
        }

        dialog.show();
    }

    private void showChangeToFarmerPasswordDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_to_farmer_password, null);
        dialog.setContentView(dialogView);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
        }

        ImageView btnBack = dialogView.findViewById(R.id.btnBackToInfo);
        ImageView btnClose = dialogView.findViewById(R.id.btnClose);
        TextInputEditText etPassword = dialogView.findViewById(R.id.etPassword);
        MaterialButton btnChange = dialogView.findViewById(R.id.btnChange);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                dialog.dismiss();
                showChangeToFarmerInfoDialog();
            });
        }
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        if (btnChange != null) {
            btnChange.setOnClickListener(v -> {
                String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
                if (password.isEmpty()) {
                    etPassword.setError("Password is required");
                    return;
                }
                performChangeToFarmer(password, dialog);
            });
        }

        dialog.show();
    }

    private void performChangeToFarmer(String password, Dialog dialog) {
        ChangeToFarmer.Request request = new ChangeToFarmer.Request(password);
        
        RetrofitClient.getApiService(this)
                .changeToFarmer(sessionManager.getAuthToken(), sessionManager.getUserId(), request)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<MessageResponse> call, @NonNull Response<MessageResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(SettingsActivity.this, response.body().getMessage(), Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                            // Optionally logout or update session and restart app
                            sessionManager.clearSession();
                            Intent intent = new Intent(SettingsActivity.this, com.farmo.activities.authActivities.LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            String error = "Failed to change user type";
                            if (response.errorBody() != null) {
                                try {
                                    MessageResponse errorRes = new Gson().fromJson(response.errorBody().string(), MessageResponse.class);
                                    if (errorRes != null && errorRes.getMessage() != null) error = errorRes.getMessage();
                                } catch (Exception ignored) {}
                            }
                            Toast.makeText(SettingsActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<MessageResponse> call, @NonNull Throwable t) {
                        Toast.makeText(SettingsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openEmail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"officailfarmo@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Support Request - Farmo App");
        
        try {
            startActivity(Intent.createChooser(intent, "Send email using..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showChangePasswordDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        dialog.setContentView(dialogView);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
        }

        ImageView btnClose = dialogView.findViewById(R.id.btnClose);
        TextInputEditText etCurrentPassword = dialogView.findViewById(R.id.etCurrentPassword);
        TextInputEditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        TextInputEditText etRetypeNewPassword = dialogView.findViewById(R.id.etRetypeNewPassword);
        MaterialButton btnChangePassword = dialogView.findViewById(R.id.btnChangePassword);
        View btnCancel = dialogView.findViewById(R.id.btnCancel);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> {
                String currentPw = etCurrentPassword.getText().toString();
                String newPw = etNewPassword.getText().toString();
                String retypePw = etRetypeNewPassword.getText().toString();

                if (currentPw.isEmpty() || newPw.isEmpty() || retypePw.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!newPw.equals(retypePw)) {
                    Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Password complexity check (8-15 chars, alpha-numeric, special char)
                if (newPw.length() < 8 || newPw.length() > 15) {
                    Toast.makeText(this, "Password must be 8-15 characters", Toast.LENGTH_SHORT).show();
                    return;
                }

                performChangePassword(currentPw, newPw, dialog);
            });
        }

        dialog.show();
    }

    private void performChangePassword(String currentPw, String newPw, Dialog dialog) {
        ChangePassword.Request request = new ChangePassword.Request(currentPw, newPw);
        
        RetrofitClient.getApiService(this)
                .changePassword(sessionManager.getAuthToken(), sessionManager.getUserId(), request)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<MessageResponse> call, @NonNull Response<MessageResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(SettingsActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            String error = "Failed to change password";
                            if (response.errorBody() != null) {
                                try {
                                    MessageResponse errorRes = new Gson().fromJson(response.errorBody().string(), MessageResponse.class);
                                    if (errorRes != null) {
                                        if (errorRes.getError() != null) error = errorRes.getError();
                                        else if (errorRes.getMessage() != null) error = errorRes.getMessage();
                                        else if (errorRes.getError() != null) error = "Missing: " + errorRes.getError();
                                    }
                                } catch (Exception ignored) {}
                            }
                            Toast.makeText(SettingsActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<MessageResponse> call, @NonNull Throwable t) {
                        Toast.makeText(SettingsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showChangePinDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_transaction_pin, null);
        dialog.setContentView(dialogView);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvPinTitle);
        LinearLayout layoutChange = dialogView.findViewById(R.id.layoutChangePin);
        LinearLayout layoutForgot = dialogView.findViewById(R.id.layoutForgotPin);
        TextView tvToggle = dialogView.findViewById(R.id.tvToggleForgotPin);
        
        EditText etOldPin = dialogView.findViewById(R.id.etOldPin);
        EditText etLoginPassword = dialogView.findViewById(R.id.etLoginPassword);
        EditText etNewPin = dialogView.findViewById(R.id.etNewPin);
        EditText etConfirmPin = dialogView.findViewById(R.id.etConfirmPin);
        MaterialButton btnProceed = dialogView.findViewById(R.id.btnProceedPin);

        tvToggle.setOnClickListener(v -> {
            if (layoutChange.getVisibility() == View.VISIBLE) {
                layoutChange.setVisibility(View.GONE);
                layoutForgot.setVisibility(View.VISIBLE);
                tvTitle.setText("Forgot Wallet PIN");
                btnProceed.setText("Reset Wallet PIN");
                tvToggle.setText("Back to Change PIN");
            } else {
                layoutChange.setVisibility(View.VISIBLE);
                layoutForgot.setVisibility(View.GONE);
                tvTitle.setText("Change Wallet PIN");
                btnProceed.setText("Change Wallet PIN");
                tvToggle.setText("Forgot Wallet PIN?");
            }
        });

        btnProceed.setOnClickListener(v -> {
            String newPin = etNewPin.getText().toString();
            String confirmPin = etConfirmPin.getText().toString();

            if (newPin.length() != 4 || confirmPin.length() != 4) {
                Toast.makeText(this, "PIN must be 4 digits", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPin.equals(confirmPin)) {
                Toast.makeText(this, "PINs do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            WalletService.ChangePinRequest req = new WalletService.ChangePinRequest();
            req.setNewPin(newPin);

            if (layoutChange.getVisibility() == View.VISIBLE) {
                String oldPin = etOldPin.getText().toString();
                if (oldPin.length() != 4) {
                    Toast.makeText(this, "Enter 4-digit old PIN", Toast.LENGTH_SHORT).show();
                    return;
                }
                req.setOldPin(oldPin);
                performPinUpdate(req, true, dialog);
            } else {
                String password = etLoginPassword.getText().toString();
                if (password.isEmpty()) {
                    Toast.makeText(this, "Enter login password", Toast.LENGTH_SHORT).show();
                    return;
                }
                req.setPassword(password);
                performPinUpdate(req, false, dialog);
            }
        });

        ImageView btnClose = dialogView.findViewById(R.id.btnClosePin);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void performPinUpdate(WalletService.ChangePinRequest req, boolean isChange, Dialog dialog) {
        Call<MessageResponse> call = isChange 
                ? RetrofitClient.getApiService(this).changePin(sessionManager.getAuthToken(), sessionManager.getUserId(), req)
                : RetrofitClient.getApiService(this).forgetPin(sessionManager.getAuthToken(), sessionManager.getUserId(), req);

        call.enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(@NonNull Call<MessageResponse> call, @NonNull Response<MessageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(SettingsActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(SettingsActivity.this, "Action failed", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(@NonNull Call<MessageResponse> call, @NonNull Throwable t) {
                Toast.makeText(SettingsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchDataAndShowEdit() {
        RetrofitClient.getApiService(this)
                .getProfileData(sessionManager.getAuthToken(), sessionManager.getUserId())
                .enqueue(new Callback<ProfileServices.ProfileResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ProfileServices.ProfileResponse> call, @NonNull Response<ProfileServices.ProfileResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            currentProfile = response.body();
                            fetchAddressAndShowEdit();
                        } else {
                            Toast.makeText(SettingsActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ProfileServices.ProfileResponse> call, @NonNull Throwable t) {
                        Toast.makeText(SettingsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void fetchAddressAndShowEdit() {
        RetrofitClient.getApiService(this)
                .getOWNAddress(sessionManager.getAuthToken(), sessionManager.getUserId())
                .enqueue(new Callback<AddressService.AddressResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<AddressService.AddressResponse> call, @NonNull Response<AddressService.AddressResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            currentAddress = response.body();
                        }
                        showEditProfile();
                    }

                    @Override
                    public void onFailure(@NonNull Call<AddressService.AddressResponse> call, @NonNull Throwable t) {
                        showEditProfile(); // show anyway
                    }
                });
    }


    private void showEditProfile() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        dialog.setContentView(dialogView);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
        }

        // ── Views ──
        ImageView btnClose       = dialogView.findViewById(R.id.btnClose);
        TextInputEditText etFirstName      = dialogView.findViewById(R.id.etFirstName);
        TextInputEditText etMiddleName     = dialogView.findViewById(R.id.etMiddleName);
        TextInputEditText etLastName       = dialogView.findViewById(R.id.etLastName);
        TextInputEditText etPhone          = dialogView.findViewById(R.id.etPhone);
        TextInputEditText etSecondaryPhone = dialogView.findViewById(R.id.etSecondaryPhone);
        TextInputEditText etDob            = dialogView.findViewById(R.id.etDob);
        AutoCompleteTextView spinnerSex       = dialogView.findViewById(R.id.spinnerSex);
        AutoCompleteTextView spinnerProvince  = dialogView.findViewById(R.id.spinnerProvince);
        AutoCompleteTextView spinnerDistrict  = dialogView.findViewById(R.id.spinnerDistrict);
        AutoCompleteTextView spinnerMunicipal = dialogView.findViewById(R.id.spinnerMunicipal);
        TextInputEditText etWard           = dialogView.findViewById(R.id.etWard);
        TextInputEditText etTole           = dialogView.findViewById(R.id.etTole);
        TextInputEditText etFacebook       = dialogView.findViewById(R.id.etFacebook);
        TextInputEditText etWhatsapp       = dialogView.findViewById(R.id.etWhatsapp);
        TextInputEditText etAbout          = dialogView.findViewById(R.id.etAbout);
        MaterialButton btnNext             = dialogView.findViewById(R.id.btnNext);
        MaterialButton btnCancel           = dialogView.findViewById(R.id.btnCancel);

        // ── Populate Details ──
        if (currentProfile != null) {
            String fullName = currentProfile.getFullName();
            if (fullName != null) {
                String[] parts = fullName.split(" ");
                if (parts.length >= 1) etFirstName.setText(parts[0]);
                if (parts.length == 2) etLastName.setText(parts[1]);
                if (parts.length >= 3) {
                    etMiddleName.setText(parts[1]);
                    etLastName.setText(parts[2]);
                }
            }
            etPhone.setText(currentProfile.getPhone());
            etSecondaryPhone.setText(currentProfile.getPhone2());
            etDob.setText(currentProfile.getDob());
            spinnerSex.setText(currentProfile.getSex(), false);
            etFacebook.setText(currentProfile.getFacebook());
            etWhatsapp.setText(currentProfile.getWhatsapp());
            etAbout.setText(currentProfile.getAbout());
        }

        if (currentAddress != null) {
            spinnerProvince.setText(currentAddress.getProvince(), false);
            spinnerDistrict.setText(currentAddress.getDistrict(), false);
            spinnerMunicipal.setText(currentAddress.getMunicipal(), false);
            etWard.setText(currentAddress.getWard());
            etTole.setText(currentAddress.getTole());
        }

        // ── Verified User Restriction ──
        String userType = sessionManager.getUserType();
        boolean isVerified = "VerifiedFarmer".equals(userType) || "VerifiedConsumer".equals(userType);
        
        if (isVerified) {
            etFirstName.setEnabled(false);
            etMiddleName.setEnabled(false);
            etLastName.setEnabled(false);
            spinnerProvince.setEnabled(false);
            spinnerDistrict.setEnabled(false);
            spinnerMunicipal.setEnabled(false);
            etWard.setEnabled(false);
            etTole.setEnabled(false);
        }

        // ── Dropdowns ──
        String[] sexOptions       = {"Male", "Female", "Other"};
        String[] provinceOptions  = {"Province 1", "Madhesh", "Bagmati", "Gandaki", "Lumbini", "Karnali", "Sudurpashchim"};
        String[] districtOptions  = {"Kathmandu", "Lalitpur", "Bhaktapur"}; // update as needed
        String[] municipalOptions = {"Kathmandu Metropolitan", "Lalitpur Metropolitan"}; // update as needed

        spinnerSex.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, sexOptions));
        spinnerProvince.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, provinceOptions));
        spinnerDistrict.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, districtOptions));
        spinnerMunicipal.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, municipalOptions));

        // ── DOB Date Picker ──
        etDob.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
                etDob.setText(date);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // ── Close / Cancel ──
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        // ── Next → open password confirmation dialog ──
        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                if (!validateEditProfileForm(etFirstName, etLastName, etPhone,
                        etDob, spinnerSex, spinnerProvince, spinnerDistrict,
                        spinnerMunicipal, etWard, etTole)) {
                    return;
                }
                
                pendingProfileRequest = new UpdateProfile.Request(
                        etFirstName.getText().toString().trim(),
                        etMiddleName.getText().toString().trim(),
                        etLastName.getText().toString().trim(),
                        etPhone.getText().toString().trim(),
                        etSecondaryPhone.getText().toString().trim(),
                        etFacebook.getText().toString().trim(),
                        etWhatsapp.getText().toString().trim(),
                        spinnerProvince.getText().toString().trim(),
                        spinnerDistrict.getText().toString().trim(),
                        spinnerMunicipal.getText().toString().trim(),
                        etWard.getText().toString().trim(),
                        etTole.getText().toString().trim(),
                        etAbout.getText().toString().trim(),
                        etDob.getText().toString().trim(),
                        spinnerSex.getText().toString().trim()
                );

                dialog.dismiss();
                showEditProfileCheckPassword();
            });
        }

        dialog.show();
    }

    // ─────────────────────────────────────────────────────────────
//  Password confirmation dialog
// ─────────────────────────────────────────────────────────────
    private void showEditProfileCheckPassword() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile_check_password, null);
        dialog.setContentView(dialogView);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
        }

        ImageView btnBack              = dialogView.findViewById(R.id.btnBackToEdit);
        TextInputEditText etPassword   = dialogView.findViewById(R.id.etConfirmPassword);
        MaterialButton btnSubmit       = dialogView.findViewById(R.id.btnSubmit);
        MaterialButton btnCancel       = dialogView.findViewById(R.id.btnCancelConfirm);

        // Back arrow → go back to edit profile
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                dialog.dismiss();
                showEditProfile();
            });
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                String password = etPassword.getText() != null
                        ? etPassword.getText().toString().trim() : "";

                if (password.isEmpty()) {
                    etPassword.setError("Password is required");
                    etPassword.requestFocus();
                    return;
                }

                submitEditProfile(dialog);
            });
        }

        dialog.show();
    }

    private void submitEditProfile(Dialog dialog) {
        if (pendingProfileRequest == null) return;

        RetrofitClient.getApiService(this)
                .updateProfile(sessionManager.getAuthToken(), sessionManager.getUserId(), pendingProfileRequest)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<MessageResponse> call, @NonNull Response<MessageResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(SettingsActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            String error = "Failed to update profile";
                            if (response.errorBody() != null) {
                                try {
                                    MessageResponse errorRes = new Gson().fromJson(response.errorBody().string(), MessageResponse.class);
                                    if (errorRes != null) {
                                        if (errorRes.getError() != null) error = errorRes.getError();
                                        else if (errorRes.getMessage() != null) error = errorRes.getMessage();
                                        else if (errorRes.getError() != null) error = "Missing: " + errorRes.getError();
                                    }
                                } catch (Exception ignored) {}
                            }
                            Toast.makeText(SettingsActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<MessageResponse> call, @NonNull Throwable t) {
                        Toast.makeText(SettingsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────
//  Form validation helper
// ─────────────────────────────────────────────────────────────
    private boolean validateEditProfileForm(
            TextInputEditText etFirstName, TextInputEditText etLastName,
            TextInputEditText etPhone, TextInputEditText etDob,
            AutoCompleteTextView spinnerSex, AutoCompleteTextView spinnerProvince,
            AutoCompleteTextView spinnerDistrict, AutoCompleteTextView spinnerMunicipal,
            TextInputEditText etWard, TextInputEditText etTole) {

        String firstName  = etFirstName.getText()  != null ? etFirstName.getText().toString().trim()  : "";
        String lastName   = etLastName.getText()   != null ? etLastName.getText().toString().trim()   : "";
        String phone      = etPhone.getText()      != null ? etPhone.getText().toString().trim()      : "";
        String dob        = etDob.getText()        != null ? etDob.getText().toString().trim()        : "";
        String sex        = spinnerSex.getText().toString().trim();
        String province   = spinnerProvince.getText().toString().trim();
        String district   = spinnerDistrict.getText().toString().trim();
        String municipal  = spinnerMunicipal.getText().toString().trim();
        String ward       = etWard.getText()       != null ? etWard.getText().toString().trim()       : "";
        String tole       = etTole.getText()       != null ? etTole.getText().toString().trim()       : "";

        if (firstName.isEmpty())  { etFirstName.setError("First name is required");   etFirstName.requestFocus();  return false; }
        if (lastName.isEmpty())   { etLastName.setError("Last name is required");     etLastName.requestFocus();   return false; }
        if (phone.isEmpty())      { etPhone.setError("Phone is required");            etPhone.requestFocus();      return false; }
        if (dob.isEmpty())        { etDob.setError("Date of birth is required");      etDob.requestFocus();        return false; }
        if (sex.isEmpty())        { spinnerSex.setError("Sex is required");           spinnerSex.requestFocus();   return false; }
        if (province.isEmpty())   { spinnerProvince.setError("Province is required"); spinnerProvince.requestFocus(); return false; }
        if (district.isEmpty())   { spinnerDistrict.setError("District is required"); spinnerDistrict.requestFocus(); return false; }
        if (municipal.isEmpty())  { spinnerMunicipal.setError("Municipal is required"); spinnerMunicipal.requestFocus(); return false; }
        if (ward.isEmpty())       { etWard.setError("Ward is required");              etWard.requestFocus();       return false; }
        if (tole.isEmpty())       { etTole.setError("Tole is required");              etTole.requestFocus();       return false; }

        return true;
    }
}
