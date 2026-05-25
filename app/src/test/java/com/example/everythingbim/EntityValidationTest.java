package com.example.everythingbim;

import static org.junit.Assert.*;

import com.example.everythingbim.data.local.entities.CommentEntity;
import com.example.everythingbim.data.local.entities.LikeEntity;
import com.example.everythingbim.data.local.entities.LocationEntity;
import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.data.local.entities.ReviewEntity;

import org.junit.Test;

/**
 * Unit tests for Room entity classes.
 * Tests entity construction using actual constructors.
 */
public class   EntityValidationTest {

    // ========== LocationEntity Tests ==========

    @Test
    public void locationEntity_constructor_setsAllFields() {
        LocationEntity location = new LocationEntity(
            "Test Location",
            13.0,
            -59.5,
            4.0f,
            true,
            "testUser",
            "Test description",
            "testCategory",
            "https://example.com/image.jpg",
            "Test Address"
        );

        assertEquals("Name should be Test Location", "Test Location", location.name);
        assertEquals("Latitude should be 13.0", 13.0, location.latitude, 0.0001);
        assertEquals("Longitude should be -59.5", -59.5, location.longitude, 0.0001);
        assertEquals("Rating should be 4.0", 4.0f, location.rating, 0.0f);
        assertTrue("isVerified should be true", location.isVerified);
        assertEquals("addedBy should be testUser", "testUser", location.addedBy);
        assertEquals("description should match", "Test description", location.description);
        assertEquals("category should be testCategory", "testCategory", location.category);
        assertEquals("imageUrl should match", "https://example.com/image.jpg", location.imageUrl);
        assertEquals("address should match", "Test Address", location.address);
    }

    @Test
    public void locationEntity_ratingCanBeMaxValue() {
        LocationEntity location = new LocationEntity(
            "Test Location",
            0, 0, 5.0f, false, "user", "desc", "cat", "url", "addr"
        );
        assertEquals("Rating of 5.0 should be allowed", 5.0f, location.rating, 0.0f);
    }

    @Test
    public void locationEntity_ratingCanBeZero() {
        LocationEntity location = new LocationEntity(
            "Test Location", 0, 0, 0.0f, false, "user", "desc", "cat", "url", "addr"
        );
        assertEquals("Rating of 0.0 should be allowed", 0.0f, location.rating, 0.0f);
    }

    @Test
    public void locationEntity_canHandleNullFields() {
        LocationEntity location = new LocationEntity(
            null, 0, 0, 0.0f, false, null, null, null, null, null
        );
        assertNull("Name should be null", location.name);
        assertNull("Description should be null", location.description);
        assertNull("ImageUrl should be null", location.imageUrl);
        assertNull("Address should be null", location.address);
        assertNull("AddedBy should be null", location.addedBy);
        assertNull("Category should be null", location.category);
    }

    // ========== PostEntity Tests ==========

    @Test
    public void postEntity_constructor_setsAllFields() {
        long timestamp = System.currentTimeMillis();
        PostEntity post = new PostEntity(123L, 456L, "Great beach day!", "https://example.com/post.jpg", timestamp);

        assertEquals("LocationId should be 123", 123L, post.locationId);
        assertEquals("AuthorId should be 456", 456L, post.authorId);
        assertEquals("Caption should be Great beach day!", "Great beach day!", post.caption);
        assertEquals("ImageUrl should match", "https://example.com/post.jpg", post.imageUrl);
        assertEquals("CreatedAt should match timestamp", timestamp, post.createdAt);
    }

    @Test
    public void postEntity_canHandleNullFields() {
        PostEntity post = new PostEntity(1L, 2L, null, null, 0L);
        assertNull("Caption should be null", post.caption);
        assertNull("ImageUrl should be null", post.imageUrl);
    }

    // ========== CommentEntity Tests ==========

    @Test
    public void commentEntity_constructor_setsAllFields() {
        long timestamp = System.currentTimeMillis();
        CommentEntity comment = new CommentEntity("Great post!", "user123", "post456", "POST", timestamp);

        assertEquals("Body should be Great post!", "Great post!", comment.body);
        assertEquals("AuthorId should be user123", "user123", comment.authorId);
        assertEquals("TargetId should be post456", "post456", comment.targetId);
        assertEquals("TargetType should be POST", "POST", comment.targetType);
        assertEquals("CreatedAt should match timestamp", timestamp, comment.createdAt);
    }

    @Test
    public void commentEntity_canHandleNullFields() {
        CommentEntity comment = new CommentEntity(null, null, null, null, 0L);
        assertNull("Body should be null", comment.body);
        assertNull("AuthorId should be null", comment.authorId);
        assertNull("TargetId should be null", comment.targetId);
        assertNull("TargetType should be null", comment.targetType);
    }

    // ========== LikeEntity Tests ==========

    @Test
    public void likeEntity_constructor_setsAllFields() {
        long timestamp = System.currentTimeMillis();
        LikeEntity like = new LikeEntity("user123", "post456", "POST", timestamp);

        assertEquals("UserId should be user123", "user123", like.userId);
        assertEquals("TargetId should be post456", "post456", like.targetId);
        assertEquals("TargetType should be POST", "POST", like.targetType);
        assertEquals("LikedAt should match timestamp", timestamp, like.likedAt);
    }

    // ========== ReviewEntity Tests ==========

    @Test
    public void reviewEntity_constructor_setsAllFields() {
        long timestamp = System.currentTimeMillis();
        ReviewEntity review = new ReviewEntity(123L, 456L, "Great place!", 4.5f, timestamp);

        assertEquals("LocationId should be 123", 123L, review.locationId);
        assertEquals("AuthorId should be 456", 456L, review.authorId);
        assertEquals("Body should be Great place!", "Great place!", review.body);
        assertEquals("Rating should be 4.5", 4.5f, review.rating, 0.0f);
        assertEquals("CreatedAt should match timestamp", timestamp, review.createdAt);
    }

    @Test
    public void reviewEntity_ratingCanBeMaxValue() {
        ReviewEntity review = new ReviewEntity(1L, 2L, "body", 5.0f, 0L);
        assertEquals("Rating of 5.0 should be allowed", 5.0f, review.rating, 0.0f);
    }

    @Test
    public void reviewEntity_ratingCanBeZero() {
        ReviewEntity review = new ReviewEntity(1L, 2L, "body", 0.0f, 0L);
        assertEquals("Rating of 0.0 should be allowed", 0.0f, review.rating, 0.0f);
    }

    @Test
    public void reviewEntity_canHandleNullBody() {
        ReviewEntity review = new ReviewEntity(1L, 2L, null, 4.0f, 0L);
        assertNull("Body should be null", review.body);
    }

    // ========== Field Accessibility Tests ==========

    @Test
    public void locationEntity_fieldsArePublic() {
        LocationEntity location = new LocationEntity("name", 1.0, 2.0, 3.0f, true, "added", "desc", "cat", "url", "addr");
        assertEquals("locationId should be accessible", 0L, location.locationId);
        location.locationId = 999L;
        assertEquals("locationId should be settable", 999L, location.locationId);
    }

    @Test
    public void postEntity_fieldsArePublic() {
        PostEntity post = new PostEntity(1L, 2L, "caption", "url", 0L);
        assertEquals("postId should be accessible", 0L, post.postId);
        post.postId = 888L;
        assertEquals("postId should be settable", 888L, post.postId);
    }

    @Test
    public void commentEntity_fieldsArePublic() {
        CommentEntity comment = new CommentEntity("body", "author", "target", "POST", 0L);
        assertEquals("commentId should be accessible", 0L, comment.commentId);
        comment.commentId = 777L;
        assertEquals("commentId should be settable", 777L, comment.commentId);
    }

    @Test
    public void reviewEntity_fieldsArePublic() {
        ReviewEntity review = new ReviewEntity(1L, 2L, "body", 4.0f, 0L);
        assertEquals("reviewId should be accessible", 0L, review.reviewId);
        review.reviewId = 666L;
        assertEquals("reviewId should be settable", 666L, review.reviewId);
    }
}