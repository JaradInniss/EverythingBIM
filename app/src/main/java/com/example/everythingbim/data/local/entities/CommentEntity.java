package com.example.everythingbim.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "comments")
public class CommentEntity {
    @PrimaryKey(autoGenerate = true)
    public long commentId;

    public String body;
    public String authorId;
    public String targetId;   // The ID of the Post or Review being commented on
    public String targetType; // "POST" or "REVIEW"
    public long createdAt;

    public CommentEntity(String body, String authorId, String targetId, String targetType, long createdAt) {
        this.body = body;
        this.authorId = authorId;
        this.targetId = targetId;
        this.targetType = targetType;
        this.createdAt = createdAt;
    }
}