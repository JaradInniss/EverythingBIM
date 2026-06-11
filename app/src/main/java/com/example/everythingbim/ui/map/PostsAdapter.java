package com.example.everythingbim.ui.map;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.ui.utils.ImageReferenceLoader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.PostViewHolder> {

    private final Context context;
    private List<PostEntity> postList = new ArrayList<>();
    private Set<Long> likedPostIds = new HashSet<>();
    private OnPostClickListener onPostClickListener;

    public interface OnPostClickListener {
        void onPostClick(@NonNull PostEntity post);
    }

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
        holder.likesCountTv.setText(String.valueOf(post.likeCount != null ? post.likeCount : 0));
        holder.commentsCountTv.setText(String.valueOf(post.commentCount != null ? post.commentCount : 0));
        holder.uploadDateTv.setText(resolveUploadDate(post));
        int likeTint = likedPostIds.contains(post.postId) ? R.color.red : R.color.dark_amaranth;
        holder.likesIcon.setColorFilter(ContextCompat.getColor(context, likeTint));

        ImageReferenceLoader.loadInto(holder.postImageView, post.imageUrl, R.drawable.ic_images);
        holder.itemView.setOnClickListener(v -> {
            if (onPostClickListener != null) {
                onPostClickListener.onPostClick(post);
            }
        });
    }

    private String resolveAuthorLabel(PostEntity post) {
        if (post.authorName != null && !post.authorName.trim().isEmpty()) {
            return post.authorName.trim();
        }
        return "User " + post.authorId;
    }

    @NonNull
    private CharSequence resolveUploadDate(@NonNull PostEntity post) {
        if (post.createdAt <= 0L) {
            return "Recently";
        }
        return DateUtils.getRelativeTimeSpanString(
                post.createdAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
        );
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public void setPosts(List<PostEntity> posts) {
        this.postList = posts;
        notifyDataSetChanged();
    }

    public void setLikedPostIds(@NonNull Set<Long> likedPostIds) {
        this.likedPostIds = new HashSet<>(likedPostIds);
        notifyDataSetChanged();
    }

    public void setOnPostClickListener(OnPostClickListener onPostClickListener) {
        this.onPostClickListener = onPostClickListener;
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView postImageView, likesIcon;
        TextView usernameTv, captionTv, likesCountTv, commentsCountTv, uploadDateTv;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            postImageView = itemView.findViewById(R.id.map_location_related_post_image);
            likesIcon = itemView.findViewById(R.id.likes_icon);
            usernameTv = itemView.findViewById(R.id.map_location_related_post_username);
            captionTv = itemView.findViewById(R.id.map_location_related_post_caption);
            likesCountTv = itemView.findViewById(R.id.map_location_related_post_likes);
            commentsCountTv = itemView.findViewById(R.id.map_location_related_post_comments);
            uploadDateTv = itemView.findViewById(R.id.map_location_related_post_upload_date);
        }
    }
}
