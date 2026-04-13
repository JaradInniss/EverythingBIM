package com.example.everythingbim.ui.home;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.everythingbim.data.models.SelectedImage;

public class AIIdentifierViewModel extends ViewModel {
    private static final long ANALYSIS_DELAY_MS = 1500L;

    private final MutableLiveData<HomeUiState> uiState = new MutableLiveData<>(HomeUiState.idle());
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final DemoIdentifier demoIdentifier = new DemoIdentifier(new DemoLandmarkRepository());

    private SelectedImage selectedImage;

    @NonNull
    public LiveData<HomeUiState> getUiState() {
        return uiState;
    }

    public void initialize(@NonNull SelectedImage image) {
        if (this.selectedImage != null) return; // Already initialized
        
        this.selectedImage = image;
        uiState.setValue(HomeUiState.preview(image));
        startAnalysis();
    }

    private void startAnalysis() {
        if (selectedImage == null) return;
        
        uiState.setValue(HomeUiState.analyzing(selectedImage, "Analyzing image in demo mode..."));
        handler.postDelayed(this::completeAnalysis, ANALYSIS_DELAY_MS);
    }

    private void completeAnalysis() {
        if (selectedImage == null) return;
        
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
