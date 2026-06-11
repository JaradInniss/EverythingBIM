package com.example.everythingbim.ui.posts;

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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.LocationEntity;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.data.local.entities.UserEntity;
import com.example.everythingbim.databinding.ActivityViewPostBinding;
import com.example.everythingbim.ui.main.MainActivity;
import com.example.everythingbim.ui.utils.ImageReferenceLoader;
import com.example.everythingbim.ui.utils.KeyboardScrollHintHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    private ViewPostTagAdapter taggedUsersAdapter;
    private long postId;
    private long lastObservedLocationId = -1L;
    private PostEntity currentPost;

    private Long currentParentCommentId = null;
    private String currentParentAuthorName = null;

    private TextView username;
    private TextView location;
    private TextView likes;
    private TextView commentsCount;
    private TextView caption;
    private TextView uploadDate;
    private TextView submitCommentBttn;
    private TextView submitReplyBttn;
    private TextView replyingToUsername;
    private TextView viewTaggedUsersBttn;
    private ImageView postImage;
    private ImageView profilePic;
    private ImageView reportBttn;
    private ImageView likesIcon;
    private ImageView commentsIcon;
    private EditText commentInput;
    private RecyclerView commentsRv;
    private RecyclerView taggedUsersRv;
    private CardView taggedUsersCard;
    private LinearLayout returnBttn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityViewPostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        postId = getIntent().getLongExtra("POST_ID", -1);
        if (postId == -1) {
            Toast.makeText(this, "Error loading post", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(PostViewModel.class);

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
        viewTaggedUsersBttn = binding.viewTaggedUsersBttn;
        taggedUsersCard = binding.viewpostTaggedUsersCard;
        taggedUsersRv = binding.viewpostTaggedUsersRv;

        replyingToUsername.setVisibility(View.GONE);
        submitReplyBttn.setVisibility(View.GONE);
        taggedUsersCard.setVisibility(View.GONE);
    }

    private void setupRecyclerView() {
        commentAdapter = new CommentAdapter();
        commentsRv.setLayoutManager(new LinearLayoutManager(this));
        commentsRv.setAdapter(commentAdapter);
        commentsRv.setNestedScrollingEnabled(false);
        taggedUsersAdapter = new ViewPostTagAdapter(this::openTaggedUserProfile);
        taggedUsersRv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        taggedUsersRv.setAdapter(taggedUsersAdapter);
        taggedUsersRv.setNestedScrollingEnabled(false);

        commentAdapter.setOnReplyClickListener(comment -> {
            currentParentCommentId = comment.commentId;
            currentParentAuthorName = comment.authorName;

            replyingToUsername.setText("Re: @" + currentParentAuthorName);
            replyingToUsername.setVisibility(View.VISIBLE);

            submitCommentBttn.setVisibility(View.GONE);
            submitReplyBttn.setVisibility(View.VISIBLE);

            commentInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(commentInput, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void setupObservers() {
        viewModel.getPostById(postId).observe(this, post -> {
            if (post != null) {
                currentPost = post;
                populatePostDetails(post);
            }
        });

        viewModel.getCommentsForPost(postId).observe(this, comments -> {
            if (comments != null) {
                commentAdapter.setComments(comments);
                int totalCount = calculateTotalComments(comments);
                commentsCount.setText(String.valueOf(totalCount));
            }
        });
        viewModel.getTaggedUsersForPost(postId).observe(this, this::renderTaggedUsers);
        viewModel.isLikedByCurrentUser(postId).observe(this, this::renderLikeState);
    }

    private void renderLikeState(Boolean isLiked) {
        if (isLiked == null) {
            return;
        }
        int tintRes = isLiked ? R.color.red : R.color.pale_slate;
        likesIcon.setColorFilter(ContextCompat.getColor(this, tintRes));
    }

    private int calculateTotalComments(List<CommentUIModel> topLevelComments) {
        int total = topLevelComments.size();
        for (CommentUIModel comment : topLevelComments) {
            total += comment.getTotalRepliesCount();
        }
        return total;
    }

    private void populatePostDetails(PostEntity post) {
        username.setText(resolveAuthorLabel(post));
        caption.setText(post.caption);
        int likeCount = post.likeCount != null ? post.likeCount : 0;
        likes.setText(String.valueOf(likeCount));

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        uploadDate.setText(sdf.format(new Date(post.createdAt)));

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

    private void renderTaggedUsers(List<UserEntity> users) {
        if (users == null || users.isEmpty()) {
            viewTaggedUsersBttn.setVisibility(View.GONE);
            taggedUsersCard.setVisibility(View.GONE);
            taggedUsersAdapter.setTaggedUsers(new ArrayList<>());
            return;
        }
        viewTaggedUsersBttn.setVisibility(View.VISIBLE);
        taggedUsersAdapter.setTaggedUsers(users);
    }

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

    private void setupListeners() {
        returnBttn.setOnClickListener(v -> finish());
        viewTaggedUsersBttn.setOnClickListener(v -> {
            boolean shouldShow = taggedUsersCard.getVisibility() != View.VISIBLE;
            taggedUsersCard.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
        });

        likesIcon.setOnClickListener(v -> {
            FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
            if (current == null || current.getUid() == null) {
                Toast.makeText(this, "Sign in to like posts", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentPost == null || !isCurrentPostSynced()) {
                Toast.makeText(this, "This post is not synced yet", Toast.LENGTH_SHORT).show();
                return;
            }
            observeOnce(viewModel.toggleLike(postId), nowLiked -> {
                if (nowLiked == null) {
                    Toast.makeText(this, "Failed to update like", Toast.LENGTH_SHORT).show();
                }
            });
        });

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
            if (!isCurrentPostSynced()) {
                Toast.makeText(this, "This post is not synced yet", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
            if (current == null) {
                Toast.makeText(this, "Sign in to comment", Toast.LENGTH_SHORT).show();
                return;
            }
            submitComment(currentPost, null, null, body, current);
        });

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
            if (!isCurrentPostSynced()) {
                Toast.makeText(this, "This post is not synced yet", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
            if (current == null) {
                Toast.makeText(this, "Sign in to reply", Toast.LENGTH_SHORT).show();
                return;
            }
            submitComment(currentPost, currentParentCommentId, currentParentAuthorName, body, current);
        });
    }

    private void submitComment(@NonNull PostEntity post,
                               Long parentCommentId,
                               String parentAuthorName,
                               @NonNull String body,
                               @NonNull FirebaseUser currentUser) {
        String authorName = resolveCurrentUserName(currentUser);
        observeOnce(
                viewModel.addComment(post, parentCommentId, authorName, currentUser.getUid(), parentAuthorName, body),
                persisted -> {
                    if (persisted == null) {
                        Toast.makeText(this, "Failed to add comment", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    commentInput.setText("");
                    currentParentCommentId = null;
                    currentParentAuthorName = null;
                    replyingToUsername.setVisibility(View.GONE);
                    submitReplyBttn.setVisibility(View.GONE);
                    submitCommentBttn.setVisibility(View.VISIBLE);
                    Toast.makeText(this,
                            parentCommentId == null ? "Comment added" : "Reply added",
                            Toast.LENGTH_SHORT).show();
                }
        );
    }

    @NonNull
    private String resolveCurrentUserName(@NonNull FirebaseUser currentUser) {
        if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().trim().isEmpty()) {
            return currentUser.getDisplayName().trim();
        }
        String email = currentUser.getEmail();
        if (email != null) {
            int at = email.indexOf('@');
            if (at > 0) {
                return email.substring(0, at);
            }
            return email;
        }
        return "User";
    }

    private boolean isCurrentPostSynced() {
        return currentPost != null
                && currentPost.firestoreId != null
                && !currentPost.firestoreId.trim().isEmpty();
    }

    private <T> void observeOnce(@NonNull LiveData<T> liveData, @NonNull Observer<T> observer) {
        liveData.observe(this, new Observer<T>() {
            @Override
            public void onChanged(T value) {
                liveData.removeObserver(this);
                observer.onChanged(value);
            }
        });
    }
}
