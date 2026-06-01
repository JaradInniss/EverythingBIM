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

    // Getters and setters for all fields

    public long getLocationId() {
        return locationId;
    }

    public void setLocationId(long locationId) {
        this.locationId = locationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
