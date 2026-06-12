package com.example.everythingbim.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
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

    @Query("SELECT * FROM posts")
    List<PostEntity> getAllPostsSync();

    /**
     * Inserts a post into the database. On primary-key conflict, the existing
     * row is left untouched (no-op). Callers that want a true upsert should
     * use {@link #upsert(PostEntity)} instead.
     *
     * <p>The previous implementation used {@link OnConflictStrategy#REPLACE},
     * which SQLite implements as DELETE-then-INSERT. That had two side
     * effects we kept hitting:
     * <ol>
     *   <li>If the new row failed the FK to {@code locations} (because the
     *       location hadn't been mirrored into the local cache yet), the
     *       DELETE still ran and the row was left missing.</li>
     *   <li>If {@code comments} or {@code likes} referenced this post, the
     *       DELETE on the parent triggered a CASCADE check that could fail
     *       and leak orphan rows.</li>
     * </ol>
     * Switching to IGNORE keeps the existing row in place when there's a
     * conflict, so neither failure mode can happen on the insert path. New
     * posts (no existing row) are inserted as before.</p>
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(PostEntity post);

    /**
     * Inserts a post, or updates the existing row in place if a post with
     * the same primary key already exists. Implemented as a
     * {@link Transaction}-wrapped default method: we try the IGNORE-insert
     * first (which is cheap and side-effect-free on conflict), and only
     * fall back to the {@code UPDATE} if the insert didn't actually write
     * a new row.
     *
     * <p>Use this from any code path that mirrors a Firestore snapshot
     * into Room. Compared to the previous REPLACE-insert this is
     * idempotent (the same doc can be mirrored twice with no error) and
     * safe to call without first ensuring the location is cached.</p>
     */
    @Transaction
    default long upsert(PostEntity post) {
        long rowId = insert(post);
        if (rowId == -1L) {
            // Conflict: an existing row with this postId is present.
            // Update it in place. SQLite's @Update does INSERT OR UPDATE
            // by primary key, so this is safe even if the row was
            // somehow deleted between the two calls.
            update(post);
            return post.postId;
        }
        return rowId;
    }

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

    /**
     * Retrieves all posts authored by the given local user id.
     * @param authorId The local Room user id of the author.
     */
    @Query("SELECT * FROM posts WHERE authorId = :authorId")
    LiveData<List<PostEntity>> getPostsByUserId(long authorId);

    /**
     * Retrieves all posts authored by the given Firebase UID.
     * @param authorUid The Firebase Auth UID of the author.
     */
    @Query("SELECT * FROM posts WHERE authorUid = :authorUid")
    LiveData<List<PostEntity>> getPostsByAuthorUid(String authorUid);

    /**
     * Synchronous variant of {@link #getPostsByAuthorUid(String)} for use
     * from background threads (e.g. inside a Firestore error fallback that
     * needs the cached posts). MUST NOT be called from the main thread -
     * Room enforces this.
     */
    @Query("SELECT * FROM posts WHERE authorUid = :authorUid")
    List<PostEntity> getPostsByAuthorUidSync(String authorUid);
}
