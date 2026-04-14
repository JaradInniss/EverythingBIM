package com.example.everythingbim.ui.home;

import android.util.Base64;

import androidx.annotation.NonNull;

import com.example.everythingbim.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class RestBinaryClassifierClient {
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 20_000;

    @NonNull
    public RestPredictionResult predict(@NonNull byte[] imageBytes, @NonNull String baseUrl) throws IOException, JSONException {
        String encodedImage = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
        JSONObject requestJson = new JSONObject();
        requestJson.put("image_base64", encodedImage);

        HttpURLConnection connection = null;
        try {
            URL url = new URL(baseUrl + "/predict");
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setDoOutput(true);

            byte[] requestBytes = requestJson.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(requestBytes);
            }

            int responseCode = connection.getResponseCode();
            InputStream responseStream = responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            String responseBody = readFully(responseStream);
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("Server returned " + responseCode + ": " + responseBody);
            }

            JSONObject responseJson = new JSONObject(responseBody);
            String predictedLabel = responseJson.optString("predicted_label", "");
            String positiveLabel = responseJson.optString("positive_label", "");
            double positiveProbability = responseJson.optDouble("positive_probability", 0d);
            Map<String, Double> classProbabilities = parseProbabilities(
                    responseJson.optJSONObject("class_probabilities"));

            return new RestPredictionResult(
                    predictedLabel,
                    positiveLabel,
                    positiveProbability,
                    classProbabilities
            );
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @NonNull
    public String healthCheck(@NonNull String baseUrl) throws IOException, JSONException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(baseUrl + "/health");
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            InputStream responseStream = responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            String responseBody = readFully(responseStream);
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("Server returned " + responseCode + ": " + responseBody);
            }

            if (responseBody.isEmpty()) {
                return "Health check passed.";
            }

            try {
                JSONObject responseJson = new JSONObject(responseBody);
                if (responseJson.has("status")) {
                    return responseJson.optString("status", "Health check passed.");
                }
            } catch (JSONException ignored) {
                // Fall back to raw body for simple text responses.
            }

            return responseBody;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @NonNull
    private Map<String, Double> parseProbabilities(JSONObject probabilitiesJson) {
        Map<String, Double> probabilities = new LinkedHashMap<>();
        if (probabilitiesJson == null) {
            return probabilities;
        }

        Iterator<String> keys = probabilitiesJson.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            probabilities.put(key, probabilitiesJson.optDouble(key, 0d));
        }
        return probabilities;
    }

    @NonNull
    private String readFully(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }

        try (InputStream in = inputStream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}
