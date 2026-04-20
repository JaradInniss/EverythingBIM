package com.example.everythingbim.ui.home;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.everythingbim.data.models.SelectedImage;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AIIdentifierViewModel extends AndroidViewModel {
    private final MutableLiveData<HomeUiState> uiState = new MutableLiveData<>(HomeUiState.idle());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final DemoLandmarkRepository landmarkRepository = new DemoLandmarkRepository();

    private ParliamentClassifier classifier;
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

        if (ParliamentClassifier.LABEL_PARLIAMENT.equalsIgnoreCase(prediction.getLabel())) {
            DemoLandmark landmark = landmarkRepository.findByIdOrToken("parliament");
            if (landmark != null) {
                String confidenceText = formatConfidence(prediction.getParliamentProbability());
                uiState.postValue(HomeUiState.result(selectedImage, landmark, confidenceText, landmark.getDescription()));
                return;
            }
            uiState.postValue(HomeUiState.error(selectedImage, "Parliament metadata is unavailable."));
            return;
        }

        if (ParliamentClassifier.LABEL_UNCERTAIN.equalsIgnoreCase(prediction.getLabel())) {
            uiState.postValue(HomeUiState.uncertain(
                    selectedImage,
                    formatConfidence(prediction.getParliamentProbability()),
                    "Possible Parliament match",
                    "The model sees some Parliament-like features, but this image is still too close to call confidently."
                            + "\nTry a clearer, front-facing photo with better lighting."
                            + "\nPossible Parliament match: " + formatConfidence(prediction.getParliamentProbability())
            ));
            return;
        }

        uiState.postValue(HomeUiState.unknown(
                selectedImage,
                "This image was not identified as the Barbados Parliament Buildings."
        ));
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

    @Override
    protected void onCleared() {
        executorService.shutdownNow();
        if (classifier != null) {
            classifier.close();
        }
        super.onCleared();
    }
}
