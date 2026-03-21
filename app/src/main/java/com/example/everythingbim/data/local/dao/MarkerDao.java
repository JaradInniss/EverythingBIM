package com.example.everythingbim.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.everythingbim.data.local.entities.MarkerEntity;

import java.util.List;

@Dao
public interface MarkerDao {

    @Query("SELECT * FROM markers")
    LiveData<List<MarkerEntity>> getAllMarkers();

    @Insert
    void insert(MarkerEntity marker);

    @Delete
    void delete(MarkerEntity marker);
}