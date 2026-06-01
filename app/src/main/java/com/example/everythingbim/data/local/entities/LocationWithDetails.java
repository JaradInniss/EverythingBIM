package com.example.everythingbim.data.local.entities;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

/**
 * Plain Old Java Object to represent a Location along with its associated Posts and Reviews.
 * This is used by Room to perform an automated "join" query.
 */
public class LocationWithDetails {

    @Embedded
    public LocationEntity location;

    // Fetches all posts where post.locationId matches location.locationId
    @Relation(
            parentColumn = "locationId",
            entityColumn = "locationId"
    )
    public List<PostEntity> posts;

    // Fetches all reviews where review.locationId matches location.locationId
    @Relation(
            parentColumn = "locationId",
            entityColumn = "locationId"
    )
    public List<ReviewEntity> reviews;
}