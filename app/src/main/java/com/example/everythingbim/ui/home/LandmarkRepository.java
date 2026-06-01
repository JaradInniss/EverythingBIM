package com.example.everythingbim.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class LandmarkRepository {
    private static final double PARLIAMENT_LATITUDE = 13.0969861d;
    private static final double PARLIAMENT_LONGITUDE = -59.6139194d;
    private static final int PARLIAMENT_NEARBY_RADIUS_METERS = 1000;
    private static final int KENSINGTON_NEARBY_RADIUS_METERS = PARLIAMENT_NEARBY_RADIUS_METERS;

    private final List<Landmark> landmarks = new ArrayList<>();
    private final Map<String, String> aliasToLandmarkId = new HashMap<>();
    private final Random random = new Random();

    public LandmarkRepository() {
        Landmark parliament = new Landmark(
                "parliament",
                "parliament",
                "Barbados Parliament Buildings",
                "Historic neo-Gothic government buildings in Bridgetown and among Barbados' most recognizable civic landmarks.",
                PARLIAMENT_LATITUDE,
                PARLIAMENT_LONGITUDE,
                PARLIAMENT_NEARBY_RADIUS_METERS
        );
        Landmark kensington = new Landmark(
                "kensington",
                "kensington",
                "Kensington Oval",
                "A famous Bridgetown cricket ground and one of Barbados' best-known sporting venues.",
                13.1000d,
                -59.6160d,
                KENSINGTON_NEARBY_RADIUS_METERS
        );
        Landmark cathedral = new Landmark(
                "cathedral",
                "cathedral",
                "St. Michael's Cathedral",
                "A major Anglican cathedral in Bridgetown known for its long history and distinctive architecture.",
                13.0979d,
                -59.6105d,
                0
        );

        landmarks.add(parliament);
        landmarks.add(kensington);
        landmarks.add(cathedral);

        registerAliases(parliament, Arrays.asList("parliament", "parliament_01", "1000791723", "1000791723.jpg", "35.jpg"));
        registerAliases(kensington, Arrays.asList("kensington", "kensington_02", "1000791722", "1000791722.jpg", "37.jpg"));
        registerAliases(cathedral, Arrays.asList("cathedral", "cathedral_03", "1000791724", "1000791724.jpg", "36.jpg"));
    }

    @Nullable
    public Landmark findByIdOrToken(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String normalized = value.toLowerCase(Locale.US);
        Landmark byId = findById(normalized);
        if (byId != null) {
            return byId;
        }

        for (Landmark landmark : landmarks) {
            if (landmark.getToken().toLowerCase(Locale.US).equals(normalized)) {
                return landmark;
            }
        }

        String aliasMatch = aliasToLandmarkId.get(normalized);
        return aliasMatch != null ? findById(aliasMatch) : null;
    }

    @Nullable
    public Landmark findByFileName(@Nullable String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return null;
        }

        String normalizedName = fileName.toLowerCase(Locale.US);
        for (Map.Entry<String, String> entry : aliasToLandmarkId.entrySet()) {
            if (normalizedName.contains(entry.getKey())) {
                return findById(entry.getValue());
            }
        }

        for (Landmark landmark : landmarks) {
            if (normalizedName.contains(landmark.getToken().toLowerCase(Locale.US))) {
                return landmark;
            }
        }
        return null;
    }

    @NonNull
    public Landmark getRandomLandmark() {
        return landmarks.get(random.nextInt(landmarks.size()));
    }

    private void registerAliases(@NonNull Landmark landmark, @NonNull List<String> aliases) {
        for (String alias : aliases) {
            aliasToLandmarkId.put(alias.toLowerCase(Locale.US), landmark.getId());
        }
    }

    @Nullable
    private Landmark findById(@NonNull String landmarkId) {
        for (Landmark landmark : landmarks) {
            if (landmark.getId().equals(landmarkId)) {
                return landmark;
            }
        }
        return null;
    }
}
