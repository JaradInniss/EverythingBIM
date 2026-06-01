package com.example.everythingbim.data.local.entities;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;

@Entity(
        tableName = "general_users",
        foreignKeys = @ForeignKey(
                entity = UserEntity.class,
                parentColumns = "userId",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("userId")
)
public class GeneralUserEntity {
    @PrimaryKey(autoGenerate = true)
    public long userId;

    public String profilePictureUrl;
    public String bio;

    public GeneralUserEntity(String profilePictureUrl, String bio) {
        this.profilePictureUrl = profilePictureUrl;
        this.bio = bio;
    }
}
