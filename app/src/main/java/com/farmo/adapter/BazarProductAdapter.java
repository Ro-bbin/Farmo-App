package com.farmo.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.farmo.R;
import com.farmo.activities.commonActivities.ProductDetailActivity;
import com.farmo.network.CommonServices.BazarModuleService;
import com.farmo.utils.SessionManager;

import java.io.File;
import java.util.List;

/**
 * BazarProductAdapter
 *
 * Purely a DISPLAY adapter.
 * All downloading is handled by BazarActivity before notifyItemInserted().
 * By the time onBindViewHolder() runs, the image is already on disk (or absent).
 * No download logic here — eliminates stale-position bugs entirely.
 */
public class BazarProductAdapter extends RecyclerView.Adapter<BazarProductAdapter.ViewHolder> {

    private final Context        context;
    private final List<BazarModuleService.BazarProduct> products;
    private final SessionManager sessionManager;

    public BazarProductAdapter(Context context,
                               List<BazarModuleService.BazarProduct> products) {
        this.context        = context;
        this.products       = products;
        this.sessionManager = new SessionManager(context);
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
        String productId = product.getId();

        // ── Text ────────────────────────────────────────────────────────────
        holder.tvName.setText(product.getName());

        String priceUnit = (product.getPriceUnit() != null && !product.getPriceUnit().isEmpty())
                ? " / " + product.getPriceUnit() : "";
        holder.tvPrice.setText("Rs. " + product.getPrice() + priceUnit);

        String originalPrice = product.getOriginalPrice();
        if (originalPrice != null
                && !originalPrice.isEmpty()
                && !originalPrice.equals(product.getPrice())) {

            holder.tvOldPrice.setVisibility(View.VISIBLE);
            holder.tvOldPrice.setText("Rs. " + originalPrice);
            holder.tvOldPrice.setPaintFlags(
                    holder.tvOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            String discountAmt = product.getDiscountAmount();
            if (discountAmt != null && !discountAmt.isEmpty()) {
                holder.tvDiscount.setVisibility(View.VISIBLE);
                if ("Percentage".equalsIgnoreCase(product.getDiscountType())) {
                    holder.tvDiscount.setText(discountAmt + "% OFF");
                } else {
                    holder.tvDiscount.setText("Rs. " + discountAmt + " OFF");
                }
            } else {
                holder.tvDiscount.setVisibility(View.GONE);
            }
        } else {
            holder.tvOldPrice.setVisibility(View.GONE);
            holder.tvDiscount.setVisibility(View.GONE);
        }

        if (holder.ratingBar != null) {
            holder.ratingBar.setRating(product.getRating());
        }
        holder.tvReviewCount.setText("(0)");

        // ── Image ────────────────────────────────────────────────────────────
        // Activity already downloaded + saved image before notifyItemInserted().
        // Just read from disk — zero downloads here, zero stale-position bugs.
        bindImage(holder.ivImage, productId, product.getImageUrl());

        // ── Click ────────────────────────────────────────────────────────────
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", productId);
            context.startActivity(intent);
        });
    }

    private void bindImage(ImageView imageView, String productId, String imageUrl) {
        File cachedFile = getLocalImageFile(productId);

        if (cachedFile.exists()) {
            String signature = (imageUrl != null && !imageUrl.isEmpty()) ? imageUrl : productId;
            Glide.with(context)
                    .load(cachedFile)
                    .signature(new ObjectKey(signature))
                    .placeholder(R.drawable.vegetables)
                    .error(R.drawable.vegetables)
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.vegetables);
        }
    }

    private File getLocalImageFile(String productId) {
        File dir = new File(context.getFilesDir(), "products");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "prod_" + productId + ".jpg");
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
            tvReviewCount = itemView.findViewById(R.id.tvReviewCount);
            ratingBar     = itemView.findViewById(R.id.productRating); // Fixed ID mismatch
        }
    }
}
