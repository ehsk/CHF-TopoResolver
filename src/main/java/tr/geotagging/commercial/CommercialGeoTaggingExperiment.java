package tr.geotagging.commercial;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import tr.geotagging.GeoTaggingExperiment;
import tr.geotagging.recognition.GeoRecognizer;
import tr.geotagging.resolution.GeoResolver;
import tr.util.ArgUtil;
import tr.util.Config;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 4/2/2018
 * Time: 4:22 PM
 */
public abstract class CommercialGeoTaggingExperiment extends GeoTaggingExperiment {
    public CommercialGeoTaggingExperiment(GeoRecognizer geoRecognizer, GeoResolver geoResolver, boolean saveResults) {
        super(geoRecognizer, geoResolver, saveResults);
    }

    protected static ArgumentParser buildArgParser() {
        ArgumentParser argParser = ArgumentParsers.newFor("GoogleCloudGeoTagger")
                .build()
                .defaultHelp(true)
                .description("Evaluates a Google Cloud NL geotagger.");
        argParser.addArgument("-d", "--data")
                .choices(ArgUtil.getDataOptions()).setDefault("tr-news")
                .help("Specify dataset on which the experiment runs");
        argParser.addArgument("-c", "--matcher")
                .choices("distance", "bbox", "optimistic")
                .setDefault("distance")
                .help("Specify matcher that determines a correct resolution");
        argParser.addArgument("-e", "--distance")
                .type(Double.class)
                .setDefault(Config.Eval.DEFAULT_DISTANCE)
                .help("The distance, based on which a resolution can be deemed as correct");
        argParser.addArgument("-s", "--save")
                .action(Arguments.storeTrue())
                .help("Enable saving the result");
        return argParser;
    }
}
