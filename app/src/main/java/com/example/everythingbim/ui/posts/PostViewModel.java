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
 * Handles fetching posts, filtering, and building the comment hierarchy.
 */
public class PostViewModel extends AndroidViewModel {
    private final PostRepository repository;
    private final UserRepository userRepository;
    private final LiveData<List<PostEntity>> posts;
    private final LiveData<List<LocationEntity>> locations;
    private final MutableLiveData<String> filterType = new MutableLiveData<>("account");

    public PostViewModel(@NonNull Application application) {
        super(application);
        repository = new PostRepository(application);
        userRepository = new UserRepository(application);
        posts = repository.getRandomizedPosts();
        locations = repository.getAllLocations();

        repository.seedDataIfEmpty();
    }

    public LiveData<List<PostEntity>> getPosts() {
        return posts;
    }

    public LiveData<PostEntity> getPostById(long postId) {
        return repository.getPostById(postId);
    }

    public LiveData<LocationEntity> getLocationById(long locationId) {
        return repository.getLocationById(locationId);
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

    public LiveData<List<UserEntity>> getTaggedUsersForPost(long postId) {
        return Transformations.switchMap(repository.getPostById(postId), post -> {
            if (post == null || post.taggedUserUids == null || post.taggedUserUids.isEmpty()) {
                MutableLiveData<List<UserEntity>> empty = new MutableLiveData<>();
                empty.setValue(Collections.emptyList());
                return empty;
            }
            return userRepository.getUsersByFirebaseUids(post.taggedUserUids);
        });
    }

    public LiveData<List<CommentUIModel>> getCommentsForPost(long postId) {
        return Transformations.switchMap(repository.getPostById(postId), post -> {
            MediatorLiveData<List<CommentUIModel>> result = new MediatorLiveData<>();
            if (!hasSyncedFirestorePost(post)) {
                result.setValue(Collections.emptyList());
                return result;
            }
            LiveData<List<CommentEntity>> source = repository.getCommentsForPost(post.firestoreId);
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
            LiveData<Boolean> source = repository.isLikedByCurrentUser(post.firestoreId, post.postId, current.getUid());
            result.addSource(source, result::setValue);
            return result;
        });
    }

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
            LiveData<Boolean> source = repository.toggleLike(post.firestoreId, post.postId, current.getUid());
            result.addSource(source, result::setValue);
            return result;
        });
    }

    private boolean hasSyncedFirestorePost(PostEntity post) {
        return post != null
                && post.firestoreId != null
                && !post.firestoreId.trim().isEmpty();
    }
}
