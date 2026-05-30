package com.example.everythingbim.ui.home;

import androidx.annotation.NonNull;

public class Landmark {
    private final String id;
    private final String token;
    private final String displayName;
    private final String description;
    private final double latitude;
    private final double longitude;
    private final int nearbyRadiusMeters;

    public Landmark(@NonNull String id,
                    @NonNull String token,
                    @NonNull String displayName,
                    @NonNull String description,
                    double latitude,
                    double longitude,
                    int nearbyRadiusMeters) {
        this.id = id;
        this.token = token;
        this.displayName = displayName;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.nearbyRadiusMeters = nearbyRadiusMeters;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getToken() {
        return token;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getNearbyRadiusMeters() {
        return nearbyRadiusMeters;
    }
}
