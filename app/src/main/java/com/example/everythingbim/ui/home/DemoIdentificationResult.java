package com.example.everythingbim.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DemoIdentificationResult {
    public enum Type {
        MATCH,
        UNKNOWN
    }

    private final Type type;
    private final DemoLandmark landmark;
    private final String matchDetail;

    private DemoIdentificationResult(@NonNull Type type,
                                     @Nullable DemoLandmark landmark,
                                     @Nullable String matchDetail) {
        this.type = type;
        this.landmark = landmark;
        this.matchDetail = matchDetail;
    }

    @NonNull
    public static DemoIdentificationResult match(@NonNull DemoLandmark landmark,
                                                 @NonNull String matchDetail) {
        return new DemoIdentificationResult(Type.MATCH, landmark, matchDetail);
    }

    @NonNull
    public static DemoIdentificationResult unknown(@NonNull String matchDetail) {
        return new DemoIdentificationResult(Type.UNKNOWN, null, matchDetail);
    }

    @NonNull
    public Type getType() {
        return type;
    }

    @Nullable
    public DemoLandmark getLandmark() {
        return landmark;
    }

    @Nullable
    public String getMatchDetail() {
        return matchDetail;
    }
}
