package com.example.everythingbim.ui.posts;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.everythingbim.R;
import com.example.everythingbim.data.local.entities.CommentEntity;
import com.example.everythingbim.data.local.entities.LocationEntity;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.data.local.entities.UserEntity;
import com.example.everythingbim.databinding.ActivityViewPostBinding;
import com.example.everythingbim.ui.main.MainActivity;
import com.example.everythingbim.ui.utils.ImageReferenceLoader;
import com.example.everythingbim.ui.utils.KeyboardScrollHintHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Activity for viewing a single post in detail, including its comments and replies.
 * Handles adding new comments and replies with a nested UI.
 */
public class ViewPost extends AppCompatActivity {
    private static final String PREF_VIEW_POST_SCROLL_HINT_SEEN =
            KeyboardScrollHintHelper.PREF_VIEW_POST_SCROLL_HINT_SEEN;

    ActivityViewPostBinding binding;
    private PostViewModel viewModel;
    private CommentAdapter commentAdapter;
    private ViewPostTagAdapter taggedUsersAdapter;
    private long postId;
    private long lastObservedLocationId = -1L;
    private String lastObservedLocationName = null;
    private LiveData<LocationEntity> fallbackLocationLiveData;
    private Observer<LocationEntity> fallbackLocationObserver;

    // The most recently observed post; used to add comments without
    // re-resolving the firestoreId at click time.
    private PostEntity currentPost;

    // State for managing replies
    private Long currentParentCommentId = null;
    private String currentParentAuthorName = null;
    private String currentParentAuthorUid = null;
    private boolean isSubmittingComment = false;

    private TextView username, location, likes, commentsCount, caption, uploadDate, submitCommentBttn, submitReplyBttn, replyingToUsername;
    private ImageView postImage, profilePic, reportBttn, likesIcon, commentsIcon, viewTaggedUsersBttn;
    private EditText commentInput;
    private RecyclerView commentsRv, taggedUsersRv;
    private LinearLayout returnBttn;
    private CardView taggedUsersCard;
    private NestedScrollView viewPostScrollView;
    private int currentKeyboardExtraBottom;

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

        // Refresh post from Firestore before setting up observers
        // This ensures we have fresh data (e.g., tagged users, author info)
        viewModel.refreshPost(postId);

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
        viewPostScrollView = binding.viewPostScroll;

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

        setupKeyboardInsets();
        setupFocusedFieldScroll();
    }

    // Set up the RecyclerView for comments and handles reply button clicks.
    private void setupRecyclerView() {
        commentAdapter = new CommentAdapter();
        commentAdapter.setOnCommentClickListener(this::openCommentAuthorProfile);
        commentsRv.setLayoutManager(new LinearLayoutManager(this));
        commentsRv.setAdapter(commentAdapter);
        // Disable nested scrolling to let the parent ScrollView handle it if necessary
        commentsRv.setNestedScrollingEnabled(false);

        // When a reply button is clicked in the adapter, update the UI to "reply mode"
        commentAdapter.setOnReplyClickListener(comment -> {
            currentParentCommentId = comment.commentId;
            currentParentAuthorName = comment.authorName;
            currentParentAuthorUid = comment.authorUid;

            // Show loading state while we resolve the name
            replyingToUsername.setText("Re: @" + comment.authorName);
            replyingToUsername.setVisibility(View.VISIBLE);

            submitCommentBttn.setVisibility(View.GONE);
            submitReplyBttn.setVisibility(View.VISIBLE);

            // Focus input and show keyboard
            commentInput.requestFocus();
            scrollCommentComposerAboveKeyboard();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(commentInput, InputMethodManager.SHOW_IMPLICIT);
            }

            // Resolve the correct parent author name from Firestore
            if (currentParentAuthorUid != null && !currentParentAuthorUid.isEmpty()) {
                resolveParentAuthorName(currentParentAuthorUid, resolvedName -> {
                    if (resolvedName != null && !resolvedName.isEmpty()) {
                        currentParentAuthorName = resolvedName;
                        runOnUiThread(() -> replyingToUsername.setText("Re: @" + resolvedName));
                    }
                });
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
     * Navigates to {@link ViewUserProfileActivity} for the post author.
     * Always passes both authorId and authorUid to ensure we can load the profile
     * from either Room (if cached) or Firestore (if not cached but have UID).
     */
    private void openAuthorProfile(@NonNull PostEntity post) {
        Intent intent = new Intent(this, ViewUserProfileActivity.class);

        // Always pass both ID and UID when available
        if (post.authorId > 0L) {
            intent.putExtra("USER_ID", post.authorId);
        }
        if (post.authorUid != null && !post.authorUid.isEmpty()) {
            intent.putExtra("USER_UID", post.authorUid);
        }

        // Must have at least one identifier
        boolean hasValidId = post.authorId > 0L;
        boolean hasValidUid = post.authorUid != null && !post.authorUid.isEmpty();
        if (!hasValidId && !hasValidUid) {
            Toast.makeText(this, "Unable to open user profile", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(intent);
    }

    /**
     * Navigates to {@link ViewUserProfileActivity} for the comment author.
     */
    private void openCommentAuthorProfile(@NonNull CommentEntity comment) {
        Intent intent = new Intent(this, ViewUserProfileActivity.class);
        if (comment.authorUid != null && !comment.authorUid.isEmpty()) {
            intent.putExtra("USER_UID", comment.authorUid);
        }
        boolean hasValidUid = comment.authorUid != null && !comment.authorUid.isEmpty();
        if (!hasValidUid) {
            Toast.makeText(this, "Unable to open user profile", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(intent);
    }

    /**
     * Collects all unique author UIDs from comments and nested replies.
     */
    private Set<String> collectAuthorUids(List<CommentUIModel> comments) {
        Set<String> uids = new HashSet<>();
        for (CommentUIModel uiModel : comments) {
            CommentEntity comment = uiModel.getComment();
            if (comment.authorUid != null && !comment.authorUid.isEmpty()) {
                uids.add(comment.authorUid);
            }
            // Also collect parent author UIDs for resolving "Re: @username"
            if (comment.parentAuthorUid != null && !comment.parentAuthorUid.isEmpty()) {
                uids.add(comment.parentAuthorUid);
            }
            uids.addAll(collectAuthorUids(uiModel.getReplies()));
        }
        return uids;
    }

    /**
     * Resolves author names from Firestore for all comments and replies,
     * then updates the adapter with correct usernames/businessNames.
     */
    private void resolveCommentAuthorNames(List<CommentUIModel> comments) {
        Set<String> authorUids = collectAuthorUids(comments);
        if (authorUids.isEmpty()) {
            commentAdapter.setComments(comments);
            int totalCount = calculateTotalComments(comments);
            commentsCount.setText(String.valueOf(totalCount));
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final Map<String, String> uidToNameMap = new HashMap<>();
        final int[] pendingFetches = {authorUids.size()};

        for (String authorUid : authorUids) {
            final String uid = authorUid;
            db.collection("users").document(authorUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        String name = doc.getString("username");
                        if (name != null && !name.isEmpty()) {
                            uidToNameMap.put(uid, name);
                        }
                    }
                    pendingFetches[0]--;
                    if (pendingFetches[0] == 0) {
                        fetchRemainingBusinessNames(uidToNameMap, authorUids, comments);
                    }
                })
                .addOnFailureListener(e -> {
                    pendingFetches[0]--;
                    if (pendingFetches[0] == 0) {
                        fetchRemainingBusinessNames(uidToNameMap, authorUids, comments);
                    }
                });
        }
    }

    /**
     * Fetches business names for any UIDs not found in users collection.
     */
    private void fetchRemainingBusinessNames(Map<String, String> uidToNameMap, Set<String> authorUids, List<CommentUIModel> comments) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        int remaining = 0;
        for (String uid : authorUids) {
            if (!uidToNameMap.containsKey(uid)) remaining++;
        }

        if (remaining == 0) {
            applyCommentAuthorNamesAndUpdateAdapter(uidToNameMap, comments);
            return;
        }

        final int totalPending = remaining;
        final int[] completed = {0};

        for (String authorUid : authorUids) {
            if (uidToNameMap.containsKey(authorUid)) continue;
            final String uid = authorUid;
            db.collection("businesses").document(authorUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        String name = doc.getString("businessName");
                        if (name == null || name.isEmpty()) {
                            name = doc.getString("companyName");
                        }
                        if (name != null && !name.isEmpty()) {
                            uidToNameMap.put(uid, name);
                        }
                    }
                    completed[0]++;
                    if (completed[0] == totalPending) {
                        applyCommentAuthorNamesAndUpdateAdapter(uidToNameMap, comments);
                    }
                })
                .addOnFailureListener(e -> {
                    completed[0]++;
                    if (completed[0] == totalPending) {
                        applyCommentAuthorNamesAndUpdateAdapter(uidToNameMap, comments);
                    }
                });
        }
    }

    /**
     * Applies resolved names to CommentEntity objects, updates them in Room database,
     * and updates the adapter.
     */
    private void applyCommentAuthorNamesAndUpdateAdapter(Map<String, String> uidToNameMap, List<CommentUIModel> comments) {
        for (CommentUIModel uiModel : comments) {
            CommentEntity comment = uiModel.getComment();
            boolean updated = false;
            if (comment.authorUid != null && uidToNameMap.containsKey(comment.authorUid)) {
                String newName = uidToNameMap.get(comment.authorUid);
                if (!newName.equals(comment.authorName)) {
                    comment.setAuthorName(newName);
                    updated = true;
                }
            }
            // Also resolve parentAuthorName if we have parentAuthorUid
            if (comment.parentAuthorUid != null && uidToNameMap.containsKey(comment.parentAuthorUid)) {
                String newParentName = uidToNameMap.get(comment.parentAuthorUid);
                if (newParentName != null && !newParentName.equals(comment.parentAuthorName)) {
                    comment.setParentAuthorName(newParentName);
                    updated = true;
                }
            }
            // Persist the updated comment to Room if name changed
            if (updated) {
                viewModel.updateComment(comment);
            }
            applyCommentAuthorNamesAndUpdateAdapter(uidToNameMap, uiModel.getReplies());
        }
        runOnUiThread(() -> {
            commentAdapter.setComments(comments);
            int totalCount = calculateTotalComments(comments);
            commentsCount.setText(String.valueOf(totalCount));
        });
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
                resolveCommentAuthorNames(comments);
            }
        });

        // Observe Tagged Users: the "view tagged users" button is shown
        // only when the post has tagged users. The RecyclerView itself is
        // toggled by the button click.
        viewModel.getTaggedUsersForPost(postId).observe(this, this::renderTaggedUsers);

        // Observe the current user's like state. Drives the heart tint.
        viewModel.isLikedByCurrentUser(postId).observe(this, this::renderLikeState);
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

    // Populates the post UI elements with data from a PostEntity.
    private void populatePostDetails(PostEntity post) {
        // Set username - will be updated async if we have authorUid
        username.setText(resolveAuthorLabel(post));
        caption.setText(post.caption);

        // Make username and profile pic clickable to open author profile
        username.setOnClickListener(v -> openAuthorProfile(post));
        profilePic.setOnClickListener(v -> openAuthorProfile(post));

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        uploadDate.setText(sdf.format(new Date(post.createdAt)));

        // Like count: comes from the denormalized counter on the post
        // document in Firestore. Null means the post hasn't been
        // touched by a like yet, so default to 0.
        int likeCount = post.likeCount != null ? post.likeCount : 0;
        likes.setText(String.valueOf(likeCount));

        // Load the post image
        ImageReferenceLoader.loadInto(postImage, post.imageUrl, R.drawable.butterfly);

        // Fetch correct author name from Firestore if we have authorUid
        if (post.authorUid != null && !post.authorUid.isEmpty()) {
            fetchAuthorName(post.authorUid);
        }

        setupLocationTag(post);
    }

    /**
     * Fetches the correct author name from Firestore based on authorUid.
     * Tries users collection first, then businesses collection.
     */
    private void fetchAuthorName(String authorUid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final TextView usernameView = username;

        // Try users collection first (general users)
        db.collection("users").document(authorUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        final String authorName = doc.getString("username");
                        if (authorName != null && !authorName.isEmpty()) {
                            runOnUiThread(() -> usernameView.setText(authorName));
                            return;
                        }
                    }
                    // Not in users - try businesses
                    fetchAuthorNameFromBusinesses(authorUid, usernameView);
                })
                .addOnFailureListener(e -> {
                    // Try businesses on failure
                    fetchAuthorNameFromBusinesses(authorUid, usernameView);
                });
    }

    private void fetchAuthorNameFromBusinesses(String authorUid, final TextView usernameView) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("businesses").document(authorUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        // Try businessName first, fall back to companyName
                        String name = doc.getString("businessName");
                        if (name == null || name.isEmpty()) {
                            name = doc.getString("companyName");
                        }
                        final String authorName = name;
                        if (authorName != null && !authorName.isEmpty()) {
                            runOnUiThread(() -> usernameView.setText(authorName));
                        }
                    }
                });
    }

    private String resolveAuthorLabel(PostEntity post) {
        if (post.authorName != null && !post.authorName.trim().isEmpty()) {
            return post.authorName.trim();
        }
        return "User " + post.authorId;
    }

    private void setupLocationTag(@NonNull PostEntity post) {
        String normalizedLocationName = post.locationName != null ? post.locationName.trim() : null;
        if (post.locationId == lastObservedLocationId
                && ((normalizedLocationName == null && lastObservedLocationName == null)
                || (normalizedLocationName != null && normalizedLocationName.equals(lastObservedLocationName)))) {
            return;
        }
        lastObservedLocationId = post.locationId;
        lastObservedLocationName = normalizedLocationName;

        if ((normalizedLocationName == null || normalizedLocationName.isEmpty()) && post.locationId <= 0L) {
            showUnknownLocationTag();
            return;
        }

        if (hasValidCoordinates(post.locationLatitude, post.locationLongitude)) {
            clearFallbackLocationObserver();
            String label = normalizedLocationName != null && !normalizedLocationName.isEmpty()
                    ? normalizedLocationName
                    : "Saved location";
            binding.viewpostLocation.setText(label);
            binding.viewpostLocation.setOnClickListener(v -> openLocationOnMap(post));
            return;
        }

        if (post.locationId > 0L) {
            viewModel.getLocationById(post.locationId).observe(this, locationEntity ->
                    bindLocationTag(post, locationEntity));
            return;
        }

        bindLocationTag(post, null);
    }

    private void bindLocationTag(@NonNull PostEntity post, @Nullable LocationEntity locationEntity) {
        if (hasValidCoordinates(locationEntity)) {
            clearFallbackLocationObserver();
            binding.viewpostLocation.setText(locationEntity.name);
            binding.viewpostLocation.setOnClickListener(v -> openLocationOnMap(locationEntity));
            return;
        }

        if (post.locationName == null || post.locationName.trim().isEmpty()) {
            showUnknownLocationTag();
            return;
        }

        binding.viewpostLocation.setText(post.locationName.trim());
        observeFallbackLocation(post, fallbackLocation -> {
            if (hasValidCoordinates(fallbackLocation)) {
                binding.viewpostLocation.setOnClickListener(v -> openLocationOnMap(fallbackLocation));
            } else {
                binding.viewpostLocation.setOnClickListener(v -> openLocationOnMapByName(post.locationName));
            }
        });
    }

    private void showUnknownLocationTag() {
        binding.viewpostLocation.setText("Unknown location");
        binding.viewpostLocation.setOnClickListener(null);
    }

    private boolean hasValidCoordinates(@Nullable LocationEntity locationEntity) {
        return locationEntity != null
                && (locationEntity.latitude != 0.0d || locationEntity.longitude != 0.0d);
    }

    private boolean hasValidCoordinates(@Nullable Double latitude, @Nullable Double longitude) {
        return latitude != null
                && longitude != null
                && (latitude != 0.0d || longitude != 0.0d);
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

    private void openLocationOnMap(@NonNull PostEntity post) {
        if (!hasValidCoordinates(post.locationLatitude, post.locationLongitude)) {
            if (post.locationName != null && !post.locationName.trim().isEmpty()) {
                openLocationOnMapByName(post.locationName);
                return;
            }
            Toast.makeText(this, "This post's saved location could not be resolved on the map yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_MAP_FOCUS, true);
        if (post.locationId > 0L) {
            intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_LOCATION_ID, post.locationId);
        }
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_LATITUDE, post.locationLatitude);
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_LONGITUDE, post.locationLongitude);
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_NAME,
                post.locationName != null && !post.locationName.trim().isEmpty()
                        ? post.locationName.trim()
                        : "Saved location");
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_SUBTITLE,
                post.locationAddress != null ? post.locationAddress : "");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void openLocationOnMapByName(@Nullable String locationName) {
        if (locationName == null || locationName.trim().isEmpty()) {
            Toast.makeText(this, "This post's saved location could not be resolved on the map yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_MAP_FOCUS, true);
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_LATITUDE, 0d);
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_LONGITUDE, 0d);
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_NAME, locationName.trim());
        intent.putExtra(MainActivity.EXTRA_MAP_FOCUS_SUBTITLE, "");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private boolean isCurrentPostSynced() {
        return currentPost != null
                && currentPost.firestoreId != null
                && !currentPost.firestoreId.trim().isEmpty();
    }

    private void showPostNotSyncedMessage() {
        Toast.makeText(this, "This post is not synced yet", Toast.LENGTH_SHORT).show();
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

    private void observeFallbackLocation(@NonNull PostEntity post,
                                         @NonNull Observer<LocationEntity> observer) {
        clearFallbackLocationObserver();
        fallbackLocationLiveData = viewModel.getResolvedLocationByName(post.locationName);
        fallbackLocationObserver = observer;
        fallbackLocationLiveData.observe(this, fallbackLocationObserver);
    }

    private void clearFallbackLocationObserver() {
        if (fallbackLocationLiveData != null && fallbackLocationObserver != null) {
            fallbackLocationLiveData.removeObserver(fallbackLocationObserver);
        }
        fallbackLocationLiveData = null;
        fallbackLocationObserver = null;
    }

    private void setupKeyboardInsets() {
        int initialLeft = viewPostScrollView.getPaddingLeft();
        int initialTop = viewPostScrollView.getPaddingTop();
        int initialRight = viewPostScrollView.getPaddingRight();
        int initialBottom = viewPostScrollView.getPaddingBottom();

        KeyboardScrollHintHelper.attach(
                binding.viewPosts,
                viewPostScrollView,
                viewPostScrollView,
                PREF_VIEW_POST_SCROLL_HINT_SEEN,
                keyboardExtraBottom -> {
                    currentKeyboardExtraBottom = keyboardExtraBottom;
                    viewPostScrollView.setClipToPadding(false);
                    viewPostScrollView.setPadding(
                            initialLeft,
                            initialTop,
                            initialRight,
                            initialBottom + keyboardExtraBottom
                    );
                }
        );
    }

    private void setupFocusedFieldScroll() {
        commentInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                scrollCommentComposerAboveKeyboard();
            }
        });
    }

    private void scrollCommentComposerAboveKeyboard() {
        viewPostScrollView.post(() -> {
            if (currentKeyboardExtraBottom <= 0) {
                return;
            }
            if (!isDescendant(viewPostScrollView, binding.writeReviewContainer)) {
                return;
            }

            Rect rect = new Rect();
            binding.writeReviewContainer.getDrawingRect(rect);
            viewPostScrollView.offsetDescendantRectToMyCoords(binding.writeReviewContainer, rect);

            int visibleHeight = viewPostScrollView.getHeight() - currentKeyboardExtraBottom;
            int desiredBottomMargin = dpToPx(24);
            int targetBottom = visibleHeight - desiredBottomMargin;
            int delta = rect.bottom - targetBottom;
            if (delta > 0) {
                viewPostScrollView.smoothScrollBy(0, delta);
            }
        });
    }

    private boolean isDescendant(ViewGroup parent, View child) {
        ViewParent p = child.getParent();
        while (p != null) {
            if (p == parent) {
                return true;
            }
            p = p.getParent();
        }
        return false;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // Sets up click listeners for the return button and comment submission buttons
    private void setupListeners() {
        // Back button functionality
        returnBttn.setOnClickListener(v -> finish());

        // Report button
        reportBttn.setOnClickListener(v -> showReportDialog());

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
            if (!isCurrentPostSynced()) {
                showPostNotSyncedMessage();
                return;
            }
            observeOnce(viewModel.toggleLike(postId), nowLiked -> {
                if (nowLiked == null) {
                    Toast.makeText(this, "Failed to update like", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Submit a new top-level comment
        submitCommentBttn.setOnClickListener(v -> {
            if (isSubmittingComment) {
                return;
            }
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
                showPostNotSyncedMessage();
                return;
            }
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(this, "Sign in to comment", Toast.LENGTH_SHORT).show();
                return;
            }
            submitComment(currentPost, /*parentCommentId*/ null, /*parentAuthorName*/ null, /*parentAuthorUid*/ null, body);
        });

        // Submit a reply to an existing comment
        submitReplyBttn.setOnClickListener(v -> {
            if (isSubmittingComment) {
                return;
            }
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
                showPostNotSyncedMessage();
                return;
            }
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(this, "Sign in to reply", Toast.LENGTH_SHORT).show();
                return;
            }
            submitComment(currentPost, currentParentCommentId, currentParentAuthorName, currentParentAuthorUid, body);
        });
    }

    @Override
    protected void onDestroy() {
        clearFallbackLocationObserver();
        super.onDestroy();
    }

    // Report dialog reasons
    private static final String[] POST_REPORT_REASONS = {
        "Spam or fake engagement - bots,repetitive posting",
        "Hate speech - targeting race,religion,gender,sexuality,disability,etc",
        "Nudity or sexual content",
        "Graphic violence/Gore",
        "Dangerous or illegal activity - drugs,weapons,self-harm,eating disorder promotion",
        "Intellectual property violation - copyright or trademark infringement",
        "Impersonation - pretending to be someone else"
    };

    private static final String[] ACCOUNT_REPORT_REASONS = {
        "Hacked account - reporting on behalf of someone else",
        "Fake account or bot",
        "Impersonating a real person or brand",
        "Deceased person's account"
    };

    private void showReportDialog() {
        if (currentPost == null) {
            Toast.makeText(this, "Loading post, please try again", Toast.LENGTH_SHORT).show();
            return;
        }

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_report_confirm);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = android.view.Gravity.CENTER;
            window.setAttributes(params);
        }

        // Get views
        ImageView closeBtn = dialog.findViewById(R.id.close_report_bttn);
        RadioGroup reportTypeGroup = dialog.findViewById(R.id.report_type_group);
        Spinner reasonSpinner = dialog.findViewById(R.id.report_reason_spinner);
        EditText descriptionEt = dialog.findViewById(R.id.report_description_et);
        Button cancelBtn = dialog.findViewById(R.id.report_cancel_btn);
        Button confirmBtn = dialog.findViewById(R.id.report_confirm_btn);
        ScrollView dialogScrollView = dialog.findViewById(R.id.dialog_report_confirm_scroll);
        View dialogRoot = dialog.findViewById(R.id.dialog_keyboard_root);

        KeyboardScrollHintHelper.attach(
                dialogRoot,
                dialogRoot,
                dialogScrollView,
                "view_post_report_dialog_scroll_hint_seen",
                extraBottom -> {
                    if (dialogScrollView == null) return;
                    dialogScrollView.setPadding(
                            dialogScrollView.getPaddingLeft(),
                            dialogScrollView.getPaddingTop(),
                            dialogScrollView.getPaddingRight(),
                            extraBottom);
                    dialogScrollView.setClipToPadding(false);
                });

        // Setup spinner with reasons (default to POST_REASONS)
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, POST_REPORT_REASONS);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        reasonSpinner.setAdapter(adapter);

        // Update spinner when report type changes
        reportTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isPostReport = checkedId == R.id.report_type_post;
            String[] reasons = isPostReport ? POST_REPORT_REASONS : ACCOUNT_REPORT_REASONS;
            ArrayAdapter<String> newAdapter = new ArrayAdapter<>(this,
                    R.layout.spinner_item, reasons);
            newAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            reasonSpinner.setAdapter(newAdapter);
        });

        // Close button
        closeBtn.setOnClickListener(v -> dialog.dismiss());

        // Cancel button
        cancelBtn.setOnClickListener(v -> dialog.dismiss());

        // Confirm button
        confirmBtn.setOnClickListener(v -> {
            boolean isPostReport = reportTypeGroup.getCheckedRadioButtonId() == R.id.report_type_post;
            String[] reasons = isPostReport ? POST_REPORT_REASONS : ACCOUNT_REPORT_REASONS;
            String reason = reasons[reasonSpinner.getSelectedItemPosition()];
            String description = descriptionEt.getText().toString().trim();

            submitReport(isPostReport, reason, description);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void submitReport(boolean isPostReport, String reason, String description) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        // Get current user info
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String reporterId = prefs.getString("userId", "");
        String reporterType = prefs.getString("userType", "general");
        // Get reporter's Firebase Auth UID for notifications
        String reporterUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        java.util.Map<String, Object> reportData = new java.util.HashMap<>();
        reportData.put("reportType", isPostReport ? "Post" : "Account");
        reportData.put("reason", reason);
        reportData.put("description", description);
        reportData.put("reporterId", reporterId);
        reportData.put("reporterUid", reporterUid != null ? reporterUid : "");
        reportData.put("reporterType", reporterType);
        reportData.put("status", "In Review");
        reportData.put("read", false);
        reportData.put("submittedAt", com.google.firebase.Timestamp.now());

        if (isPostReport && currentPost != null) {
            // Post report data
            reportData.put("reportedUser", currentPost.authorName != null ? currentPost.authorName : "User " + currentPost.authorId);
            reportData.put("reportedUserId", currentPost.authorId);
            // Save authorUid (Firebase Auth UID) for notifications - this is the correct ID to use
            reportData.put("reportedUserUid", currentPost.authorUid != null ? currentPost.authorUid : "");
            reportData.put("postId", currentPost.firestoreId != null ? currentPost.firestoreId : String.valueOf(currentPost.postId));
            reportData.put("postCaption", currentPost.caption != null ? currentPost.caption : "");
            reportData.put("postImageUrl", currentPost.imageUrl != null ? currentPost.imageUrl : "");
            reportData.put("postDate", currentPost.createdAt);
        } else {
            // Account report data - get from post author
            if (currentPost != null) {
                reportData.put("reportedUser", currentPost.authorName != null ? currentPost.authorName : "User " + currentPost.authorId);
                reportData.put("reportedUserId", currentPost.authorId);
                // Save authorUid (Firebase Auth UID) for notifications
                reportData.put("reportedUserUid", currentPost.authorUid != null ? currentPost.authorUid : "");
                reportData.put("accountId", currentPost.authorId);

                // Use authorUid (Firebase Auth UID) to look up in Firestore, not authorId (Room PK)
                final String authorUid;
                if (currentPost.authorUid != null && !currentPost.authorUid.isEmpty()) {
                    authorUid = currentPost.authorUid;
                } else {
                    // Fallback to using String.valueOf(authorId) if authorUid not available
                    authorUid = String.valueOf(currentPost.authorId);
                }

                // Fetch account email from Firestore using authorUid
                db.collection("users").document(authorUid).get()
                        .addOnSuccessListener(doc -> {
                            if (doc != null && doc.exists()) {
                                String email = doc.getString("email");
                                if (email == null) email = doc.getString("businessEmail");
                                reportData.put("contactInfo", email != null ? email : "Not available");
                                reportData.put("userType", "general");
                                saveReportToFirestore(db, reportData);
                            } else {
                                // Try businesses collection
                                db.collection("businesses").document(authorUid).get()
                                        .addOnSuccessListener(doc2 -> {
                                            String email = doc2 != null && doc2.exists() ? doc2.getString("businessEmail") : null;
                                            reportData.put("contactInfo", email != null ? email : "Not available");
                                            reportData.put("userType", "business");
                                            saveReportToFirestore(db, reportData);
                                        })
                                        .addOnFailureListener(e -> {
                                            reportData.put("contactInfo", "Not available");
                                            reportData.put("userType", "unknown");
                                            saveReportToFirestore(db, reportData);
                                        });
                            }
                        })
                        .addOnFailureListener(e -> {
                            reportData.put("contactInfo", "Not available");
                            reportData.put("userType", "unknown");
                            saveReportToFirestore(db, reportData);
                        });
                return; // async operation, don't fall through
            }
        }

        saveReportToFirestore(db, reportData);
    }

    private void saveReportToFirestore(FirebaseFirestore db, java.util.Map<String, Object> reportData) {
        db.collection("reports").add(reportData)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Report submitted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit report: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
                               String parentAuthorUid,
                               @NonNull String body) {
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        if (current == null) return;
        setCommentSubmissionInProgress(true);
        final String authorUid = current.getUid();

        // Resolve current user's display name from Firestore
        resolveParentAuthorName(authorUid, resolvedAuthorName -> {
            String authorName = resolvedAuthorName != null ? resolvedAuthorName : resolveCurrentUserName(current);

            // If we have parentAuthorUid, resolve the correct name from Firestore
            if (parentAuthorUid != null && !parentAuthorUid.isEmpty()) {
                resolveParentAuthorName(parentAuthorUid, resolvedParentName -> {
                    String resolvedParentAuthorName = resolvedParentName != null ? resolvedParentName : parentAuthorName;
                    finalizeSubmitComment(post, parentCommentId, authorName, authorUid, resolvedParentAuthorName, parentAuthorUid, body);
                });
            } else {
                finalizeSubmitComment(post, parentCommentId, authorName, authorUid, parentAuthorName, parentAuthorUid, body);
            }
        });
    }

    /**
     * Resolves the parent comment author's display name from Firestore.
     */
    private void resolveParentAuthorName(String parentAuthorUid, OnNameResolvedListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Try users collection first
        db.collection("users").document(parentAuthorUid).get()
            .addOnSuccessListener(doc -> {
                if (doc != null && doc.exists()) {
                    String name = doc.getString("username");
                    if (name != null && !name.isEmpty()) {
                        listener.onResolved(name);
                        return;
                    }
                }
                // Try businesses collection
                resolveFromBusinesses(parentAuthorUid, listener);
            })
            .addOnFailureListener(e -> resolveFromBusinesses(parentAuthorUid, listener));
    }

    private void resolveFromBusinesses(String parentAuthorUid, OnNameResolvedListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("businesses").document(parentAuthorUid).get()
            .addOnSuccessListener(doc -> {
                if (doc != null && doc.exists()) {
                    String name = doc.getString("businessName");
                    if (name == null || name.isEmpty()) {
                        name = doc.getString("companyName");
                    }
                    if (name != null && !name.isEmpty()) {
                        listener.onResolved(name);
                        return;
                    }
                }
                listener.onResolved(null);
            })
            .addOnFailureListener(e -> listener.onResolved(null));
    }

    private interface OnNameResolvedListener {
        void onResolved(String name);
    }

    /**
     * Finalizes the comment submission after resolving parent author name if needed.
     */
    private void finalizeSubmitComment(@NonNull PostEntity post,
                                        Long parentCommentId,
                                        String authorName,
                                        String authorUid,
                                        String parentAuthorName,
                                        String parentAuthorUid,
                                        @NonNull String body) {
        observeOnce(
                viewModel.addComment(post, parentCommentId, authorName, authorUid, parentAuthorName, parentAuthorUid, body),
                persisted -> {
                    setCommentSubmissionInProgress(false);
                    if (persisted == null) {
                        Toast.makeText(this, "Failed to add comment", Toast.LENGTH_SHORT).show();
                    } else {
                        commentInput.setText("");
                        resetReplyMode();
                        Toast.makeText(this,
                                parentCommentId == null ? "Comment added" : "Reply added",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setCommentSubmissionInProgress(boolean inProgress) {
        isSubmittingComment = inProgress;
        submitCommentBttn.setEnabled(!inProgress);
        submitReplyBttn.setEnabled(!inProgress);
        submitCommentBttn.setAlpha(inProgress ? 0.6f : 1f);
        submitReplyBttn.setAlpha(inProgress ? 0.6f : 1f);
    }

    private void resetReplyMode() {
        currentParentCommentId = null;
        currentParentAuthorName = null;
        currentParentAuthorUid = null;
        replyingToUsername.setVisibility(View.GONE);
        submitReplyBttn.setVisibility(View.GONE);
        submitCommentBttn.setVisibility(View.VISIBLE);
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
