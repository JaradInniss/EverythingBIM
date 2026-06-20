package com.example.everythingbim.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

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

    @Query("SELECT * FROM reviews WHERE locationId = :locationId AND authorUid = :authorUid LIMIT 1")
    ReviewEntity getReviewByLocationAndAuthorUidSync(long locationId, String authorUid);

    @Query("SELECT * FROM reviews WHERE locationId = :locationId AND authorId = :authorId ORDER BY createdAt DESC, reviewId DESC LIMIT 1")
    ReviewEntity getLatestReviewByLocationAndAuthorIdSync(long locationId, long authorId);

    @Update
    void update(ReviewEntity review);

    @Query("DELETE FROM reviews WHERE locationId = :locationId AND authorUid = :authorUid AND reviewId != :keepReviewId")
    void deleteDuplicateReviewsForAuthorUid(long locationId, String authorUid, long keepReviewId);

    @Query("DELETE FROM reviews WHERE locationId = :locationId AND authorId = :authorId AND reviewId != :keepReviewId")
    void deleteDuplicateReviewsForAuthorId(long locationId, long authorId, long keepReviewId);

    @Query("SELECT COUNT(*) FROM reviews WHERE locationId = :locationId")
    int getReviewCountForLocationSync(long locationId);

    @Query("SELECT AVG(rating) FROM reviews WHERE locationId = :locationId")
    Float getAverageRatingForLocationSync(long locationId);
}
