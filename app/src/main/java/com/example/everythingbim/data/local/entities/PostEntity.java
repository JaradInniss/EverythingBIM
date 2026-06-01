package com.example.everythingbim.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;

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

    public long locationId;

    public long authorId;
    public String caption;
    public String imageUrl;
    public long createdAt;

    public PostEntity(long locationId, long authorId, String caption, String imageUrl, long createdAt) {
        this.locationId = locationId;
        this.authorId = authorId;
        this.caption = caption;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
    }
}
