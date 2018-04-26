package tr.geotagging.resolution.contextbound;

import tr.Toponym;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the following paper (section 3.2: location disambiguation):
 * Geotagging Named Entities in News and Online Documents
 * By Jiangwei Yu and Davood Rafiei
 * @link http://dl.acm.org/citation.cfm?id=2983795
 * @author ehsan
 * Date: 12/18/2016
 * Time: 4:48 PM
 */
public class PreliminaryContextBoundGeoResolver extends ContextBoundResolver {

    public PreliminaryContextBoundGeoResolver(int k) {
        super(k);
    }

    public PreliminaryContextBoundGeoResolver() {
        super();
    }

    @Override
    protected List<Toponym> detectGeoTags(YRArticleBucket articleBucket) {
        return new ArrayList<>(articleBucket.disambiguatedMap.values());
    }

    @Override
    public String toString() {
        return "PreliminaryContextBound";
    }
}
