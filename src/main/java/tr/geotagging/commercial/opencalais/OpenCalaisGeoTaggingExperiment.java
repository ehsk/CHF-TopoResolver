package tr.geotagging.commercial.opencalais;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import tr.Article;
import tr.dataset.Dataset;
import tr.dataset.DatasetException;
import tr.dataset.DatasetSummary;
import tr.geotagging.commercial.CommercialGeoTaggingExperiment;
import tr.geotagging.evaluation.GeoTagResultMatcher;
import tr.geotagging.recognition.NullRecognizer;
import tr.util.ArgUtil;
import tr.util.DistanceUnit;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 4/2/2018
 * Time: 3:18 PM
 */
public class OpenCalaisGeoTaggingExperiment extends CommercialGeoTaggingExperiment {
    public OpenCalaisGeoTaggingExperiment(boolean saveResults) {
        super(new NullRecognizer(), new OpenCalaisGeoResolver(), saveResults);
    }

    private OpenCalaisGeoTaggingExperiment() {
        this(true);
    }

    public static void main(String[] args) {
        ArgumentParser argParser = buildArgParser();

        final Namespace ns = argParser.parseArgsOrFail(args);

        final String matcherArg = ns.getString("matcher");
        final GeoTagResultMatcher.MatcherType matcherType = ArgUtil.getMatcherType(matcherArg);

        final Dataset<Article, DatasetSummary> dataset = ArgUtil.getDataset(ns.getString("data"));
        final OpenCalaisGeoTaggingExperiment experiment = new OpenCalaisGeoTaggingExperiment();

        try {
            experiment.evaluate(dataset, new GeoTagResultMatcher(matcherType, DistanceUnit.mi.of(ns.getDouble("distance"))));
        } catch (DatasetException e) {
            e.printStackTrace();
        }
    }

}
