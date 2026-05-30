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

        locations.add(new LocationEntity(
                "Brandons Beach",
                13.1028d,
                -59.6246d,
                DEFAULT_RATING,
                VERIFIED,
                ADDED_BY,
                "Popular west-coast beach close to Bridgetown and a relaxed stop to pair with a Kensington Oval route.",
                "beach",
                "brandons_beach",
                "Brandons Beach, St. Michael, Barbados"
        ));

        locations.add(new LocationEntity(
                "The Careenage",
                13.0967d,
                -59.6146d,
                DEFAULT_RATING,
                VERIFIED,
                ADDED_BY,
                "Scenic waterfront stretch in central Bridgetown known for boardwalk views, moored boats, and easy access to nearby heritage sites.",
                "waterfront",
                "the_careenage",
                "The Careenage, Bridgetown, Barbados"
        ));

        locations.add(new LocationEntity(
                "Cheapside Market",
                13.1012d,
                -59.6188d,
                DEFAULT_RATING,
                VERIFIED,
                ADDED_BY,
                "Bustling Bridgetown market area that adds everyday local culture and food-shopping energy to nearby walking routes.",
                "market",
                "cheapside_market",
                "Cheapside, Bridgetown, Barbados"
        ));

        locations.add(new LocationEntity(
                "Independence Square",
                13.0965d,
                -59.6129d,
                DEFAULT_RATING,
                VERIFIED,
                ADDED_BY,
                "Central public square near the waterfront that works well as a civic stop on Bridgetown routes anchored around Kensington Oval.",
                "civic",
                "independence_square",
                "Independence Square, Bridgetown, Barbados"
        ));

        return locations;
    }
}
