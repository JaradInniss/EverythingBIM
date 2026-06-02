package com.example.everythingbim.ui.map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.PostEntity;

import java.util.ArrayList;
import java.util.List;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostViewHolder> {

    private final Context context;
    private List<PostEntity> postList = new ArrayList<>();

    public PostsAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_map_viewall_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        PostEntity post = postList.get(position);

        holder.usernameTv.setText(resolveAuthorLabel(post));
        holder.captionTv.setText(post.caption);
        holder.likesCountTv.setText("0"); // Placeholder
        holder.commentsCountTv.setText("0"); // Placeholder
        holder.uploadDateTv.setText("Recently"); // Placeholder

        Glide.with(context)
                .load(post.imageUrl)
                .placeholder(R.color.dim_grey)
                .centerCrop()
                .into(holder.postImageView);
    }

    private String resolveAuthorLabel(PostEntity post) {
        if (post.authorName != null && !post.authorName.trim().isEmpty()) {
            return post.authorName.trim();
        }
        return "User " + post.authorId;
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public void setPosts(List<PostEntity> posts) {
        this.postList = posts;
        notifyDataSetChanged();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView postImageView;
        TextView usernameTv, captionTv, likesCountTv, commentsCountTv, uploadDateTv;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            // Looking at item_map_details_post.xml for IDs
            // The ImageView inside the CardView didn't have an ID in the provided XML snippet, 
            // but let's assume it should have one or we find it by position/type if missing.
            // Actually I should check the XML again.
            postImageView = itemView.findViewById(R.id.map_location_related_post_image);
            usernameTv = itemView.findViewById(R.id.map_location_related_post_username);
            captionTv = itemView.findViewById(R.id.map_location_related_post_caption);
            likesCountTv = itemView.findViewById(R.id.map_location_related_post_likes);
            commentsCountTv = itemView.findViewById(R.id.map_location_related_post_comments);
            uploadDateTv = itemView.findViewById(R.id.map_location_related_post_upload_date);
        }
    }
}
