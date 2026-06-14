package com.example.everythingbim.data.local;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LandmarkCatalogSeedProvider {
    private static final String SOURCE_TYPE_CNN_SEED = "cnn_seed";
    private static final String DEFAULT_ADDED_BY = "admin";

    private LandmarkCatalogSeedProvider() {
    }

    @NonNull
    public static List<LandmarkCatalogSeed> createSeedLandmarks() {
        List<LandmarkCatalogSeed> seeds = new ArrayList<>();

        seeds.add(new LandmarkCatalogSeed(
                "parliament",
                "parliament",
                "Barbados Parliament Buildings",
                "civic",
                "Broad Street/Rickett Street, Bridgetown",
                "Historic neo-Gothic government buildings in Bridgetown and among Barbados' most recognizable civic landmarks.",
                "parliament_building.jpg",
                13.0969861d,
                -59.6139194d,
                1000
        ));

        seeds.add(new LandmarkCatalogSeed(
                "kensington",
                "kensington_oval",
                "Kensington Oval",
                "sport",
                "Fontabelle, St. Michael, Barbados",
                "A famous Bridgetown cricket ground and one of Barbados' best-known sporting venues.",
                "kensington_oval.jpg",
                13.1000d,
                -59.6160d,
                1000
        ));

        seeds.add(new LandmarkCatalogSeed(
                "cathedral",
                "cathedral",
                "St. Michael's Cathedral",
                "historic",
                "St. Michael's Row, Bridgetown, Barbados",
                "A major Anglican cathedral in Bridgetown known for its long history and distinctive architecture.",
                "",
                13.0979d,
                -59.6105d,
                0
        ));

        return Collections.unmodifiableList(seeds);
    }

    public static final class LandmarkCatalogSeed {
        public final String legacyLandmarkId;
        public final String classifierKey;
        public final String displayName;
        public final String category;
        public final String mapSubtitle;
        public final String description;
        public final String imageUrl;
        public final double latitude;
        public final double longitude;
        public final int nearbyRadiusMeters;

        private LandmarkCatalogSeed(@NonNull String legacyLandmarkId,
                                    @NonNull String classifierKey,
                                    @NonNull String displayName,
                                    @NonNull String category,
                                    @NonNull String mapSubtitle,
                                    @NonNull String description,
                                    @NonNull String imageUrl,
                                    double latitude,
                                    double longitude,
                                    int nearbyRadiusMeters) {
            this.legacyLandmarkId = legacyLandmarkId;
            this.classifierKey = classifierKey;
            this.displayName = displayName;
            this.category = category;
            this.mapSubtitle = mapSubtitle;
            this.description = description;
            this.imageUrl = imageUrl;
            this.latitude = latitude;
            this.longitude = longitude;
            this.nearbyRadiusMeters = nearbyRadiusMeters;
        }

        @NonNull
        public String getSourceType() {
            return SOURCE_TYPE_CNN_SEED;
        }

        @NonNull
        public String getAddedBy() {
            return DEFAULT_ADDED_BY;
        }
    }
}
