package com.farmo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.farmo.R;
import com.farmo.model.Order;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    public interface OnOrderActionListener {
        void onAccept(Order order, int position);
        void onReject(Order order, int position);
    }

    private final Context     context;
    private final List<Order> orders;
    private OnOrderActionListener listener;

    public OrderAdapter(Context context, List<Order> orders) {
        this.context = context;
        this.orders  = orders;
    }

    public void setOnOrderActionListener(OnOrderActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);

        if (holder.tvOrderId   != null) holder.tvOrderId.setText("Order #" + order.getId());
        if (holder.tvConsumer  != null) holder.tvConsumer.setText(order.getConsumerName());
        if (holder.tvProduct   != null) holder.tvProduct.setText(order.getProductName());
        if (holder.tvQuantity  != null) holder.tvQuantity.setText("Qty: " + order.getQuantity());
        if (holder.tvTotal     != null) holder.tvTotal.setText("Rs. " + order.getTotalAmount());
        if (holder.tvStatus    != null) holder.tvStatus.setText(order.getStatus());
        if (holder.tvOrderDate != null) holder.tvOrderDate.setText(order.getOrderDate());

        if (holder.btnAccept != null) {
            holder.btnAccept.setOnClickListener(v -> {
                if (listener != null) listener.onAccept(order, holder.getAdapterPosition());
            });
        }
        if (holder.btnReject != null) {
            holder.btnReject.setOnClickListener(v -> {
                if (listener != null) listener.onReject(order, holder.getAdapterPosition());
            });
        }
    }

    @Override
    public int getItemCount() {
        return orders != null ? orders.size() : 0;
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvConsumer, tvProduct, tvQuantity, tvTotal, tvStatus, tvOrderDate;
        Button   btnAccept, btnReject;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId   = itemView.findViewById(R.id.tvOrderId);
            tvConsumer  = itemView.findViewById(R.id.tvConsumerName);
            tvProduct   = itemView.findViewById(R.id.tvProductName);
            tvQuantity  = itemView.findViewById(R.id.tvQuantity);
            tvTotal     = itemView.findViewById(R.id.tvTotalAmount);
            tvStatus    = itemView.findViewById(R.id.tvOrderStatus);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            btnAccept   = itemView.findViewById(R.id.btnAccept);
            btnReject   = itemView.findViewById(R.id.btnReject);
        }
    }
}