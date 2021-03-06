package tr.geotagging.commercial.googlenl;

import tr.Toponym;
import tr.geotagging.commercial.CommercialGeoResolver;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 6/26/2017
 * Time: 1:39 PM
 */
public class GoogleNLGeoResolver extends CommercialGeoResolver {

    private final GoogleCloudFacade facade = new GoogleCloudFacade();

    @Override
    protected List<Toponym> extract(CommercialArticleBucket articleBucket) {
        return facade.extractGeoTags(articleBucket.article);
    }

    @Override
    public String toString() {
        return "GoogleCloud";
    }

}
