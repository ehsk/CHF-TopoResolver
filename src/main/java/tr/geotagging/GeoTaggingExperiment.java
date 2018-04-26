package tr.geotagging;

import tr.Article;
import tr.TaggedWord;
import tr.Toponym;
import tr.dataset.Dataset;
import tr.dataset.DatasetException;
import tr.dataset.DatasetSummary;
import tr.geotagging.evaluation.EvaluationResult;
import tr.geotagging.evaluation.EvaluationResultExporter;
import tr.geotagging.evaluation.GeoTagEvaluator;
import tr.geotagging.evaluation.GeoTagResultMatcher;
import tr.geotagging.recognition.GeoRecognizer;
import tr.geotagging.resolution.GeoResolver;
import tr.util.ArgUtil;
import tr.util.Config;
import tr.util.Distance;
import tr.util.DistanceUnit;
import tr.util.math.MathUtil;
import com.google.common.base.Stopwatch;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 12/19/2016
 * Time: 10:30 PM
 */
public class GeoTaggingExperiment {
    private static final Logger logger = LogManager.getLogger(GeoTaggingExperiment.class);

    private final GeoRecognizer geoRecognizer;
    private final GeoResolver geoResolver;

    private final boolean saveResults;

    public GeoTaggingExperiment(GeoRecognizer geoRecognizer, GeoResolver geoResolver, boolean saveResults) {
        this.geoRecognizer = geoRecognizer;
        this.geoResolver = geoResolver;
        this.saveResults = saveResults;
    }

    public GeoTaggingExperiment(GeoRecognizer geoRecognizer, GeoResolver geoResolver) {
        this(geoRecognizer, geoResolver, false);
    }

    public EvaluationResult evaluate(Dataset<Article, DatasetSummary> testDataset, Distance threshold) throws DatasetException {
        return evaluate(testDataset, new GeoTagResultMatcher(threshold));
    }

    public EvaluationResult evaluate(Dataset<Article, DatasetSummary> testDataset, GeoTagResultMatcher matcher) throws DatasetException {
        final Stopwatch stopwatch = Stopwatch.createStarted();

        final GeoTagEvaluator evaluator = new GeoTagEvaluator(matcher);
        final EvaluationResult result = new EvaluationResult();

        final Optional<EvaluationResultExporter> exporter = saveResults ? newExporter(getResultsFileName(testDataset)) : Optional.empty();

        final String recognizerName = geoRecognizer.getClass().getSimpleName();
        final String resolverName = geoResolver.toString();
        final String experimentName = String.format("%s|%s", recognizerName, resolverName);
        exporter.ifPresent(ex -> ex.exportHeader(recognizerName, resolverName, testDataset.getName()));

        final DatasetSummary summary = testDataset.forEach(article -> {
            if (!article.isAnnotated())
                return;

            final List<TaggedWord> taggedWords = geoRecognizer.extract(article);
            final List<Toponym> resolvedToponyms = geoResolver.resolve(article, taggedWords);

            final EvaluationResult r = evaluator.measure(article.getToponyms(), resolvedToponyms);

            logger.info(String.format("[%s] [%s|%s] topos: %d " +
                            "correct: %d (%d) incorrect: %d notFound: %d (%d) falseFound: %d - " +
                            "errorDistance: median %.3f  mean %.3f",
                    experimentName,
                    testDataset.getName(),
                    article.getArticleId(), article.getToponyms().size(),
                    r.getTotalCorrect(), r.getApproxCorrect(), r.getIncorrect(),
                    r.getTotalNotFound(), r.getNonCoordinated(), r.getFalseFound(),
                    r.getMedianErrorDistance(), r.getMeanErrorDistance()));

            if (r.getFalseFound() > 0)
                logger.warn(String.format("[Article %s] %d POTENTIAL toponyms found", article.getArticleId(), r.getFalseFound()));

            exporter.ifPresent(ex -> ex.exportArticle(article, r));

            result.add(r);
        });

        logger.info(String.format("[%s] [%s] correct: %d (%d) incorrect: %d notFound: %d (%d) falseFound: %d",
                experimentName, testDataset,
                result.getTotalCorrect(), result.getApproxCorrect(), result.getIncorrect(),
                result.getTotalNotFound(), result.getNonCoordinated(),
                result.getFalseFound()));

        result.setActualCorrects(summary.getNumberOfToponymsWithGazetteer());

        double precision = result.getPrecision();
        double recall = result.getRecall();
        double f1Measure = MathUtil.f1Measure(precision, recall);

        logger.info(String.format("[%s] [%s] precision: %.3f recall: %.3f f-measure: %.3f",
                experimentName, testDataset,
                precision, recall, f1Measure));
        logger.info(String.format("[%s] [%s] ErrorDistance: median %.3f - mean %.3f",
                experimentName, testDataset,
                result.getMedianErrorDistance(), result.getMeanErrorDistance()));

        exporter.ifPresent(ex -> ex.exportFooter(result, summary));

        exporter.ifPresent(EvaluationResultExporter::close);

        stopwatch.stop();
        logger.info(String.format("[%s] [%s] done in %d s",
                experimentName,
                testDataset.getName(),
                stopwatch.elapsed(TimeUnit.SECONDS)));

        return result;
    }

    private Optional<EvaluationResultExporter> newExporter(String fileName) {
        try {
            Files.deleteIfExists(Paths.get(fileName));
            return Optional.of(new EvaluationResultExporter(fileName));
        } catch (IOException e) {
            logger.warn("unable to save results in experiment: " + e.getMessage());
            return Optional.empty();
        }
    }

    private String getResultsFileName(Dataset<Article, DatasetSummary> dataset) {
        return String.format("exp-results-%s-%s-%s.xml",
                geoRecognizer.getClass().getSimpleName(),
                geoResolver,
                dataset.getName());
    }

    public static void main(String[] args) {
        ArgumentParser argParser = buildArgParser();

        final Namespace ns = argParser.parseArgsOrFail(args);

        final GeoRecognizer recognizer = ArgUtil.getRecognizer(ns.getString("recognizer"));
        final GeoResolver resolver = ArgUtil.getResolver(
                ns.getString("resolver"),
                ns.getString("model"),
                ns.getDouble("threshold"));

        final Dataset<Article, DatasetSummary> dataset = ArgUtil.getDataset(ns.getString("data"));

        final GeoTagResultMatcher.MatcherType matcherType = ArgUtil.getMatcherType(ns.getString("matcher"));

        final GeoTaggingExperiment experiment = new GeoTaggingExperiment(recognizer, resolver, ns.getBoolean("save"));
        final GeoTagResultMatcher matcher = new GeoTagResultMatcher(matcherType,
                DistanceUnit.mi.of(ns.getDouble("distance")));

        try {
            experiment.evaluate(dataset, matcher);
        } catch (DatasetException e) {
            e.printStackTrace();
        }
    }

    private static ArgumentParser buildArgParser() {
        ArgumentParser argParser = ArgumentParsers.newFor("GeoTaggingExperiment")
                .build()
                .defaultHelp(true)
                .description("Evaluates a geotagger including recognizer and resolver.");
        argParser.addArgument("-g", "--recognizer")
                .choices(ArgUtil.getRecognizerOptions())
                .setDefault("ner")
                .help("Specify toponym recognition method");
        argParser.addArgument("-r", "--resolver")
                .choices(ArgUtil.getResolverOptions())
                .setDefault("fused")
                .help("Specify toponym resolution method");
        argParser.addArgument("-d", "--data")
                .choices(ArgUtil.getDataOptions()).setDefault("tr-news")
                .help("Specify dataset on which the experiment runs");
        argParser.addArgument("-c", "--matcher")
                .choices("distance", "bbox", "optimistic")
                .setDefault("distance")
                .help("Specify matcher that determines a correct resolution");
        argParser.addArgument("-m", "--model")
                .help("The model file path, required only when adaptive resolver is specified");
        argParser.addArgument("-t", "--threshold")
                .type(Double.class)
                .setDefault(Config.CHF.DEFAULT_THRESHOLD)
                .help("The threshold, required only when fused resolver is specified");
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
