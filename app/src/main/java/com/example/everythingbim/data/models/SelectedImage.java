package com.example.everythingbim.data.models;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SelectedImage {
    public static final String SOURCE_CAMERA = "Camera";
    public static final String SOURCE_GALLERY = "Gallery";

    private final Uri uri;
    private final String source;
    private final String displayName;

    public SelectedImage(@NonNull Uri uri, @NonNull String source, @Nullable String displayName) {
        this.uri = uri;
        this.source = source;
        this.displayName = displayName;
    }

    @NonNull
    public Uri getUri() {
        return uri;
    }

    @NonNull
    public String getSource() {
        return source;
    }

    @Nullable
    public String getDisplayName() {
        return displayName;
    }
}
