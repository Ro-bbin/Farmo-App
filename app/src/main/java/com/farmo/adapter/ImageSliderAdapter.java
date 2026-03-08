package com.farmo.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.farmo.R;

import java.io.File;
import java.util.List;

public class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.ViewHolder> {
    private List<File> imageFiles;

    public ImageSliderAdapter(List<File> imageFiles) {
        this.imageFiles = imageFiles;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(parent.getContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return new ViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = imageFiles.get(position);
        if (file != null && file.exists()) {
            Glide.with(holder.imageView.getContext())
                    .load(file)
                    .placeholder(R.drawable.vegetables)
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.vegetables);
        }
    }

    @Override
    public int getItemCount() {
        return imageFiles.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView;
        }
    }
}
