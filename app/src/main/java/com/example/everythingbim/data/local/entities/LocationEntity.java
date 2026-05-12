package com.example.everythingbim.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "locations")
public class LocationEntity {

    @PrimaryKey(autoGenerate = true)
    public long locationId;

    public String name;
    public double latitude;
    public double longitude;
    public float rating;
    public boolean isVerified;
    public String addedBy;
    public String description;
    public String category;
    public String imageUrl;
    public String address;

    public LocationEntity(String name,
                          double latitude,
                          double longitude,
                          float rating,
                          boolean isVerified,
                          String addedBy,
                          String description,
                          String category,
                          String imageUrl,
                          String address) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.rating = rating;
        this.isVerified = isVerified;
        this.addedBy = addedBy;
        this.description = description;
        this.category = category;
        this.imageUrl = imageUrl;
        this.address = address;
    }
}
