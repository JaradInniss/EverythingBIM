package com.example.everythingbim.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.everythingbim.data.local.entities.CommentEntity;

import java.util.List;



/**
 * Comment Data Access Object (DAO) for the comments table.
 * Provides methods for inserting and retrieving comments from the local Room database.
 */

@Dao
public interface CommentDao {
    /**
     * Inserts a new comment into the database.
     * @param comment The CommentEntity to be inserted.
     * @return The row ID of the newly inserted comment.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(CommentEntity comment);

    /**
     * Retrieves all comments associated with a specific post.
     * Results are returned in ascending order of their timestamp (oldest first).
     * 
     * @param postId The ID of the post whose comments are to be retrieved.
     * @return A LiveData list of comments for the specified post.
     */
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt ASC")
    LiveData<List<CommentEntity>> getCommentsForPost(long postId);

    @Query("DELETE FROM comments WHERE postId = :postId")
    void deleteCommentsForPost(long postId);
}
