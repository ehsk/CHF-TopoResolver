package tr.geotagging.commercial.placemaker;

import tr.Toponym;
import tr.geotagging.commercial.CommercialGeoResolver;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 6/26/2017
 * Time: 1:39 PM
 */
public class YahooPlacemakerGeoResolver extends CommercialGeoResolver {

    private final YahooPlacemakerFacade facade = YahooPlacemakerFacade.getInstance();

    @Override
    protected List<Toponym> extract(CommercialArticleBucket articleBucket) {
        return facade.extractGeoTags(articleBucket.article);
    }

    @Override
    public String toString() {
        return "YahooPlacemaker";
    }

}
