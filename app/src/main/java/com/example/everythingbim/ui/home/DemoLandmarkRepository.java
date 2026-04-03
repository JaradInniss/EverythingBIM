package com.example.everythingbim.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class DemoLandmarkRepository {
    private final List<DemoLandmark> landmarks = new ArrayList<>();
    private final Map<String, String> aliasToLandmarkId = new HashMap<>();
    private final Random random = new Random();

    public DemoLandmarkRepository() {
        DemoLandmark parliament = new DemoLandmark(
                "parliament",
                "parliament",
                "Barbados Parliament Buildings",
                "Historic neo-Gothic government buildings in Bridgetown and among Barbados' most recognizable civic landmarks."
        );
        DemoLandmark kensington = new DemoLandmark(
                "kensington",
                "kensington",
                "Kensington Oval",
                "A famous Bridgetown cricket ground and one of Barbados' best-known sporting venues."
        );
        DemoLandmark cathedral = new DemoLandmark(
                "cathedral",
                "cathedral",
                "St. Michael's Cathedral",
                "A major Anglican cathedral in Bridgetown known for its long history and distinctive architecture."
        );

        landmarks.add(parliament);
        landmarks.add(kensington);
        landmarks.add(cathedral);

        registerAliases(parliament, Arrays.asList("parliament", "parliament_01", "1000791723", "1000791723.jpg"));
        registerAliases(kensington, Arrays.asList("kensington", "kensington_02", "1000791722", "1000791722.jpg"));
        registerAliases(cathedral, Arrays.asList("cathedral", "cathedral_03", "1000791724", "1000791724.jpg"));
    }

    @Nullable
    public DemoLandmark findByFileName(@Nullable String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return null;
        }

        String normalizedName = fileName.toLowerCase(Locale.US);
        for (Map.Entry<String, String> entry : aliasToLandmarkId.entrySet()) {
            if (normalizedName.contains(entry.getKey())) {
                return findById(entry.getValue());
            }
        }

        for (DemoLandmark landmark : landmarks) {
            if (normalizedName.contains(landmark.getToken().toLowerCase(Locale.US))) {
                return landmark;
            }
        }
        return null;
    }

    @NonNull
    public DemoLandmark getRandomLandmark() {
        return landmarks.get(random.nextInt(landmarks.size()));
    }

    private void registerAliases(@NonNull DemoLandmark landmark, @NonNull List<String> aliases) {
        for (String alias : aliases) {
            aliasToLandmarkId.put(alias.toLowerCase(Locale.US), landmark.getId());
        }
    }

    @Nullable
    private DemoLandmark findById(@NonNull String landmarkId) {
        for (DemoLandmark landmark : landmarks) {
            if (landmark.getId().equals(landmarkId)) {
                return landmark;
            }
        }
        return null;
    }
}
