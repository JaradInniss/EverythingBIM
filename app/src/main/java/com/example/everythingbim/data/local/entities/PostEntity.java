package com.example.everythingbim.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;

import com.google.firebase.firestore.Exclude;

import java.util.List;

/**
 * PostEntity represents a single post in the app.
 *
 * This entity serves dual purposes:
 *  - As a Room database entity for local cache/offline access.
 *  - As a Firestore document model for remote storage.
 *
 * The no-arg constructor is required by Firestore's {@code DocumentSnapshot.toObject()}.
 * The {@code firestoreId} field is annotated with {@code @Exclude} so it is not
 * serialized to Room (the local {@code postId} is the primary key there) and is
 * only used when mapping from a Firestore document.
 */
@Entity(
        tableName = "posts",
        foreignKeys = @ForeignKey(
                entity = LocationEntity.class,
                parentColumns = "locationId",
                childColumns = "locationId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("locationId")
)
public class PostEntity {

    @PrimaryKey(autoGenerate = true)
    public long postId;

    /**
     * Firestore document id for the location (if applicable). Excluded from Room persistence.
     */
    @Exclude
    public String locationFirestoreId;

    /**
     * Firestore document id for this post. Used for Firestore operations.
     */
    public String firestoreId;

    /**
     * Firebase Auth UID of the post's author. Used for Firestore queries
     * (e.g., fetching all posts by a user). Null for legacy Room-only posts.
     */
    public String authorUid;

    public long locationId;
    public String locationName;
    public long authorId;
    public String authorName;
    public String caption;
    public String imageUrl;
    public long createdAt;

    /**
     * Firebase UIDs of users tagged in this post. Stored as a list of strings
     * (Firestore-compatible). {@code null} is allowed for posts without tags.
     */
    public List<String> taggedUserUids;

    /**
     * Denormalized like count for the post. Maintained in Firestore via
     * {@code FieldValue.increment(+1/-1)} inside a transaction on every
     * like/unlike; the post document's {@code likeCount} field is the
     * single source of truth for the heart count in the UI.
     *
     * <p>{@code Integer} (boxed) is nullable so that older Firestore
     * documents without the field don't crash Room when mapped back.</p>
     */
    public Integer likeCount;

    /**
     * Denormalized comment count for the post. Currently computed
     * client-side from the comments subcollection snapshot size; left as
     * a field on the entity for forward-compat (we can switch to a
     * server-side counter later without changing consumers).
     */
    public Integer commentCount;

    /**
     * No-arg constructor required by Firestore for {@code DocumentSnapshot.toObject()}.
     * Do not remove; Firestore uses reflection to instantiate the class.
     */
    public PostEntity() {
    }

    public PostEntity(long locationId,
                      String locationName,
                      long authorId,
                      String authorName,
                      String caption,
                      String imageUrl,
                      long createdAt,
                      List<String> taggedUserUids) {
        this.locationId = locationId;
        this.locationName = locationName;
        this.authorId = authorId;
        this.authorName = authorName;
        this.caption = caption;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.taggedUserUids = taggedUserUids;
    }

    public PostEntity(long locationId,
                      String locationName,
                      long authorId,
                      String authorName,
                      String authorUid,
                      String caption,
                      String imageUrl,
                      long createdAt,
                      List<String> taggedUserUids) {
        this.locationId = locationId;
        this.locationName = locationName;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorUid = authorUid;
        this.caption = caption;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.taggedUserUids = taggedUserUids;
    }
}
