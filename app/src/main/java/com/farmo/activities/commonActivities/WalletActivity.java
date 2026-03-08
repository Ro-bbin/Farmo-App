package com.farmo.activities.commonActivities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.farmo.R;
import com.farmo.network.ApiService;
import com.farmo.network.MessageResponse;
import com.farmo.network.RetrofitClient;
import com.farmo.network.CommonServices.TransactionService;
import com.farmo.network.CommonServices.WalletService;
import com.farmo.network.Dashboard.RefreshWallet;
import com.farmo.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletActivity extends AppCompatActivity {

    private static final String TAG = "WalletActivity";

    private TextView tvWalletBalance, tvTodayIncome, tvTodayExpense;
    private ImageView ivVisibility;
    private LinearLayout layoutTodayIncome, layoutTodayExpense;
    private RecyclerView rvTransactions;
    private TextView tvSeeMore;
    private MaterialButton btnAddMoney, btnWithdraw, btnPayOrder;
    private MaterialButton btn7, btn14, btnMonth;
    private SwipeRefreshLayout swipeRefreshLayout;

    private boolean isBalanceVisible = false;
    private double balance = 0.0, todayIncome = 0.0, todayExpense = 0.0;
    private String userType;
    
    private SessionManager sessionManager;
    private ApiService apiService;
    private TransactionAdapter transactionAdapter;
    private List<TransactionService.RecentTransaction> transactionList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getApiService(this);
        userType = sessionManager.getUserType().toLowerCase();

        initViews();
        fetchWalletData();
        fetchTransactions();
    }

    private void initViews() {
        tvWalletBalance = findViewById(R.id.tvWalletBalance);
        tvTodayIncome = findViewById(R.id.tvTodayIncome);
        tvTodayExpense = findViewById(R.id.tvTodayExpense);
        ivVisibility = findViewById(R.id.ivVisibility);
        layoutTodayIncome = findViewById(R.id.layoutTodayIncome);
        layoutTodayExpense = findViewById(R.id.layoutTodayExpense);
        rvTransactions = findViewById(R.id.rvTransactions);
        tvSeeMore = findViewById(R.id.tvSeeMore);
        btnAddMoney = findViewById(R.id.btnAddMoney);
        btnWithdraw = findViewById(R.id.btnWithdraw);
        btnPayOrder = findViewById(R.id.btnPayOrder);
        btn7 = findViewById(R.id.btn7Days);
        btn14 = findViewById(R.id.btn14Days);
        btnMonth = findViewById(R.id.btn1Month);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshWallet);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // View logic based on User Type
        updateTodayIncomeVisibility();

        ivVisibility.setOnClickListener(v -> toggleVisibility());

        // Time Filters
        if (btn7 != null) btn7.setOnClickListener(v -> selectTimeFilter(7, btn7));
        if (btn14 != null) btn14.setOnClickListener(v -> selectTimeFilter(14, btn14));
        if (btnMonth != null) btnMonth.setOnClickListener(v -> selectTimeFilter(30, btnMonth));

        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchWalletData();
            fetchTransactions();
        });

        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        transactionAdapter = new TransactionAdapter(transactionList);
        rvTransactions.setAdapter(transactionAdapter);

        tvSeeMore.setOnClickListener(v -> {
            Intent intent = new Intent(this, TransactionActivity.class);
            startActivity(intent);
        });

        btnAddMoney.setOnClickListener(v -> showAmountPopup("Add"));
        btnWithdraw.setOnClickListener(v -> showAmountPopup("Withdraw"));
        btnPayOrder.setOnClickListener(v -> Toast.makeText(this, "Pay Order clicked", Toast.LENGTH_SHORT).show());
    }

    private void showAmountPopup(String action) {
        View view = LayoutInflater.from(this).inflate(R.layout.popup_wallet_amount, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.RoundedDialog).setView(view).create();

        TextView tvTitle = view.findViewById(R.id.tvPopupTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvPopupSubtitle);
        TextInputEditText etAmount = view.findViewById(R.id.etAmount);
        Button btnBack = view.findViewById(R.id.btnBack);
        Button btnProceed = view.findViewById(R.id.btnProceed);

        tvTitle.setText(action + " Money");
        tvSubtitle.setText("Enter the amount you wish to " + action.toLowerCase() + ".");

        btnBack.setOnClickListener(v -> dialog.dismiss());

        btnProceed.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString();
            if (amountStr.isEmpty()) {
                etAmount.setError("Please enter amount");
                return;
            }
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                etAmount.setError("Amount must be greater than zero");
                return;
            }
            if ("Withdraw".equals(action) && amount > balance) {
                etAmount.setError("Insufficient balance");
                return;
            }

            dialog.dismiss();
            showPinConfirmationPopup(action, amount);
        });

        dialog.show();
    }

    private void showPinConfirmationPopup(String action, double amount) {
        View view = LayoutInflater.from(this).inflate(R.layout.popup_wallet_pin_confirm, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.RoundedDialog).setView(view).setCancelable(false).create();

        TextView tvDetails = view.findViewById(R.id.tvConfirmDetails);
        TextInputEditText etPin = view.findViewById(R.id.etWalletPin);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnConfirm = view.findViewById(R.id.btnConfirm);

        tvDetails.setText(String.format(Locale.getDefault(), "Process %s request for Rs. %.2f. Enter PIN to confirm.", action, amount));

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String pin = etPin.getText().toString();
            if (pin.length() != 4) {
                etPin.setError("PIN must be 4 digits");
                return;
            }
            performAddWithdraw(action, amount, pin, dialog);
        });

        dialog.show();
    }

    private void performAddWithdraw(String action, double amount, String pin, AlertDialog dialog) {
        WalletService.add_withdrawRequest req = new WalletService.add_withdrawRequest(amount, action, pin);

        apiService.add_withdraw(sessionManager.getAuthToken(), sessionManager.getUserId(), req)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<MessageResponse> call, @NonNull Response<MessageResponse> response) {
                        if (response.isSuccessful()) {
                            dialog.dismiss();
                            Toast.makeText(WalletActivity.this, action + " request successful", Toast.LENGTH_SHORT).show();
                            fetchWalletData();
                            fetchTransactions();
                        } else {
                            String msg = "Failed: " + response.code();
                            if (response.code() == 401) msg = "Incorrect PIN";
                            Toast.makeText(WalletActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<MessageResponse> call, @NonNull Throwable t) {
                        Toast.makeText(WalletActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void selectTimeFilter(int days, MaterialButton selectedBtn) {
        updateTimeFilterButtonUI(selectedBtn);
        Toast.makeText(this, "Filtering transactions for last " + days + " days", Toast.LENGTH_SHORT).show();
        // Additional logic to fetch filtered data could be added here
    }

    private void updateTimeFilterButtonUI(MaterialButton selectedBtn) {
        MaterialButton[] buttons = {btn7, btn14, btnMonth};
        for (MaterialButton btn : buttons) {
            if (btn == null) continue;
            if (btn == selectedBtn) {
                btn.setBackgroundColor(getResources().getColor(R.color.topical_forest));
                btn.setTextColor(Color.WHITE);
            } else {
                btn.setBackgroundColor(Color.TRANSPARENT);
                btn.setTextColor(getResources().getColor(R.color.topical_forest));
            }
        }
    }

    private void updateTodayIncomeVisibility() {
        if (userType != null && userType.contains("farmer") && !userType.contains("consumer")) {
            layoutTodayIncome.setVisibility(View.VISIBLE);
        } else {
            layoutTodayIncome.setVisibility(View.GONE);
        }
        layoutTodayExpense.setVisibility(View.VISIBLE);
    }

    private void toggleVisibility() {
        isBalanceVisible = !isBalanceVisible;
        updateBalanceUI();
    }

    private void updateBalanceUI() {
        if (isBalanceVisible) {
            tvWalletBalance.setText(String.format(Locale.getDefault(), "Rs. %.2f", balance));
            if (layoutTodayIncome.getVisibility() == View.VISIBLE) {
                tvTodayIncome.setText(String.format(Locale.getDefault(), "Rs. %.2f", todayIncome));
            }
            tvTodayExpense.setText(String.format(Locale.getDefault(), "Rs. %.2f", todayExpense));
            ivVisibility.setImageResource(R.drawable.ic_visibility_off);
        } else {
            tvWalletBalance.setText("*****");
            tvTodayIncome.setText("*****");
            tvTodayExpense.setText("*****");
            ivVisibility.setImageResource(R.drawable.ic_visibility);
        }
    }

    private void fetchWalletData() {
        if (!swipeRefreshLayout.isRefreshing()) {
            // Show loading if not triggered by swipe
        }
        apiService.getWalletPage(sessionManager.getAuthToken(), sessionManager.getUserId())
                .enqueue(new Callback<WalletService.walletPageResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<WalletService.walletPageResponse> call, @NonNull Response<WalletService.walletPageResponse> response) {
                        swipeRefreshLayout.setRefreshing(false);
                        if (response.code() == 304 || response.code() == 403) {
                            showSetPinPopup();
                        } else if (response.isSuccessful() && response.body() != null) {
                            WalletService.walletPageResponse data = response.body();
                            balance = data.getBalance();
                            todayIncome = data.getTodayIncome();
                            todayExpense = data.getTodayExpense();
                            updateBalanceUI();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<WalletService.walletPageResponse> call, @NonNull Throwable t) {
                        swipeRefreshLayout.setRefreshing(false);
                        Log.e(TAG, "fetchWalletData failed", t);
                    }
                });
    }

    private void fetchTransactions() {
        apiService.getRecentTransactions(sessionManager.getAuthToken(), sessionManager.getUserId())
                .enqueue(new Callback<TransactionService.RecentTransResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<TransactionService.RecentTransResponse> call, @NonNull Response<TransactionService.RecentTransResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            transactionList.clear();
                            if (response.body().getTransactions() != null) {
                                transactionList.addAll(response.body().getTransactions());
                            }
                            transactionAdapter.notifyDataSetChanged();
                        }
                    }
                    @Override public void onFailure(@NonNull Call<TransactionService.RecentTransResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "fetchTransactions failed", t);
                    }
                });
    }

    private void showSetPinPopup() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.popup_set_wallet_pin, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.RoundedDialog).setView(view).setCancelable(false).create();

        EditText etNewPin = view.findViewById(R.id.etNewPin);
        EditText etConfirmPin = view.findViewById(R.id.etConfirmPin);
        Button btnActivate = view.findViewById(R.id.btnPinActivate);
        Button btnBack = view.findViewById(R.id.btnPinCancel);

        btnBack.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        btnActivate.setOnClickListener(v -> {
            String pin = etNewPin.getText().toString();
            String cPin = etConfirmPin.getText().toString();

            if (pin.length() != 4 || !pin.matches("\\d+")) { 
                Toast.makeText(this, "PIN must be exactly 4 digits", Toast.LENGTH_SHORT).show(); 
                return; 
            }
            if (!pin.equals(cPin)) { 
                Toast.makeText(this, "PINs do not match", Toast.LENGTH_SHORT).show(); 
                return; 
            }

            performActivation(pin, dialog);
        });

        dialog.show();
    }

    private void performActivation(String pin, AlertDialog dialog) {
        WalletService.setup_pinReq req = new WalletService.setup_pinReq(pin);

        apiService.activateWallet(sessionManager.getAuthToken(), sessionManager.getUserId(), req)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<MessageResponse> call, @NonNull Response<MessageResponse> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(WalletActivity.this, "Wallet PIN set successfully", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            fetchWalletData();
                        } else {
                            Toast.makeText(WalletActivity.this, "Failed to set PIN: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<MessageResponse> call, @NonNull Throwable t) {
                        Toast.makeText(WalletActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private static class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
        private final List<TransactionService.RecentTransaction> list;

        TransactionAdapter(List<TransactionService.RecentTransaction> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TransactionService.RecentTransaction item = list.get(position);
            holder.tvName.setText(item.getOtherPartyName() != null ? item.getOtherPartyName() : "System");
            holder.tvDate.setText(item.getDate());
            holder.tvStatus.setText(item.getStatus().toUpperCase());
            
            boolean isCredit = "CR".equalsIgnoreCase(item.getType());
            holder.tvAmount.setText(String.format(Locale.getDefault(), "%s Rs. %s", isCredit ? "+" : "-", item.getAmount()));
            
            int color = isCredit ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336");
            holder.tvAmount.setTextColor(color);
            holder.tvStatus.setTextColor(color);
            
            holder.ivIcon.setImageResource(isCredit ? R.drawable.ic_refresh : R.drawable.ic_refresh); // Placeholder icon
            holder.ivIcon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(isCredit ? Color.parseColor("#E8F5E9") : Color.parseColor("#FFEBEE")));
        }

        @Override public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDate, tvAmount, tvStatus;
            ImageView ivIcon;
            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvTransName);
                tvDate = v.findViewById(R.id.tvTransDate);
                tvAmount = v.findViewById(R.id.tvTransAmount);
                tvStatus = v.findViewById(R.id.tvTransStatus);
                ivIcon = v.findViewById(R.id.ivTransIcon);
            }
        }
    }
}
