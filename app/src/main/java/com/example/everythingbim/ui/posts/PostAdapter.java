package com.example.everythingbim.ui.posts;

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



/**
 * Post Adapter - Adapter for displaying a list of posts in a RecyclerView in a grid.
 */


public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<PostEntity> posts = new ArrayList<>();
    private OnPostClickListener listener;

     // Interface for handling click events on individual posts.
    public interface OnPostClickListener {
        void onPostClick(PostEntity post);
    }

     // Sets the listener for post click events.
    public void setOnPostClickListener(OnPostClickListener listener) {
        this.listener = listener;
    }


     // Updates the data set and refreshes the RecyclerView.
     public void setPosts(List<PostEntity> posts) {
        this.posts = posts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item_post layout for each item
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        PostEntity post = posts.get(position);
        
        // Load the post image using Glide
        ImageReferenceLoader.loadInto(holder.imageView, post.imageUrl, R.drawable.bg_main);
        
        // Set click listener for the entire item view
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPostClick(post);
            }
        });
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    /**
     * ViewHolder for post items, holding references to the UI components.
     */
    static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.post_image);
        }
    }
}
