package tr.geotagging.commercial.googlenl;

import tr.util.StringUtil;
import tr.util.geo.GeoCoordinate;
import tr.util.redis.RedisHash;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 8/11/2017
 * Time: 2:33 AM
 */
public class WikipediaRepository {

    public Optional<GeoCoordinate> getCoordinate(String url) {
        if (!StringUtil.hasText(url))
            return Optional.empty();
        
        final String resource = url.substring(url.lastIndexOf("/") + 1);
        final RedisHash<Double> wikiHash = RedisHash.newDoubleSet("wiki/" + resource);
        if (wikiHash.isEmpty()) {
            final Optional<GeoCoordinate> coordinate = Dbpedia.getCoordinate(resource);
            if (coordinate.isPresent()) {
                Map<String, Double> vals = new HashMap<>();
                vals.put("lat", coordinate.get().getLatitude());
                vals.put("lng", coordinate.get().getLongitude());
                wikiHash.set(vals);
            } else {
                wikiHash.set("0", 0d);
            }

            return coordinate;
        } else if (wikiHash.contains("0")) {
            return Optional.empty();
        } else {
            final Map<String, Double> coordinates = wikiHash.get("lat", "lng");
            return Optional.of(new GeoCoordinate(coordinates.get("lat"), coordinates.get("lng")));
        }
    }
}
