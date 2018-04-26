package tr.util.geo;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 11/30/2016
 * Time: 6:52 PM
 */
class GeoUtilTest {

    private final double PRECISION = 0.001;

    @Test
    void testReflexivePropertyOfDistance() {
        final GeoCoordinate p = new GeoCoordinate(53.526778, -113.526644, "Athabasca Hall");
        assertEquals(GeoUtil.distance(p, p).toKilometres(), 0, PRECISION);
    }

    @Test
    void testSymmetricPropertyOfDistance() {
        final GeoCoordinate p1 = new GeoCoordinate(53.526778, -113.526644, "Athabasca Hall");
        final GeoCoordinate p2 = new GeoCoordinate(53.526708, -113.527116, "CSC building");
        assertEquals(GeoUtil.distance(p1, p2).toMetres(), GeoUtil.distance(p2, p1).toMetres(), PRECISION);
    }

    @Test
    void testCloseDistance() {
        final GeoCoordinate p1 = new GeoCoordinate(53.526778, -113.526644, "Athabasca Hall");
        final GeoCoordinate p2 = new GeoCoordinate(53.522142, -113.530464, "Lister Centre");
        assertEquals(0.574, GeoUtil.distance(p1, p2).toKilometres(), PRECISION);
    }

    @Test
    void testFarDistance() {
        final GeoCoordinate p1 = new GeoCoordinate(53.55014, -113.46871, "Edmonton");
        final GeoCoordinate p2 = new GeoCoordinate(-26.20227, 28.04363, "Johannesburg");
        assertEquals(15630.866, GeoUtil.distance(p1, p2).toKilometres(), PRECISION);
    }
}
