package com.farmo.activities.commonActivities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.farmo.R;
import com.farmo.network.ApiService;
import com.farmo.network.MessageResponse;
import com.farmo.network.RetrofitClient;
import com.farmo.network.CommonServices.OrderManagementService;
import com.farmo.utils.SessionManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrdersActivity extends AppCompatActivity {

    private static final String TAG = "OrdersActivity";

    private Spinner spinnerOrderType, spinnerOrderStatus;
    private RecyclerView rvOrders;
    private TextView tvTotalOrders, tvDateFrom, tvDateTo;
    private Button btnLoadMore;
    private ProgressBar progressBar;
    private LinearLayout layoutFilterType;
    private ImageView btnClearDate;

    private SessionManager sessionManager;
    private ApiService apiService;
    private OrderAdapter adapter;
    private List<OrderManagementService.Order> orderList = new ArrayList<>();

    private int currentPage = 1;
    private int totalPages = 1;
    private boolean isLoading = false;
    private String userType;

    private String currentType = "all";
    private String currentStatus = "all";
    private String dateFrom = "";
    private String dateTo = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getApiService(this);
        userType = sessionManager.getUserType().toLowerCase();

        initViews();
        setupSpinners();
        setupDateFilters();
        setupRecyclerView();

        fetchOrders(false);
    }

    private void initViews() {
        spinnerOrderType = findViewById(R.id.spinnerOrderType);
        spinnerOrderStatus = findViewById(R.id.spinnerOrderStatus);
        rvOrders = findViewById(R.id.rvOrders);
        tvTotalOrders = findViewById(R.id.tvTotalOrders);
        tvDateFrom = findViewById(R.id.tvDateFrom);
        tvDateTo = findViewById(R.id.tvDateTo);
        btnClearDate = findViewById(R.id.btnClearDate);
        btnLoadMore = findViewById(R.id.btnLoadMore);
        progressBar = findViewById(R.id.progressBar);
        layoutFilterType = findViewById(R.id.layoutFilterType);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnLoadMore.setOnClickListener(v -> {
            if (currentPage < totalPages) {
                currentPage++;
                fetchOrders(true);
            }
        });
    }

    private void setupSpinners() {
        // Filter 1: Type
        if (userType.contains("farmer")) {
            layoutFilterType.setVisibility(View.VISIBLE);
            String[] types = {"All", "Order Requested", "Order Received"};
            ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerOrderType.setAdapter(typeAdapter);
        } else {
            layoutFilterType.setVisibility(View.GONE);
            currentType = "requested"; 
        }

        // Filter 2: Status
        String[] statuses = {"All", "Pending", "Accepted", "Rejected", "Delivered", "Pending Delivery"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOrderStatus.setAdapter(statusAdapter);

        spinnerOrderType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if (selected.equals("Order Requested")) currentType = "requested";
                else if (selected.equals("Order Received")) currentType = "received";
                else currentType = "all";
                
                currentPage = 1;
                fetchOrders(false);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerOrderStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString().toLowerCase();
                if (selected.equals("pending delivery")) currentStatus = "pending_delivery";
                else currentStatus = selected;
                currentPage = 1;
                fetchOrders(false);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupDateFilters() {
        tvDateFrom.setOnClickListener(v -> showDatePicker(true));
        tvDateTo.setOnClickListener(v -> showDatePicker(false));
        btnClearDate.setOnClickListener(v -> {
            dateFrom = "";
            dateTo = "";
            tvDateFrom.setText("From: yyyy-mm-dd");
            tvDateTo.setText("To: yyyy-mm-dd");
            currentPage = 1;
            fetchOrders(false);
        });
    }

    private void showDatePicker(boolean isFrom) {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            String tempFrom = isFrom ? date : dateFrom;
            String tempTo = isFrom ? dateTo : date;

            if (!tempFrom.isEmpty() && !tempTo.isEmpty()) {
                if (!validateDateRange(tempFrom, tempTo)) {
                    Toast.makeText(this, "Date range cannot exceed 90 days", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            if (isFrom) {
                dateFrom = date;
                tvDateFrom.setText("From: " + date);
            } else {
                dateTo = date;
                tvDateTo.setText("To: " + date);
            }
            currentPage = 1;
            fetchOrders(false);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private boolean validateDateRange(String start, String end) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            Date d1 = sdf.parse(start);
            Date d2 = sdf.parse(end);
            if (d1 == null || d2 == null) return true;
            long diff = d2.getTime() - d1.getTime();
            long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
            return days <= 90 && days >= 0;
        } catch (ParseException e) {
            return false;
        }
    }

    private void setupRecyclerView() {
        adapter = new OrderAdapter(orderList);
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setAdapter(adapter);
    }

    private void fetchOrders(boolean isLoadMore) {
        if (isLoading) return;
        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);

        // Convert empty strings to null or keep them as empty if backend prefers
        String dFrom = dateFrom.isEmpty() ? "" : dateFrom;
        String dTo = dateTo.isEmpty() ? "" : dateTo;

        OrderManagementService.Request req = new OrderManagementService.Request(
                currentPage, dTo, dFrom, currentStatus, currentType
        );

        apiService.getOrderList(sessionManager.getAuthToken(), sessionManager.getUserId(), req)
                .enqueue(new Callback<OrderManagementService.Response>() {
            @Override
            public void onResponse(@NonNull Call<OrderManagementService.Response> call, @NonNull Response<OrderManagementService.Response> response) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    OrderManagementService.Response res = response.body();
                    totalPages = res.totalPages;
                    tvTotalOrders.setText("Total Orders: " + res.total);

                    if (!isLoadMore) orderList.clear();
                    if (res.orders != null) orderList.addAll(res.orders);
                    adapter.notifyDataSetChanged();

                    btnLoadMore.setVisibility(res.hasNext ? View.VISIBLE : View.GONE);
                } else {
                    Toast.makeText(OrdersActivity.this, "Failed to load orders", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<OrderManagementService.Response> call, @NonNull Throwable t) {
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "onFailure: " + t.getMessage());
                Toast.makeText(OrdersActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showStatusUpdatePopup(String orderId, String action, String currentOtp) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View popupView = inflater.inflate(R.layout.popup_confirm_status_update, null);
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.RoundedDialog).setView(popupView).create();

        TextView tvTitle = popupView.findViewById(R.id.tvPopupTitle);
        TextView tvOrderId = popupView.findViewById(R.id.tvPopupOrderId);
        EditText etOtp = popupView.findViewById(R.id.etPopupOtp);
        TextView tvNewStatus = popupView.findViewById(R.id.tvPopupNewStatus);
        EditText etMessage = popupView.findViewById(R.id.etPopupMessage);
        Button btnCancel = popupView.findViewById(R.id.btnPopupCancel);
        Button btnProceed = popupView.findViewById(R.id.btnPopupProceed);

        tvTitle.setText("Confirm " + action.substring(0, 1).toUpperCase() + action.substring(1));
        tvOrderId.setText(orderId);
        etOtp.setText(currentOtp != null ? currentOtp : "");
        tvNewStatus.setText(action.toUpperCase());

        // Make OTP editable only for Delivered/Confirm Delivery or Cancel if needed
        boolean needsOtp = action.equalsIgnoreCase("delivered") || action.equalsIgnoreCase("cancel");
        etOtp.setEnabled(needsOtp);
        if (!needsOtp) {
            etOtp.setBackgroundColor(Color.parseColor("#F5F5F5"));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnProceed.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            String otpValue = etOtp.getText().toString().trim();
            dialog.dismiss();
            if (action.equalsIgnoreCase("delivered")) {
                performConfirmDelivery(orderId, otpValue);
            } else if (action.equalsIgnoreCase("cancel")) {
                performCancelOrder(orderId, otpValue);
            } else {
                performStatusUpdate(orderId, action, otpValue, message);
            }
        });

        dialog.show();
    }

    private void performStatusUpdate(String orderId, String action, String otp, String message) {
        progressBar.setVisibility(View.VISIBLE);
        OrderManagementService.UpdateStatusRequest req = new OrderManagementService.UpdateStatusRequest(orderId, otp, action, message);

        apiService.updateOrderStatus(sessionManager.getAuthToken(), sessionManager.getUserId(), req)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<MessageResponse> call, @NonNull Response<MessageResponse> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            String msg = (response.body() != null && response.body().getMessage() != null) 
                                    ? response.body().getMessage() : "Order updated";
                            Toast.makeText(OrdersActivity.this, msg, Toast.LENGTH_SHORT).show();
                            currentPage = 1;
                            fetchOrders(false);
                        } else {
                            Toast.makeText(OrdersActivity.this, "Failed to update status", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<MessageResponse> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(OrdersActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void performConfirmDelivery(String orderId, String otp) {
        progressBar.setVisibility(View.VISIBLE);
        OrderManagementService.ConfirmOrderRequest req = new OrderManagementService.ConfirmOrderRequest(orderId, otp);

        apiService.confirmDelivery(sessionManager.getAuthToken(), sessionManager.getUserId(), req)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<MessageResponse> call, @NonNull Response<MessageResponse> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            String msg = (response.body() != null && response.body().getMessage() != null) 
                                    ? response.body().getMessage() : "Delivery confirmed";
                            Toast.makeText(OrdersActivity.this, msg, Toast.LENGTH_SHORT).show();
                            currentPage = 1;
                            fetchOrders(false);
                        } else {
                            Toast.makeText(OrdersActivity.this, "Confirmation failed", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<MessageResponse> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(OrdersActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void performCancelOrder(String orderId, String otp) {
        progressBar.setVisibility(View.VISIBLE);
        OrderManagementService.ConfirmOrderRequest req = new OrderManagementService.ConfirmOrderRequest(orderId, otp);

        apiService.cancelOrder(sessionManager.getAuthToken(), sessionManager.getUserId(), req)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<MessageResponse> call, @NonNull Response<MessageResponse> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            String msg = (response.body() != null && response.body().getMessage() != null) 
                                    ? response.body().getMessage() : "Order cancelled";
                            Toast.makeText(OrdersActivity.this, msg, Toast.LENGTH_SHORT).show();
                            currentPage = 1;
                            fetchOrders(false);
                        } else {
                            Toast.makeText(OrdersActivity.this, "Cancellation failed", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<MessageResponse> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(OrdersActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {
        private List<OrderManagementService.Order> orders;

        public OrderAdapter(List<OrderManagementService.Order> orders) {
            this.orders = orders;
        }

        @NonNull
        @Override
        public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
            return new OrderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
            OrderManagementService.Order order = orders.get(position);
            holder.tvOrderId.setText("Order ID: " + order.orderId);
            holder.tvOrderDate.setText("Date: " + order.orderDate);
            holder.tvProductName.setText("Product: " + order.productName);
            holder.tvTotalAmount.setText("Cost: Rs. " + order.cost);
            holder.tvOrderStatus.setText("STATUS: " + order.status.toUpperCase().replace("_", " "));
            
            String roleText = isReceived(order.type) ? "Buyer: " : "Seller: ";
            holder.tvConsumerName.setText(roleText + order.foreignUserName);

            // Type Label and Card Background Colors
            if (isRequested(order.type)) {
                holder.tvOrderTypeLabel.setText("REQUESTED");
                holder.tvOrderTypeLabel.setBackgroundColor(Color.parseColor("#2196F3")); // Blue
                holder.cardView.setCardBackgroundColor(Color.parseColor("#BBDEFB")); // Light Blueish
            } else if (isReceived(order.type)) {
                holder.tvOrderTypeLabel.setText("RECEIVED");
                holder.tvOrderTypeLabel.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
                holder.cardView.setCardBackgroundColor(Color.parseColor("#C8E6C9")); // Light Greenish
            }

            // Buttons visibility reset
            holder.btnAccept.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);

            // Status Styling
            updateStatusBadgeStyle(holder.tvOrderStatus, order.status);

            // Logic for "Order Received" (Seller perspective)
            if (isReceived(order.type)) {
                if (order.status.equalsIgnoreCase("pending")) {
                    holder.btnAccept.setVisibility(View.VISIBLE);
                    holder.btnAccept.setText("Accept");
                    holder.btnAccept.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                    holder.btnAccept.setOnClickListener(v -> showStatusUpdatePopup(order.orderId, "accept", null));
                    
                    holder.btnReject.setVisibility(View.VISIBLE);
                    holder.btnReject.setText("Reject");
                    holder.btnReject.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336")));
                    holder.btnReject.setOnClickListener(v -> showStatusUpdatePopup(order.orderId, "reject", null));
                } else if (order.status.equalsIgnoreCase("pending_delivery")) {
                    holder.btnAccept.setVisibility(View.VISIBLE);
                    holder.btnAccept.setText("Confirm Delivery");
                    holder.btnAccept.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#9C27B0")));
                    holder.btnAccept.setOnClickListener(v -> showStatusUpdatePopup(order.orderId, "delivered", null));
                }
            } 
            // Logic for "Order Requested" (Buyer perspective)
            else if (isRequested(order.type)) {
                if (order.status.equalsIgnoreCase("pending")) {
                    holder.btnReject.setVisibility(View.VISIBLE);
                    holder.btnReject.setText("Cancel");
                    holder.btnReject.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336")));
                    holder.btnReject.setOnClickListener(v -> showStatusUpdatePopup(order.orderId, "cancel", null));
                } else if (order.status.equalsIgnoreCase("accepted")) {
                    holder.btnAccept.setVisibility(View.VISIBLE);
                    holder.btnAccept.setText("Received Delivery");
                    holder.btnAccept.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2196F3")));
                    holder.btnAccept.setOnClickListener(v -> showStatusUpdatePopup(order.orderId, "delivered", null));
                } else if (order.status.equalsIgnoreCase("rejected")) {
                    holder.btnReject.setVisibility(View.VISIBLE);
                    holder.btnReject.setText("Resend Order Request");
                    holder.btnReject.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF9800")));
                    holder.btnReject.setOnClickListener(v -> showStatusUpdatePopup(order.orderId, "resend", null));
                } else if (order.status.equalsIgnoreCase("pending_delivery")) {
                    holder.btnAccept.setVisibility(View.VISIBLE);
                    holder.btnAccept.setText("Confirm Receipt");
                    holder.btnAccept.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#9C27B0")));
                    holder.btnAccept.setOnClickListener(v -> showStatusUpdatePopup(order.orderId, "delivered", null));
                }
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(OrdersActivity.this, OrderDetailsActivity.class);
                intent.putExtra("ORDER_ID", order.orderId);
                startActivity(intent);
            });
        }

        private void updateStatusBadgeStyle(TextView tvStatus, String status) {
            if (status == null) return;
            String s = status.toLowerCase();
            int color;
            if (s.equals("delivered")) color = ContextCompat.getColor(OrdersActivity.this, R.color.status_delivered);
            else if (s.equals("accepted")) color = ContextCompat.getColor(OrdersActivity.this, R.color.status_accepted);
            else if (s.equals("pending_delivery")) color = ContextCompat.getColor(OrdersActivity.this, R.color.status_pending_delivery);
            else if (s.equals("rejected") || s.equals("cancelled")) color = ContextCompat.getColor(OrdersActivity.this, R.color.status_rejected);
            else color = ContextCompat.getColor(OrdersActivity.this, R.color.status_pending); // Default/Pending
            
            tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
            tvStatus.setTextColor(Color.WHITE);
        }

        private boolean isRequested(String type) {
            if (type == null) return false;
            String t = type.toLowerCase().trim();
            return t.equals("requested") || t.equals("order requested") || t.equals("orderrequested") || t.equals("sent");
        }

        private boolean isReceived(String type) {
            if (type == null) return false;
            String t = type.toLowerCase().trim();
            return t.equals("received") || t.equals("order received") || t.equals("orderreceived");
        }

        @Override
        public int getItemCount() {
            return orders.size();
        }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView tvOrderId, tvOrderDate, tvConsumerName, tvProductName, tvTotalAmount, tvOrderStatus, tvOrderTypeLabel;
            Button btnAccept, btnReject;
            CardView cardView;

            public OrderViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = (CardView) itemView;
                tvOrderId = itemView.findViewById(R.id.tvOrderId);
                tvOrderTypeLabel = itemView.findViewById(R.id.tvOrderTypeLabel);
                tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
                tvConsumerName = itemView.findViewById(R.id.tvConsumerName);
                tvProductName = itemView.findViewById(R.id.tvProductName);
                tvTotalAmount = itemView.findViewById(R.id.tvTotalAmount);
                tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
                btnAccept = itemView.findViewById(R.id.btnAccept);
                btnReject = itemView.findViewById(R.id.btnReject);
                
                TextView tvQuantity = itemView.findViewById(R.id.tvQuantity);
                if (tvQuantity != null) tvQuantity.setVisibility(View.GONE);
            }
        }
    }
}
