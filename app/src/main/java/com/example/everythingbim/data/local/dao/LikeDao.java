package com.example.everythingbim.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.everythingbim.data.local.entities.LikeEntity;

import java.util.List;

/**
 * DAO for the {@code likes} table. Used by {@code PostRepository} as the
 * Room-side cache for the Firestore {@code likes} collection.
 */
@Dao
public interface LikeDao {

    /**
     * Inserts (or replaces) a like row. Replaces on conflict so that
     * Firestore snapshot re-emissions are idempotent.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(LikeEntity like);

    /**
     * Removes a like row for the given (postId, userId). Used when the
     * Firestore mirror un-likes the post.
     */
    @Query("DELETE FROM likes WHERE postId = :postId AND userId = :userId")
    void deleteByPostAndUser(long postId, String userId);

    /**
     * Streams every like for a given post (used to seed counts when
     * the Firestore denormalized counter is unavailable, e.g. offline).
     */
    @Query("SELECT * FROM likes WHERE postId = :postId")
    LiveData<List<LikeEntity>> getLikesForPost(long postId);

    /**
     * Streams the like row for a specific (postId, userId) pair, or
     * {@code null} if the user hasn't liked the post.
     */
    @Query("SELECT * FROM likes WHERE postId = :postId AND userId = :userId LIMIT 1")
    LiveData<LikeEntity> getLikeForPostAndUser(long postId, String userId);

    /**
     * Synchronously returns the number of likes for a post. MUST NOT be
     * called from the main thread; Room enforces this.
     */
    @Query("SELECT COUNT(*) FROM likes WHERE postId = :postId")
    int getLikeCountForPostSync(long postId);
}
