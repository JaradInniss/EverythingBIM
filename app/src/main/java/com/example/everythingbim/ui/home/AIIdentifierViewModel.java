package com.example.everythingbim.ui.home;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.everythingbim.data.local.AppDatabase;
import com.example.everythingbim.data.models.SelectedImage;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AIIdentifierViewModel extends AndroidViewModel {
    private static final float PARLIAMENT_CONFIRMATION_THRESHOLD = 0.80f;
    private static final float PARLIAMENT_UNCERTAIN_THRESHOLD = 0.45f;
    private static final float GPS_SUPPORT_DISTANCE_METERS = 3000f;

    private final MutableLiveData<HomeUiState> uiState = new MutableLiveData<>(HomeUiState.idle());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final DemoLandmarkRepository landmarkRepository = new DemoLandmarkRepository();
    private final NearbySavedLocationsRepository nearbySavedLocationsRepository;

    private ParliamentClassifier classifier;
    private SelectedImage selectedImage;
    private boolean gpsAvailable;
    private boolean locationPermissionGranted;
    private Double userLatitude;
    private Double userLongitude;

    public AIIdentifierViewModel(@NonNull Application application) {
        super(application);
        nearbySavedLocationsRepository = new NearbySavedLocationsRepository(
                AppDatabase.getInstance(application).locationDao()
        );
    }

    @NonNull
    public LiveData<HomeUiState> getUiState() {
        return uiState;
    }

    public void initialize(@NonNull SelectedImage image,
                           boolean gpsAvailable,
                           boolean locationPermissionGranted,
                           @Nullable Double userLatitude,
                           @Nullable Double userLongitude) {
        if (this.selectedImage != null) return;

        this.selectedImage = image;
        this.gpsAvailable = gpsAvailable;
        this.locationPermissionGranted = locationPermissionGranted;
        this.userLatitude = userLatitude;
        this.userLongitude = userLongitude;
        uiState.setValue(HomeUiState.preview(image));
        startAnalysis();
    }

    public void rerunAnalysis() {
        if (selectedImage == null) {
            return;
        }
        startAnalysis();
    }

    private void startAnalysis() {
        if (selectedImage == null) {
            return;
        }

        uiState.setValue(HomeUiState.analyzing(
                selectedImage,
                "Analyzing image offline..."
        ));

        executorService.execute(() -> {
            try {
                Bitmap bitmap = readBitmap(selectedImage.getUri());
                ParliamentClassifier.Result prediction = getClassifier().predict(bitmap);
                handlePrediction(prediction);
            } catch (Exception exception) {
                uiState.postValue(HomeUiState.error(
                        selectedImage,
                        exception.getMessage() != null ? exception.getMessage() : "Offline identification failed."
                ));
            }
        });
    }

    private void handlePrediction(@NonNull ParliamentClassifier.Result prediction) {
        if (selectedImage == null) {
            return;
        }

        DemoLandmark landmark = landmarkRepository.findByIdOrToken("parliament");
        if (landmark == null) {
            uiState.postValue(HomeUiState.error(selectedImage, "Parliament metadata is unavailable."));
            return;
        }

        float parliamentProbability = prediction.getParliamentProbability();
        double userDistanceToParliament = computeDistanceToLandmark(landmark);
        boolean canUseGps = gpsAvailable && userLatitude != null && userLongitude != null;
        boolean isGalleryImage = SelectedImage.SOURCE_GALLERY.equalsIgnoreCase(selectedImage.getSource());

        if (ParliamentClassifier.LABEL_PARLIAMENT.equalsIgnoreCase(prediction.getLabel())
                || parliamentProbability >= PARLIAMENT_CONFIRMATION_THRESHOLD) {
            if (canUseGps && !isGalleryImage && userDistanceToParliament > GPS_SUPPORT_DISTANCE_METERS) {
                uiState.postValue(HomeUiState.uncertain(
                        selectedImage,
                        formatConfidence(parliamentProbability),
                        "Possible Parliament match",
                        "The image looks like Parliament, but your current GPS is far from Bridgetown. Because this was a camera image, the app is treating it as uncertain instead of confirmed."
                                + "\nCurrent distance from Parliament: " + formatDistance((float) userDistanceToParliament),
                        true,
                        true,
                        userDistanceToParliament
                ));
                return;
            }

            NearbySavedLocationsResult nearbyResult = nearbySavedLocationsRepository.getNearbySavedLocations(
                    landmark,
                    canUseGps ? userLatitude : null,
                    canUseGps ? userLongitude : null
            );

            String detail = buildConfirmedDetail(
                    landmark,
                    canUseGps,
                    isGalleryImage,
                    userDistanceToParliament,
                    nearbyResult.getNearbyLocations().size()
            );

            uiState.postValue(HomeUiState.result(
                    selectedImage,
                    landmark,
                    formatConfidence(parliamentProbability),
                    detail,
                    gpsAvailable,
                    canUseGps && !isGalleryImage,
                    canUseGps && userDistanceToParliament <= GPS_SUPPORT_DISTANCE_METERS,
                    !canUseGps || isGalleryImage,
                    canUseGps ? userDistanceToParliament : null,
                    nearbyResult.getNearbyLocations()
            ));
            return;
        }

        if (ParliamentClassifier.LABEL_UNCERTAIN.equalsIgnoreCase(prediction.getLabel())
                || parliamentProbability >= PARLIAMENT_UNCERTAIN_THRESHOLD) {
            uiState.postValue(HomeUiState.uncertain(
                    selectedImage,
                    formatConfidence(parliamentProbability),
                    "Possible Parliament match",
                    "The model sees some Parliament-like features, but this image is still too close to call confidently."
                            + "\nTry a clearer, front-facing photo with better lighting."
                            + buildUncertainGpsHint(canUseGps, isGalleryImage, userDistanceToParliament),
                    gpsAvailable,
                    canUseGps && !isGalleryImage,
                    canUseGps ? userDistanceToParliament : null
            ));
            return;
        }

        uiState.postValue(HomeUiState.unknown(
                selectedImage,
                "This image was not identified as the Barbados Parliament Buildings."
        ));
    }

    private double computeDistanceToLandmark(@NonNull DemoLandmark landmark) {
        if (userLatitude == null || userLongitude == null) {
            return Double.NaN;
        }

        float[] result = new float[1];
        Location.distanceBetween(
                userLatitude,
                userLongitude,
                landmark.getLatitude(),
                landmark.getLongitude(),
                result
        );
        return result[0];
    }

    @NonNull
    private String buildConfirmedDetail(@NonNull DemoLandmark landmark,
                                        boolean canUseGps,
                                        boolean isGalleryImage,
                                        double userDistanceToParliament,
                                        int nearbyCount) {
        StringBuilder detail = new StringBuilder(landmark.getDescription());

        if (!canUseGps) {
            detail.append("\nConfirmed by image only because current GPS was unavailable.");
        } else if (isGalleryImage) {
            detail.append("\nConfirmed by image. Current GPS was ignored because this is a gallery photo.");
        } else if (userDistanceToParliament <= GPS_SUPPORT_DISTANCE_METERS) {
            detail.append("\nLocation context available. You appear to be ")
                    .append(formatDistance((float) userDistanceToParliament))
                    .append(" from Parliament.");
        }

        if (nearbyCount > 0) {
            detail.append("\nShowing ").append(nearbyCount).append(" nearby saved locations within 1.0 km.");
        } else {
            detail.append("\nNo saved locations were found within 1.0 km yet.");
        }
        return detail.toString();
    }

    @NonNull
    private String buildUncertainGpsHint(boolean canUseGps, boolean isGalleryImage, double userDistanceToParliament) {
        if (!canUseGps) {
            return "\nGPS was unavailable, so this result is based on the image only.";
        }
        if (isGalleryImage) {
            return "\nCurrent GPS was ignored because gallery images may have been taken earlier in a different place.";
        }
        return "\nCurrent GPS is about " + formatDistance((float) userDistanceToParliament) + " from Parliament.";
    }

    @NonNull
    private Bitmap readBitmap(@NonNull Uri imageUri) throws IOException {
        try (InputStream inputStream = getApplication().getContentResolver().openInputStream(imageUri)) {
            if (inputStream == null) {
                throw new IOException("Unable to open the selected image.");
            }
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                throw new IOException("Unable to decode the selected image.");
            }
            return bitmap;
        }
    }

    @NonNull
    private ParliamentClassifier getClassifier() throws IOException {
        if (classifier == null) {
            classifier = new ParliamentClassifier(getApplication());
        }
        return classifier;
    }

    @NonNull
    private String formatConfidence(float probability) {
        return String.format(java.util.Locale.US, "%.1f%%", probability * 100f);
    }

    @NonNull
    private String formatDistance(float meters) {
        if (meters >= 1000f) {
            return String.format(java.util.Locale.US, "%.1f km", meters / 1000f);
        }
        return String.format(java.util.Locale.US, "%.0f m", meters);
    }

    @Override
    protected void onCleared() {
        executorService.shutdownNow();
        if (classifier != null) {
            classifier.close();
        }
        super.onCleared();
    }
}
