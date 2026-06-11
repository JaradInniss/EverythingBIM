package com.example.everythingbim.data.local.entities;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.firebase.firestore.Exclude;

import java.util.List;

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

    @Exclude
    public String locationFirestoreId;

    @Exclude
    public String firestoreId;

    public String authorUid;

    public long locationId;
    public String locationName;

    public long authorId;
    public String authorName;
    public String caption;
    public String imageUrl;
    public long createdAt;

    public List<String> taggedUserUids;
    public Integer likeCount;
    public Integer commentCount;

    public PostEntity() {
    }

    public PostEntity(long locationId,
                      long authorId,
                      String authorName,
                      String caption,
                      String imageUrl,
                      long createdAt) {
        this(locationId, null, authorId, authorName, caption, imageUrl, createdAt, null);
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
