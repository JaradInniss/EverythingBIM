package com.example.everythingbim.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "locations")
public class LocationEntity {

    @PrimaryKey(autoGenerate = true)
    public long locationId;

    public String locationFirestoreId;
    public String name;
    public double latitude;
    public double longitude;
    public float rating;
    public boolean isActive;
    public boolean isVerified;
    public String addedBy;
    public String sourceType;
    public String description;
    public String category;
    public String imageUrl;
    public String address;
    public String placeId;
    public long updatedAt;

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
        this.isActive = true;
        this.isVerified = isVerified;
        this.addedBy = addedBy;
        this.sourceType = addedBy;
        this.description = description;
        this.category = category;
        this.imageUrl = imageUrl;
        this.address = address;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getLocationId() {
        return locationId;
    }

    public void setLocationId(long locationId) {
        this.locationId = locationId;
    }

    public String getName() {
        return name;
    }

    public String getLocationFirestoreId() {
        return locationFirestoreId;
    }

    public void setLocationFirestoreId(String locationFirestoreId) {
        this.locationFirestoreId = locationFirestoreId;
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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
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

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
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

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
