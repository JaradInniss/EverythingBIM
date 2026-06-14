package com.example.everythingbim.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.everythingbim.data.local.entities.ReviewEntity;

import java.util.List;

@Dao
public interface ReviewDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ReviewEntity review);

    @Query("SELECT * FROM reviews WHERE locationId = :locationId ORDER BY createdAt DESC, reviewId DESC")
    LiveData<List<ReviewEntity>> getReviewsByLocation(long locationId);

    @Query("SELECT * FROM reviews WHERE locationId = :locationId ORDER BY createdAt DESC, reviewId DESC")
    List<ReviewEntity> getReviewsByLocationSync(long locationId);

    @Query("SELECT COUNT(*) FROM reviews WHERE locationId = :locationId")
    int getReviewCountForLocationSync(long locationId);

    @Query("SELECT AVG(rating) FROM reviews WHERE locationId = :locationId")
    Float getAverageRatingForLocationSync(long locationId);
}
