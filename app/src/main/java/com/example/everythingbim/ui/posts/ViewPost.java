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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.CommentEntity;
import com.example.everythingbim.data.local.entities.LocationEntity;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.data.local.entities.UserEntity;
import com.example.everythingbim.databinding.ActivityViewPostBinding;
import com.example.everythingbim.ui.main.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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
    private PostViewModel viewModel;
    private CommentAdapter commentAdapter;
    private ViewPostTagAdapter taggedUsersAdapter;
    private long postId;
    private long lastObservedLocationId = -1L;

    // The most recently observed post; used to add comments without
    // re-resolving the firestoreId at click time.
    private PostEntity currentPost;

    // State for managing replies
    private Long currentParentCommentId = null;
    private String currentParentAuthorName = null;

    private TextView username, location, likes, commentsCount, caption, uploadDate, submitCommentBttn, submitReplyBttn, replyingToUsername;
    private ImageView postImage, profilePic, reportBttn, likesIcon, commentsIcon, viewTaggedUsersBttn;
    private EditText commentInput;
    private RecyclerView commentsRv, taggedUsersRv;
    private LinearLayout returnBttn;
    private CardView taggedUsersCard;

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

        // Tagged users views
        viewTaggedUsersBttn = binding.viewTaggedUsersBttn;
        taggedUsersCard = binding.viewpostTaggedUsersCard;
        taggedUsersRv = binding.viewpostTaggedUsersRv;

        // Set initial visibility for reply-related UI
        replyingToUsername.setVisibility(View.GONE);
        submitReplyBttn.setVisibility(View.GONE);

        // Tagged-users card is hidden until the user taps the button.
        // The button itself is shown/hidden by the tagged-users observer
        // based on whether the post has any tagged users.
        taggedUsersCard.setVisibility(View.GONE);
        taggedUsersRv.setVisibility(View.GONE);
    }

    // Sets up click listeners for the return button and comment submission buttons
    private void setupListeners() {
        // Back button functionality
        returnBttn.setOnClickListener(v -> finish());

        viewTaggedUsersBttn.setOnClickListener(v -> {
            boolean show = taggedUsersCard.getVisibility() != View.VISIBLE;
            taggedUsersCard.setVisibility(show ? View.VISIBLE : View.GONE);
            taggedUsersRv.setVisibility(show ? View.VISIBLE : View.GONE);
        });

        // Like / unlike. Guarded for unauthenticated users.
        likesIcon.setOnClickListener(v -> {
            FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
            if (current == null || current.getUid() == null) {
                Toast.makeText(this, "Sign in to like posts", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentPost == null) {
                // Post not yet loaded; the like listener will catch up once it is.
                return;
            }
            viewModel.toggleLike(postId);
        });

        // Submit a new top-level comment
        submitCommentBttn.setOnClickListener(v -> {
            String body = commentInput.getText().toString().trim();
            if (body.isEmpty()) {
                Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentPost == null) {
                Toast.makeText(this, "Loading post, please try again", Toast.LENGTH_SHORT).show();
                return;
            }
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(this, "Sign in to comment", Toast.LENGTH_SHORT).show();
                return;
            }
            submitComment(currentPost, /*parentCommentId*/ null, /*parentAuthorName*/ null, body);
        });

        // Submit a reply to an existing comment
        submitReplyBttn.setOnClickListener(v -> {
            String body = commentInput.getText().toString().trim();
            if (body.isEmpty()) {
                Toast.makeText(this, "Please enter a reply", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentPost == null) {
                Toast.makeText(this, "Loading post, please try again", Toast.LENGTH_SHORT).show();
                return;
            }
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(this, "Sign in to reply", Toast.LENGTH_SHORT).show();
                return;
            }
            submitComment(currentPost, currentParentCommentId, currentParentAuthorName, body);

            // Reset UI to comment mode
            commentInput.setText("");
            currentParentCommentId = null;
            currentParentAuthorName = null;
            replyingToUsername.setVisibility(View.GONE);
            submitReplyBttn.setVisibility(View.GONE);
            submitCommentBttn.setVisibility(View.VISIBLE);

            Toast.makeText(this, "Reply added", Toast.LENGTH_SHORT).show();
        });

        // Report Button
        reportBttn.setOnClickListener(v -> {
            showReportDialog();
        });
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

        // Tagged users: horizontal list. We pass a click listener that
        // navigates to the user profile, preferring the local Room id
        // when available and falling back to the Firebase UID.
        taggedUsersAdapter = new ViewPostTagAdapter(this::openTaggedUserProfile);
        taggedUsersRv.setLayoutManager(new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false));
        taggedUsersRv.setAdapter(taggedUsersAdapter);
        taggedUsersRv.setNestedScrollingEnabled(false);
    }

    // Set up LiveData observers for post details and comments.
    private void setupObservers() {
        // Observe Post Details and populate the UI
        viewModel.getPostById(postId).observe(this, post -> {
            if (post != null) {
                currentPost = post;
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

        // Observe Tagged Users: the "view tagged users" button is shown
        // only when the post has tagged users. The RecyclerView itself is
        // toggled by the button click.
        viewModel.getTaggedUsersForPost(postId).observe(this, this::renderTaggedUsers);

        // Observe the current user's like state. Drives the heart tint.
        viewModel.isLikedByCurrentUser(postId).observe(this, this::renderLikeState);
    }

    // Populates the post UI elements with data from a PostEntity.
    private void populatePostDetails(PostEntity post) {
        username.setText(resolveAuthorLabel(post));
        caption.setText(post.caption);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        uploadDate.setText(sdf.format(new Date(post.createdAt)));

        // Like count: comes from the denormalized counter on the post
        // document in Firestore. Null means the post hasn't been
        // touched by a like yet, so default to 0.
        int likeCount = post.likeCount != null ? post.likeCount : 0;
        likes.setText(String.valueOf(likeCount));

        // Load the post image
        Glide.with(this)
                .load(post.imageUrl)
                .placeholder(R.drawable.butterfly)
                .into(postImage);

        setupLocationTag(post.locationId);
    }

    /**
     * Navigates to {@link ViewUserProfileActivity} for the given tagged user.
     * Prefers the local Room {@code userId} (so any locally-cached profile
     * data, including business tabs etc., keeps working); falls back to the
     * Firebase UID when the user is not in the local cache (the activity
     * supports both extras).
     */
    private void openTaggedUserProfile(@NonNull UserEntity user) {
        Intent intent = new Intent(this, ViewUserProfileActivity.class);
        if (user.userId > 0L) {
            intent.putExtra("USER_ID", user.userId);
        } else if (user.firebaseUid != null && !user.firebaseUid.isEmpty()) {
            intent.putExtra("USER_UID", user.firebaseUid);
        } else {
            Toast.makeText(this, "Unable to open user profile", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(intent);
    }

    /**
     * Updates the heart icon's tint based on whether the current user
     * has liked the post. Red when liked, pale slate when not.
     */
    private void renderLikeState(Boolean isLiked) {
        if (isLiked == null) return;
        int tintRes = isLiked ? R.color.red : R.color.pale_slate;
        likesIcon.setColorFilter(ContextCompat.getColor(this, tintRes));
    }

    /**
     * Updates the "view tagged users" button visibility and pushes the
     * resolved user list to the adapter. The card containing the
     * RecyclerView is intentionally left alone here - it's controlled by
     * the button's click listener so the user controls when it appears.
     */
    private void renderTaggedUsers(List<UserEntity> users) {
        if (users == null || users.isEmpty()) {
            viewTaggedUsersBttn.setVisibility(View.GONE);
            taggedUsersAdapter.setTaggedUsers(new java.util.ArrayList<>());
            // Hide the card too: nothing to show, even if the user
            // expanded it before the tagged list emptied.
            taggedUsersCard.setVisibility(View.GONE);
            taggedUsersRv.setVisibility(View.GONE);
            return;
        }
        viewTaggedUsersBttn.setVisibility(View.VISIBLE);
        taggedUsersAdapter.setTaggedUsers(users);
    }

    // Calculates the total number of comments by summing top-level comments and all their replies.
    private int calculateTotalComments(List<CommentUIModel> topLevelComments) {
        int total = topLevelComments.size();
        for (CommentUIModel comment : topLevelComments) {
            total += comment.getTotalRepliesCount();
        }
        return total;
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

    /**
     * Resolves the current Firebase user and forwards the comment write
     * to the ViewModel. We observe the resulting LiveData so we can show
     * a toast on failure (the LiveData emits {@code null} on error).
     */
    private void submitComment(@NonNull PostEntity post,
                               Long parentCommentId,
                               String parentAuthorName,
                               @NonNull String body) {
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        if (current == null) return;
        String authorUid = current.getUid();
        String authorName = resolveCurrentUserName(current);
        viewModel.addComment(post, parentCommentId, authorName, authorUid, parentAuthorName, body)
                .observe(this, persisted -> {
                    if (persisted == null) {
                        Toast.makeText(this, "Failed to add comment", Toast.LENGTH_SHORT).show();
                    } else {
                        commentInput.setText("");
                        Toast.makeText(this,
                                parentCommentId == null ? "Comment added" : "Reply added",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Mirrors the name resolution used by {@code CreatePostViewModel}:
     * prefer the display name, fall back to the email prefix, and
     * finally to a generic "User" placeholder.
     */
    private String resolveCurrentUserName(@NonNull FirebaseUser current) {
        if (current.getDisplayName() != null && !current.getDisplayName().trim().isEmpty()) {
            return current.getDisplayName().trim();
        }
        String email = current.getEmail();
        if (email != null) {
            int at = email.indexOf('@');
            if (at > 0) {
                return email.substring(0, at);
            }
            return email;
        }
        return "User";
    }
}
