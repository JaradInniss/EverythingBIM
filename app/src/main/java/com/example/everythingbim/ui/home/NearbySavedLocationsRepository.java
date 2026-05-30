package com.example.everythingbim.ui.home;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.everythingbim.data.local.LocationSeedProvider;
import com.example.everythingbim.data.local.dao.LocationDao;
import com.example.everythingbim.data.local.entities.LocationEntity;
import com.example.everythingbim.data.local.entities.LocationWithDetails;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NearbySavedLocationsRepository {
    private static final int DEFAULT_RADIUS_METERS = 1000;
    private static final String SEEDED_BY = "seed_data";
    private static final int EXPECTED_SEED_COUNT = 12;

    private final LocationDao locationDao;

    public NearbySavedLocationsRepository(@NonNull LocationDao locationDao) {
        this.locationDao = locationDao;
    }

    @NonNull
    public NearbySavedLocationsResult getNearbySavedLocations(@NonNull Landmark landmark,
                                                              @Nullable Double userLatitude,
                                                              @Nullable Double userLongitude) {
        ensureSeedLocations();
        List<LocationWithDetails> allLocations = locationDao.getAllLocationsWithDetailsList();
        Map<String, NearbySavedLocation> nearbyLocationsByKey = new LinkedHashMap<>();

        for (LocationWithDetails item : allLocations) {
            LocationEntity location = item.location;
            if (location == null) {
                continue;
            }

            float distanceFromParliament = distanceBetween(
                    landmark.getLatitude(),
                    landmark.getLongitude(),
                    location.latitude,
                    location.longitude
            );

            if (distanceFromParliament > landmark.getNearbyRadiusMeters()) {
                continue;
            }

            Float distanceFromUser = null;
            if (userLatitude != null && userLongitude != null) {
                distanceFromUser = distanceBetween(
                        userLatitude,
                        userLongitude,
                        location.latitude,
                        location.longitude
                );
            }

            NearbySavedLocation nearbyLocation = new NearbySavedLocation(
                    location.locationId,
                    location.name != null && !location.name.trim().isEmpty() ? location.name.trim() : "Saved location",
                    location.latitude,
                    location.longitude,
                    location.rating,
                    location.isVerified,
                    distanceFromParliament,
                    distanceFromUser,
                    location.description,
                    location.category,
                    location.imageUrl,
                    location.address
            );

            String dedupeKey = buildLocationKey(nearbyLocation);
            NearbySavedLocation existing = nearbyLocationsByKey.get(dedupeKey);
            if (existing == null
                    || nearbyLocation.getDistanceFromParliamentMeters() < existing.getDistanceFromParliamentMeters()) {
                nearbyLocationsByKey.put(dedupeKey, nearbyLocation);
            }
        }

        List<NearbySavedLocation> nearbyLocations = new ArrayList<>(nearbyLocationsByKey.values());
        nearbyLocations.sort(Comparator.comparingDouble(NearbySavedLocation::getDistanceFromParliamentMeters));
        return new NearbySavedLocationsResult(
                landmark.getDisplayName(),
                landmark.getLatitude(),
                landmark.getLongitude(),
                landmark.getNearbyRadiusMeters() > 0 ? landmark.getNearbyRadiusMeters() : DEFAULT_RADIUS_METERS,
                nearbyLocations
        );
    }

    private void ensureSeedLocations() {
        int seedCount = locationDao.getLocationCountByAddedBy(SEEDED_BY);
        if (seedCount == 0) {
            locationDao.insertLocations(LocationSeedProvider.createSeedLocations());
            return;
        }

        int missingSeedImages = locationDao.getSeededLocationCountMissingImages(SEEDED_BY);
        if (seedCount != EXPECTED_SEED_COUNT || missingSeedImages > 0) {
            locationDao.deleteLocationsByAddedBy(SEEDED_BY);
            locationDao.insertLocations(LocationSeedProvider.createSeedLocations());
        }
    }

    @NonNull
    private String buildLocationKey(@NonNull NearbySavedLocation location) {
        String normalizedName = location.getName().trim().toLowerCase(Locale.US);
        return normalizedName + "|"
                + String.format(Locale.US, "%.5f", location.getLatitude()) + "|"
                + String.format(Locale.US, "%.5f", location.getLongitude());
    }

    private float distanceBetween(double startLatitude,
                                  double startLongitude,
                                  double endLatitude,
                                  double endLongitude) {
        float[] result = new float[1];
        Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, result);
        return result[0];
    }
}
