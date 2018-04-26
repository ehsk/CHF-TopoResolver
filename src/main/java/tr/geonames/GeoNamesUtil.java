package tr.geonames;

import tr.util.StringUtil;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 12/18/2016
 * Time: 6:15 PM
 */
public class GeoNamesUtil {
    private GeoNamesUtil() {
    }

    private static final GeoNamesRepository geoNamesRepository = new GeoNamesRepository();

    public static final GeoNamesEntry EARTH = geoNamesRepository.earthEntry;

    public static List<String> getAlternateNames(GeoNamesEntry geoNamesEntry) {
        return Splitter.on(',').omitEmptyStrings()
                .splitToList(Strings.nullToEmpty(geoNamesEntry.getAlternateNames()));
    }

    public static boolean isAdminOrPopulated(GeoNamesEntry geoNamesEntry) {
        return geoNamesEntry.getFeatureCode().equalsIgnoreCase("A") || geoNamesEntry.getFeatureCode().equalsIgnoreCase("P");
    }

    private static String[] getHierarchyCodes(GeoNamesEntry entry) {
        String[] levels = new String[] {"", "", ""};

        if (StringUtil.hasText(entry.getCountryCode())) {
            levels[0] = entry.getCountryCode();
        }

        if (StringUtil.hasText(entry.getAdmin1code()))
            levels[1] = (StringUtil.hasText(entry.getCountryCode()) ? (entry.getCountryCode() + ".") : "") + entry.getAdmin1code();

        if (StringUtil.hasText(entry.getAdmin2code())) {
            levels[2] = (StringUtil.hasText(entry.getCountryCode()) ? (entry.getCountryCode() + ".") : "") +
                    (StringUtil.hasText(entry.getAdmin1code()) ? (entry.getAdmin1code() + ".") : "") +
                    entry.getAdmin2code();
        }

        return levels;
    }

    public static Map<GeoNamesLevel, GeoNamesEntry> getGeoNamesHierarchy(GeoNamesEntry entry) {
        final Map<GeoNamesLevel, GeoNamesEntry> hierarchyGeoNames = new HashMap<>();

        final String[] hierarchyCodes = getHierarchyCodes(entry);

        if (StringUtil.hasText(hierarchyCodes[0])) {
            geoNamesRepository.loadCountryByCode(hierarchyCodes[0]).ifPresent(country -> {
                if (!country.getGeonameId().equals(entry.getGeonameId()))
                    hierarchyGeoNames.put(GeoNamesLevel.COUNTRY, country);
            });
        }

        if (StringUtil.hasText(hierarchyCodes[1])) {
            geoNamesRepository.loadAdmin1ByCode(hierarchyCodes[1]).ifPresent(admin1 -> {
                if (!admin1.getGeonameId().equals(entry.getGeonameId()))
                    hierarchyGeoNames.put(GeoNamesLevel.ADMIN1, admin1);
            });
        }

        if (StringUtil.hasText(hierarchyCodes[2])) {
            geoNamesRepository.loadAdmin2ByCode(hierarchyCodes[2]).ifPresent(admin2 -> {
                if (!admin2.getGeonameId().equals(entry.getGeonameId()))
                    hierarchyGeoNames.put(GeoNamesLevel.ADMIN2, admin2);
            });
        }

        return hierarchyGeoNames;
    }
}
