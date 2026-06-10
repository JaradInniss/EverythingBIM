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
import com.example.everythingbim.data.local.entities.ReportEntity;
import com.example.everythingbim.data.local.entities.UserEntity;
import com.example.everythingbim.data.models.RequestReportStatus;
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
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final LiveData<List<PostEntity>> posts;
    private final LiveData<List<LocationEntity>> locations;
    private final MutableLiveData<String> filterType = new MutableLiveData<>("account");
    private final MutableLiveData<String> authorUidFilter = new MutableLiveData<>();

    public PostViewModel(@NonNull Application application) {
        super(application);
        postRepository = new PostRepository(application);
        userRepository = new UserRepository(application);
        posts = postRepository.getRandomizedPosts();
        locations = postRepository.getAllLocations();

        // Seed placeholder data if database is empty to ensure UI is populated during testing
        postRepository.seedDataIfEmpty();
    }

    public LiveData<List<PostEntity>> getPosts() {
        return posts;
    }

    public LiveData<PostEntity> getPostById(long postId) {
        return postRepository.getPostById(postId);
    }

    public LiveData<LocationEntity> getLocationById(long locationId) {
        return postRepository.getLocationById(locationId);
    }

    public LiveData<List<UserEntity>> getTaggedUsersForPost(long postId) {
        return Transformations.switchMap(postRepository.getPostById(postId), post -> {
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

    public void setAuthorUidFilter(@NonNull String uid) {
        authorUidFilter.setValue(uid);
    }

    public LiveData<List<PostEntity>> getPostsByAuthorUid() {
        return Transformations.switchMap(authorUidFilter, postRepository::getPostsByAuthorUid);
    }

    public LiveData<List<CommentUIModel>> getCommentsForPost(long postId) {
        return Transformations.switchMap(postRepository.getPostById(postId), post -> {
            MediatorLiveData<List<CommentUIModel>> result = new MediatorLiveData<>();
            if (post == null) {
                result.setValue(Collections.emptyList());
                return result;
            }
            String postFirestoreId = post.firestoreId;
            if (postFirestoreId == null || postFirestoreId.isEmpty()) {
                result.setValue(Collections.emptyList());
                return result;
            }
            LiveData<List<CommentEntity>> source = postRepository.getCommentsForPost(postFirestoreId);
            result.addSource(source, comments -> result.setValue(buildCommentHierarchy(comments)));
            return result;
        });
    }

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

    public LiveData<CommentEntity> addComment(@NonNull PostEntity post,
                                              Long parentCommentId,
                                              @NonNull String authorName,
                                              @Nullable String authorUid,
                                              @Nullable String parentAuthorName,
                                              @NonNull String body) {
        if (post.firestoreId == null || post.firestoreId.isEmpty()) {
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
        return postRepository.addCommentToFirestore(post.firestoreId, comment);
    }

    public void reportPost(long postId, long reporterId, String reason, String description) {
        ReportEntity report = new ReportEntity(postId, reporterId, reason, description, RequestReportStatus.PENDING, System.currentTimeMillis());
        postRepository.insertReport(report);
    }

    /**
     * Streams whether the current user has liked the given post. Emits
     * {@code false} when there's no signed-in user, when the post has no
     * Firestore id, or when the like document doesn't exist.
     */
    public LiveData<Boolean> isLikedByCurrentUser(long postId) {
        return Transformations.switchMap(postRepository.getPostById(postId), post -> {
            MediatorLiveData<Boolean> result = new MediatorLiveData<>();
            if (post == null || post.firestoreId == null) {
                result.setValue(false);
                return result;
            }
            FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
            if (current == null || current.getUid() == null) {
                result.setValue(false);
                return result;
            }
            LiveData<Boolean> source = postRepository.isLikedByCurrentUser(
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
        return Transformations.switchMap(postRepository.getPostById(postId), post -> {
            MediatorLiveData<Boolean> result = new MediatorLiveData<>();
            if (post == null || post.firestoreId == null) {
                result.setValue(false);
                return result;
            }
            FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
            if (current == null || current.getUid() == null) {
                result.setValue(false);
                return result;
            }
            LiveData<Boolean> source = postRepository.toggleLike(
                    post.firestoreId, post.postId, current.getUid());
            result.addSource(source, result::setValue);
            return result;
        });
    }
}
