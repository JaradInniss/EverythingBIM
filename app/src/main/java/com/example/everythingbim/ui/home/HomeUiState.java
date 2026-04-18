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
        UNCERTAIN,
        UNKNOWN,
        ERROR
    }

    private final Status status;
    private final SelectedImage selectedImage;
    private final DemoLandmark landmark;
    private final String confidenceText;
    private final String message;
    private final String detail;

    private HomeUiState(@NonNull Status status,
                        @Nullable SelectedImage selectedImage,
                        @Nullable DemoLandmark landmark,
                        @Nullable String confidenceText,
                        @Nullable String message,
                        @Nullable String detail) {
        this.status = status;
        this.selectedImage = selectedImage;
        this.landmark = landmark;
        this.confidenceText = confidenceText;
        this.message = message;
        this.detail = detail;
    }

    @NonNull
    public static HomeUiState idle() {
        return new HomeUiState(Status.IDLE, null, null, null, null, null);
    }

    @NonNull
    public static HomeUiState preview(@NonNull SelectedImage selectedImage) {
        return new HomeUiState(Status.PREVIEW_READY, selectedImage, null, null, null, null);
    }

    @NonNull
    public static HomeUiState analyzing(@NonNull SelectedImage selectedImage, @NonNull String message) {
        return new HomeUiState(Status.ANALYZING, selectedImage, null, null, message, null);
    }

    @NonNull
    public static HomeUiState result(@NonNull SelectedImage selectedImage,
                                     @NonNull DemoLandmark landmark,
                                     @NonNull String confidenceText,
                                     @NonNull String detail) {
        return new HomeUiState(Status.RESULT, selectedImage, landmark, confidenceText, null, detail);
    }

    @NonNull
    public static HomeUiState unknown(@NonNull SelectedImage selectedImage, @NonNull String detail) {
        return new HomeUiState(Status.UNKNOWN, selectedImage, null, null, "No landmark match found", detail);
    }

    @NonNull
    public static HomeUiState uncertain(@NonNull SelectedImage selectedImage,
                                        @NonNull String confidenceText,
                                        @NonNull String message,
                                        @NonNull String detail) {
        return new HomeUiState(Status.UNCERTAIN, selectedImage, null, confidenceText, message, detail);
    }

    @NonNull
    public static HomeUiState error(@Nullable SelectedImage selectedImage, @NonNull String message) {
        return new HomeUiState(Status.ERROR, selectedImage, null, null, message, null);
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
    public String getConfidenceText() {
        return confidenceText;
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
