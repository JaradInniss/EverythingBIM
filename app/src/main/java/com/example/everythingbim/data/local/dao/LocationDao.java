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
    @Transaction
    @Query("SELECT * FROM locations WHERE locationId = :id")
    LiveData<LocationWithDetails> getLocationWithDetailsById(long id);

    /**
     * Retrieves all locations stored in the database, including their associated details.
     * 
     * @return LiveData list of all locations with their details.
     */
    @Transaction
    @Query("SELECT * FROM locations")
    LiveData<List<LocationWithDetails>> getAllLocationsWithDetails();

    /**
     * Inserts a new location or replaces an existing one if there's a conflict.
     * 
     * @param location The LocationEntity to save.
     * @return The row ID of the inserted location.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(LocationEntity location);
}
