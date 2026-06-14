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
    private static final float PARLIAMENT_UNCERTAIN_THRESHOLD = 0.45f;
    private static final float GPS_SUPPORT_DISTANCE_METERS = 3000f;

    private final MutableLiveData<HomeUiState> uiState = new MutableLiveData<>(HomeUiState.idle());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final LandmarkRepository landmarkRepository = new LandmarkRepository();
    private final NearbySavedLocationsRepository nearbySavedLocationsRepository;

    private LandmarkClassifier classifier;
    private SelectedImage selectedImage;
    private boolean gpsAvailable;
    private boolean locationPermissionGranted;
    private Double userLatitude;
    private Double userLongitude;
    private Integer selectedNearbyRadiusMeters;
    private Landmark currentConfirmedLandmark;
    private Float currentConfirmedProbability;
    private Boolean currentConfirmedGpsContextAvailable;
    private Boolean currentConfirmedImageIsGallery;
    private Double currentConfirmedUserDistanceMeters;

    public AIIdentifierViewModel(@NonNull Application application) {
        super(application);
        nearbySavedLocationsRepository = new NearbySavedLocationsRepository(
                AppDatabase.getInstance(application).locationDao(),
                AppDatabase.getInstance(application).postDao()
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
                LandmarkClassifier.Result prediction = getClassifier().predict(bitmap);
                handlePrediction(prediction);
            } catch (Exception exception) {
                uiState.postValue(HomeUiState.error(
                        selectedImage,
                        exception.getMessage() != null ? exception.getMessage() : "Offline identification failed."
                ));
            }
        });
    }

    private void handlePrediction(@NonNull LandmarkClassifier.Result prediction) {
        if (selectedImage == null) {
            return;
        }

        float parliamentProbability = prediction.getParliamentProbability();
        float kensingtonProbability = prediction.getKensingtonProbability();
        float topProbability = prediction.getTopProbability();
        boolean canUseGps = gpsAvailable && userLatitude != null && userLongitude != null;
        boolean isGalleryImage = SelectedImage.SOURCE_GALLERY.equalsIgnoreCase(selectedImage.getSource());
        Landmark predictedLandmark = resolveLandmark(prediction);

        if (predictedLandmark != null && isConfirmedPrediction(prediction)) {
            double userDistanceToLandmark = computeDistanceToLandmark(predictedLandmark);

            if (isParliament(predictedLandmark)
                    && canUseGps
                    && !isGalleryImage
                    && userDistanceToLandmark > GPS_SUPPORT_DISTANCE_METERS) {
                uiState.postValue(HomeUiState.uncertain(
                        selectedImage,
                        formatConfidence(parliamentProbability),
                        "Possible Parliament match",
                        "The image looks like Parliament, but your current GPS is far from Bridgetown. Because this was a camera image, the app is treating it as uncertain instead of confirmed."
                                + "\nCurrent distance from Parliament: " + formatDistance((float) userDistanceToLandmark),
                        true,
                        true,
                        userDistanceToLandmark
                ));
                return;
            }

            NearbySavedLocationsResult nearbyResult = nearbySavedLocationsRepository.getNearbySavedLocations(
                    predictedLandmark,
                    canUseGps ? userLatitude : null,
                    canUseGps ? userLongitude : null,
                    selectedNearbyRadiusMeters
            );

            currentConfirmedLandmark = predictedLandmark;
            currentConfirmedProbability = getDisplayProbability(prediction, predictedLandmark);
            currentConfirmedGpsContextAvailable = canUseGps;
            currentConfirmedImageIsGallery = isGalleryImage;
            currentConfirmedUserDistanceMeters = canUseGps ? userDistanceToLandmark : null;
            postConfirmedResult(nearbyResult);
            return;
        }

        if (predictedLandmark != null
                && (LandmarkClassifier.LABEL_UNCERTAIN.equalsIgnoreCase(prediction.getLabel())
                || topProbability >= PARLIAMENT_UNCERTAIN_THRESHOLD
                || parliamentProbability >= PARLIAMENT_UNCERTAIN_THRESHOLD
                || kensingtonProbability >= PARLIAMENT_UNCERTAIN_THRESHOLD)) {
            double userDistanceToLandmark = computeDistanceToLandmark(predictedLandmark);
            String possibleName = predictedLandmark.getDisplayName();
            uiState.postValue(HomeUiState.uncertain(
                    selectedImage,
                    formatConfidence(getDisplayProbability(prediction, predictedLandmark)),
                    "Possible " + possibleName + " match",
                    "The model sees some " + possibleName + "-like features, but this image is still too close to call confidently."
                            + "\nTry a clearer, front-facing photo with better lighting."
                            + buildUncertainGpsHint(predictedLandmark, canUseGps, isGalleryImage, userDistanceToLandmark),
                    gpsAvailable,
                    canUseGps && !isGalleryImage,
                    canUseGps ? userDistanceToLandmark : null
            ));
            return;
        }

        uiState.postValue(HomeUiState.unknown(
                selectedImage,
                "This image was not identified as the Barbados Parliament Buildings."
        ));
    }

    public void updateNearbyRadius(int radiusMeters) {
        selectedNearbyRadiusMeters = radiusMeters > 0 ? radiusMeters : null;
        if (currentConfirmedLandmark == null || selectedImage == null) {
            return;
        }

        executorService.execute(() -> {
            boolean canUseGps = Boolean.TRUE.equals(currentConfirmedGpsContextAvailable);
            NearbySavedLocationsResult nearbyResult = nearbySavedLocationsRepository.getNearbySavedLocations(
                    currentConfirmedLandmark,
                    canUseGps ? userLatitude : null,
                    canUseGps ? userLongitude : null,
                    selectedNearbyRadiusMeters
            );
            postConfirmedResult(nearbyResult);
        });
    }

    private void postConfirmedResult(@NonNull NearbySavedLocationsResult nearbyResult) {
        if (currentConfirmedLandmark == null || selectedImage == null) {
            return;
        }

        boolean canUseGps = Boolean.TRUE.equals(currentConfirmedGpsContextAvailable);
        boolean isGalleryImage = Boolean.TRUE.equals(currentConfirmedImageIsGallery);
        double userDistanceToLandmark = currentConfirmedUserDistanceMeters != null
                ? currentConfirmedUserDistanceMeters
                : Double.NaN;

        String detail = buildConfirmedDetail(
                currentConfirmedLandmark,
                canUseGps,
                isGalleryImage,
                userDistanceToLandmark,
                nearbyResult.getNearbyLocations().size(),
                nearbyResult.getSearchRadiusMeters()
        );

        uiState.postValue(HomeUiState.result(
                selectedImage,
                currentConfirmedLandmark,
                formatConfidence(currentConfirmedProbability != null ? currentConfirmedProbability : 0f),
                detail,
                gpsAvailable,
                canUseGps && !isGalleryImage,
                !isParliament(currentConfirmedLandmark)
                        || (canUseGps && userDistanceToLandmark <= GPS_SUPPORT_DISTANCE_METERS),
                !canUseGps || isGalleryImage,
                currentConfirmedUserDistanceMeters,
                nearbyResult.getNearbyLocations(),
                nearbyResult.getSearchRadiusMeters()
        ));
    }

    private double computeDistanceToLandmark(@NonNull Landmark landmark) {
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
    private String buildConfirmedDetail(@NonNull Landmark landmark,
                                        boolean canUseGps,
                                        boolean isGalleryImage,
                                        double userDistanceToLandmark,
                                        int nearbyCount,
                                        int nearbyRadiusMeters) {
        StringBuilder detail = new StringBuilder(landmark.getDescription());
        String shortName = landmark.getDisplayName();

        if (!canUseGps) {
            detail.append("\nConfirmed by image only because current GPS was unavailable.");
        } else if (isGalleryImage) {
            detail.append("\nConfirmed by image. Current GPS was ignored because this is a gallery photo.");
        } else if (!isParliament(landmark) || userDistanceToLandmark <= GPS_SUPPORT_DISTANCE_METERS) {
            detail.append("\nLocation context available. You appear to be ")
                    .append(formatDistance((float) userDistanceToLandmark))
                    .append(" from ")
                    .append(shortName)
                    .append(".");
        } else {
            detail.append("\nImage match is strong, but current GPS is ")
                    .append(formatDistance((float) userDistanceToLandmark))
                    .append(" from ")
                    .append(shortName)
                    .append(".");
        }

        if (nearbyCount > 0) {
            detail.append("\nShowing ")
                    .append(nearbyCount)
                    .append(" nearby saved locations within ")
                    .append(formatRadius(nearbyRadiusMeters))
                    .append(".");
        } else {
            detail.append("\nNo saved locations were found within ")
                    .append(formatRadius(nearbyRadiusMeters))
                    .append(" yet.");
        }
        return detail.toString();
    }

    @NonNull
    private String buildUncertainGpsHint(@NonNull Landmark landmark,
                                         boolean canUseGps,
                                         boolean isGalleryImage,
                                         double userDistanceToLandmark) {
        if (!canUseGps) {
            return "\nGPS was unavailable, so this result is based on the image only.";
        }
        if (isGalleryImage) {
            return "\nCurrent GPS was ignored because gallery images may have been taken earlier in a different place.";
        }
        return "\nCurrent GPS is about " + formatDistance((float) userDistanceToLandmark)
                + " from " + landmark.getDisplayName() + ".";
    }

    @Nullable
    private Landmark resolveLandmark(@NonNull LandmarkClassifier.Result prediction) {
        if (LandmarkClassifier.LABEL_PARLIAMENT.equalsIgnoreCase(prediction.getLabel())) {
            return landmarkRepository.findByIdOrToken("parliament");
        }
        if (LandmarkClassifier.LABEL_KENSINGTON_OVAL.equalsIgnoreCase(prediction.getLabel())) {
            return landmarkRepository.findByIdOrToken("kensington");
        }
        if (prediction.getParliamentProbability() >= prediction.getKensingtonProbability()
                && prediction.getParliamentProbability() >= prediction.getOtherProbability()) {
            return landmarkRepository.findByIdOrToken("parliament");
        }
        if (prediction.getKensingtonProbability() > prediction.getParliamentProbability()
                && prediction.getKensingtonProbability() >= prediction.getOtherProbability()) {
            return landmarkRepository.findByIdOrToken("kensington");
        }
        return null;
    }

    private boolean isConfirmedPrediction(@NonNull LandmarkClassifier.Result prediction) {
        return LandmarkClassifier.LABEL_PARLIAMENT.equalsIgnoreCase(prediction.getLabel())
                || LandmarkClassifier.LABEL_KENSINGTON_OVAL.equalsIgnoreCase(prediction.getLabel());
    }

    private boolean isParliament(@NonNull Landmark landmark) {
        return "parliament".equalsIgnoreCase(landmark.getId());
    }

    private float getDisplayProbability(@NonNull LandmarkClassifier.Result prediction,
                                        @NonNull Landmark landmark) {
        if (isParliament(landmark)) {
            return prediction.getParliamentProbability();
        }
        if ("kensington".equalsIgnoreCase(landmark.getId())) {
            return prediction.getKensingtonProbability();
        }
        return prediction.getTopProbability();
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
    private LandmarkClassifier getClassifier() throws IOException {
        if (classifier == null) {
            classifier = new LandmarkClassifier(getApplication());
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

    @NonNull
    private String formatRadius(int meters) {
        if (meters >= 1000) {
            return String.format(java.util.Locale.US, "%.1f km", meters / 1000f);
        }
        return String.format(java.util.Locale.US, "%d m", meters);
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
