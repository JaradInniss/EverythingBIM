package com.example.everythingbim.ui.home;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

public class NearbySavedLocationsResult {
    private final String anchorName;
    private final double anchorLatitude;
    private final double anchorLongitude;
    private final int searchRadiusMeters;
    private final List<NearbySavedLocation> nearbyLocations;

    public NearbySavedLocationsResult(@NonNull String anchorName,
                                      double anchorLatitude,
                                      double anchorLongitude,
                                      int searchRadiusMeters,
                                      @NonNull List<NearbySavedLocation> nearbyLocations) {
        this.anchorName = anchorName;
        this.anchorLatitude = anchorLatitude;
        this.anchorLongitude = anchorLongitude;
        this.searchRadiusMeters = searchRadiusMeters;
        this.nearbyLocations = Collections.unmodifiableList(nearbyLocations);
    }

    @NonNull
    public String getAnchorName() {
        return anchorName;
    }

    public double getAnchorLatitude() {
        return anchorLatitude;
    }

    public double getAnchorLongitude() {
        return anchorLongitude;
    }

    public int getSearchRadiusMeters() {
        return searchRadiusMeters;
    }

    @NonNull
    public List<NearbySavedLocation> getNearbyLocations() {
        return nearbyLocations;
    }
}
