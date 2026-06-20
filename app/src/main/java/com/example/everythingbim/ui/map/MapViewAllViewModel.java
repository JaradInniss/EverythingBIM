package com.example.everythingbim.ui.map;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.everythingbim.data.local.AppDatabase;
import com.example.everythingbim.data.local.dao.LocationDao;
import com.example.everythingbim.data.local.dao.ReviewDao;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.data.local.entities.ReviewEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapViewAllViewModel extends AndroidViewModel {

    private final MutableLiveData<List<PostEntity>> posts = new MutableLiveData<>();
    private final MutableLiveData<List<ReviewEntity>> reviews = new MutableLiveData<>();
    private final MutableLiveData<Set<Long>> likedPostIds = new MutableLiveData<>(new HashSet<>());
    private final MutableLiveData<Float> overallRating = new MutableLiveData<>(0f);
    private final ReviewDao reviewDao;
    private final LocationDao locationDao;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private long locationId;

    public MapViewAllViewModel(@NonNull Application application) {
        super(application);
        AppDatabase database = AppDatabase.getInstance(application);
        reviewDao = database.reviewDao();
        locationDao = database.locationDao();
    }

    public void setLocationId(long locationId, String viewType) {
        this.locationId = locationId;
        if ("IMAGES".equalsIgnoreCase(viewType) || "POSTS".equalsIgnoreCase(viewType)) {
            loadPostsPlaceholder();
        } else if ("REVIEWS".equalsIgnoreCase(viewType)) {
            loadReviews();
        }
    }

    public LiveData<List<PostEntity>> getPosts() {
        return posts;
    }

    public LiveData<List<ReviewEntity>> getReviews() {
        return reviews;
    }

    public LiveData<Set<Long>> getLikedPostIds() {
        return likedPostIds;
    }

    public LiveData<Float> getOverallRating() {
        return overallRating;
    }

    private void loadPostsPlaceholder() {
        List<PostEntity> placeholderList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            placeholderList.add(new PostEntity(
                    locationId,
                    "Sample Location",
                    101L,
                    "TravelAddict",
                    "Sample post caption for post #" + (i + 1),
                    "https://picsum.photos/seed/" + (locationId + i) + "/400",
                    System.currentTimeMillis(),
                    new ArrayList<>()
            ));
        }
        posts.setValue(placeholderList);
        likedPostIds.setValue(new HashSet<>());
    }

    private void loadReviews() {
        executorService.execute(() -> {
            List<ReviewEntity> actualReviews = reviewDao.getReviewsByLocationSync(locationId);
            reviews.postValue(actualReviews);
            com.example.everythingbim.data.local.entities.LocationEntity location =
                    locationDao.getLocationByIdSync(locationId);
            overallRating.postValue(location != null ? location.rating : 0f);
        });
    }

    @Override
    protected void onCleared() {
        executorService.shutdownNow();
        super.onCleared();
    }
}
