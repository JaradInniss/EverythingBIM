package com.example.everythingbim.ui.home;

import android.location.Location;

import androidx.annotation.NonNull;

import java.util.List;

final class RouteEstimateHelper {
    private static final double DEFAULT_WALKING_SPEED_METERS_PER_MINUTE = 5000d / 60d;

    private RouteEstimateHelper() {
    }

    static float calculateRouteDistanceMeters(double anchorLatitude,
                                              double anchorLongitude,
                                              @NonNull List<NearbySavedLocation> selectedLocations) {
        if (selectedLocations.isEmpty()) {
            return 0f;
        }

        float totalDistance = 0f;
        double currentLatitude = anchorLatitude;
        double currentLongitude = anchorLongitude;
        for (NearbySavedLocation location : selectedLocations) {
            float[] result = new float[1];
            Location.distanceBetween(
                    currentLatitude,
                    currentLongitude,
                    location.getLatitude(),
                    location.getLongitude(),
                    result
            );
            totalDistance += result[0];
            currentLatitude = location.getLatitude();
            currentLongitude = location.getLongitude();
        }
        return totalDistance;
    }

    static int estimateWalkingMinutes(float distanceMeters) {
        if (distanceMeters <= 0f) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(distanceMeters / DEFAULT_WALKING_SPEED_METERS_PER_MINUTE));
    }
}
