package com.example.everythingbim.ui.home;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RestPredictionResult {
    private final String predictedLabel;
    private final String positiveLabel;
    private final double positiveProbability;
    private final Map<String, Double> classProbabilities;

    public RestPredictionResult(@NonNull String predictedLabel,
                                @NonNull String positiveLabel,
                                double positiveProbability,
                                @NonNull Map<String, Double> classProbabilities) {
        this.predictedLabel = predictedLabel;
        this.positiveLabel = positiveLabel;
        this.positiveProbability = positiveProbability;
        this.classProbabilities = Collections.unmodifiableMap(new LinkedHashMap<>(classProbabilities));
    }

    @NonNull
    public String getPredictedLabel() {
        return predictedLabel;
    }

    @NonNull
    public String getPositiveLabel() {
        return positiveLabel;
    }

    public double getPositiveProbability() {
        return positiveProbability;
    }

    @NonNull
    public Map<String, Double> getClassProbabilities() {
        return classProbabilities;
    }
}
