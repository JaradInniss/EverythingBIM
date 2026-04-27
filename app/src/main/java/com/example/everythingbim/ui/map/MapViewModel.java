package com.example.everythingbim.ui.map;

import android.app.Application;
import android.os.Bundle;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.everythingbim.data.local.AppDatabase;
import com.example.everythingbim.data.local.dao.MarkerDao;
import com.example.everythingbim.data.local.entities.MarkerEntity;
import com.example.everythingbim.data.models.MapDetailsState;
import com.example.everythingbim.ui.login.LoginViewModel;
import com.example.everythingbim.ui.utils.NavigationCommand;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapViewModel extends AndroidViewModel {

    private final MarkerDao markerDao;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final LiveData<List<MarkerEntity>> allMarkers;
    private final LoginViewModel.SingleLiveEvent<NavigationCommand> navigationEvent = new LoginViewModel.SingleLiveEvent<>();

    // Tracks if a location is currently selected to manage UI visibility and map padding
    private final MutableLiveData<MapDetailsState> detailsUIState = new MutableLiveData<>(MapDetailsState.HIDDEN);
    
    // Tracks the specific location to focus on when selected
    private final MutableLiveData<LatLng> focusedLocation = new MutableLiveData<>();

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

    public LoginViewModel.SingleLiveEvent<NavigationCommand> getNavigationEvent() {
        return navigationEvent;
    }

    public void onViewAllClicked(String viewType) {
        Bundle extras = new Bundle();
        extras.putLong("LOCATION_ID", selectedLocationId);
        extras.putString("LOCATION_NAME", selectedLocationName);
        extras.putString("VIEW_TYPE", viewType);

        navigationEvent.setValue(new NavigationCommand(MapViewAllActivity.class, extras));
    }
}
