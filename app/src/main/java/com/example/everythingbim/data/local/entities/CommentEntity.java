package com.example.everythingbim.data.local.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.firebase.firestore.Exclude;

/**
 * Comment Entity representing a comment in the database.
 * Supports a hierarchical structure by referencing a parent comment ID.
 *
 * <p>Dual-purpose: the entity is both a Room row (for offline cache) and a
 * Firestore document (for cross-device sync). The no-arg constructor is
 * required by Firestore's {@code DocumentSnapshot.toObject()}. The
 * {@code firestoreId} field is {@code @Exclude}-annotated so it is not
 * persisted to Room and is only used when mapping from a Firestore document.
 * </p>
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
    @PrimaryKey
    public long commentId;

    public long postId;

    /**
     * ID of the comment being replied to. Null for top-level comments.
     */
    public Long parentCommentId;

    public String authorName;

    /**
     * Name of the author of the parent comment (e.g., for "Re: @username").
     */
    public String parentAuthorName;

    /**
     * Firebase Auth UID of the parent comment's author. Used to resolve
     * the correct parentAuthorName when the parent's name changes.
     */
    public String parentAuthorUid;

    public String body;

    /**
     * Firebase Auth UID of the comment's author. Null for legacy Room-only
     * comments. Used for Firestore queries and security-rule checks.
     */
    public String authorUid;

    /**
     * Profile picture URL of the comment's author. Loaded from Firestore
     * when the comment is displayed.
     */
    public String authorProfilePictureUrl;

    /**
     * Creation timestamp of the comment, in epoch milliseconds. Named
     * {@code createdAt} to match the Firestore field name; Room stores
     * this as a boxed {@code Long}.
     *
     * <p>This is intentionally boxed (not primitive {@code long}) so
     * Firestore's {@code BeanMapper} can call the setter with
     * {@code null} when the document doesn't have the field yet (e.g.
     * during the brief window where a freshly-written comment still
     * holds a server-timestamp sentinel). A primitive setter would
     * throw {@code IllegalArgumentException: argument ... has type long,
     * got null} and crash the app. Consumers should treat {@code null}
     * as "no timestamp yet" and fall back to
     * {@link System#currentTimeMillis()} for display purposes.</p>
     */
    public Long createdAt;

    /**
     * Firestore document id for this comment. Excluded from Room
     * persistence. The local {@code commentId} (long) is the primary key
     * there; we keep a stable hash of the firestoreId so re-snapshots
     * are idempotent.
     */
    @Exclude
    public String firestoreId;

    /**
     * No-arg constructor required by Firestore for {@code DocumentSnapshot.toObject()}.
     * Do not remove; Firestore uses reflection to instantiate the class.
     */
    public CommentEntity() {
    }

    public CommentEntity(long postId, Long parentCommentId, String authorName, String parentAuthorName, String body, Long createdAt) {
        this(postId, parentCommentId, authorName, null, parentAuthorName, null, body, createdAt);
    }

    public CommentEntity(long postId,
                         Long parentCommentId,
                         String authorName,
                         String authorUid,
                         String parentAuthorName,
                         String parentAuthorUid,
                         String body,
                         Long createdAt) {
        this.postId = postId;
        this.parentCommentId = parentCommentId;
        this.authorName = authorName;
        this.authorUid = authorUid;
        this.parentAuthorName = parentAuthorName;
        this.parentAuthorUid = parentAuthorUid;
        this.body = body;
        this.createdAt = createdAt;
    }

    public long getCommentId() {
        return commentId;
    }

    public void setCommentId(long commentId) {
        this.commentId = commentId;
    }

    public long getPostId() {
        return postId;
    }

    public void setPostId(long postId) {
        this.postId = postId;
    }

    public Long getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(Long parentCommentId) {
        this.parentCommentId = parentCommentId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getParentAuthorName() {
        return parentAuthorName;
    }

    public void setParentAuthorName(String parentAuthorName) {
        this.parentAuthorName = parentAuthorName;
    }

    public String getParentAuthorUid() {
        return parentAuthorUid;
    }

    public void setParentAuthorUid(String parentAuthorUid) {
        this.parentAuthorUid = parentAuthorUid;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    /**
     * @deprecated Use {@link #getCreatedAt()} instead. Retained as a
     * delegate so any remaining callers keep compiling during the
     * Firestore refactor.
     */
    @Deprecated
    public Long getTimestamp() {
        return createdAt;
    }

    /**
     * @deprecated Use {@link #setCreatedAt(Long)} instead.
     */
    @Deprecated
    public void setTimestamp(Long timestamp) {
        this.createdAt = timestamp;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public String getAuthorUid() {
        return authorUid;
    }

    public void setAuthorUid(String authorUid) {
        this.authorUid = authorUid;
    }

    public String getAuthorProfilePictureUrl() {
        return authorProfilePictureUrl;
    }

    public void setAuthorProfilePictureUrl(String authorProfilePictureUrl) {
        this.authorProfilePictureUrl = authorProfilePictureUrl;
    }

    public String getFirestoreId() {
        return firestoreId;
    }

    public void setFirestoreId(String firestoreId) {
        this.firestoreId = firestoreId;
    }
}

