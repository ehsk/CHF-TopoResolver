package tr.util;

import tr.Article;
import tr.dataset.AnnotatedDatasets;
import tr.dataset.Dataset;
import tr.dataset.DatasetSummary;
import tr.geotagging.evaluation.GeoTagResultMatcher;
import tr.geotagging.recognition.GeoRecognizer;
import tr.geotagging.recognition.MockRecognizer;
import tr.geotagging.recognition.StanfordNERecognizer;
import tr.geotagging.resolution.GeoResolver;
import tr.geotagging.resolution.contextbound.ContextBoundResolver;
import tr.geotagging.resolution.contextbound.ContextBoundInhHypothesisResolver;
import tr.geotagging.resolution.contextbound.ContextBoundNearbyHypothesisResolver;
import tr.geotagging.resolution.contextbound.PreliminaryContextBoundGeoResolver;
import tr.geotagging.resolution.contextfusion.CHFResolver;
import tr.geotagging.resolution.lieberman.LiebermanAdaptiveGeoResolver;
import tr.geotagging.resolution.spatialset.SpatialHierarchySetResolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class to handle command line arguments when running a geotagger
 */
public interface ArgUtil {
    /**
     * The options for toponym recognition are defined in this method.
     * <ul>
     *     <li>
     *         <strong>mock</strong>: Perfect recognizer. Uses all labelled toponyms for resolution.
     *     </li>
     *     <li>
     *         <strong>ner</strong>: Named Entity recognizer. Applies Stanford NER to extract toponyms.
     *     </li>
     * </ul>
     *
     * @return the possible options for recognition (i.e., <strong>mock</strong> and <strong>ner</strong>)
     */
    static String[] getRecognizerOptions() {
        return new String[]{"mock", "ner"};
    }

    /**
     *
     * @return
     */
    static String[] getResolverOptions() {
        return new String[]{"shs", "chf", "cbh", "cbh-pre", "cbh-nearby", "cbh-inh", "adaptive"};
    }

    static String[] getDataOptions() {
        return new String[]{ "tr-news", };
    }

    static GeoResolver getResolver(String resolverOption, String modelFile, Double threshold) {
        if (resolverOption.equalsIgnoreCase("shs"))
            return new SpatialHierarchySetResolver();
        else if (resolverOption.equalsIgnoreCase("cbh-pre"))
            return new PreliminaryContextBoundGeoResolver();
        else if (resolverOption.equalsIgnoreCase("cbh-inh"))
            return new ContextBoundInhHypothesisResolver();
        else if (resolverOption.equalsIgnoreCase("cbh-nearby"))
            return new ContextBoundNearbyHypothesisResolver();
        else if (resolverOption.equalsIgnoreCase("cbh"))
            return new ContextBoundResolver();
        else if (resolverOption.equalsIgnoreCase("adaptive")) {
            if (!StringUtil.hasText(modelFile))
                throw new IllegalArgumentException("Adaptive resolver: model file is required");

            final int wb;
            final Matcher matcher = Pattern.compile("\\.wb(\\d+)\\.", Pattern.CASE_INSENSITIVE).matcher(modelFile);
            if (matcher.find())
                wb = Integer.valueOf(matcher.group(1));
            else {
                wb = Config.Adaptive.DEFAULT_Wb;
            }

            return new LiebermanAdaptiveGeoResolver(modelFile, wb, Integer.MAX_VALUE);
        } else if (resolverOption.equalsIgnoreCase("chf")) {
            if (threshold == null)
                throw new IllegalArgumentException("Fused resolver: threshold is required");

            return new CHFResolver(threshold);
        }

        throw new IllegalArgumentException("Unknown resolver: " + resolverOption);
    }

    static GeoRecognizer getRecognizer(String recognizerOption) {
        if (recognizerOption.equalsIgnoreCase("mock"))
            return new MockRecognizer();
        else if (recognizerOption.equalsIgnoreCase("ner"))
            return new StanfordNERecognizer();

        throw new IllegalArgumentException("Unknown recognizer: " + recognizerOption);
    }

    static Dataset<Article, DatasetSummary> getDataset(String dataOption) {
        if (dataOption.equalsIgnoreCase(AnnotatedDatasets.TRNews.getName()))
            return AnnotatedDatasets.TRNews;
        else if (dataOption.equalsIgnoreCase(AnnotatedDatasets.SAMPLE.getName()))
            return AnnotatedDatasets.SAMPLE;

        throw new IllegalArgumentException("Unknown dataset: " + dataOption);
    }

    static GeoTagResultMatcher.MatcherType getMatcherType(String matcherOption) {
        if (matcherOption.equalsIgnoreCase("distance"))
            return GeoTagResultMatcher.MatcherType.DISTANCE;
        else if (matcherOption.equalsIgnoreCase("bbox"))
            return GeoTagResultMatcher.MatcherType.BOUNDING_BOX;
        else
            return GeoTagResultMatcher.MatcherType.OPTIMISTIC;
    }
}
