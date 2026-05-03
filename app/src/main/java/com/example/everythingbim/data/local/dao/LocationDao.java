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

@Dao
public interface LocationDao {
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

    @Transaction
    @Query("SELECT * FROM locations")
    LiveData<List<LocationWithDetails>> getAllLocationsWithDetails();

    @Transaction
    @Query("SELECT * FROM locations")
    List<LocationWithDetails> getAllLocationsWithDetailsList();
}
