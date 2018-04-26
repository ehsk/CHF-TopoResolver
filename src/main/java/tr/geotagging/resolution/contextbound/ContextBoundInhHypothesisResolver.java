package tr.geotagging.resolution.contextbound;

import tr.TaggedWord;
import tr.geonames.GeoNamesLevel;
import tr.geotagging.resolution.GeoCandidateEntry;
import com.google.common.collect.Table;

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
public class ContextBoundInhHypothesisResolver extends ContextBoundResolver {

    public ContextBoundInhHypothesisResolver(int k) {
        super(k);
    }

    public ContextBoundInhHypothesisResolver() {
        super();
    }

    @Override
    protected double calcKLDivergence(GeoNamesLevel level, TaggedWord taggedWord, Table<String, GeoCandidateEntry, Double> nearProbModel, List<GeoCandidateEntry> candidateEntries) {
        return Math.log(candidateEntries.size());
    }

    @Override
    public String toString() {
        return "ContextBoundInhHypothesis";
    }
}
