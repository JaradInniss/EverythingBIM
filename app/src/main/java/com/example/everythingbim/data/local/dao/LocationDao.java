package com.example.everythingbim.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

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

    @Query("UPDATE locations SET imageUrl = :imageUrl WHERE locationId = :locationId")
    void updateImageUrl(long locationId, String imageUrl);

    @Transaction
    @Query("SELECT * FROM locations")
    List<LocationWithDetails> getAllLocationsWithDetailsList();
}
