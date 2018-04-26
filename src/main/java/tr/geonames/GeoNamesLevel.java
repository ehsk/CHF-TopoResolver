package tr.geonames;

import java.util.Arrays;
import java.util.Optional;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 3/27/2017
 * Time: 9:44 AM
 */
public enum GeoNamesLevel {
    LEAF, ADMIN2, ADMIN1, COUNTRY, CONTINENT, EARTH;

    public Optional<GeoNamesLevel> getParent() {
        try {
            return Optional.of(values()[ordinal() + 1]);
        } catch (IndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }

    public Optional<GeoNamesLevel> getChild() {
        try {
            return Optional.of(values()[ordinal() - 1]);
        } catch (IndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }

    public static GeoNamesLevel[] getHierarchyLevels() {
        return new GeoNamesLevel[] { ADMIN2, ADMIN1, COUNTRY };
    }

    public static boolean isHierarchyLevel(GeoNamesLevel level) {
        return Arrays.stream(getHierarchyLevels()).anyMatch(l -> l == level);
    }
}
