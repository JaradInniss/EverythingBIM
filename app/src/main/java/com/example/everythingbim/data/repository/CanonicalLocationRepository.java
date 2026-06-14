package com.example.everythingbim.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.everythingbim.ui.home.Landmark;
import com.google.android.gms.tasks.Tasks;
import com.example.everythingbim.data.local.AppDatabase;
import com.example.everythingbim.data.local.dao.LocationDao;
import com.example.everythingbim.data.local.entities.LocationEntity;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Mirrors canonical Firestore-backed locations into Room so the app can keep
 * reading from the local cache while moving toward a shared cross-device
 * location identity.
 */
public class CanonicalLocationRepository {
    private static final String TAG = "CanonicalLocationRepo";
    private static final String COLLECTION_LANDMARK_CATALOG = "landmark_catalog";

    private final LocationDao locationDao;
    private final FirebaseFirestore firestore;
    private final ExecutorService executorService;

    public CanonicalLocationRepository(@NonNull Context context) {
        AppDatabase db = AppDatabase.getInstance(context.getApplicationContext());
        locationDao = db.locationDao();
        firestore = FirebaseFirestore.getInstance();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void syncCanonicalLocations() {
        firestore.collection(COLLECTION_LANDMARK_CATALOG)
                .get()
                .addOnSuccessListener(this::mirrorCanonicalLocations)
                .addOnFailureListener(error ->
                        Log.w(TAG, "Failed to sync canonical locations", error));
    }

    @Nullable
    public Landmark findCanonicalLandmark(@NonNull List<String> candidateKeys) {
        List<String> normalizedKeys = normalizeCandidateKeys(candidateKeys);
        if (normalizedKeys.isEmpty()) {
            return null;
        }

        try {
            QuerySnapshot snapshot = Tasks.await(
                    firestore.collection(COLLECTION_LANDMARK_CATALOG).get());
            MatchCandidate bestCandidate = null;
            for (DocumentSnapshot document : snapshot.getDocuments()) {
                MatchCandidate candidate = mapCanonicalLandmark(document, normalizedKeys);
                if (candidate != null
                        && (bestCandidate == null || candidate.matchScore > bestCandidate.matchScore)) {
                    bestCandidate = candidate;
                }
            }
            return bestCandidate != null ? bestCandidate.landmark : null;
        } catch (Exception error) {
            Log.w(TAG, "Failed to resolve canonical landmark for keys " + normalizedKeys, error);
        }

        return null;
    }

    private void mirrorCanonicalLocations(@NonNull QuerySnapshot snapshot) {
        executorService.execute(() -> {
            for (DocumentSnapshot document : snapshot.getDocuments()) {
                LocationEntity location = mapCanonicalLocation(document);
                if (location == null) {
                    continue;
                }
                try {
                    locationDao.upsertCanonical(location);
                } catch (Exception error) {
                    Log.w(TAG, "Failed to upsert canonical location " + document.getId(), error);
                }
            }
        });
    }

    private LocationEntity mapCanonicalLocation(@NonNull DocumentSnapshot document) {
        String name = trimToNull(document.getString("name"));
        Double latitude = getNullableDouble(document.get("latitude"));
        Double longitude = getNullableDouble(document.get("longitude"));

        if (name == null || latitude == null || longitude == null) {
            return null;
        }

        LocationEntity location = new LocationEntity(
                name,
                latitude,
                longitude,
                getNullableFloat(document.get("rating"), 0f),
                getNullableBoolean(document.getBoolean("isVerified"), true),
                trimToNull(document.getString("addedBy")) != null
                        ? trimToNull(document.getString("addedBy"))
                        : "canonical_sync",
                defaultString(document.getString("description")),
                defaultString(document.getString("category")),
                defaultString(document.getString("imageUrl")),
                defaultString(document.getString("address"))
        );
        location.locationFirestoreId = document.getId();
        location.isActive = getNullableBoolean(document.getBoolean("isActive"), true);
        location.sourceType = trimToNull(document.getString("sourceType")) != null
                ? trimToNull(document.getString("sourceType"))
                : "canonical";
        location.placeId = trimToNull(document.getString("placeId"));
        location.updatedAt = readTimestampMillis(document.get("updatedAt"));
        return location;
    }

    @Nullable
    private MatchCandidate mapCanonicalLandmark(@NonNull DocumentSnapshot document,
                                                @NonNull List<String> normalizedKeys) {
        int matchScore = getCanonicalLandmarkMatchScore(document, normalizedKeys);
        if (matchScore < 0) {
            return null;
        }

        String displayName = firstNonEmpty(
                trimToNull(document.getString("name")),
                trimToNull(document.getString("displayName")));
        Double latitude = getNullableDouble(document.get("latitude"));
        Double longitude = getNullableDouble(document.get("longitude"));

        if (displayName == null || latitude == null || longitude == null) {
            return null;
        }

        String classifierKey = firstNonEmpty(
                trimToNull(document.getString("classifierKey")),
                trimToNull(document.getString("legacyLandmarkId")),
                trimToNull(document.getId()));
        String landmarkId = firstNonEmpty(
                trimToNull(document.getString("legacyLandmarkId")),
                classifierKey,
                trimToNull(document.getId()));
        String mapSubtitle = firstNonEmpty(
                trimToNull(document.getString("address")),
                trimToNull(document.getString("mapSubtitle")),
                displayName);

        Integer nearbyRadius = getNullableInteger(document.get("nearbyRadiusMeters"));

        Landmark landmark = new Landmark(
                landmarkId,
                classifierKey,
                displayName,
                defaultString(document.getString("description")),
                mapSubtitle,
                latitude,
                longitude,
                nearbyRadius != null ? nearbyRadius : 0
        );
        return new MatchCandidate(landmark, matchScore);
    }

    private int getCanonicalLandmarkMatchScore(@NonNull DocumentSnapshot document,
                                               @NonNull List<String> normalizedKeys) {
        int bestScore = -1;

        String documentId = normalizeLookupKey(document.getId());
        String classifierKey = normalizeLookupKey(document.getString("classifierKey"));
        String legacyLandmarkId = normalizeLookupKey(document.getString("legacyLandmarkId"));
        String name = normalizeLookupKey(document.getString("name"));
        String displayName = normalizeLookupKey(document.getString("displayName"));

        for (int index = 0; index < normalizedKeys.size(); index++) {
            String key = normalizedKeys.get(index);
            int priorityBoost = (normalizedKeys.size() - index) * 10;
            if (classifierKey != null && classifierKey.equals(key)) {
                bestScore = Math.max(bestScore, 500 + priorityBoost);
            }
            if (legacyLandmarkId != null && legacyLandmarkId.equals(key)) {
                bestScore = Math.max(bestScore, 400 + priorityBoost);
            }
            if (documentId != null && documentId.equals(key)) {
                bestScore = Math.max(bestScore, 300 + priorityBoost);
            }
            if (name != null && name.equals(key)) {
                bestScore = Math.max(bestScore, 200 + priorityBoost);
            }
            if (displayName != null && displayName.equals(key)) {
                bestScore = Math.max(bestScore, 190 + priorityBoost);
            }
        }

        Object aliasesValue = document.get("aliases");
        if (aliasesValue instanceof List<?>) {
            for (Object alias : (List<?>) aliasesValue) {
                String normalizedAlias = alias != null ? normalizeLookupKey(alias.toString()) : null;
                if (normalizedAlias == null) {
                    continue;
                }
                for (int index = 0; index < normalizedKeys.size(); index++) {
                    String key = normalizedKeys.get(index);
                    int priorityBoost = (normalizedKeys.size() - index) * 10;
                    if (normalizedAlias.equals(key)) {
                        bestScore = Math.max(bestScore, 100 + priorityBoost);
                    }
                }
            }
        }

        return bestScore;
    }

    @NonNull
    private List<String> normalizeCandidateKeys(@NonNull List<String> candidateKeys) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String candidateKey : candidateKeys) {
            addNormalizedKey(normalized, candidateKey);
        }
        return new ArrayList<>(normalized);
    }

    private void addNormalizedKey(@NonNull Set<String> keys, @Nullable String value) {
        String normalized = normalizeLookupKey(value);
        if (normalized != null) {
            keys.add(normalized);
        }
    }

    @Nullable
    private String normalizeLookupKey(@Nullable String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        return trimmed.toLowerCase(Locale.US)
                .replace("'", "")
                .replace(".", "")
                .replace(",", "");
    }

    @Nullable
    private Integer getNullableInteger(@Nullable Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    @Nullable
    private String firstNonEmpty(@Nullable String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private static final class MatchCandidate {
        private final Landmark landmark;
        private final int matchScore;

        private MatchCandidate(@NonNull Landmark landmark, int matchScore) {
            this.landmark = landmark;
            this.matchScore = matchScore;
        }
    }

    private long readTimestampMillis(Object value) {
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toDate().getTime();
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return System.currentTimeMillis();
    }

    private Double getNullableDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    private float getNullableFloat(Object value, float fallback) {
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return fallback;
    }

    private boolean getNullableBoolean(Boolean value, boolean fallback) {
        return value != null ? value : fallback;
    }

    private String defaultString(String value) {
        return value != null ? value : "";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
