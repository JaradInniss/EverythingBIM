package com.example.everythingbim.data.local.entities;

import androidx.room.Embedded;
import androidx.room.Relation;

// UserWithProfile Entity

public class UserWithProfile {

    @Embedded
    public UserEntity user;

    // Fetches profile where user.userId matches generalUser.userId
    @Relation(
            parentColumn = "userId",
            entityColumn = "userId"
    )
    public GeneralUserEntity generalUser;

    // Fetches profile where user.userId matches businessUser.userId
    @Relation(
            parentColumn = "userId",
            entityColumn = "userId"
    )
    public BusinessUserEntity businessUser;

}
