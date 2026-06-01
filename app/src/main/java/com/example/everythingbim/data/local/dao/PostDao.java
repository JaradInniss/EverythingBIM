package com.example.everythingbim.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.everythingbim.data.local.entities.PostEntity;

import java.util.List;

/**
 * Data Access Object (DAO) for the posts table.
 * Handles database operations for creating and retrieving social media style posts.
 */
@Dao
public interface PostDao {
    /**
     * Retrieves all posts from the database.
     * @return LiveData list of all PostEntity objects.
     */
    @Query("SELECT * FROM posts")
    LiveData<List<PostEntity>> getAllPosts();

    /**
     * Synchronously retrieves all posts from the database.
     */
    @Query("SELECT * FROM posts")
    List<PostEntity> getAllPostsSync();

    /**
     * Inserts a new post into the database.
     * @param post The PostEntity to insert.
     * @return The row ID of the newly inserted post.
     */
    @Insert
    long insert(PostEntity post);

    /**
     * Updates an existing post in the database.
     * @param post The PostEntity to update.
     */
    @Update
    void update(PostEntity post);

    /**
     * Retrieves all posts in a random order. 
     * Useful for providing a randomized "feed" experience for users.
     * @return LiveData list of posts in random order.
     */
    @Query("SELECT * FROM posts ORDER BY RANDOM()")
    LiveData<List<PostEntity>> getRandomizedPosts();

    /**
     * Retrieves a single post by its unique ID.
     * @param postId The ID of the post to retrieve.
     * @return LiveData containing the post if found.
     */
    @Query("SELECT * FROM posts WHERE postId = :postId LIMIT 1")
    LiveData<PostEntity> getPostById(long postId);

    /**
     * Synchronously returns the total number of posts in the database.
     * @return The count of posts.
     */
    @Query("SELECT COUNT(*) FROM posts")
    int getPostCount();

    @Query("SELECT * FROM posts WHERE authorId = :authorId")
    LiveData<List<PostEntity>> getPostsByUserId(long authorId);
}
