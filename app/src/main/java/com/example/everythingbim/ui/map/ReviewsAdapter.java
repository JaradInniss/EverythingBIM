package com.example.everythingbim.ui.map;

import android.content.Context;
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
        
        holder.usernameTv.setText("User " + review.authorId); // In a real app, you'd fetch the actual username
        holder.bodyTv.setText(review.body);
        holder.ratingTv.setText(String.valueOf(review.rating));
        holder.commentsCountTv.setText("0"); // Placeholder for comments count
        holder.uploadDateTv.setText("Recently"); // Placeholder for date formatting
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    public void setReviews(List<ReviewEntity> reviews) {
        this.reviewList = reviews;
        notifyDataSetChanged();
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
