package com.example.everythingbim.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.everythingbim.data.local.AppDatabase;
import com.example.everythingbim.data.local.dao.LocationDao;
import com.example.everythingbim.data.local.entities.LocationEntity;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

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
