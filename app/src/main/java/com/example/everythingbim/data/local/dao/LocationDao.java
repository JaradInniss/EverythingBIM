package com.example.everythingbim.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.everythingbim.data.local.entities.LocationEntity;
import com.example.everythingbim.data.local.entities.LocationWithDetails;

import java.util.List;

/**
 * Data Access Object (DAO) for location-related database operations.
 * Handles retrieval of locations along with their nested details (like markers or files).
 */
@Dao
public interface LocationDao {
    /**
     * Retrieves a single location by its ID, including all its associated details.
     * Uses @Transaction because it involves multiple tables (via LocationWithDetails).
     * 
     * @param id The unique ID of the location.
     * @return LiveData containing the location and its associated data.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertLocation(LocationEntity location);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLocations(List<LocationEntity> locations);

    @Query("SELECT COUNT(*) FROM locations")
    int getLocationCount();

    @Query("SELECT COUNT(*) FROM locations WHERE addedBy = :addedBy")
    int getLocationCountByAddedBy(String addedBy);

    @Query("SELECT COUNT(*) FROM locations WHERE addedBy = :addedBy AND (imageUrl IS NULL OR imageUrl = '')")
    int getSeededLocationCountMissingImages(String addedBy);

    @Query("DELETE FROM locations WHERE addedBy = :addedBy")
    void deleteLocationsByAddedBy(String addedBy);

    @Transaction
    @Query("SELECT * FROM locations WHERE locationId = :id")
    LiveData<LocationWithDetails> getLocationWithDetailsById(long id);

    @Query("SELECT * FROM locations WHERE locationId = :id LIMIT 1")
    LiveData<LocationEntity> getLocationById(long id);

    @Query("SELECT * FROM locations WHERE locationFirestoreId = :firestoreId LIMIT 1")
    LiveData<LocationEntity> getLocationByFirestoreId(String firestoreId);

    @Query("SELECT * FROM locations WHERE locationFirestoreId = :firestoreId LIMIT 1")
    LocationEntity getLocationByFirestoreIdSync(String firestoreId);

    @Query("SELECT * FROM locations WHERE lower(trim(name)) = lower(trim(:name)) LIMIT 1")
    LocationEntity getLocationByNameSync(String name);

    @Query("SELECT * FROM locations WHERE lower(trim(name)) = lower(trim(:name)) AND (latitude != 0 OR longitude != 0) ORDER BY locationId DESC LIMIT 1")
    LiveData<LocationEntity> getResolvedLocationByName(String name);

    @Query("SELECT * FROM locations WHERE lower(trim(name)) = lower(trim(:name)) AND (latitude != 0 OR longitude != 0) ORDER BY locationId DESC LIMIT 1")
    LocationEntity getResolvedLocationByNameSync(String name);

    @Query("SELECT * FROM locations " +
            "WHERE (latitude != 0 OR longitude != 0) " +
            "AND (" +
            "lower(name) LIKE '%' || lower(trim(:name)) || '%' " +
            "OR lower(trim(:name)) LIKE '%' || lower(name) || '%'" +
            ") " +
            "ORDER BY locationId DESC LIMIT 1")
    LiveData<LocationEntity> getResolvedLocationByNameLoose(String name);

    @Query("SELECT * FROM locations " +
            "WHERE (latitude != 0 OR longitude != 0) " +
            "AND (" +
            "lower(name) LIKE '%' || lower(trim(:name)) || '%' " +
            "OR lower(trim(:name)) LIKE '%' || lower(name) || '%'" +
            ") " +
            "ORDER BY locationId DESC LIMIT 1")
    LocationEntity getResolvedLocationByNameLooseSync(String name);

    /**
     * Synchronous variant of {@link #getLocationById(long)} for use from
     * background threads (e.g. inside a Firestore mirror that needs to
     * check the cache before inserting a child row). MUST NOT be called
     * from the main thread; Room enforces this.
     *
     * @return the {@link LocationEntity} with the given id, or
     *         {@code null} if it isn't cached locally.
     */
    @Query("SELECT * FROM locations WHERE locationId = :id LIMIT 1")
    LocationEntity getLocationByIdSync(long id);

    /**
     * Retrieves all locations stored in the database, including their associated details.
     * 
     * @return LiveData list of all locations with their details.
     */
    @Transaction
    @Query("SELECT * FROM locations")
    LiveData<List<LocationWithDetails>> getAllLocationsWithDetails();

    @Query("SELECT * FROM locations")
    LiveData<List<LocationEntity>> getAllLocations();

    @Query("SELECT * FROM locations WHERE isActive = 1")
    LiveData<List<LocationEntity>> getActiveLocations();

    @Query("SELECT * FROM locations")
    List<LocationEntity> getAllLocationsSync();

    /**
     * Inserts a new location or replaces an existing one if there's a conflict.
     * 
     * @param location The LocationEntity to save.
     * @return The row ID of the inserted location.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(LocationEntity location);

    @Update
    void update(LocationEntity location);

    @Transaction
    default long upsertCanonical(LocationEntity location) {
        if (location.locationFirestoreId != null && !location.locationFirestoreId.trim().isEmpty()) {
            LocationEntity existingByFirestoreId =
                    getLocationByFirestoreIdSync(location.locationFirestoreId.trim());
            if (existingByFirestoreId != null) {
                location.locationId = existingByFirestoreId.locationId;
                update(location);
                return location.locationId;
            }
        }

        if (location.name != null && !location.name.trim().isEmpty()) {
            LocationEntity existingByName = getLocationByNameSync(location.name.trim());
            if (existingByName != null) {
                location.locationId = existingByName.locationId;
                update(location);
                return location.locationId;
            }
        }

        return insert(location);
    }

    @Query("UPDATE locations SET imageUrl = :imageUrl WHERE locationId = :locationId")
    void updateImageUrl(long locationId, String imageUrl);

    @Transaction
    @Query("SELECT * FROM locations")
    List<LocationWithDetails> getAllLocationsWithDetailsList();
}
