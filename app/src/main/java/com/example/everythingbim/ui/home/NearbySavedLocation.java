package com.example.everythingbim.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.Locale;

public class NearbySavedLocation implements Serializable {
    private final long locationId;
    private final String name;
    private final double latitude;
    private final double longitude;
    private final float rating;
    private final boolean verified;
    private final float distanceFromParliamentMeters;
    private final Float distanceFromUserMeters;
    private final String description;
    private final String category;
    private final String imageUrl;
    private final String address;

    public NearbySavedLocation(long locationId,
                               @NonNull String name,
                               double latitude,
                               double longitude,
                               float rating,
                               boolean verified,
                               float distanceFromParliamentMeters,
                               @Nullable Float distanceFromUserMeters,
                               @Nullable String description,
                               @Nullable String category,
                               @Nullable String imageUrl,
                               @Nullable String address) {
        this.locationId = locationId;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.rating = rating;
        this.verified = verified;
        this.distanceFromParliamentMeters = distanceFromParliamentMeters;
        this.distanceFromUserMeters = distanceFromUserMeters;
        this.description = description;
        this.category = category;
        this.imageUrl = imageUrl;
        this.address = address;
    }

    public long getLocationId() {
        return locationId;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public float getRating() {
        return rating;
    }

    public boolean isVerified() {
        return verified;
    }

    public float getDistanceFromParliamentMeters() {
        return distanceFromParliamentMeters;
    }

    @Nullable
    public Float getDistanceFromUserMeters() {
        return distanceFromUserMeters;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Nullable
    public String getCategory() {
        return category;
    }

    @Nullable
    public String getImageUrl() {
        return imageUrl;
    }

    @Nullable
    public String getAddress() {
        return address;
    }

    @NonNull
    public String getDisplayLabel() {
        StringBuilder label = new StringBuilder(name)
                .append(" | ")
                .append(formatDistance(distanceFromParliamentMeters))
                .append(" away");

        if (verified) {
            label.append(" | Verified");
        }
        return label.toString();
    }

    @NonNull
    public String getDistanceLabel() {
        return formatDistance(distanceFromParliamentMeters) + " from anchor";
    }

    @NonNull
    public String getDescriptionOrFallback() {
        if (description != null && !description.trim().isEmpty()) {
            return description.trim();
        }
        if (address != null && !address.trim().isEmpty()) {
            return address.trim();
        }
        return "No description available yet.";
    }

    @NonNull
    public String getAddressOrFallback() {
        if (address != null && !address.trim().isEmpty()) {
            return address.trim();
        }
        return formatLatLng(latitude, longitude);
    }

    @NonNull
    public String getCategoryLabel() {
        if (category != null && !category.trim().isEmpty()) {
            String normalized = category.trim().replace('_', ' ');
            if (normalized.length() == 1) {
                return normalized.toUpperCase(Locale.US);
            }
            return normalized.substring(0, 1).toUpperCase(Locale.US)
                    + normalized.substring(1);
        }
        return "Nearby location";
    }

    @NonNull
    private String formatDistance(float meters) {
        if (meters >= 1000f) {
            return String.format(Locale.US, "%.1f km", meters / 1000f);
        }
        return String.format(Locale.US, "%.0f m", meters);
    }

    @NonNull
    private String formatLatLng(double lat, double lng) {
        return String.format(Locale.US, "Lat: %.5f, Lng: %.5f", lat, lng);
    }
}
