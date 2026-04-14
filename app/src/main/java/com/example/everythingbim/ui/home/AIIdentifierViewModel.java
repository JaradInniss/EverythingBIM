package com.example.everythingbim.ui.home;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.everythingbim.data.models.SelectedImage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AIIdentifierViewModel extends AndroidViewModel {
    private final MutableLiveData<HomeUiState> uiState = new MutableLiveData<>(HomeUiState.idle());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final RestBinaryClassifierClient classifierClient = new RestBinaryClassifierClient();
    private final DemoLandmarkRepository landmarkRepository = new DemoLandmarkRepository();

    private SelectedImage selectedImage;

    public AIIdentifierViewModel(@NonNull Application application) {
        super(application);
    }

    @NonNull
    public LiveData<HomeUiState> getUiState() {
        return uiState;
    }

    public void initialize(@NonNull SelectedImage image) {
        if (this.selectedImage != null) return;

        this.selectedImage = image;
        uiState.setValue(HomeUiState.preview(image));
        startAnalysis();
    }

    public void rerunAnalysis() {
        if (selectedImage == null) {
            return;
        }
        startAnalysis();
    }

    public void runHealthCheck(@NonNull HealthCheckCallback callback) {
        String baseUrl = ModelServerSettings.getBaseUrl(getApplication());
        executorService.execute(() -> {
            try {
                String message = classifierClient.healthCheck(baseUrl);
                callback.onComplete(true, message);
            } catch (Exception exception) {
                callback.onComplete(
                        false,
                        exception.getMessage() != null ? exception.getMessage() : "Health check failed."
                );
            }
        });
    }

    private void startAnalysis() {
        if (selectedImage == null) {
            return;
        }

        String baseUrl = ModelServerSettings.getBaseUrl(getApplication());
        uiState.setValue(HomeUiState.analyzing(
                selectedImage,
                "Sending image to " + baseUrl + "..."
        ));

        executorService.execute(() -> {
            try {
                byte[] imageBytes = readImageBytes(selectedImage.getUri());
                RestPredictionResult prediction = classifierClient.predict(imageBytes, baseUrl);
                handlePrediction(prediction);
            } catch (Exception exception) {
                uiState.postValue(HomeUiState.error(
                        selectedImage,
                        exception.getMessage() != null ? exception.getMessage() : "Prediction failed."
                ));
            }
        });
    }

    private void handlePrediction(@NonNull RestPredictionResult prediction) {
        if (selectedImage == null) {
            return;
        }

        DemoLandmark landmark = landmarkRepository.findByIdOrToken(prediction.getPredictedLabel());
        String confidenceText = String.format(Locale.US, "%.1f%%", prediction.getPositiveProbability() * 100d);

        if (landmark != null && landmark.getId().equalsIgnoreCase(prediction.getPositiveLabel())) {
            String detail = formatProbabilities(prediction.getClassProbabilities());
            uiState.postValue(HomeUiState.result(selectedImage, landmark, confidenceText, detail));
            return;
        }

        String unknownDetail = "Predicted label: " + prediction.getPredictedLabel()
                + "\nPositive probability: " + confidenceText
                + "\n" + formatProbabilities(prediction.getClassProbabilities());
        uiState.postValue(HomeUiState.unknown(selectedImage, unknownDetail));
    }

    @NonNull
    private byte[] readImageBytes(@NonNull Uri imageUri) throws IOException {
        try (InputStream inputStream = getApplication().getContentResolver().openInputStream(imageUri);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (inputStream == null) {
                throw new IOException("Unable to open the selected image.");
            }

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        }
    }

    @NonNull
    private String formatProbabilities(@NonNull Map<String, Double> probabilities) {
        if (probabilities.isEmpty()) {
            return "No class probabilities returned by the server.";
        }

        StringBuilder builder = new StringBuilder("Class probabilities:");
        for (Map.Entry<String, Double> entry : probabilities.entrySet()) {
            builder.append("\n")
                    .append(entry.getKey())
                    .append(": ")
                    .append(String.format(Locale.US, "%.4f", entry.getValue()));
        }
        return builder.toString();
    }

    @Override
    protected void onCleared() {
        executorService.shutdownNow();
        super.onCleared();
    }

    public interface HealthCheckCallback {
        void onComplete(boolean success, @NonNull String message);
    }
}
