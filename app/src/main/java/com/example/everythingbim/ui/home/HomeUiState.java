package com.example.everythingbim.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.everythingbim.data.models.SelectedImage;

public class HomeUiState {
    public enum Status {
        IDLE,
        PREVIEW_READY,
        ANALYZING,
        RESULT,
        UNKNOWN,
        ERROR
    }

    private final Status status;
    private final SelectedImage selectedImage;
    private final DemoLandmark landmark;
    private final String message;
    private final String detail;

    private HomeUiState(@NonNull Status status,
                        @Nullable SelectedImage selectedImage,
                        @Nullable DemoLandmark landmark,
                        @Nullable String message,
                        @Nullable String detail) {
        this.status = status;
        this.selectedImage = selectedImage;
        this.landmark = landmark;
        this.message = message;
        this.detail = detail;
    }

    @NonNull
    public static HomeUiState idle() {
        return new HomeUiState(Status.IDLE, null, null, null, null);
    }

    @NonNull
    public static HomeUiState preview(@NonNull SelectedImage selectedImage) {
        return new HomeUiState(Status.PREVIEW_READY, selectedImage, null, null, null);
    }

    @NonNull
    public static HomeUiState analyzing(@NonNull SelectedImage selectedImage, @NonNull String message) {
        return new HomeUiState(Status.ANALYZING, selectedImage, null, message, null);
    }

    @NonNull
    public static HomeUiState result(@NonNull SelectedImage selectedImage,
                                     @NonNull DemoLandmark landmark,
                                     @NonNull String detail) {
        return new HomeUiState(Status.RESULT, selectedImage, landmark, null, detail);
    }

    @NonNull
    public static HomeUiState unknown(@NonNull SelectedImage selectedImage, @NonNull String detail) {
        return new HomeUiState(Status.UNKNOWN, selectedImage, null, "No demo match found", detail);
    }

    @NonNull
    public static HomeUiState error(@Nullable SelectedImage selectedImage, @NonNull String message) {
        return new HomeUiState(Status.ERROR, selectedImage, null, message, null);
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    @Nullable
    public SelectedImage getSelectedImage() {
        return selectedImage;
    }

    @Nullable
    public DemoLandmark getLandmark() {
        return landmark;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @Nullable
    public String getDetail() {
        return detail;
    }
}
