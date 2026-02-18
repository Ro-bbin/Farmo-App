package com.farmo.activities.authActivities;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.farmo.R;
import com.farmo.network.MessageResponse;
import com.farmo.network.auth.RegisterRequest;
import com.farmo.network.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    private TextInputEditText etUserId, etPassword, etConfirmPassword, etFirstName, etMiddleName, etLastName;
    private TextInputEditText etWard, etTole, etMobile, etSecondaryMobile, etEmail, etWhatsapp, etFacebook;
    private EditText etDOB, etAbout;
    private RadioGroup rgUserType, rgSex;
    private AutoCompleteTextView spinnerProvince, spinnerDistrict, spinnerMunicipality;
    private JSONObject locationsJson;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize views
        etUserId = findViewById(R.id.etUserId);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etFirstName = findViewById(R.id.etFirstName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etLastName = findViewById(R.id.etLastName);
        etWard = findViewById(R.id.etWard);
        etTole = findViewById(R.id.etTole);
        etMobile = findViewById(R.id.etMobile);
        etSecondaryMobile = findViewById(R.id.etSecondaryMobile);
        etEmail = findViewById(R.id.etEmail);
        etWhatsapp = findViewById(R.id.etWhatsapp);
        etFacebook = findViewById(R.id.etFacebook);
        etDOB = findViewById(R.id.etDOB);
        etAbout = findViewById(R.id.etAbout);
        rgUserType = findViewById(R.id.rgUserType);
        rgSex = findViewById(R.id.rgSex);
        spinnerProvince = findViewById(R.id.spinnerProvince);
        spinnerDistrict = findViewById(R.id.spinnerDistrict);
        spinnerMunicipality = findViewById(R.id.spinnerMunicipality);
        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnCancel = findViewById(R.id.btnCancel);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Registering...");
        progressDialog.setCancelable(false);

        etDOB.setOnClickListener(v -> showDatePickerDialog());
        btnCancel.setOnClickListener(v -> finish());
        btnRegister.setOnClickListener(v -> performRegistration());

        setupLocationSpinners();
    }

    private void performRegistration() {
        String userId = etUserId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (userId.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        int selectedUserTypeId = rgUserType.getCheckedRadioButtonId();
        String userType = "Consumer";
        if (selectedUserTypeId == R.id.rbFarmer) userType = "Farmer";

        int selectedSexId = rgSex.getCheckedRadioButtonId();
        String sex = "Male";
        if (selectedSexId == R.id.rbFemale) sex = "Female";

        RegisterRequest request = new RegisterRequest(
                userId,
                password,
                etFirstName.getText().toString().trim(),
                etMiddleName.getText().toString().trim(),
                etLastName.getText().toString().trim(),
                sex,
                etDOB.getText().toString().trim(), // Ensure format matches Django's parse_date (YYYY-MM-DD)
                userType,
                spinnerProvince.getText().toString(),
                spinnerDistrict.getText().toString(),
                spinnerMunicipality.getText().toString(),
                etWard.getText().toString().trim(),
                etTole.getText().toString().trim(),
                etMobile.getText().toString().trim(),
                etSecondaryMobile.getText().toString().trim(),
                etEmail.getText().toString().trim(),
                etFacebook.getText().toString().trim(),
                etWhatsapp.getText().toString().trim(),
                etAbout.getText().toString().trim(),
                "Itself" // Default created_by
        );

        progressDialog.show();
        RetrofitClient.getApiService(this).register(request).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(@NonNull Call<MessageResponse> call, @NonNull Response<MessageResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(SignupActivity.this, response.body().getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(SignupActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleErrorResponse(Response<?> response) {
        if (response.errorBody() != null) {
            try {
                String errorBody = response.errorBody().string();
                MessageResponse errorRes = new Gson().fromJson(errorBody, MessageResponse.class);
                String msg = (errorRes != null && errorRes.getError() != null) ? errorRes.getError() : "Registration Failed";
                Toast.makeText(SignupActivity.this, msg, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(SignupActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(SignupActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupLocationSpinners() {
        String jsonString = loadJSONFromAsset();
        if (jsonString == null) return;

        try {
            locationsJson = new JSONObject(jsonString);
            List<String> provinceList = new ArrayList<>();
            Iterator<String> keys = locationsJson.keys();
            while (keys.hasNext()) {
                provinceList.add(keys.next());
            }

            ArrayAdapter<String> provinceAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, provinceList);
            spinnerProvince.setAdapter(provinceAdapter);

            spinnerProvince.setOnItemClickListener((parent, view, position, id) -> {
                String selectedProvince = (String) parent.getItemAtPosition(position);
                updateDistrictSpinner(selectedProvince);
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateDistrictSpinner(String provinceName) {
        try {
            JSONObject districtsJson = locationsJson.getJSONObject(provinceName);
            List<String> districtList = new ArrayList<>();
            Iterator<String> keys = districtsJson.keys();
            while (keys.hasNext()) {
                districtList.add(keys.next());
            }

            ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, districtList);
            spinnerDistrict.setText("");
            spinnerMunicipality.setText("");
            spinnerDistrict.setAdapter(districtAdapter);

            spinnerDistrict.setOnItemClickListener((parent, view, position, id) -> {
                String selectedDistrict = (String) parent.getItemAtPosition(position);
                updateMunicipalitySpinner(provinceName, selectedDistrict);
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateMunicipalitySpinner(String provinceName, String districtName) {
        try {
            JSONObject provinceJson = locationsJson.getJSONObject(provinceName);
            JSONObject districtJson = provinceJson.getJSONObject(districtName);
            List<String> municipalityList = new ArrayList<>();

            String[] types = {"Ma.Na.Pa.", "Upa.Ma.", "Na.Pa.", "Ga.Pa."};
            for (String type : types) {
                if (districtJson.has(type)) {
                    JSONArray typeArray = districtJson.getJSONArray(type);
                    for (int i = 0; i < typeArray.length(); i++) {
                        municipalityList.add(typeArray.getString(i));
                    }
                }
            }

            ArrayAdapter<String> municipalityAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, municipalityList);
            spinnerMunicipality.setText("");
            spinnerMunicipality.setAdapter(municipalityAdapter);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String loadJSONFromAsset() {
        String json;
        try {
            InputStream is = getAssets().open("provinces_with_districts_and_municipalities.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            //ex.printStackTrace();
            return null;
        }
        return json;
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    // Django parse_date expects YYYY-MM-DD
                    String selectedDate = year1 + "-" + (monthOfYear + 1) + "-" + dayOfMonth;
                    etDOB.setText(selectedDate);
                },
                year, month, day);
        datePickerDialog.show();
    }
}
