package com.example.everythingbim.ui.map;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.data.local.entities.ReviewEntity;

import java.util.ArrayList;
import java.util.List;

public class MapViewAllViewModel extends ViewModel {

    private final MutableLiveData<List<PostEntity>> posts = new MutableLiveData<>();
    private final MutableLiveData<List<ReviewEntity>> reviews = new MutableLiveData<>();
    private final MutableLiveData<Float> overallRating = new MutableLiveData<>(4.5f);
    private long locationId;

    public void setLocationId(long locationId, String viewType) {
        this.locationId = locationId;
        if ("IMAGES".equalsIgnoreCase(viewType) || "POSTS".equalsIgnoreCase(viewType)) {
            loadPostsPlaceholder();
        } else if ("REVIEWS".equalsIgnoreCase(viewType)) {
            loadReviewsPlaceholder();
        }
    }

    public LiveData<List<PostEntity>> getPosts() {
        return posts;
    }

    public LiveData<List<ReviewEntity>> getReviews() {
        return reviews;
    }

    public LiveData<Float> getOverallRating() {
        return overallRating;
    }

    private void loadPostsPlaceholder() {
        List<PostEntity> placeholderList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            placeholderList.add(new PostEntity(
                    locationId, 
                    101, 
                    "Sample post caption for post #" + (i + 1), 
                    "https://picsum.photos/seed/" + (locationId + i) + "/400", 
                    System.currentTimeMillis()
            ));
        }
        posts.setValue(placeholderList);
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
