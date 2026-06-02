package com.example.everythingbim.ui.posts;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.databinding.ActivityViewPostBinding;
import com.example.everythingbim.ui.main.MainActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity for viewing a single post in detail, including its comments and replies.
 * Handles adding new comments and replies with a nested UI.
 */


public class ViewPost extends AppCompatActivity {

    ActivityViewPostBinding binding;
    private ViewPostViewModel viewModel;
    private CommentAdapter commentAdapter;
    private long postId;
    private long authorId; // Store authorId for navigation
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private long lastObservedLocationId = -1;

    // State for managing replies
    private Long currentParentCommentId = null;
    private String currentParentAuthorName = null;

    private TextView username, location, likes, commentsCount, caption, uploadDate, submitCommentBttn, submitReplyBttn, replyingToUsername;
    private ImageView postImage, profilePic, reportBttn, likesIcon, commentsIcon;
    private EditText commentInput;
    private RecyclerView commentsRv;
    private LinearLayout returnBttn, userProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityViewPostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Retrieve post ID from intent
        postId = getIntent().getLongExtra("POST_ID", -1);
        if (postId == -1) {
            Toast.makeText(this, "Error loading post", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(ViewPostViewModel.class);

        // Handle window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(binding.viewPosts, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupRecyclerView();
        setupObservers();
        setupListeners();
    }

    private void initViews() {
        returnBttn = binding.returnBttn;
        profilePic = binding.viewpostUserProfilePic;
        username = binding.viewpostUserName;
        reportBttn = binding.reportPostBttn;
        location = binding.viewpostLocation;
        postImage = binding.viewpostImage;
        likesIcon = binding.likesIcon;
        likes = binding.viewpostLikes;
        commentsIcon = binding.commentsIcon;
        commentsCount = binding.viewpostComments;
        uploadDate = binding.viewpostUploadDate;
        caption = binding.viewpostCaption;
        commentInput = binding.newCommentInput;
        submitCommentBttn = binding.submitCommentBttn;
        submitReplyBttn = binding.submitReplyBttn;
        replyingToUsername = binding.replyingToUsername;
        commentsRv = binding.viewpostCommentsRv;

        userProfile = binding.viewpostUserProfile;

        // Set initial visibility for reply-related UI
        replyingToUsername.setVisibility(View.GONE);
        submitReplyBttn.setVisibility(View.GONE);
    }

    // Set up the RecyclerView for comments and handles reply button clicks.
    private void setupRecyclerView() {
        commentAdapter = new CommentAdapter();
        commentsRv.setLayoutManager(new LinearLayoutManager(this));
        commentsRv.setAdapter(commentAdapter);
        // Disable nested scrolling to let the parent ScrollView handle it if necessary
        commentsRv.setNestedScrollingEnabled(false);

        // When a reply button is clicked in the adapter, update the UI to "reply mode"
        commentAdapter.setOnReplyClickListener(comment -> {
            currentParentCommentId = comment.commentId;
            currentParentAuthorName = comment.authorName;

            replyingToUsername.setText("Re: @" + currentParentAuthorName);
            replyingToUsername.setVisibility(View.VISIBLE);

            submitCommentBttn.setVisibility(View.GONE);
            submitReplyBttn.setVisibility(View.VISIBLE);

            // Focus input and show keyboard
            commentInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(commentInput, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    // Set up LiveData observers for post details and comments.
    private void setupObservers() {
        // Observe Post Details and populate the UI
        viewModel.getPostById(postId).observe(this, post -> {
            if (post != null) {
                populatePostDetails(post);
            }
        });

        // Observe Comments and update the adapter and total count
        viewModel.getCommentsForPost(postId).observe(this, comments -> {
            if (comments != null) {
                commentAdapter.setComments(comments);
                // Calculate and display total count including all nested replies
                int totalCount = calculateTotalComments(comments);
                commentsCount.setText(String.valueOf(totalCount));
            }
        });
    }

    // Calculates the total number of comments by summing top-level comments and all their replies.
    private int calculateTotalComments(List<CommentUIModel> topLevelComments) {
        int total = topLevelComments.size();
        for (CommentUIModel comment : topLevelComments) {
            total += comment.getTotalRepliesCount();
        }
        return total;
    }

    // Populates the post UI elements with data from a PostEntity.
    private void populatePostDetails(PostEntity post) {
        authorId = post.authorId;
        username.setText(post.authorName);
        caption.setText(post.caption);
        location.setText(post.locationName); // Set initial text immediately

        uploadDate.setText(DATE_FORMATTER.format(new Date(post.createdAt)));

        // Load the post image with error handling
        Glide.with(this)
                .load(post.imageUrl)
                .placeholder(R.drawable.butterfly)
                .error(R.drawable.butterfly)
                .into(postImage);

        setupLocationTag(post.locationId);
    }

    private void setupLocationTag(long locationId) {
        // Prevent multiple observers if the post data updates but location remains same
        if (locationId == lastObservedLocationId) return;
        lastObservedLocationId = locationId;

        viewModel.getLocationById(locationId).observe(this, loc -> {
            if (loc != null) {
                binding.viewpostLocation.setText(loc.getName());

                binding.viewpostLocationTag.setOnClickListener(v -> {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra(MainActivity.EXTRA_OPEN_MAP, true);

                    intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_LOCATION_ID, loc.getLocationId());
                    intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_LATITUDE, loc.getLatitude());
                    intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_LONGITUDE, loc.getLongitude());
                    intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_NAME, loc.getName());
                    intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_SUBTITLE, loc.getAddress());

                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                });
            }
        });
    }

    // Sets up click listeners for the return button and comment submission buttons
    private void setupListeners() {
        // Back button functionality
        returnBttn.setOnClickListener(v -> finish());

        // Navigate to view user profile
        userProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ViewUserProfileActivity.class);
            intent.putExtra("USER_ID", authorId);
            startActivity(intent);
        });

        // Click listeners for direct children of userProfile (profile pic and username)
        profilePic.setOnClickListener(v -> userProfile.performClick());
        username.setOnClickListener(v -> userProfile.performClick());


        // Submit a new top-level comment
        submitCommentBttn.setOnClickListener(v -> {
            String body = commentInput.getText().toString().trim();
            if (!body.isEmpty()) {
                viewModel.addComment(postId, null, "Current User", null, body);
                commentInput.setText("");
                Toast.makeText(this, "Comment added", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show();
            }
        });

        // Submit a reply to an existing comment
        submitReplyBttn.setOnClickListener(v -> {
            String body = commentInput.getText().toString().trim();
            if (!body.isEmpty()) {
                viewModel.addComment(postId, currentParentCommentId, "Current User", currentParentAuthorName, body);

                // Reset UI to comment mode
                commentInput.setText("");
                currentParentCommentId = null;
                currentParentAuthorName = null;
                replyingToUsername.setVisibility(View.GONE);
                submitReplyBttn.setVisibility(View.GONE);
                submitCommentBttn.setVisibility(View.VISIBLE);
                
                Toast.makeText(this, "Reply added", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please enter a reply", Toast.LENGTH_SHORT).show();
            }
        });

        // Report Button
        reportBttn.setOnClickListener(v -> {
            showReportDialog();
        });
    }

    private void showReportDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_report_post, null);
        RadioGroup postReasonGroup = dialogView.findViewById(R.id.report_post_reason_group);
        RadioGroup accountReasonGroup = dialogView.findViewById(R.id.report_account_reason_group);
        EditText reportDescriptionEt = dialogView.findViewById(R.id.report_description_et);

        // Set up dialog buttons
        LinearLayout submitReportBttn = dialogView.findViewById(R.id.submit_report_bttn);
        ImageView closeReportBttn = dialogView.findViewById(R.id.close_report_bttn);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialog.show();

        android.view.Window window = dialog.getWindow();
        if (window != null) {
            // Set width to 90% of screen width, height to wrap_content
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.90);
            window.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

            // Optional: Change gravity to center or bottom
            window.setGravity(android.view.Gravity.CENTER);
        }

        // Toggle logic for RadioGroups
        postReasonGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != -1) accountReasonGroup.clearCheck();
        });
        accountReasonGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != -1) postReasonGroup.clearCheck();
        });

        submitReportBttn.setOnClickListener(v -> {
            int selectedPostId = postReasonGroup.getCheckedRadioButtonId();
            int selectedAccountId = accountReasonGroup.getCheckedRadioButtonId();

            if (selectedPostId != -1 || selectedAccountId != -1) {
                int selectedId = (selectedPostId != -1) ? selectedPostId : selectedAccountId;
                RadioButton radioButton = dialogView.findViewById(selectedId);
                String reason = radioButton.getText().toString();
                String description = reportDescriptionEt.getText().toString();

                viewModel.reportPost(postId, 12345, reason, description);
                Toast.makeText(this, "Report submitted", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
            else {
                Toast.makeText(this, "Please select a reason for reporting", Toast.LENGTH_LONG).show();
            }
        });

        closeReportBttn.setOnClickListener(v -> {
            reportDescriptionEt.setText("");
            dialog.dismiss();
        });
    }
}
