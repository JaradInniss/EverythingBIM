package com.example.everythingbim.ui.home;

import androidx.annotation.NonNull;

public class DemoLandmark {
    private final String id;
    private final String token;
    private final String displayName;
    private final String description;

    public DemoLandmark(@NonNull String id,
                        @NonNull String token,
                        @NonNull String displayName,
                        @NonNull String description) {
        this.id = id;
        this.token = token;
        this.displayName = displayName;
        this.description = description;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getToken() {
        return token;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    @NonNull
    public String getDescription() {
        return description;
    }
}
