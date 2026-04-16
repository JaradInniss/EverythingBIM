package com.example.everythingbim.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.everythingbim.data.local.entities.LocationWithDetails;

import java.util.List;

public interface LocationDao {
    @Transaction
    @Query("SELECT * FROM locations WHERE locationId = :id")
    LiveData<LocationWithDetails> getLocationWithDetailsById(long id);

    @Transaction
    @Query("SELECT * FROM locations")
    LiveData<List<LocationWithDetails>> getAllLocationsWithDetails();
}
