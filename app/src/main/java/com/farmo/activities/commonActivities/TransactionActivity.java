package com.farmo.activities.commonActivities;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.farmo.R;
import com.farmo.network.ApiService;
import com.farmo.network.CommonServices.TransactionService;
import com.farmo.network.RetrofitClient;
import com.farmo.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransactionActivity extends AppCompatActivity {

    private static final String TAG = "TransactionActivity";

    private LinearLayout transactionContainer;
    private LinearLayout noDataView;
    private MaterialButton btn7, btn14, btnMonth;
    private Spinner spinnerTransType;
    private SwipeRefreshLayout swipeRefreshLayout;

    private View btnDateFrom, btnDateTo;
    private TextView tvDateFrom, tvDateTo;

    private SessionManager sessionManager;
    private ApiService apiService;

    private String selectedType = "All"; // "All", "CR", "DR"
    private String dateFrom = "";
    private String dateTo = "";
    private int currentPage = 1;

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getApiService(this);

        initViews();
        setupFilters();
        
        // Initial load: 7 days
        selectTimeFilter(7, btn7);
    }

    private void initViews() {
        transactionContainer = findViewById(R.id.transactionContainer);
        noDataView = findViewById(R.id.noDataView);
        btn7 = findViewById(R.id.btn7Days);
        btn14 = findViewById(R.id.btn14Days);
        btnMonth = findViewById(R.id.btn1Month);
        spinnerTransType = findViewById(R.id.spinnerTransType);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshTransactions);

        btnDateFrom = findViewById(R.id.btnDateFrom);
        btnDateTo = findViewById(R.id.btnDateTo);
        tvDateFrom = findViewById(R.id.tvDateFrom);
        tvDateTo = findViewById(R.id.tvDateTo);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        swipeRefreshLayout.setOnRefreshListener(this::fetchTransactions);
    }

    private void setupFilters() {
        // Type Spinner
        String[] types = {"All", "Credit (CR)", "Debit (DR)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTransType.setAdapter(adapter);

        spinnerTransType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String type = "All";
                if (position == 1) type = "CR";
                if (position == 2) type = "DR";
                
                if (!type.equals(selectedType)) {
                    selectedType = type;
                    fetchTransactions();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Time Buttons
        btn7.setOnClickListener(v -> selectTimeFilter(7, btn7));
        btn14.setOnClickListener(v -> selectTimeFilter(14, btn14));
        btnMonth.setOnClickListener(v -> selectTimeFilter(30, btnMonth));

        // Date Pickers
        btnDateFrom.setOnClickListener(v -> showDatePicker(true));
        btnDateTo.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isFrom) {
        Calendar cal = Calendar.getInstance();
        try {
            String currentStr = isFrom ? dateFrom : dateTo;
            if (!currentStr.isEmpty()) {
                Date d = sdf.parse(currentStr);
                if (d != null) cal.setTime(d);
            }
        } catch (ParseException ignored) {}

        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth);
            String formatted = sdf.format(selected.getTime());

            if (isFrom) {
                if (validateGap(formatted, dateTo)) {
                    dateFrom = formatted;
                    tvDateFrom.setText(dateFrom);
                    updateQuickButtonsUI(null);
                    fetchTransactions();
                }
            } else {
                if (validateGap(dateFrom, formatted)) {
                    dateTo = formatted;
                    tvDateTo.setText(dateTo);
                    updateQuickButtonsUI(null);
                    fetchTransactions();
                }
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        picker.getDatePicker().setMaxDate(System.currentTimeMillis());
        picker.show();
    }

    private boolean validateGap(String from, String to) {
        if (from.isEmpty() || to.isEmpty()) return true;
        try {
            Date dFrom = sdf.parse(from);
            Date dTo = sdf.parse(to);
            if (dFrom == null || dTo == null) return true;

            if (dFrom.after(dTo)) {
                Toast.makeText(this, "From date cannot be after To date", Toast.LENGTH_SHORT).show();
                return false;
            }

            long diff = dTo.getTime() - dFrom.getTime();
            long days = diff / (24 * 60 * 60 * 1000);
            if (days > 90) {
                Toast.makeText(this, "Maximum date gap is 90 days", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        } catch (ParseException e) {
            return true;
        }
    }

    private void selectTimeFilter(int days, MaterialButton selectedBtn) {
        updateQuickButtonsUI(selectedBtn);
        
        Calendar cal = Calendar.getInstance();
        dateTo = sdf.format(cal.getTime());
        tvDateTo.setText(dateTo);
        
        cal.add(Calendar.DAY_OF_YEAR, -days);
        dateFrom = sdf.format(cal.getTime());
        tvDateFrom.setText(dateFrom);
        
        fetchTransactions();
    }

    private void fetchTransactions() {
        swipeRefreshLayout.setRefreshing(true);
        transactionContainer.removeAllViews();

        TransactionService.TransactionHistoryRequest request = new TransactionService.TransactionHistoryRequest(
                selectedType, dateTo, dateFrom, currentPage
        );

        apiService.getTransactionHistory(sessionManager.getAuthToken(), sessionManager.getUserId(), request)
                .enqueue(new Callback<TransactionService.TransactionHistoryResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<TransactionService.TransactionHistoryResponse> call, 
                                           @NonNull Response<TransactionService.TransactionHistoryResponse> response) {
                        swipeRefreshLayout.setRefreshing(false);
                        if (response.isSuccessful() && response.body() != null) {
                            renderTransactions(response.body().getData());
                        } else {
                            showNoData();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<TransactionService.TransactionHistoryResponse> call, @NonNull Throwable t) {
                        swipeRefreshLayout.setRefreshing(false);
                        Log.e(TAG, "API error", t);
                        showNoData();
                    }
                });
    }

    private void renderTransactions(List<TransactionService.data> data) {
        if (data == null || data.isEmpty()) {
            showNoData();
            return;
        }

        noDataView.setVisibility(View.GONE);
        transactionContainer.setVisibility(View.VISIBLE);

        for (TransactionService.data day : data) {
            addDateHeader(day.getDate(), day.getMonthYear(), day.getClosingBalance());
            for (TransactionService.Transaction txn : day.getTransactions()) {
                addTransactionCard(txn);
            }
        }
    }

    private void addDateHeader(String date, String monthYear, String balance) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_date_header, transactionContainer, false);
        ((TextView) v.findViewById(R.id.tvDayNumber)).setText(date);
        ((TextView) v.findViewById(R.id.tvMonthYear)).setText(monthYear);
        ((TextView) v.findViewById(R.id.tvClosingBalance)).setText(balance);
        transactionContainer.addView(v);
    }

    private void addTransactionCard(TransactionService.Transaction txn) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_transaction_card, transactionContainer, false);
        TextView tvAmt = v.findViewById(R.id.tvTxnAmount);
        ((TextView) v.findViewById(R.id.tvTxnTitle)).setText(txn.getFullName() != null ? txn.getFullName() : "Transaction");
        ((TextView) v.findViewById(R.id.tvTxnSubtitle)).setText(txn.getForProduct() != null ? txn.getForProduct() : "Wallet adjustment");
        ((TextView) v.findViewById(R.id.tvTxnTimestamp)).setText(txn.getTimestamp());

        boolean isCredit = "credit".equalsIgnoreCase(txn.getType());
        tvAmt.setText(txn.getAmount());
        tvAmt.setTextColor(isCredit ? Color.parseColor("#2E7D32") : Color.parseColor("#C62828"));
        
        transactionContainer.addView(v);
    }

    private void showNoData() {
        transactionContainer.setVisibility(View.GONE);
        noDataView.setVisibility(View.VISIBLE);
    }

    private void updateQuickButtonsUI(MaterialButton activeBtn) {
        MaterialButton[] buttons = {btn7, btn14, btnMonth};
        for (MaterialButton btn : buttons) {
            if (btn == null) continue;
            if (btn == activeBtn) {
                btn.setBackgroundColor(getResources().getColor(R.color.topical_forest));
                btn.setTextColor(Color.WHITE);
            } else {
                btn.setBackgroundColor(Color.TRANSPARENT);
                btn.setTextColor(getResources().getColor(R.color.topical_forest));
            }
        }
    }
}
