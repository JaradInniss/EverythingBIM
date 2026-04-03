package com.example.everythingbim.ui.home;

import androidx.annotation.NonNull;

public class DemoIdentifier {
    private final DemoLandmarkRepository landmarkRepository;

    public DemoIdentifier(@NonNull DemoLandmarkRepository landmarkRepository) {
        this.landmarkRepository = landmarkRepository;
    }

    @NonNull
    public DemoIdentificationResult identify(@NonNull SelectedImage selectedImage) {
        if (SelectedImage.SOURCE_CAMERA.equals(selectedImage.getSource())) {
            DemoLandmark landmark = landmarkRepository.getRandomLandmark();
            return DemoIdentificationResult.match(landmark, "Simulated camera result");
        }

        DemoLandmark landmark = landmarkRepository.findByFileName(selectedImage.getDisplayName());
        if (landmark != null) {
            return DemoIdentificationResult.match(
                    landmark,
                    "Matched demo token in filename"
            );
        }

        return DemoIdentificationResult.unknown(
                "No demo token found. Try a filename containing parliament, kensington, or cathedral."
        );
    }
}
