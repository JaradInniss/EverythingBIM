package com.example.everythingbim.ui.map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.ui.utils.ImageReferenceLoader;

import java.util.ArrayList;
import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder> {

    private final Context context;
    private List<PostEntity> postList = new ArrayList<>();
    private final OnImageClickListener listener;

    public interface OnImageClickListener {
        void onImageClick(PostEntity post);
    }

    public GalleryAdapter(Context context, OnImageClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_map_viewall_image, parent, false);
        return new GalleryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        PostEntity post = postList.get(position);

        // Use Glide to load the image efficiently
        ImageReferenceLoader.loadInto(holder.imageView, post.imageUrl, R.drawable.ic_images);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageClick(post);
            }
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public void setPosts(List<PostEntity> posts) {
        this.postList = posts;
        notifyDataSetChanged();
    }

    static class GalleryViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.gallery_item_image);
        }
    }
}
