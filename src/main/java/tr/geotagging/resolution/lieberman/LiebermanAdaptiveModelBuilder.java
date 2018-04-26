package tr.geotagging.resolution.lieberman;

import com.google.common.base.Stopwatch;
import hr.irb.fastRandomForest.FastRandomForest;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tr.Article;
import tr.dataset.Dataset;
import tr.dataset.DatasetException;
import tr.dataset.DatasetSummary;
import tr.geotagging.recognition.GeoRecognizer;
import tr.geotagging.recognition.StanfordNERecognizer;
import tr.util.ArgUtil;
import tr.util.Config;
import tr.util.ml.WekaUtil;
import weka.classifiers.Evaluation;
import weka.core.Instances;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 7/17/2017
 * Time: 10:13 PM
 */
public class LiebermanAdaptiveModelBuilder {
    private final Logger logger = LogManager.getLogger(getClass());

    private static final String LGL_noRECOG_wb80 = "data/adaptive/adaptive.LGL.noRECOG.wb80";
    private static final String LGL_stanfordNER_wb80 = "data/adaptive/adaptive.LGL.stanfordNER.wb80";
    private static final String CLUST_noRECOG_wb80 = "data/adaptive/adaptive.CLUST.noRECOG.wb80";
    private static final String CLUST_stanfordNER_wb80 = "data/adaptive/adaptive.CLUST.stanfordNER.wb80";
    private static final String OUR_noRECOG_wb80 = "data/adaptive/adaptive.TRNews.noRECOG.wb80";
    private static final String OUR_stanfordNER_wb80 = "data/adaptive/adaptive.TRNews.stanfordNER.wb80";

    public static class Arff {
        public static final String LGL_noRECOG_wb80 = LiebermanAdaptiveModelBuilder.LGL_noRECOG_wb80 + ".arff";
        public static final String LGL_stanfordNER_wb80 = LiebermanAdaptiveModelBuilder.LGL_stanfordNER_wb80 + ".arff";
        public static final String CLUST_noRECOG_wb80 = LiebermanAdaptiveModelBuilder.CLUST_noRECOG_wb80 + ".arff";
        public static final String CLUST_stanfordNER_wb80 = LiebermanAdaptiveModelBuilder.CLUST_stanfordNER_wb80 + ".arff";
        public static final String OUR_noRECOG_wb80 = LiebermanAdaptiveModelBuilder.OUR_noRECOG_wb80 + ".arff";
        public static final String OUR_stanfordNER_wb80 = LiebermanAdaptiveModelBuilder.OUR_stanfordNER_wb80 + ".arff";
    }

    public static class Model {
        public static final String LGL_noRECOG_wb80 = LiebermanAdaptiveModelBuilder.LGL_noRECOG_wb80 + ".model";
        public static final String LGL_stanfordNER_wb80 = LiebermanAdaptiveModelBuilder.LGL_stanfordNER_wb80 + ".model";
        public static final String CLUST_noRECOG_wb80 = LiebermanAdaptiveModelBuilder.CLUST_noRECOG_wb80 + ".model";
        public static final String CLUST_stanfordNER_wb80 = LiebermanAdaptiveModelBuilder.CLUST_stanfordNER_wb80 + ".model";
        public static final String OUR_noRECOG_wb80 = LiebermanAdaptiveModelBuilder.OUR_noRECOG_wb80 + ".model";
        public static final String OUR_stanfordNER_wb80 = LiebermanAdaptiveModelBuilder.OUR_stanfordNER_wb80 + ".model";
    }


    private String getArffFilePath(Dataset<Article, DatasetSummary> dataset, GeoRecognizer recognizer, int windowBreadth) {
        final String recognizerName = recognizer instanceof StanfordNERecognizer ? "stanfordNER" : "noRECOG";
        return String.format("%s/adaptive.%s.%s.wb%d.arff", dataset.getContainingDir(), dataset.getName(), recognizerName, windowBreadth);
    }

    private String getModelFilePath(String arffFilePath) {
        return arffFilePath.replaceFirst("\\.arff$", ".model");
    }

    public String exportToArff(Dataset<Article, DatasetSummary> dataset, GeoRecognizer recognizer) throws DatasetException, IOException {
        return exportToArff(dataset, recognizer, Config.Adaptive.DEFAULT_Wb);
    }

    private String exportToArff(Dataset<Article, DatasetSummary> dataset, GeoRecognizer recognizer, int windowBreadth) throws DatasetException, IOException {
        final LiebermanAdaptiveFeaturesBuilder featuresBuilder = new LiebermanAdaptiveFeaturesBuilder(windowBreadth, Integer.MAX_VALUE);
        final LiebermanAdaptiveFeaturesBuilder.Accumulator accumulator = featuresBuilder.accumulator();

        dataset.forEach(article -> {
            if (!article.isAnnotated())
                return;
            final Stopwatch stopwatch = Stopwatch.createStarted();
            accumulator.accept(recognizer.extract(article), article.getToponyms());
            stopwatch.stop();

            final long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            if (elapsed > 500)
                logger.warn(String.format("[%s|%s] @SLOWLOG instances created in %d ms", dataset.getName(), article.getArticleId(), elapsed));

            logger.debug(String.format("[%s|%s] instances created in %d ms", dataset.getName(), article.getArticleId(), elapsed));
        });

        final String arffFilePath = getArffFilePath(dataset, recognizer, windowBreadth);
        WekaUtil.saveDataset(accumulator.toWekaDataset(dataset.getName()), arffFilePath);

        return arffFilePath;
    }

    public String trainOn(String arffFile) throws Exception {
        return trainOn(arffFile, Config.Adaptive.DEFAULT_N_TREES, Config.Adaptive.DEFAULT_N_FEATURES);
    }

    private String trainOn(String arffFile, int numTrees, int numFeatures) throws Exception {
        final Instances dataset = WekaUtil.loadDataset(arffFile);

        FastRandomForest forest = new FastRandomForest();
        forest.setNumTrees(numTrees);
        forest.setNumFeatures(numFeatures);
        forest.setComputeImportances(true);

        forest.buildClassifier(dataset);

        Arrays.stream(forest.getFeatureImportances()).forEach(System.out::println);

        final String modelFile = getModelFilePath(arffFile);
        WekaUtil.saveClassificationModel(forest, modelFile);
        return modelFile;
    }

    private Evaluation validateOn(String arffFile, int numFolds) throws Exception {
        return validateOn(arffFile, Config.Adaptive.DEFAULT_N_TREES, Config.Adaptive.DEFAULT_N_FEATURES, numFolds);
    }

    private Evaluation validateOn(String arffFile, int numTrees, int numFeatures, int numFolds) throws Exception {
        final Instances dataset = WekaUtil.loadDataset(arffFile);

        FastRandomForest forest = new FastRandomForest();
        forest.setNumTrees(numTrees);
        forest.setNumFeatures(numFeatures);

        Evaluation evaluation = new Evaluation(dataset);
        evaluation.crossValidateModel(forest, dataset, numFolds, new Random());

        logger.info(evaluation.toSummaryString());
        logger.info("confusion matrix: " + Arrays.stream(evaluation.confusionMatrix())
                .map(Arrays::toString)
                .collect(Collectors.joining(",")));
        logger.info(String.format("precision: %.5f,%.5f", evaluation.precision(0), evaluation.precision(1)));
        logger.info(String.format("recall: %.5f,%.5f", evaluation.recall(0), evaluation.recall(1)));
        logger.info(String.format("f-measure: %.5f,%.5f", evaluation.fMeasure(0), evaluation.fMeasure(1)));

        return evaluation;
    }

    public static void main(String[] args) {
        final ArgumentParser argParser = buildArgParser();

        final Namespace ns = argParser.parseArgsOrFail(args);

        final GeoRecognizer recognizer = ArgUtil.getRecognizer(ns.getString("recognizer"));
        final Dataset<Article, DatasetSummary> dataset = ArgUtil.getDataset(ns.getString("data"));
        final int wb = ns.getInt("wb");
        final int numFolds = ns.getInt("folds");
        final String op = ns.getString("op");

        final LiebermanAdaptiveModelBuilder modelBuilder = new LiebermanAdaptiveModelBuilder();

        if (op.equalsIgnoreCase("factory")) {
            try {
                System.out.printf("Successfully exported to arff: '%s'\n", modelBuilder.exportToArff(dataset, recognizer, wb));
            } catch (DatasetException | IOException e) {
                System.out.println("oh crap! Error during factory :(");
                e.printStackTrace();
            }
        } else if (op.equalsIgnoreCase("train")) {
            try {
                modelBuilder.trainOn(modelBuilder.getArffFilePath(dataset, recognizer, wb));
            } catch (Exception e) {
                System.out.println("oh crap! Error during training :(");
                e.printStackTrace();
            }
        } else if (op.equalsIgnoreCase("validate")) {
            try {
                final Evaluation evaluation = modelBuilder.validateOn(modelBuilder.getArffFilePath(dataset, recognizer, wb), numFolds);
                System.out.println(evaluation.toSummaryString());
                System.out.println("confusion matrix: " + Arrays.stream(evaluation.confusionMatrix())
                        .map(Arrays::toString)
                        .collect(Collectors.joining(",")));
                System.out.println(String.format("precision: %.5f,%.5f", evaluation.precision(0), evaluation.precision(1)));
                System.out.println(String.format("recall: %.5f,%.5f", evaluation.recall(0), evaluation.recall(1)));
                System.out.println(String.format("f-measure: %.5f,%.5f", evaluation.fMeasure(0), evaluation.fMeasure(1)));
            } catch (Exception e) {
                System.out.println("oh crap! Error during validation :(");
                e.printStackTrace();
            }
        }
    }

    private static ArgumentParser buildArgParser() {
        ArgumentParser argParser = ArgumentParsers.newFor("LiebermanAdaptiveModel")
                .build()
                .defaultHelp(true)
                .description("Tool for running and evaluating the Adaptive method.");
        argParser.addArgument("op")
                .choices("factory", "train", "validate")
                .required(true)
                .help("Specify operation");
        argParser.addArgument("-g", "--recognizer")
                .choices(ArgUtil.getRecognizerOptions())
                .setDefault("ner")
                .help("Specify toponym recognition method");
        argParser.addArgument("-d", "--data")
                .choices(ArgUtil.getDataOptions()).setDefault("tr-news")
                .help("Specify dataset on which the model runs");
        argParser.addArgument("-w", "--wb")
                .type(Integer.class)
                .setDefault(Config.Adaptive.DEFAULT_Wb)
                .help("Window Breadth");
        argParser.addArgument("-f", "--folds")
                .type(Integer.class)
                .setDefault(Config.Eval.DEFUALT_N_FOLDS)
                .help("Number of folds for cross validation");
        return argParser;
    }
}
