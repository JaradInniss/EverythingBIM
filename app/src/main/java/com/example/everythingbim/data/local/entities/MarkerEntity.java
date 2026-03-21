package com.example.everythingbim.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "markers")
public class MarkerEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public double latitude;
    public double longitude;
    public String title;
    public String snippet;

    // Optional: you can add a constructor and getters/setters if you prefer encapsulation
    public MarkerEntity(double latitude, double longitude, String title, String snippet) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.title = title;
        this.snippet = snippet;
    }
}