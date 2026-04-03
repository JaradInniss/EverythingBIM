package com.example.everythingbim.ui.home;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {
    private static final long PREVIEW_DELAY_MS = 250L;
    private static final long ANALYSIS_DELAY_MS = 1200L;

    private final MutableLiveData<HomeUiState> uiState = new MutableLiveData<>(HomeUiState.idle());
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final DemoIdentifier demoIdentifier = new DemoIdentifier(new DemoLandmarkRepository());

    @NonNull
    public LiveData<HomeUiState> getUiState() {
        return uiState;
    }

    public void onImageSelected(@NonNull SelectedImage selectedImage) {
        handler.removeCallbacksAndMessages(null);
        uiState.setValue(HomeUiState.preview(selectedImage));
        handler.postDelayed(() -> startAnalysis(selectedImage), PREVIEW_DELAY_MS);
    }

    public void onSelectionError(@NonNull String message) {
        handler.removeCallbacksAndMessages(null);
        uiState.setValue(HomeUiState.error(null, message));
    }

    public void reset() {
        handler.removeCallbacksAndMessages(null);
        uiState.setValue(HomeUiState.idle());
    }

    private void startAnalysis(@NonNull SelectedImage selectedImage) {
        uiState.setValue(HomeUiState.analyzing(selectedImage, "Analyzing image in demo mode..."));
        handler.postDelayed(() -> completeAnalysis(selectedImage), ANALYSIS_DELAY_MS);
    }

    private void completeAnalysis(@NonNull SelectedImage selectedImage) {
        DemoIdentificationResult result = demoIdentifier.identify(selectedImage);
        if (result.getType() == DemoIdentificationResult.Type.MATCH && result.getLandmark() != null) {
            uiState.setValue(HomeUiState.result(selectedImage, result.getLandmark(), result.getMatchDetail()));
        } else {
            uiState.setValue(HomeUiState.unknown(
                    selectedImage,
                    result.getMatchDetail() != null ? result.getMatchDetail() : ""
            ));
        }
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
