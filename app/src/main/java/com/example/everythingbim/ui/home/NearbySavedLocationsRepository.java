package com.example.everythingbim.ui.home;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.everythingbim.data.local.LocationSeedProvider;
import com.example.everythingbim.data.local.dao.LocationDao;
import com.example.everythingbim.data.local.dao.PostDao;
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
    private static final String LOCAL_BATHSHEBA_IMAGE = "bathsheba.jpg";
    private static final String LOCAL_HARRISONS_CAVE_IMAGE = "harrisons_cave.jpg";
    private static final String LOCAL_GAIA_IMAGE = "gaia.jpg";
    private static final String LOCAL_HOLETOWN_IMAGE = "holetown.jpg";
    private static final String LOCAL_PARLIAMENT_IMAGE = "parliament_building.jpg";
    private static final String LEGACY_BATHSHEBA_LOCATION_IMAGE = "bathsheba_beach";
    private static final float ANCHOR_SELF_DISTANCE_THRESHOLD_METERS = 20f;

    private final LocationDao locationDao;
    private final PostDao postDao;

    public NearbySavedLocationsRepository(@NonNull LocationDao locationDao,
                                          @NonNull PostDao postDao) {
        this.locationDao = locationDao;
        this.postDao = postDao;
    }

    @NonNull
    public NearbySavedLocationsResult getNearbySavedLocations(@NonNull Landmark landmark,
                                                              @Nullable Double userLatitude,
                                                              @Nullable Double userLongitude,
                                                              @Nullable Integer radiusOverrideMeters) {
        ensureSeedLocations();
        List<LocationWithDetails> allLocations = locationDao.getAllLocationsWithDetailsList();
        Map<String, NearbySavedLocation> nearbyLocationsByKey = new LinkedHashMap<>();
        int effectiveRadiusMeters = resolveRadiusMeters(landmark, radiusOverrideMeters);

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

            if (distanceFromParliament > effectiveRadiusMeters) {
                continue;
            }

            if (isAnchorLocation(landmark, location, distanceFromParliament)) {
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
                effectiveRadiusMeters,
                nearbyLocations
        );
    }

    private int resolveRadiusMeters(@NonNull Landmark landmark, @Nullable Integer radiusOverrideMeters) {
        if (radiusOverrideMeters != null && radiusOverrideMeters > 0) {
            return radiusOverrideMeters;
        }
        return landmark.getNearbyRadiusMeters() > 0
                ? landmark.getNearbyRadiusMeters()
                : DEFAULT_RADIUS_METERS;
    }

    private void ensureSeedLocations() {
        repairSampleLocationImages();

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

    private void repairSampleLocationImages() {
        for (LocationEntity location : locationDao.getAllLocationsSync()) {
            String correctedImageRef = getCorrectedImageRef(location);
            if (correctedImageRef == null || correctedImageRef.equals(location.imageUrl)) {
                continue;
            }
            locationDao.updateImageUrl(location.locationId, correctedImageRef);
        }
    }

    @Nullable
    private String getCorrectedImageRef(@NonNull LocationEntity location) {
        if (containsIgnoreCase(location.name, "Bathsheba")) {
            if (LEGACY_BATHSHEBA_LOCATION_IMAGE.equals(location.imageUrl)
                    || location.imageUrl == null
                    || location.imageUrl.trim().isEmpty()) {
                return LOCAL_BATHSHEBA_IMAGE;
            }
        }

        if (containsIgnoreCase(location.name, "Harrison's Cave")) {
            if (location.imageUrl == null || location.imageUrl.trim().isEmpty()) {
                return LOCAL_HARRISONS_CAVE_IMAGE;
            }
        }

        if (containsIgnoreCase(location.name, "Grantley Adams International Airport")
                || containsIgnoreCase(location.name, "GAIA")) {
            if (location.imageUrl == null || location.imageUrl.trim().isEmpty()) {
                return LOCAL_GAIA_IMAGE;
            }
        }

        if (containsIgnoreCase(location.name, "Holetown")) {
            if (location.imageUrl == null || location.imageUrl.trim().isEmpty()) {
                return LOCAL_HOLETOWN_IMAGE;
            }
        }

        if (containsIgnoreCase(location.name, "Barbados Parliament Buildings")
                || containsIgnoreCase(location.name, "Parliament Building")
                || containsIgnoreCase(location.name, "Parliament Buildings")) {
            if (location.imageUrl == null || location.imageUrl.trim().isEmpty()) {
                return LOCAL_PARLIAMENT_IMAGE;
            }
        }

        if (location.imageUrl == null || location.imageUrl.trim().isEmpty()) {
            String representativePostImage = postDao.getLatestImageUrlForLocationSync(location.locationId);
            if (representativePostImage != null && !representativePostImage.trim().isEmpty()) {
                return representativePostImage.trim();
            }
        }

        return null;
    }

    private boolean containsIgnoreCase(@Nullable String text, @NonNull String query) {
        return text != null && text.toLowerCase(Locale.US).contains(query.toLowerCase(Locale.US));
    }

    private boolean isAnchorLocation(@NonNull Landmark landmark,
                                     @NonNull LocationEntity location,
                                     float distanceFromAnchorMeters) {
        if (distanceFromAnchorMeters <= ANCHOR_SELF_DISTANCE_THRESHOLD_METERS) {
            return true;
        }

        String anchorName = normalizeName(landmark.getDisplayName());
        String locationName = normalizeName(location.name);
        if (!anchorName.isEmpty() && anchorName.equals(locationName)) {
            return true;
        }

        String anchorToken = normalizeName(landmark.getToken());
        return !anchorToken.isEmpty() && anchorToken.equals(locationName);
    }

    @NonNull
    private String normalizeName(@Nullable String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.US)
                .replace("'", "")
                .replace(".", "")
                .replace(",", "");
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
