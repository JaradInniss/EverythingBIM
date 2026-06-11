package com.example.everythingbim.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;

import com.google.firebase.firestore.Exclude;

@Entity(
        tableName = "likes",
        primaryKeys = {"userId", "targetId", "targetType"}
)
public class LikeEntity {

    @NonNull
    public String userId;
    @NonNull
    public String targetId; // Can be a postId or a reviewId
    @NonNull
    public String targetType; // "POST" or "REVIEW"

    public Long postId;

    public long likedAt;

    @Exclude
    public String firestoreId;

    public LikeEntity(@NonNull String userId, @NonNull String targetId, @NonNull String targetType, long likedAt) {
        this.userId = userId;
        this.targetId = targetId;
        this.targetType = targetType;
        this.likedAt = likedAt;
    }

    public LikeEntity() {
        this.userId = "";
        this.targetId = "";
        this.targetType = "POST";
    }
}
