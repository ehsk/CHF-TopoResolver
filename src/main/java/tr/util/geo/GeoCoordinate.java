package tr.util.geo;

import java.util.Objects;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 11/28/2016
 * Time: 9:38 PM
 */
public class GeoCoordinate {
    private final double latitude;
    private final double longitude;
    private final String name;

    public GeoCoordinate(double latitude, double longitude, String name) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
    }

    public GeoCoordinate(double latitude, double longitude) {
        this(latitude, longitude, "");
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeoCoordinate that = (GeoCoordinate) o;
        return Double.compare(that.latitude, latitude) == 0 &&
                Double.compare(that.longitude, longitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude);
    }

    @Override
    public String toString() {
        return (name.isEmpty() ? "" : name) + "(" + latitude + "," + longitude + ")";
    }
}
