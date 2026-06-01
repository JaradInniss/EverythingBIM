package com.example.everythingbim.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;

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
    public long locationId;

    public String locationName;
    public long authorId;
    public String authorName;
    public String caption;
    public String imageUrl;
    public long createdAt;
    public List<Long> taggedUserIds;

    public PostEntity(long locationId, String locationName, long authorId, String authorName, String caption, String imageUrl, long createdAt, List<Long> taggedUserIds) {
        this.locationId = locationId;
        this.locationName = locationName;
        this.authorId = authorId;
        this.authorName = authorName;
        this.caption = caption;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.taggedUserIds = taggedUserIds;
    }

}
