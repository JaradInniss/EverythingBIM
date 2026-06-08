package com.example.everythingbim.ui.posts;

import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.LocationEntity;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.databinding.ActivityViewPostBinding;
import com.example.everythingbim.ui.main.MainActivity;
import com.example.everythingbim.ui.utils.ImageReferenceLoader;
import com.example.everythingbim.ui.utils.KeyboardScrollHintHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity for viewing a single post in detail, including its comments and replies.
 * Handles adding new comments and replies with a nested UI.
 */
public class ViewPost extends AppCompatActivity {
    private static final String PREF_VIEW_POST_SCROLL_HINT_SEEN = "view_post_scroll_hint_seen";

    ActivityViewPostBinding binding;
    private PostViewModel viewModel;
    private CommentAdapter commentAdapter;
    private long postId;
    private long lastObservedLocationId = -1L;

    // State for managing replies
    private Long currentParentCommentId = null;
    private String currentParentAuthorName = null;

    private TextView username, location, likes, commentsCount, caption, uploadDate, submitCommentBttn, submitReplyBttn, replyingToUsername;
    private ImageView postImage, profilePic, reportBttn, likesIcon, commentsIcon;
    private EditText commentInput;
    private RecyclerView commentsRv;
    private LinearLayout returnBttn;

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

        viewModel = new ViewModelProvider(this).get(PostViewModel.class);
        
        // Handle window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(binding.viewPosts, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        setupKeyboardInsets();

        initViews();
        setupRecyclerView();
        setupObservers();
        setupListeners();
    }

    private void setupKeyboardInsets() {
        int initialLeft = binding.writeReviewContainer.getPaddingLeft();
        int initialTop = binding.writeReviewContainer.getPaddingTop();
        int initialRight = binding.writeReviewContainer.getPaddingRight();
        int initialBottom = binding.writeReviewContainer.getPaddingBottom();

        KeyboardScrollHintHelper.attach(
                binding.getRoot(),
                binding.writeReviewContainer,
                binding.viewPostScroll,
                PREF_VIEW_POST_SCROLL_HINT_SEEN,
                keyboardExtraBottom -> binding.writeReviewContainer.setPadding(
                        initialLeft,
                        initialTop,
                        initialRight,
                        initialBottom + keyboardExtraBottom
                )
        );
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
        username.setText(resolveAuthorLabel(post));
        caption.setText(post.caption);
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        uploadDate.setText(sdf.format(new Date(post.createdAt)));

        // Load the post image
        ImageReferenceLoader.loadInto(postImage, post.imageUrl, R.drawable.butterfly);

        setupLocationTag(post.locationId);
    }

    private String resolveAuthorLabel(PostEntity post) {
        if (post.authorName != null && !post.authorName.trim().isEmpty()) {
            return post.authorName.trim();
        }
        return "User " + post.authorId;
    }

    private void setupLocationTag(long locationId) {
        if (locationId == lastObservedLocationId) {
            return;
        }
        lastObservedLocationId = locationId;

        if (locationId <= 0L) {
            binding.viewpostLocation.setText("Unknown location");
            binding.viewpostLocation.setOnClickListener(null);
            return;
        }

        viewModel.getLocationById(locationId).observe(this, this::bindLocationTag);
    }

    private void bindLocationTag(LocationEntity locationEntity) {
        if (locationEntity == null) {
            binding.viewpostLocation.setText("Unknown location");
            binding.viewpostLocation.setOnClickListener(null);
            return;
        }

        binding.viewpostLocation.setText(locationEntity.name);
        binding.viewpostLocation.setOnClickListener(v -> openLocationOnMap(locationEntity));
    }

    private void openLocationOnMap(LocationEntity locationEntity) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_MAP_FOCUS, true);
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_LOCATION_ID, locationEntity.locationId);
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_LATITUDE, locationEntity.latitude);
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_LONGITUDE, locationEntity.longitude);
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_NAME, locationEntity.name);
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_SUBTITLE, locationEntity.address);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    // Sets up click listeners for the return button and comment submission buttons
    private void setupListeners() {
        // Back button functionality
        returnBttn.setOnClickListener(v -> finish());

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
    }
}
