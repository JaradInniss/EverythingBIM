package com.example.everythingbim.ui.map;

import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.everythingbim.data.local.AppDatabase;
import com.example.everythingbim.data.local.dao.MarkerDao;
import com.example.everythingbim.data.local.entities.MarkerEntity;
import com.example.everythingbim.data.local.entities.ReviewEntity;
import com.example.everythingbim.data.models.MapDetailsState;
import com.example.everythingbim.ui.login.LoginViewModel;
import com.example.everythingbim.ui.utils.NavigationCommand;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MapViewModel extends AndroidViewModel {

    private final MarkerDao markerDao;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final LiveData<List<MarkerEntity>> allMarkers;
    private final SingleLiveEvent<NavigationCommand> navigationEvent = new SingleLiveEvent<>();

    // Tracks if a location is currently selected to manage UI visibility and map padding
    private final MutableLiveData<MapDetailsState> detailsUIState = new MutableLiveData<>(MapDetailsState.HIDDEN);
    
    // Tracks the specific location to focus on when selected
    private final MutableLiveData<LatLng> focusedLocation = new MutableLiveData<>();
    private final MutableLiveData<Integer> rating = new MutableLiveData<>();
    private final MutableLiveData<String> reviewBody = new MutableLiveData<>();
    private final MediatorLiveData<Boolean> isReviewValid = new MediatorLiveData<>();
    private final SingleLiveEvent<Boolean> reviewSubmited = new SingleLiveEvent<>();
    private final LiveData<Long> authorId = new MutableLiveData<>();

    // Metadata for navigation
    private long selectedLocationId = -1;
    private String selectedLocationName = "";

    // --- Barbados Map Constants ---
    // Define the bounding box for Barbados
    private static final LatLng BARBADOS_SOUTHWEST = new LatLng(13.01, -59.68);
    private static final LatLng BARBADOS_NORTHEAST = new LatLng(13.34, -59.38);
    public static final LatLngBounds BARBADOS_BOUNDS = new LatLngBounds(BARBADOS_SOUTHWEST, BARBADOS_NORTHEAST);

    // Zoom settings
    public static final float MIN_ZOOM = 10.5f; // Ensures the whole island is visible and doesn't zoom out too far
    public static final float INITIAL_ZOOM = 11.0f;

    public MapViewModel(Application application) {
        super(application);
        markerDao = AppDatabase.getInstance(application).markerDao();
        allMarkers = markerDao.getAllMarkers();

        isReviewValid.addSource(reviewBody, body -> isReviewValid());
        isReviewValid.addSource(rating, rating -> isReviewValid());

        rating.setValue(0);
        reviewBody.setValue("");
    }

    public LiveData<List<MarkerEntity>> getAllMarkers() {
        return allMarkers;
    }

    public LiveData<MapDetailsState> getDetailsUIState() {
        return detailsUIState;
    }

    public void setDetailsUIState(MapDetailsState state) {
        detailsUIState.setValue(state);
    }

    public LiveData<LatLng> getFocusedLocation() { 
        return focusedLocation; 
    }

    public void setFocusedLocation(LatLng latLng) { 
        focusedLocation.setValue(latLng); 
    }

    public void setSelectedLocationMetadata(long id, String name) {
        this.selectedLocationId = id;
        this.selectedLocationName = name;
    }

    public void setRating(int newRating) { rating.setValue(newRating); }

    public LiveData<Integer> getRating() { return rating; }

    public LiveData<String> getReviewBody() { return reviewBody; }

    public void setReviewBody(String newReview) { reviewBody.setValue(newReview); }

    public LiveData<Boolean> getReviewSubmited() { return reviewSubmited; }

    public void insertMarker(MarkerEntity marker) {
        executorService.execute(() -> markerDao.insert(marker));
    }

    public void deleteMarker(MarkerEntity marker) {
        executorService.execute(() -> markerDao.delete(marker));
    }

    /**
     * Returns the center of Barbados.
     */
    public LatLng getBarbadosCenter() {
        return BARBADOS_BOUNDS.getCenter();
    }

    public SingleLiveEvent<NavigationCommand> getNavigationEvent() {
        return navigationEvent;
    }

    public void onViewAllClicked(String viewType) {
        Bundle extras = new Bundle();
        extras.putLong("LOCATION_ID", selectedLocationId);
        extras.putString("LOCATION_NAME", selectedLocationName);
        extras.putString("VIEW_TYPE", viewType);

        navigationEvent.setValue(new NavigationCommand(MapViewAllActivity.class, extras));
    }

    public void isReviewValid() {
        String currBody = reviewBody.getValue();
        Integer currRating = rating.getValue();

        boolean isValid = (currBody != null && !currBody.isEmpty()) && (currRating != null && currRating > 0);
        isReviewValid.setValue(isValid);
    }

    public LiveData<Boolean> getIsReviewValid() {
        return isReviewValid;
    }

    public void resetReviewForm() {
        reviewBody.setValue("");
        rating.setValue(0);
        reviewSubmited.setValue(false);
    }

    public void submitReview() {
        // Handle submit review here
    }


    public static class SingleLiveEvent<T> extends MutableLiveData<T> {
        private final AtomicBoolean pending = new AtomicBoolean(false);

        @Override
        public void setValue(T value) {
            pending.set(true);
            super.setValue(value);
        }

        @Override
        public void observe(@NonNull androidx.lifecycle.LifecycleOwner owner,
                            @NonNull androidx.lifecycle.Observer<? super T> observer) {
            super.observe(owner, t -> {
                if (pending.compareAndSet(true, false)) {
                    observer.onChanged(t);
                }
            });
        }
    }

}
