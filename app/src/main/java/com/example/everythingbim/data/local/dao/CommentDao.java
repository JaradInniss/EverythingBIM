package com.example.everythingbim.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.everythingbim.data.local.entities.CommentEntity;

import java.util.List;



/**
 * Comment Data Access Object (DAO) for the comments table.
 * Provides methods for inserting and retrieving comments from the local Room database.
 *
 * <p>The {@code comments} table is a local cache for the Firestore
 * {@code posts/{postId}/comments} subcollection. Writes use
 * {@link OnConflictStrategy#REPLACE} so the Firestore mirror is
 * idempotent on re-snapshot.</p>
 */

@Dao
public interface CommentDao {
    /**
     * Inserts (or replaces) a comment in the database. Replaces on
     * conflict so that Firestore snapshot re-emissions are idempotent.
     * @param comment The CommentEntity to be inserted.
     * @return The row ID of the newly inserted comment.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(CommentEntity comment);

    /**
     * Updates an existing comment in the database.
     * @param comment The CommentEntity to be updated.
     */
    @Update
    void update(CommentEntity comment);

    /**
     * Retrieves all comments associated with a specific post.
     * Results are returned in ascending order of their creation timestamp
     * (oldest first).
     *
     * @param postId The ID of the post whose comments are to be retrieved.
     * @return A LiveData list of comments for the specified post.
     */
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt ASC")
    LiveData<List<CommentEntity>> getCommentsForPost(long postId);

    /**
     * Deletes every comment for a post. Used by the Firestore mirror
     * when a full re-snapshot arrives so we don't accumulate stale rows.
     */
    @Query("DELETE FROM comments WHERE postId = :postId")
    void deleteCommentsForPost(long postId);
}

