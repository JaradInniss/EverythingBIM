package com.example.everythingbim.ui.posts;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.everythingbim.data.local.entities.CommentEntity;
import com.example.everythingbim.data.local.entities.LocationEntity;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.data.local.entities.UserEntity;
import com.example.everythingbim.data.repository.PostRepository;
import com.example.everythingbim.data.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ViewModel for managing post-related data and logic.
 * Handles fetching posts (from Firestore via {@link PostRepository}), filtering,
 * and building the comment hierarchy.
 */
public class PostViewModel extends AndroidViewModel {
    private final PostRepository repository;
    private final UserRepository userRepository;
    private final LiveData<List<PostEntity>> posts;
    private final LiveData<List<LocationEntity>> locations;
    private final MutableLiveData<String> filterType = new MutableLiveData<>("account");
    private final MutableLiveData<String> authorUidFilter = new MutableLiveData<>();

    public PostViewModel(@NonNull Application application) {
        super(application);
        repository = new PostRepository(application);
        userRepository = new UserRepository(application);
        posts = repository.getRandomizedPosts();
        locations = repository.getAllLocations();

        // Seed placeholder data if database is empty to ensure UI is populated during testing
        repository.seedDataIfEmpty();
    }

    public LiveData<List<PostEntity>> getPosts() {
        return posts;
    }

    public LiveData<PostEntity> getPostById(long postId) {
        return repository.getPostById(postId);
    }

    private boolean hasSyncedFirestorePost(@Nullable PostEntity post) {
        return post != null
                && post.firestoreId != null
                && !post.firestoreId.trim().isEmpty();
    }

    public LiveData<LocationEntity> getLocationById(long locationId) {
        return repository.getLocationById(locationId);
    }

    /**
     * LiveData that emits the list of users tagged in the given post, fetched
     * from Firestore by their Firebase UIDs ({@link PostEntity#taggedUserUids}).
     *
     * <p>The flow is:</p>
     * <ol>
     *   <li>Observe the post by id.</li>
     *   <li>If the post has no {@code taggedUserUids} (null or empty), emit
     *       an empty list immediately so the UI can hide the "view tagged
     *       users" button.</li>
     *   <li>Otherwise, ask {@link UserRepository#getUsersByFirebaseUids} for
     *       the matching {@link UserEntity} records. The entities returned
     *       here come from Firestore, so they carry {@code firebaseUid} but
     *       have a local Room {@code userId} of {@code 0L}.</li>
     * </ol>
     *
     * <p>Consumers (e.g. {@link ViewPost}) should use the entities'
     * {@code firebaseUid} to navigate to the user profile when no local
     * {@code userId} is available.</p>
     */
    public LiveData<List<UserEntity>> getTaggedUsersForPost(long postId) {
        return Transformations.switchMap(repository.getPostById(postId), post -> {
            // Use a MediatorLiveData so we can forward the Firestore
            // lookup's result through the same LiveData. When the post
            // has no tags we just emit an empty list synchronously and
            // skip the network call.
            MediatorLiveData<List<UserEntity>> result = new MediatorLiveData<>();
            if (post == null) {
                result.setValue(Collections.emptyList());
                return result;
            }
            List<String> taggedUids = post.taggedUserUids;
            if (taggedUids == null || taggedUids.isEmpty()) {
                result.setValue(Collections.emptyList());
                return result;
            }
            // Switch into the Firestore-backed lookup. The Firestore
            // LiveData is single-shot, so once it emits we can drop the
            // source.
            LiveData<List<UserEntity>> source = userRepository.getUsersByFirebaseUids(taggedUids);
            result.addSource(source, users -> {
                result.setValue(users != null ? users : Collections.emptyList());
                result.removeSource(source);
            });
            return result;
        });
    }

    public LiveData<List<LocationEntity>> getLocations() {
        return locations;
    }

    public LiveData<String> getFilterType() {
        return filterType;
    }

    public void setFilterType(String type) {
        filterType.setValue(type);
    }

    /**
     * Sets the Firebase UID whose posts should be observed. When this changes
     * the {@link #authorPosts} LiveData emits a new list backed by Firestore.
     */
    public void setAuthorUidFilter(@NonNull String uid) {
        authorUidFilter.setValue(uid);
    }

    /**
     * LiveData that emits posts authored by the user whose UID was passed to
     * {@link #setAuthorUidFilter(String)}. The LiveData automatically reacts
     * to changes in the filter.
     */
    public LiveData<List<PostEntity>> getPostsByAuthorUid() {
        return Transformations.switchMap(authorUidFilter, repository::getPostsByAuthorUid);
    }

    /**
     * Retrieves comments for a post and transforms them into a hierarchical
     * UI model structure.
     *
     * <p>The flow is:</p>
     * <ol>
     *   <li>Observe the post by id to get its Firestore document id.</li>
     *   <li>If the post is unavailable, emit an empty list.</li>
     *   <li>Otherwise, stream the comments subcollection from Firestore
     *       (via {@link PostRepository#getCommentsForPost(String)}) and
     *       build the threaded {@link CommentUIModel} hierarchy.</li>
     * </ol>
     */
    public LiveData<List<CommentUIModel>> getCommentsForPost(long postId) {
        return Transformations.switchMap(repository.getPostById(postId), post -> {
            MediatorLiveData<List<CommentUIModel>> result = new MediatorLiveData<>();
            if (!hasSyncedFirestorePost(post)) {
                result.setValue(Collections.emptyList());
                return result;
            }
            String postFirestoreId = post.firestoreId;
            LiveData<List<CommentEntity>> source = repository.getCommentsForPost(postFirestoreId);
            result.addSource(source, comments -> result.setValue(buildCommentHierarchy(comments)));
            return result;
        });
    }

    /**
     * Builds a flat-to-hierarchical {@link CommentUIModel} tree. The list
     * is expected to be already in creation order (oldest first).
     */
    @NonNull
    private List<CommentUIModel> buildCommentHierarchy(@Nullable List<CommentEntity> comments) {
        if (comments == null || comments.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, CommentUIModel> lookup = new HashMap<>();
        List<CommentUIModel> topLevelComments = new ArrayList<>();

        for (CommentEntity comment : comments) {
            lookup.put(comment.commentId, new CommentUIModel(comment));
        }

        for (CommentEntity comment : comments) {
            CommentUIModel uiModel = lookup.get(comment.commentId);
            if (comment.parentCommentId == null) {
                topLevelComments.add(uiModel);
            } else {
                CommentUIModel parent = lookup.get(comment.parentCommentId);
                if (parent != null) {
                    parent.addReply(uiModel);
                }
            }
        }
        return topLevelComments;
    }

    /**
     * Adds a new comment to a post. The caller must pass the loaded
     * {@link PostEntity} (so we have its Firestore id without firing a
     * one-shot observer here, which would leak). Writes to the
     * {@code posts/{firestoreId}/comments} subcollection via
     * {@link PostRepository#addCommentToFirestore} and mirrors into Room.
     *
     * <p>The {@code authorUid} is the Firebase Auth UID of the current
     * user. {@code authorName} is stored as a display-name snapshot at
     * write time so we can render the comment author even if the user
     * later renames themselves.</p>
     *
     * @return a {@link LiveData} that emits the persisted comment (with
     *         its firestoreId) once the write completes, or {@code null}
     *         on failure / when the post is not yet linked to Firestore.
     */
    public LiveData<CommentEntity> addComment(@NonNull PostEntity post,
                                              Long parentCommentId,
                                              @NonNull String authorName,
                                              @Nullable String authorUid,
                                              @Nullable String parentAuthorName,
                                              @NonNull String body) {
        if (!hasSyncedFirestorePost(post)) {
            MutableLiveData<CommentEntity> failure = new MutableLiveData<>();
            failure.setValue(null);
            return failure;
        }
        CommentEntity comment = new CommentEntity(
                post.postId,
                parentCommentId,
                authorName,
                authorUid,
                parentAuthorName,
                body,
                System.currentTimeMillis()
        );
        return repository.addCommentToFirestore(post.firestoreId, comment);
    }

    /**
     * Streams whether the current user has liked the given post. Emits
     * {@code false} when there's no signed-in user, when the post has no
     * Firestore id, or when the like document doesn't exist.
     */
    public LiveData<Boolean> isLikedByCurrentUser(long postId) {
        return Transformations.switchMap(repository.getPostById(postId), post -> {
            MediatorLiveData<Boolean> result = new MediatorLiveData<>();
            if (!hasSyncedFirestorePost(post)) {
                result.setValue(false);
                return result;
            }
            FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
            if (current == null || current.getUid() == null) {
                result.setValue(false);
                return result;
            }
            LiveData<Boolean> source = repository.isLikedByCurrentUser(
                    post.firestoreId, post.postId, current.getUid());
            result.addSource(source, result::setValue);
            return result;
        });
    }

    /**
     * Toggles the current user's like on the given post. No-op (and emits
     * {@code false}) when there's no signed-in user or the post has no
     * Firestore id. Returns the new like state once the Firestore
     * transaction completes; emits {@code null} on failure.
     */
    public LiveData<Boolean> toggleLike(long postId) {
        return Transformations.switchMap(repository.getPostById(postId), post -> {
            MediatorLiveData<Boolean> result = new MediatorLiveData<>();
            if (!hasSyncedFirestorePost(post)) {
                result.setValue(false);
                return result;
            }
            FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
            if (current == null || current.getUid() == null) {
                result.setValue(false);
                return result;
            }
            LiveData<Boolean> source = repository.toggleLike(
                    post.firestoreId, post.postId, current.getUid());
            result.addSource(source, result::setValue);
            return result;
        });
    }
}
