package com.example.everythingbim.data.local;

import com.example.everythingbim.data.local.entities.LocationEntity;

import java.util.ArrayList;
import java.util.List;

public final class LocationSeedProvider {
    private static final float DEFAULT_RATING = 0f;
    private static final boolean VERIFIED = true;
    private static final String ADDED_BY = "seed_data";

    private LocationSeedProvider() {
    }

    public static List<LocationEntity> createSeedLocations() {
        List<LocationEntity> locations = new ArrayList<>();

        locations.add(new LocationEntity(
                "National Heroes Square",
                13.0967631d,
                -59.6140131d,
                DEFAULT_RATING,
                VERIFIED,
                ADDED_BY,
                "Central historic square in Bridgetown surrounded by important government and heritage buildings.",
                "civic",
                "heroes_square",
                "National Heroes Square, Bridgetown, Barbados"
        ));

        locations.add(new LocationEntity(
                "St. Michael's Cathedral",
                13.0978d,
                -59.6124d,
                DEFAULT_RATING,
                VERIFIED,
                ADDED_BY,
                "Historic Anglican cathedral in Bridgetown known for its architecture and long civic history.",
                "historic",
                "st_michael_cathedral",
                "St. Michael's Row, Bridgetown, Barbados"
        ));

        locations.add(new LocationEntity(
                "Chamberlain Bridge",
                13.0961d,
                -59.6141d,
                DEFAULT_RATING,
                VERIFIED,
                ADDED_BY,
                "Well-known bridge and waterfront point near central Bridgetown and the Careenage.",
                "waterfront",
                "chamberlain_bridge",
                "Chamberlain Bridge, Bridgetown, Barbados"
        ));

        locations.add(new LocationEntity(
                "Nidhe Israel Synagogue",
                13.099391d,
                -59.615185d,
                DEFAULT_RATING,
                VERIFIED,
                ADDED_BY,
                "Important historic religious and cultural site in Bridgetown with deep heritage significance.",
                "heritage",
                "nidhe_synagogue",
                "Synagogue Lane, Bridgetown, Barbados"
        ));

        locations.add(new LocationEntity(
                "Pelican Village",
                13.09816d,
                -59.62279d,
                DEFAULT_RATING,
                VERIFIED,
                ADDED_BY,
                "Well-known craft centre near the cruise terminal and Princess Alice Highway in Bridgetown.",
                "craft_culture",
                "pelican_village",
                "Pelican Village, Bridgetown, Barbados"
        ));

        locations.add(new LocationEntity(
                "Queen's Park",
                13.09922d,
                -59.60837d,
                DEFAULT_RATING,
                VERIFIED,
                ADDED_BY,
                "Historic park in Bridgetown known for its grounds and civic significance near the city centre.",
                "park",
                "queens_park",
                "Queen's Park, Bridgetown, Barbados"
        ));

        locations.add(new LocationEntity(
                "Cave Shepherd Department Store",
                13.0969d,
                -59.6131d,
                DEFAULT_RATING,
                VERIFIED,
                ADDED_BY,
                "Major department store on Broad Street in Bridgetown, located within walking distance of the Parliament Buildings and nearby heritage sites.",
                "shopping",
                "cave_shepherd",
                "Broad Street, Bridgetown, St. Michael, Barbados"
        ));

        locations.add(new LocationEntity(
                "Golden Square Freedom Park",
                13.0950d,
                -59.6122d,
                DEFAULT_RATING,
                VERIFIED,
                ADDED_BY,
                "Central city park on Fairchild Street in Bridgetown known for the Shards of Life mural and its national historical significance.",
                "park",
                "freedom_park",
                "Fairchild Street, Bridgetown, Barbados"
        ));

        return locations;
    }
}
