package com.farmo.activities.commonActivities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.farmo.R;
import com.farmo.network.CommonServices.OrderDetails;
import com.farmo.network.RetrofitClient;
import com.farmo.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailsActivity extends AppCompatActivity {

    private static final String TAG = "OrderDetailsActivity";

    private TextView tvOrderId, tvOrderStatus, tvOrderOtp;
    private TextView tvCostPerUnit, tvQuantity, tvTotalCost, tvTransactionStatus;
    private TextView tvOrderedDate, tvExpectedDelivery, tvPaymentMethod, tvShippingAddress;
    private ImageView btnCopyOrderId, btnCopyOtp, btnBack;
    private ProgressBar progressBar;
    private RecyclerView rvMessages;

    private String orderId;
    private SessionManager sessionManager;
    private MessageAdapter messageAdapter;
    private List<OrderDetails.MessageItem> messageList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        orderId = getIntent().getStringExtra("ORDER_ID");
        sessionManager = new SessionManager(this);

        if (orderId == null) {
            Toast.makeText(this, "Order ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        fetchOrderDetails();
    }

    private void initViews() {
        tvOrderId = findViewById(R.id.tvOrderId);
        tvOrderStatus = findViewById(R.id.tvOrderStatus);
        tvOrderOtp = findViewById(R.id.tvOrderOtp);
        tvCostPerUnit = findViewById(R.id.tvCostPerUnit);
        tvQuantity = findViewById(R.id.tvQuantity);
        tvTotalCost = findViewById(R.id.tvTotalCost);
        tvTransactionStatus = findViewById(R.id.tvTransactionStatus);
        tvOrderedDate = findViewById(R.id.tvOrderedDate);
        tvExpectedDelivery = findViewById(R.id.tvExpectedDelivery);
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod);
        tvShippingAddress = findViewById(R.id.tvShippingAddress);
        
        btnCopyOrderId = findViewById(R.id.btnCopyOrderId);
        btnCopyOtp = findViewById(R.id.btnCopyOtp);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);
        
        rvMessages = findViewById(R.id.rvMessages);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new MessageAdapter(messageList, sessionManager.getUserId());
        rvMessages.setAdapter(messageAdapter);

        btnBack.setOnClickListener(v -> finish());
        
        btnCopyOrderId.setOnClickListener(v -> copyToClipboard("Order ID", tvOrderId.getText().toString()));
        btnCopyOtp.setOnClickListener(v -> copyToClipboard("OTP", tvOrderOtp.getText().toString()));
    }

    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, label + " copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void fetchOrderDetails() {
        progressBar.setVisibility(View.VISIBLE);
        OrderDetails.Request request = new OrderDetails.Request(orderId);

        RetrofitClient.getApiService(this)
                .getOrderDetails(sessionManager.getAuthToken(), sessionManager.getUserId(), request)
                .enqueue(new Callback<OrderDetails.Response>() {
                    @Override
                    public void onResponse(@NonNull Call<OrderDetails.Response> call, @NonNull Response<OrderDetails.Response> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            updateUI(response.body());
                        } else {
                            Toast.makeText(OrderDetailsActivity.this, "Failed to load order details", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<OrderDetails.Response> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "onFailure: " + t.getMessage());
                        Toast.makeText(OrderDetailsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUI(OrderDetails.Response order) {
        tvOrderId.setText(order.getOrderId());
        tvOrderStatus.setText(order.getOrderStatus().toUpperCase());
        updateStatusStyle(order.getOrderStatus());
        
        tvOrderOtp.setText(order.getOrderOtp());
        tvCostPerUnit.setText(String.format(Locale.getDefault(), "Rs. %.2f", order.getCostPerUnit()));
        tvQuantity.setText(String.format(Locale.getDefault(), "%s %s", order.getOrderedQuantity(), order.getQuantityUnit()));
        tvTotalCost.setText(String.format(Locale.getDefault(), "Rs. %s", order.getTotalCost()));
        
        String txStatus = order.getTransactionStatus();
        tvTransactionStatus.setText(txStatus != null ? txStatus.toUpperCase() : "N/A");
        if (txStatus != null && txStatus.equalsIgnoreCase("success")) {
            tvTransactionStatus.setTextColor(Color.parseColor("#4CAF50"));
        } else if (txStatus != null && txStatus.equalsIgnoreCase("pending")) {
            tvTransactionStatus.setTextColor(Color.parseColor("#FF9800"));
        } else {
            tvTransactionStatus.setTextColor(Color.RED);
        }

        tvOrderedDate.setText(formatDateTime(order.getOrderedDate()));
        tvExpectedDelivery.setText(order.getExpectedDeliveryDate());
        tvPaymentMethod.setText(order.getPaymentMethod());
        tvShippingAddress.setText(order.getShippingAddress());

        if (order.getMessage() != null) {
            messageList.clear();
            messageList.addAll(order.getMessage());
            messageAdapter.notifyDataSetChanged();
        }
    }

    private void updateStatusStyle(String status) {
        if (status == null) return;
        String s = status.toLowerCase();
        int color;
        if (s.equals("delivered")) color = Color.parseColor("#4CAF50");
        else if (s.equals("accepted")) color = Color.parseColor("#2196F3");
        else if (s.equals("pending_delivery")) color = Color.parseColor("#9C27B0"); // Purple for COD verification
        else if (s.equals("rejected")) color = Color.parseColor("#F44336");
        else color = Color.parseColor("#FF9800"); // Pending
        
        tvOrderStatus.setBackgroundColor(color);
    }

    private String formatDateTime(String isoDate) {
        if (isoDate == null) return "N/A";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(isoDate);
            if (date != null) {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date);
            }
        } catch (Exception e) {
            Log.e(TAG, "formatDateTime error: " + e.getMessage());
        }
        return isoDate;
    }

    private static class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
        private final List<OrderDetails.MessageItem> messages;
        private final String currentUserId;

        public MessageAdapter(List<OrderDetails.MessageItem> messages, String currentUserId) {
            this.messages = messages;
            this.currentUserId = currentUserId;
        }

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_message, parent, false);
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            OrderDetails.MessageItem item = messages.get(position);
            boolean isMe = item.getBy() != null && 
                          (item.getBy().equalsIgnoreCase(currentUserId) || 
                           item.getBy().equalsIgnoreCase("me") || 
                           item.getBy().equalsIgnoreCase("itself"));

            if (isMe) {
                holder.layoutLeft.setVisibility(View.GONE);
                holder.layoutRight.setVisibility(View.VISIBLE);
                holder.tvMessageRight.setText(item.getMessage());
                holder.tvDateRight.setText(formatTime(item.getDateTime()));
            } else {
                holder.layoutRight.setVisibility(View.GONE);
                holder.layoutLeft.setVisibility(View.VISIBLE);
                holder.tvMessageLeft.setText(item.getMessage());
                holder.tvDateLeft.setText(String.format(Locale.getDefault(), "%s • %s", item.getBy(), formatTime(item.getDateTime())));
            }
        }

        private String formatTime(String dateTime) {
            if (dateTime == null) return "";
            try {
                // Input: 2026-03-04 17:48:55.858676+00:00
                String clean = dateTime.split("\\.")[0];
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                Date date = sdf.parse(clean);
                if (date != null) {
                    return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
                }
            } catch (Exception e) {
                return dateTime;
            }
            return dateTime;
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        static class MessageViewHolder extends RecyclerView.ViewHolder {
            View layoutLeft, layoutRight;
            TextView tvMessageLeft, tvDateLeft, tvMessageRight, tvDateRight;

            public MessageViewHolder(@NonNull View itemView) {
                super(itemView);
                layoutLeft = itemView.findViewById(R.id.layoutLeft);
                layoutRight = itemView.findViewById(R.id.layoutRight);
                tvMessageLeft = itemView.findViewById(R.id.tvMessageLeft);
                tvDateLeft = itemView.findViewById(R.id.tvDateLeft);
                tvMessageRight = itemView.findViewById(R.id.tvMessageRight);
                tvDateRight = itemView.findViewById(R.id.tvDateRight);
            }
        }
    }
}
