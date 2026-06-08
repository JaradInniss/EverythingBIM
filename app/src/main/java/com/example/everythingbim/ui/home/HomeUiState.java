package com.example.everythingbim.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.everythingbim.data.models.SelectedImage;

import java.util.Collections;
import java.util.List;

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
    private final Landmark landmark;
    private final String confidenceText;
    private final String message;
    private final String detail;
    private final boolean gpsAvailable;
    private final boolean usedGps;
    private final boolean gpsSupportedResult;
    private final boolean imageOnlyResult;
    private final Double userDistanceToLandmarkMeters;
    private final List<NearbySavedLocation> nearbyLocations;
    private final Integer nearbyRadiusMeters;
    private final Double anchorLatitude;
    private final Double anchorLongitude;

    private HomeUiState(@NonNull Status status,
                        @Nullable SelectedImage selectedImage,
                        @Nullable Landmark landmark,
                        @Nullable String confidenceText,
                        @Nullable String message,
                        @Nullable String detail,
                        boolean gpsAvailable,
                        boolean usedGps,
                        boolean gpsSupportedResult,
                        boolean imageOnlyResult,
                        @Nullable Double userDistanceToLandmarkMeters,
                        @NonNull List<NearbySavedLocation> nearbyLocations,
                        @Nullable Integer nearbyRadiusMeters,
                        @Nullable Double anchorLatitude,
                        @Nullable Double anchorLongitude) {
        this.status = status;
        this.selectedImage = selectedImage;
        this.landmark = landmark;
        this.confidenceText = confidenceText;
        this.message = message;
        this.detail = detail;
        this.gpsAvailable = gpsAvailable;
        this.usedGps = usedGps;
        this.gpsSupportedResult = gpsSupportedResult;
        this.imageOnlyResult = imageOnlyResult;
        this.userDistanceToLandmarkMeters = userDistanceToLandmarkMeters;
        this.nearbyLocations = Collections.unmodifiableList(nearbyLocations);
        this.nearbyRadiusMeters = nearbyRadiusMeters;
        this.anchorLatitude = anchorLatitude;
        this.anchorLongitude = anchorLongitude;
    }

    @NonNull
    public static HomeUiState idle() {
        return new HomeUiState(Status.IDLE, null, null, null, null, null, false, false, false, false, null, Collections.emptyList(), null, null, null);
    }

    @NonNull
    public static HomeUiState preview(@NonNull SelectedImage selectedImage) {
        return new HomeUiState(Status.PREVIEW_READY, selectedImage, null, null, null, null, false, false, false, false, null, Collections.emptyList(), null, null, null);
    }

    @NonNull
    public static HomeUiState analyzing(@NonNull SelectedImage selectedImage, @NonNull String message) {
        return new HomeUiState(Status.ANALYZING, selectedImage, null, null, message, null, false, false, false, false, null, Collections.emptyList(), null, null, null);
    }

    @NonNull
    public static HomeUiState result(@NonNull SelectedImage selectedImage,
                                     @NonNull Landmark landmark,
                                     @NonNull String confidenceText,
                                     @NonNull String detail,
                                     boolean gpsAvailable,
                                     boolean usedGps,
                                     boolean gpsSupportedResult,
                                     boolean imageOnlyResult,
                                     @Nullable Double userDistanceToLandmarkMeters,
                                     @NonNull List<NearbySavedLocation> nearbyLocations,
                                     int nearbyRadiusMeters) {
        return new HomeUiState(
                Status.RESULT,
                selectedImage,
                landmark,
                confidenceText,
                null,
                detail,
                gpsAvailable,
                usedGps,
                gpsSupportedResult,
                imageOnlyResult,
                userDistanceToLandmarkMeters,
                nearbyLocations,
                nearbyRadiusMeters,
                landmark.getLatitude(),
                landmark.getLongitude()
        );
    }

    @NonNull
    public static HomeUiState unknown(@NonNull SelectedImage selectedImage, @NonNull String detail) {
        return new HomeUiState(Status.UNKNOWN, selectedImage, null, null, "No landmark match found", detail, false, false, false, false, null, Collections.emptyList(), null, null, null);
    }

    @NonNull
    public static HomeUiState uncertain(@NonNull SelectedImage selectedImage,
                                        @NonNull String confidenceText,
                                        @NonNull String message,
                                        @NonNull String detail,
                                        boolean gpsAvailable,
                                        boolean usedGps,
                                        @Nullable Double userDistanceToLandmarkMeters) {
        return new HomeUiState(Status.UNCERTAIN, selectedImage, null, confidenceText, message, detail, gpsAvailable, usedGps, false, false, userDistanceToLandmarkMeters, Collections.emptyList(), null, null, null);
    }

    @NonNull
    public static HomeUiState error(@Nullable SelectedImage selectedImage, @NonNull String message) {
        return new HomeUiState(Status.ERROR, selectedImage, null, null, message, null, false, false, false, false, null, Collections.emptyList(), null, null, null);
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
    public Landmark getLandmark() {
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

    public boolean isGpsAvailable() {
        return gpsAvailable;
    }

    public boolean isUsedGps() {
        return usedGps;
    }

    public boolean isGpsSupportedResult() {
        return gpsSupportedResult;
    }

    public boolean isImageOnlyResult() {
        return imageOnlyResult;
    }

    @Nullable
    public Double getUserDistanceToLandmarkMeters() {
        return userDistanceToLandmarkMeters;
    }

    @NonNull
    public List<NearbySavedLocation> getNearbyLocations() {
        return nearbyLocations;
    }

    @Nullable
    public Integer getNearbyRadiusMeters() {
        return nearbyRadiusMeters;
    }

    @Nullable
    public Double getAnchorLatitude() {
        return anchorLatitude;
    }

    @Nullable
    public Double getAnchorLongitude() {
        return anchorLongitude;
    }
}
