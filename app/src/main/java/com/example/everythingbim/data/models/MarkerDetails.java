package com.example.everythingbim.data.models;

import com.example.everythingbim.data.local.entities.PostEntity;
import com.example.everythingbim.data.local.entities.ReviewEntity;

import java.util.List;

public class MarkerDetails {

    public final String title;
    public final String subtitle;
    public final String meta;
    public final String contact;
    public final float rating;
    public final String overview;
    public String placeType;

    // Lists
    public final List<String> imageUrls;
    public final List<ReviewEntity> reviews;
    public final List<PostEntity> posts;

    public MarkerDetails(String title, String subtitle, String meta, String contact,
                         float rating, String overview, String placeType,
                         List<String> imageUrls, List<ReviewEntity> reviews, List<PostEntity> posts) {
        this.title = title;
        this.subtitle = subtitle;
        this.meta = meta;
        this.contact = contact;
        this.rating = rating;
        this.overview = overview;
        this.placeType = placeType;
        this.imageUrls = imageUrls;
        this.reviews = reviews;
        this.posts = posts;
    }
}
