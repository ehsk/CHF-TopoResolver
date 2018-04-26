package tr.geotagging.commercial.opencalais;

import tr.Toponym;
import tr.geotagging.commercial.CommercialGeoResolver;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 6/26/2017
 * Time: 1:39 PM
 */
public class OpenCalaisGeoResolver extends CommercialGeoResolver {

    private final OpenCalaisFacade facade = new OpenCalaisFacade();

    @Override
    protected List<Toponym> extract(CommercialArticleBucket articleBucket) {
        return facade.extractGeoTags(articleBucket.article);
    }

    @Override
    public String toString() {
        return "OpenCalais";
    }

}
