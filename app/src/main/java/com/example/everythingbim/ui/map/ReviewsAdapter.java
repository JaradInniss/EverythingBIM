package com.example.everythingbim.ui.map;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.ReviewEntity;

import java.util.ArrayList;
import java.util.List;

public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder> {

    private final Context context;
    private List<ReviewEntity> reviewList = new ArrayList<>();

    public ReviewsAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_map_viewall_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        ReviewEntity review = reviewList.get(position);
        
        holder.usernameTv.setText(resolveAuthorLabel(review));
        holder.bodyTv.setText(review.body);
        holder.ratingTv.setText(String.format(java.util.Locale.US, "%.1f", review.rating));
        holder.commentsCountTv.setText("0"); // Placeholder for comments count
        holder.uploadDateTv.setText(resolveUploadDate(review));
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    public void setReviews(List<ReviewEntity> reviews) {
        this.reviewList = reviews;
        notifyDataSetChanged();
    }

    @NonNull
    private String resolveAuthorLabel(@NonNull ReviewEntity review) {
        if (review.authorName != null && !review.authorName.trim().isEmpty()) {
            return review.authorName.trim();
        }
        if (review.authorId <= 0L) {
            return "Community member";
        }
        return "User " + review.authorId;
    }

    @NonNull
    private CharSequence resolveUploadDate(@NonNull ReviewEntity review) {
        if (review.createdAt <= 0L) {
            return "Recently";
        }
        return DateUtils.getRelativeTimeSpanString(
                review.createdAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
        );
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTv, bodyTv, ratingTv, commentsCountTv, uploadDateTv;

        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTv = itemView.findViewById(R.id.map_location_related_review_username);
            bodyTv = itemView.findViewById(R.id.map_location_related_review_body);
            ratingTv = itemView.findViewById(R.id.map_location_related_review_rating);
            commentsCountTv = itemView.findViewById(R.id.map_location_related_review_comments);
            uploadDateTv = itemView.findViewById(R.id.map_location_related_review_upload_date);
        }
    }
}
