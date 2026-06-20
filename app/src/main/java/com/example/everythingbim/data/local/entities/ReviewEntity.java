package com.example.everythingbim.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
        tableName = "reviews",
        foreignKeys = @ForeignKey(
                entity = LocationEntity.class,
                parentColumns = "locationId",
                childColumns = "locationId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index("locationId"),
                @Index(value = {"locationId", "authorUid"}, unique = true)
        }
)
public class ReviewEntity {

    @PrimaryKey(autoGenerate = true)
    public long reviewId;

    public long locationId;

    public long authorId;
    public String authorUid;
    public String authorName;
    public String body;
    public float rating;
    public long createdAt;

    public ReviewEntity(long locationId,
                        long authorId,
                        String authorUid,
                        String authorName,
                        String body,
                        float rating,
                        long createdAt) {
        this.locationId = locationId;
        this.authorId = authorId;
        this.authorUid = authorUid;
        this.authorName = authorName;
        this.body = body;
        this.rating = rating;
        this.createdAt = createdAt;
    }
}
