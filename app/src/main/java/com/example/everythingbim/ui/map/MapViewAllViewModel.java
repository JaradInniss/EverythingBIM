package com.example.everythingbim.ui.map;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.data.local.entities.ReviewEntity;
import com.example.everythingbim.data.repository.PostRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MapViewAllViewModel extends AndroidViewModel {

    private final PostRepository postRepository;
    private final MutableLiveData<List<ReviewEntity>> reviews = new MutableLiveData<>();
    private final MutableLiveData<Float> overallRating = new MutableLiveData<>(4.5f);
    private final MediatorLiveData<List<PostEntity>> posts = new MediatorLiveData<>();
    private final MediatorLiveData<Set<Long>> likedPostIds = new MediatorLiveData<>();
    private final LiveData<List<PostEntity>> allPosts;
    private final List<LiveData<Boolean>> activeLikeSources = new ArrayList<>();
    private long locationId = -1L;

    public MapViewAllViewModel(@NonNull Application application) {
        super(application);
        postRepository = new PostRepository(application);
        allPosts = postRepository.getRandomizedPosts();
        posts.setValue(new ArrayList<>());
        likedPostIds.setValue(new HashSet<>());
        posts.addSource(allPosts, this::applyLocationFilter);
    }

    public void setLocationId(long locationId, String viewType) {
        this.locationId = locationId;
        if ("IMAGES".equalsIgnoreCase(viewType) || "POSTS".equalsIgnoreCase(viewType)) {
            applyLocationFilter(allPosts.getValue());
        } else if ("REVIEWS".equalsIgnoreCase(viewType)) {
            loadReviewsPlaceholder();
        }
    }

    public LiveData<List<PostEntity>> getPosts() {
        return posts;
    }

    public LiveData<Set<Long>> getLikedPostIds() {
        return likedPostIds;
    }

    public LiveData<List<ReviewEntity>> getReviews() {
        return reviews;
    }

    public LiveData<Float> getOverallRating() {
        return overallRating;
    }

    private void applyLocationFilter(List<PostEntity> sourcePosts) {
        List<PostEntity> filtered = new ArrayList<>();
        if (sourcePosts != null) {
            for (PostEntity post : sourcePosts) {
                if (post != null && post.locationId == locationId) {
                    filtered.add(post);
                }
            }
        }
        posts.setValue(filtered);
        observeLikedState(filtered);
    }

    private void observeLikedState(@NonNull List<PostEntity> filteredPosts) {
        for (LiveData<Boolean> source : activeLikeSources) {
            likedPostIds.removeSource(source);
        }
        activeLikeSources.clear();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getUid() == null) {
            likedPostIds.setValue(new HashSet<>());
            return;
        }

        Set<Long> currentLikedIds = new HashSet<>();
        likedPostIds.setValue(currentLikedIds);

        for (PostEntity post : filteredPosts) {
            if (post == null || post.firestoreId == null || post.firestoreId.trim().isEmpty()) {
                continue;
            }
            LiveData<Boolean> source = postRepository.isLikedByCurrentUser(
                    post.firestoreId,
                    post.postId,
                    currentUser.getUid()
            );
            activeLikeSources.add(source);
            likedPostIds.addSource(source, isLiked -> {
                Set<Long> updated = new HashSet<>(likedPostIds.getValue() != null
                        ? likedPostIds.getValue()
                        : new HashSet<>());
                if (Boolean.TRUE.equals(isLiked)) {
                    updated.add(post.postId);
                } else {
                    updated.remove(post.postId);
                }
                likedPostIds.setValue(updated);
            });
        }
    }

    private void loadReviewsPlaceholder() {
        List<ReviewEntity> placeholderList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            placeholderList.add(new ReviewEntity(
                    locationId,
                    202,
                    "This is a sample review for location #" + locationId + ". It's a great place!",
                    4.0f + (i % 2),
                    System.currentTimeMillis()
            ));
        }
        reviews.setValue(placeholderList);
    }
}
