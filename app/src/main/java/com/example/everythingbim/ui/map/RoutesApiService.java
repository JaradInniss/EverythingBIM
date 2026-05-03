package com.example.everythingbim.ui.map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.everythingbim.ui.home.NearbySavedLocation;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class RoutesApiService {
    private static final String ROUTES_ENDPOINT = "https://routes.googleapis.com/directions/v2:computeRoutes";
    private static final String FIELD_MASK =
            "routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline," +
                    "routes.legs.steps.distanceMeters,routes.legs.steps.staticDuration," +
                    "routes.legs.steps.navigationInstruction.instructions";

    private RoutesApiService() {
    }

    @NonNull
    static RoutePreviewData fetchWalkingRoute(@NonNull String apiKey,
                                              @NonNull LatLng origin,
                                              @NonNull List<NearbySavedLocation> selectedStops) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(ROUTES_ENDPOINT).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("X-Goog-Api-Key", apiKey);
            connection.setRequestProperty("X-Goog-FieldMask", FIELD_MASK);

            String body = buildRequestBody(origin, selectedStops);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            InputStream responseStream = responseCode >= 200 && responseCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String responseText = readFully(responseStream);

            if (responseCode < 200 || responseCode >= 300) {
                throw new IllegalStateException("Routes API error " + responseCode + ": " + responseText);
            }

            JSONObject responseJson = new JSONObject(responseText);
            JSONArray routes = responseJson.optJSONArray("routes");
            if (routes == null || routes.length() == 0) {
                throw new IllegalStateException("Routes API returned no routes.");
            }

            JSONObject route = routes.getJSONObject(0);
            int distanceMeters = route.optInt("distanceMeters", 0);
            long durationSeconds = parseDurationSeconds(route.optString("duration", "0s"));

            JSONObject polylineObject = route.optJSONObject("polyline");
            String encodedPolyline = polylineObject != null ? polylineObject.optString("encodedPolyline", "") : "";
            List<LatLng> path = decodePolyline(encodedPolyline);

            ArrayList<RouteStep> steps = new ArrayList<>();
            JSONArray legs = route.optJSONArray("legs");
            if (legs != null) {
                for (int legIndex = 0; legIndex < legs.length(); legIndex++) {
                    JSONObject leg = legs.optJSONObject(legIndex);
                    if (leg == null) {
                        continue;
                    }
                    JSONArray legSteps = leg.optJSONArray("steps");
                    if (legSteps == null) {
                        continue;
                    }
                    for (int stepIndex = 0; stepIndex < legSteps.length(); stepIndex++) {
                        JSONObject step = legSteps.optJSONObject(stepIndex);
                        if (step == null) {
                            continue;
                        }
                        JSONObject navigationInstruction = step.optJSONObject("navigationInstruction");
                        String instructions = navigationInstruction != null
                                ? navigationInstruction.optString("instructions", "")
                                : "";
                        if (instructions == null || instructions.trim().isEmpty()) {
                            continue;
                        }
                        steps.add(new RouteStep(
                                instructions.trim(),
                                step.optInt("distanceMeters", 0),
                                parseDurationSeconds(step.optString("staticDuration", "0s"))
                        ));
                    }
                }
            }

            return new RoutePreviewData(distanceMeters, durationSeconds, path, steps);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @NonNull
    private static String buildRequestBody(@NonNull LatLng origin,
                                           @NonNull List<NearbySavedLocation> selectedStops) throws Exception {
        NearbySavedLocation destination = selectedStops.get(selectedStops.size() - 1);

        JSONObject body = new JSONObject();
        body.put("origin", createWaypoint(origin.latitude, origin.longitude));
        body.put("destination", createWaypoint(destination.getLatitude(), destination.getLongitude()));
        body.put("travelMode", "WALK");
        body.put("polylineQuality", "HIGH_QUALITY");
        body.put("polylineEncoding", "ENCODED_POLYLINE");
        body.put("languageCode", "en-US");
        body.put("units", "METRIC");

        if (selectedStops.size() > 1) {
            JSONArray intermediates = new JSONArray();
            for (int index = 0; index < selectedStops.size() - 1; index++) {
                NearbySavedLocation stop = selectedStops.get(index);
                intermediates.put(createWaypoint(stop.getLatitude(), stop.getLongitude()));
            }
            body.put("intermediates", intermediates);
        }

        return body.toString();
    }

    @NonNull
    private static JSONObject createWaypoint(double latitude, double longitude) throws Exception {
        JSONObject latLng = new JSONObject()
                .put("latitude", latitude)
                .put("longitude", longitude);
        JSONObject location = new JSONObject().put("latLng", latLng);
        return new JSONObject().put("location", location);
    }

    private static long parseDurationSeconds(@Nullable String durationString) {
        if (durationString == null || durationString.trim().isEmpty()) {
            return 0L;
        }
        String normalized = durationString.trim().toLowerCase(Locale.US).replace("s", "");
        try {
            return Math.round(Double.parseDouble(normalized));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    @NonNull
    private static String readFully(@Nullable InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }
        try (InputStream stream = new BufferedInputStream(inputStream);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = stream.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    @NonNull
    private static List<LatLng> decodePolyline(@Nullable String encoded) {
        ArrayList<LatLng> polyline = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) {
            return polyline;
        }

        int index = 0;
        int latitude = 0;
        int longitude = 0;

        while (index < encoded.length()) {
            int[] latitudeResult = decodeChunk(encoded, index);
            latitude += latitudeResult[0];
            index = latitudeResult[1];

            int[] longitudeResult = decodeChunk(encoded, index);
            longitude += longitudeResult[0];
            index = longitudeResult[1];

            polyline.add(new LatLng(latitude / 1E5, longitude / 1E5));
        }
        return polyline;
    }

    @NonNull
    private static int[] decodeChunk(@NonNull String encoded, int startIndex) {
        int result = 0;
        int shift = 0;
        int index = startIndex;
        int chunk;
        do {
            chunk = encoded.charAt(index++) - 63;
            result |= (chunk & 0x1f) << shift;
            shift += 5;
        } while (chunk >= 0x20 && index < encoded.length());
        int delta = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
        return new int[]{delta, index};
    }

    static final class RoutePreviewData {
        final int distanceMeters;
        final long durationSeconds;
        final List<LatLng> path;
        final List<RouteStep> steps;

        RoutePreviewData(int distanceMeters,
                         long durationSeconds,
                         @NonNull List<LatLng> path,
                         @NonNull List<RouteStep> steps) {
            this.distanceMeters = distanceMeters;
            this.durationSeconds = durationSeconds;
            this.path = path;
            this.steps = steps;
        }
    }

    static final class RouteStep {
        final String instructions;
        final int distanceMeters;
        final long durationSeconds;

        RouteStep(@NonNull String instructions, int distanceMeters, long durationSeconds) {
            this.instructions = instructions;
            this.distanceMeters = distanceMeters;
            this.durationSeconds = durationSeconds;
        }
    }
}
