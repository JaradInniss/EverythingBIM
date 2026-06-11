package com.example.everythingbim.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.everythingbim.data.local.entities.LikeEntity;

import java.util.List;

@Dao
public interface LikeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(LikeEntity like);

    @Query("DELETE FROM likes WHERE postId = :postId AND userId = :userId")
    void deleteByPostAndUser(long postId, String userId);

    @Query("SELECT * FROM likes WHERE postId = :postId")
    LiveData<List<LikeEntity>> getLikesForPost(long postId);

    @Query("SELECT * FROM likes WHERE postId = :postId AND userId = :userId LIMIT 1")
    LiveData<LikeEntity> getLikeForPostAndUser(long postId, String userId);

    @Query("SELECT COUNT(*) FROM likes WHERE postId = :postId")
    int getLikeCountForPostSync(long postId);
}
