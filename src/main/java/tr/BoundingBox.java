package tr;

import tr.util.geo.GeoCoordinate;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code BoundingBox} class represents the bounding box of a region,
 * which is recognized by its four sides: west, east, north, and south.
 *
 * @see GeoCoordinate
 */
public class BoundingBox {
    private final double west, east, north, south;

    public BoundingBox(double west, double east, double north, double south) {
        this.west = west;
        this.east = east;
        this.north = north;
        this.south = south;
    }

    /**
     * Checks whether a geo coordinate resides within the bounding box
     * @param coordinate a coordinate reference to check
     * @return {@code true} if the bounding box contains the input coordinates
     */
    public boolean contains(@Nonnull  GeoCoordinate coordinate) {
        return coordinate.getLongitude() <= east && coordinate.getLongitude() >= west &&
                coordinate.getLatitude() <= north && coordinate.getLatitude() >= south;
    }

    /**
     * Checks whether a geographic reference lies in the bounding box
     * @param geoContainer a geographic location reference to check
     * @param <G> geolocation type
     *
     * @see BoundingBox#contains(GeoCoordinate)
     */
    public<G extends GeoContainer> boolean contains(@Nonnull G geoContainer) {
        return contains(geoContainer.toCoordinate());
    }

    /**
     * Converts the bounding box object to a {@link HashMap}.
     * <p>
     * The keys are {@code w} for west, {@code e} for east,
     * {@code n} for north, {@code s} for south.
     * </p>
     * @return a {@link HashMap} representation of the bounding box
     */
    public Map<String, Double> asMap() {
        Map<String, Double> map = new HashMap<>();
        map.put("w", west);
        map.put("e", east);
        map.put("n", north);
        map.put("s", south);
        return map;
    }
}
