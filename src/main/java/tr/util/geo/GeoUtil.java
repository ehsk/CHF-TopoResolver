package tr.util.geo;

import tr.util.Distance;
import tr.util.DistanceUnit;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 11/28/2016
 * Time: 9:38 PM
 */
public class GeoUtil {

    private static final int EARTH_RADIUS = 6371;

    private GeoUtil() {
    }

    /**
     * found the method at http://stackoverflow.com/questions/3694380/
     * @param p1 the first coordinate
     * @param p2 the second coordinate
     * @return the distance between p1 and p2 in kilometres
     */
    public static Distance distance(final GeoCoordinate p1, final GeoCoordinate p2) {
        final double dLat = Math.toRadians(p1.getLatitude() - p2.getLatitude());
        final double dLon = Math.toRadians(p1.getLongitude() - p2.getLongitude());
        final double a = Math.pow(Math.sin(dLat / 2), 2) +
                Math.pow(Math.sin(dLon / 2), 2) *
                        Math.cos(Math.toRadians(p1.getLatitude())) * Math.cos(Math.toRadians(p2.getLatitude()));
        final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return DistanceUnit.km.of(EARTH_RADIUS * c);
    }

    public static double toDecimalDegree(int degree, int minute, int second) {
        return degree + (minute + second/60.0) / 60.0;
    }

}
