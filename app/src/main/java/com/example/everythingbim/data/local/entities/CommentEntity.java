package com.example.everythingbim.data.local.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Comment Entity representing a comment in the database.
 * Supports a hierarchical structure by referencing a parent comment ID.
 */
@Entity(
        tableName = "comments",
        foreignKeys = {
                // Link to the post this comment belongs to
                @ForeignKey(
                        entity = PostEntity.class,
                        parentColumns = "postId",
                        childColumns = "postId",
                        onDelete = ForeignKey.CASCADE
                ),
                // Link to a parent comment if this is a reply (self-referencing)
                @ForeignKey(
                        entity = CommentEntity.class,
                        parentColumns = "commentId",
                        childColumns = "parentCommentId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {@Index("postId"), @Index("parentCommentId")}
)
public class CommentEntity {
    @PrimaryKey(autoGenerate = true)
    public long commentId;

    public long postId;
    public Long parentCommentId; // ID of the comment being replied to. Null for top-level comments.
    public String authorName;
    public String parentAuthorName; // Name of the author of the parent comment (e.g., for "Re: @username").
    public String body;
    public long timestamp; // Creation timestamp of the comment.

    public CommentEntity(long postId, Long parentCommentId, String authorName, String parentAuthorName, String body, long timestamp) {
        this.postId = postId;
        this.parentCommentId = parentCommentId;
        this.authorName = authorName;
        this.parentAuthorName = parentAuthorName;
        this.body = body;
        this.timestamp = timestamp;
    }
}
