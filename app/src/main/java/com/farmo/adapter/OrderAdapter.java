package com.farmo.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.farmo.R;
import com.farmo.activities.commonActivities.ProductDetailActivity;
import com.farmo.model.Order;
import com.farmo.network.CommonServices.BazarModuleService;

import java.util.List;

/**
 * OrderAdapter  – RecyclerView adapter for farmer order management.
 * BazarProductAdapter – static inner class for the bazar product grid.
 */
public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    public interface OnOrderActionListener {
        void onAccept(Order order, int position);
        void onReject(Order order, int position);
    }

    private final Context      context;
    private final List<Order>  orders;
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

    // ══════════════════════════════════════════════════════════════════════
    //  BazarProductAdapter — inner adapter for bazar product grid
    // ══════════════════════════════════════════════════════════════════════
    public static class BazarProductAdapter
            extends RecyclerView.Adapter<BazarProductAdapter.ViewHolder> {

        private final Context            context;
        private final List<BazarModuleService.BazarProduct> products;

        public BazarProductAdapter(Context context, List<BazarModuleService.BazarProduct> products) {
            this.context  = context;
            this.products = products;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.item_bazar_product, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BazarModuleService.BazarProduct product = products.get(position);

            // Show name only (no ID)
            holder.tvName.setText(product.getName());
            
            String priceUnit = (product.getPriceUnit() != null && !product.getPriceUnit().isEmpty()) ? " / " + product.getPriceUnit() : "";
            
            // Final price (sent by backend)
            holder.tvPrice.setText("Rs. " + product.getPrice() + priceUnit);

            // Handle Discount logic
            String originalPrice = product.getOriginalPrice();
            if (originalPrice != null && !originalPrice.isEmpty() && !originalPrice.equals(product.getPrice())) {
                holder.tvOldPrice.setVisibility(View.VISIBLE);
                holder.tvOldPrice.setText("Rs. " + originalPrice);
                // Strike through actual price
                holder.tvOldPrice.setPaintFlags(holder.tvOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

                // Show discount text
                if (product.getDiscountAmount() != null && !product.getDiscountAmount().isEmpty()) {
                    holder.tvDiscount.setVisibility(View.VISIBLE);
                    String discountStr;
                    if ("Percentage".equalsIgnoreCase(product.getDiscountType())) {
                        discountStr = product.getDiscountAmount() + "% OFF";
                    } else if ("Flat".equalsIgnoreCase(product.getDiscountType())) {
                        discountStr = "Rs. " + product.getDiscountAmount() + " OFF";
                    } else {
                        discountStr = product.getDiscountAmount() + " OFF";
                    }
                    holder.tvDiscount.setText(discountStr);
                } else {
                    holder.tvDiscount.setVisibility(View.GONE);
                }
            } else {
                holder.tvOldPrice.setVisibility(View.GONE);
                holder.tvDiscount.setVisibility(View.GONE);
            }

            // Rating with default zero
            holder.ratingBar.setVisibility(View.VISIBLE);
            holder.ratingBar.setRating(product.getRating());

            // Default image
            Glide.with(context)
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.vegetables)
                    .error(R.drawable.vegetables)
                    .into(holder.ivImage);

            holder.tvReviewCount.setVisibility(View.VISIBLE);
            holder.tvReviewCount.setText("(0)"); // Or dynamic if available

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ProductDetailActivity.class);
                intent.putExtra("PRODUCT_ID", product.getId());
                context.startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return products != null ? products.size() : 0;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivImage;
            TextView  tvName, tvPrice, tvOldPrice, tvDiscount, tvReviewCount;
            RatingBar ratingBar;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivImage       = itemView.findViewById(R.id.ivProductImage);
                tvName        = itemView.findViewById(R.id.tvProductName);
                tvPrice       = itemView.findViewById(R.id.tvProductPrice);
                tvOldPrice    = itemView.findViewById(R.id.tvProductOldPrice);
                tvDiscount    = itemView.findViewById(R.id.tvProductDiscount);
                ratingBar     = itemView.findViewById(R.id.productRating);
                tvReviewCount = itemView.findViewById(R.id.tvReviewCount);
            }
        }
    }
}
