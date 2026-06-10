package com.example.everythingbim.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;

import com.google.firebase.firestore.Exclude;

/**
 * Like Entity representing a single user's "like" on a target (post or
 * review).
 *
 * <p>Dual-purpose: the entity is both a Room row (offline cache) and a
 * Firestore document (cross-device sync). The no-arg constructor is
 * required by Firestore's {@code DocumentSnapshot.toObject()}.</p>
 *
 * <p>For posts, {@code targetId} is the Firestore document id of the post
 * (a String), and {@code postId} is the matching local Room long
 * ({@code stableLongFromString(targetId)}). The composite primary key
 * {@code (userId, targetId, targetType)} keeps the Room table unique.</p>
 */
@Entity(
        tableName = "likes",
        primaryKeys = {"userId", "targetId", "targetType"}
)
public class LikeEntity {

    @NonNull
    public String userId;

    @NonNull
    public String targetId; // Can be a postId or a reviewId (stored as String for the composite PK)

    @NonNull
    public String targetType; // "POST" or "REVIEW"

    /**
     * Local Room long for the target post. Mirrors {@code targetId} when
     * {@code targetType == "POST"}. Null for review likes or legacy rows.
     * Used for indexed lookups in {@code LikeDao}.
     */
    public Long postId;

    public long likedAt;

    /**
     * Firestore document id. Excluded from Room persistence; the local
     * composite primary key already serves the same uniqueness role.
     */
    @Exclude
    public String firestoreId;

    public LikeEntity(@NonNull String userId, @NonNull String targetId, @NonNull String targetType, long likedAt) {
        this.userId = userId;
        this.targetId = targetId;
        this.targetType = targetType;
        this.likedAt = likedAt;
    }

    /**
     * No-arg constructor required by Firestore for {@code DocumentSnapshot.toObject()}.
     */
    public LikeEntity() {
        this.userId = "";
        this.targetId = "";
        this.targetType = "POST";
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }

    @NonNull
    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(@NonNull String targetId) {
        this.targetId = targetId;
    }

    @NonNull
    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(@NonNull String targetType) {
        this.targetType = targetType;
    }

    public long getLikedAt() {
        return likedAt;
    }

    public void setLikedAt(long likedAt) {
        this.likedAt = likedAt;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getFirestoreId() {
        return firestoreId;
    }

    public void setFirestoreId(String firestoreId) {
        this.firestoreId = firestoreId;
    }
}

