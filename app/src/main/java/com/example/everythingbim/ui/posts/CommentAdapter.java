package com.example.everythingbim.ui.posts;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.CommentEntity;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying comments and their nested replies in a RecyclerView.
 * Supports recursive nesting for threaded conversations.
 */
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<CommentUIModel> comments = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
    private OnReplyClickListener replyClickListener;
    private OnCommentClickListener commentClickListener;

    /**
     * Interface to handle clicks on the "Reply" button.
     */
    public interface OnReplyClickListener {
        void onReplyClick(CommentEntity comment);
    }

    /**
     * Interface to handle clicks on the comment username.
     */
    public interface OnCommentClickListener {
        void onCommentClick(CommentEntity comment);
    }

    /**
     * Sets the listener for reply button clicks.
     */
    public void setOnReplyClickListener(OnReplyClickListener listener) {
        this.replyClickListener = listener;
    }

    /**
     * Sets the listener for comment username clicks.
     */
    public void setOnCommentClickListener(OnCommentClickListener listener) {
        this.commentClickListener = listener;
    }

    /**
     * Updates the list of comments and refreshes the UI.
     */
    public void setComments(List<CommentUIModel> comments) {
        this.comments = comments;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the comment item layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        // Bind the comment data to the view holder
        CommentUIModel uiModel = comments.get(position);
        holder.bind(uiModel);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    /**
     * ViewHolder class for individual comment items.
     */
    class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView username, replyingTo, uploadDate, body, replyCount;
        ImageView authorProfilePic;
        LinearLayout replyLayout;
        View separator, replyBttn;
        RecyclerView repliesRv;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize view references
            username = itemView.findViewById(R.id.comment_username);
            replyingTo = itemView.findViewById(R.id.comment_replying_to);
            uploadDate = itemView.findViewById(R.id.comment_upload_date);
            body = itemView.findViewById(R.id.comment_body);
            replyCount = itemView.findViewById(R.id.comment_reply_count);
            replyLayout = itemView.findViewById(R.id.comment_replies);
            separator = itemView.findViewById(R.id.comment_reply_separator);
            repliesRv = itemView.findViewById(R.id.replies_rv);
            replyBttn = itemView.findViewById(R.id.reply_bttn);
            authorProfilePic = itemView.findViewById(R.id.comment_author_profile_pic);
        }

        /**
         * Binds comment data and sets up reply hierarchy.
         */
        public void bind(CommentUIModel uiModel) {
            CommentEntity comment = uiModel.getComment();
            username.setText(comment.authorName);
            // Make username clickable to open author profile
            username.setOnClickListener(v -> {
                if (commentClickListener != null) {
                    commentClickListener.onCommentClick(comment);
                }
            });
            // Load profile picture if available, otherwise fetch from Firestore
            String profilePicUrl = comment.authorProfilePictureUrl;
            if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(profilePicUrl)
                        .placeholder(R.drawable.ic_user_circle)
                        .circleCrop()
                        .into(authorProfilePic);
            } else {
                authorProfilePic.setImageResource(R.drawable.ic_user_circle);
                // Fetch profile picture from Firestore if not available
                if (comment.authorUid != null && !comment.authorUid.isEmpty()) {
                    fetchAuthorProfilePic(comment.authorUid, comment);
                }
            }
            body.setText(comment.body);
            // createdAt is a boxed Long; for comments that haven't
            // received a server timestamp yet we fall back to "now" so
            // the UI never shows a 1970 date.
            long createdAtMillis = comment.createdAt != null
                    ? comment.createdAt
                    : System.currentTimeMillis();
            uploadDate.setText(dateFormat.format(new Date(createdAtMillis)));

            // Display "Re: @username" if this is a reply
            if (comment.parentCommentId != null && comment.parentAuthorName != null) {
                replyingTo.setVisibility(View.VISIBLE);
                replyingTo.setText("Re: @" + comment.parentAuthorName);
            } else {
                replyingTo.setVisibility(View.GONE);
            }

            // Handle reply button click
            replyBttn.setOnClickListener(v -> {
                if (replyClickListener != null) {
                    replyClickListener.onReplyClick(comment);
                }
            });

            // Handle nested replies visibility and count
            int totalReplies = uiModel.getTotalRepliesCount();
            if (totalReplies == 0) {
                replyLayout.setVisibility(View.GONE);
                separator.setVisibility(View.GONE);
                repliesRv.setVisibility(View.GONE);
            } else {
                replyLayout.setVisibility(View.VISIBLE);
                replyCount.setText(String.valueOf(totalReplies));
                
                // Set initial expanded/collapsed state
                updateRepliesVisibility(uiModel.isExpanded());

                // Toggle visibility when clicking the reply count area
                replyLayout.setOnClickListener(v -> {
                    uiModel.setExpanded(!uiModel.isExpanded());
                    updateRepliesVisibility(uiModel.isExpanded());
                });

                // Set up nested RecyclerView for replies (recursive)
                List<CommentUIModel> replies = uiModel.getReplies();
                CommentAdapter nestedAdapter = new CommentAdapter();
                nestedAdapter.setOnReplyClickListener(replyClickListener); // Propagate listener
                nestedAdapter.setOnCommentClickListener(commentClickListener); // Propagate click listener for author names
                repliesRv.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
                repliesRv.setAdapter(nestedAdapter);
                nestedAdapter.setComments(replies);
                
                // Indent nested replies to visualize hierarchy
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) repliesRv.getLayoutParams();
                params.leftMargin = 40; 
                repliesRv.setLayoutParams(params);
            }
        }

        /**
         * Toggles the visibility of the replies section.
         */
        private void updateRepliesVisibility(boolean expanded) {
            int visibility = expanded ? View.VISIBLE : View.GONE;
            separator.setVisibility(visibility);
            repliesRv.setVisibility(visibility);
        }

        /**
         * Fetches the author's profile picture URL from Firestore and updates the comment.
         */
        private void fetchAuthorProfilePic(String authorUid, CommentEntity comment) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            final ImageView profilePicView = authorProfilePic;

            // Try users collection first (general users)
            db.collection("users").document(authorUid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc != null && doc.exists()) {
                            String profilePicUrl = doc.getString("profilePictureUrl");
                            if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                                // Update the comment with the profile picture URL
                                comment.authorProfilePictureUrl = profilePicUrl;
                                Glide.with(itemView.getContext())
                                        .load(profilePicUrl)
                                        .placeholder(R.drawable.ic_user_circle)
                                        .circleCrop()
                                        .into(profilePicView);
                                return;
                            }
                        }
                        // Not in users - try businesses
                        fetchAuthorProfilePicFromBusinesses(authorUid, comment, profilePicView);
                    })
                    .addOnFailureListener(e -> {
                        // Try businesses on failure
                        fetchAuthorProfilePicFromBusinesses(authorUid, comment, profilePicView);
                    });
        }

        private void fetchAuthorProfilePicFromBusinesses(String authorUid, CommentEntity comment, ImageView profilePicView) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("businesses").document(authorUid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc != null && doc.exists()) {
                            String profilePicUrl = doc.getString("profilePictureUrl");
                            if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                                // Update the comment with the profile picture URL
                                comment.authorProfilePictureUrl = profilePicUrl;
                                Glide.with(itemView.getContext())
                                        .load(profilePicUrl)
                                        .placeholder(R.drawable.ic_user_circle)
                                        .circleCrop()
                                        .into(profilePicView);
                            }
                        }
                    });
        }
    }
}
