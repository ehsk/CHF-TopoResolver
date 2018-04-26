package tr.geotagging;

import tr.Article;
import tr.TaggedWord;
import tr.Toponym;
import tr.dataset.Dataset;
import tr.dataset.DatasetException;
import tr.dataset.DatasetSummary;
import tr.geotagging.commercial.googlenl.GoogleNLGeoResolver;
import tr.geotagging.commercial.opencalais.OpenCalaisGeoResolver;
import tr.geotagging.commercial.placemaker.YahooPlacemakerGeoResolver;
import tr.geotagging.evaluation.GeoTagResultMatcher;
import tr.geotagging.recognition.GeoRecognizer;
import tr.geotagging.recognition.MockRecognizer;
import tr.geotagging.recognition.NullRecognizer;
import tr.geotagging.recognition.StanfordNERecognizer;
import tr.geotagging.resolution.GeoResolver;
import tr.geotagging.resolution.contextbound.ContextBoundResolver;
import tr.geotagging.resolution.contextbound.ContextBoundInhHypothesisResolver;
import tr.geotagging.resolution.contextbound.ContextBoundNearbyHypothesisResolver;
import tr.geotagging.resolution.contextbound.PreliminaryContextBoundGeoResolver;
import tr.geotagging.resolution.contextfusion.CHFResolver;
import tr.geotagging.resolution.lieberman.LiebermanAdaptiveGeoResolver;
import tr.geotagging.resolution.spatialset.SpatialHierarchySetResolver;
import tr.util.ArgUtil;
import tr.util.DistanceUnit;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 5/31/2017
 * Time: 12:47 PM
 */
public class GeoTagger {

    enum Resolver {
        SpatialHierarchySets(SpatialHierarchySetResolver.class),
        ContextBound(ContextBoundResolver.class),
        CBPreliminary(PreliminaryContextBoundGeoResolver.class),
        CBInhHypothesis(ContextBoundInhHypothesisResolver.class),
        CBNearbyHypothesis(ContextBoundNearbyHypothesisResolver.class),
        ContextHierarchyFusion(CHFResolver.class),
        Adaptive(LiebermanAdaptiveGeoResolver.class);

        private final Class<? extends GeoResolver> resolverType;

        Resolver(Class<? extends GeoResolver> resolverType) {
            this.resolverType = resolverType;
        }

        private GeoResolver newInstance() throws IllegalAccessException, InstantiationException {
            return this.resolverType.newInstance();
        }
    }

    enum CommercialResolver {
        Google(GoogleNLGeoResolver.class),
        OpenCalais(OpenCalaisGeoResolver.class),
        Yahoo(YahooPlacemakerGeoResolver.class);

        private final Class<? extends GeoResolver> resolverType;

        CommercialResolver(Class<? extends GeoResolver> resolverType) {
            this.resolverType = resolverType;
        }

        private GeoResolver newInstance() throws IllegalAccessException, InstantiationException {
            return this.resolverType.newInstance();
        }
    }

    private final GeoRecognizer geoRecognizer;
    private final GeoResolver geoResolver;
    private final StanfordNERecognizer neRecognizer = new StanfordNERecognizer();

    public GeoTagger(Resolver resolver) {
        this.geoRecognizer = new StanfordNERecognizer();

        try {
            this.geoResolver = resolver.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new IllegalArgumentException("Unknown issue occurred during building resolver", e);
        }
    }

    public GeoTagger(CommercialResolver commercialResolver) {
        this.geoRecognizer = new NullRecognizer();
        try {
            this.geoResolver = commercialResolver.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new IllegalArgumentException("Unknown issue occurred during building resolver", e);
        }
    }

    public GeoTagger() {
        this(Resolver.ContextHierarchyFusion);
    }

    public GeoTagger(double threshold) {
        this.geoRecognizer = new StanfordNERecognizer();
        this.geoResolver = new CHFResolver(threshold);
    }

    public GeoTagger(String modelFile, int windowBreadth) {
        this.geoResolver = new LiebermanAdaptiveGeoResolver(modelFile, windowBreadth);
        this.geoRecognizer = new StanfordNERecognizer();
    }

    public GeoTagger(String modelFile) {
        this.geoResolver = new LiebermanAdaptiveGeoResolver(modelFile);
        this.geoRecognizer = new StanfordNERecognizer();
    }

    private Article newDummyArticle(String text) {
        final Article article = new Article();
        article.setText(text);
        article.setArticleId("API");
        article.setAnnotated(false);
        article.setSource("<unk>");
        return article;
    }

    public List<TaggedWord> recognizeToponyms(String text) {
        return this.neRecognizer.extract(newDummyArticle(text));
    }

    public List<Toponym> resolveToponyms(String text) {
        final Article dummyArticle = newDummyArticle(text);
        final List<TaggedWord> taggedWords = this.geoRecognizer.extract(dummyArticle);
        return this.geoResolver.resolve(dummyArticle, taggedWords);
    }

    public List<Toponym> resolveToponyms(String text, List<TaggedWord> taggedWords) {
        return this.geoResolver.resolve(newDummyArticle(text), taggedWords);
    }

    public static void main(String[] args) {
        GeoTagger geoTagger = new GeoTagger();
        geoTagger.resolveToponyms("");

        final String resolverHelp = "resolver can be: setcover | fused | yr | yr-pre | yr-inh | yr-near | adaptive";
        final String datasetHelp = "dataset can be: LGL | CLUST | our | our_subset[1-9]";
        final String matcherHelp = "matcher can be: distance | bbox | opt";

        if (args.length < 4) {
            System.out.println("The following parameters required: [recognizer] [resolver] [dataset] [matcher] [additional arguments]");
            System.out.println(resolverHelp);
            System.out.println(datasetHelp);
            System.out.println(matcherHelp);
            System.exit(1);
        }

        if (!args[1].toLowerCase().matches("(setcover|yr(-(pre|inh|near))?|fused|adaptive)")) {
            System.out.println(resolverHelp);
            System.exit(1);
        }

        if (!args[3].toLowerCase().matches("(distance|bbox|opt)")) {
            System.out.println(matcherHelp);
            System.exit(1);
        }

        String modelFile = "";
        if (!args[0].toLowerCase().matches("(off|stanford)")) {
            if (args[1].equalsIgnoreCase("adaptive") && Files.exists(Paths.get(args[0]))) {
                modelFile = args[0];
                args[0] = modelFile.toLowerCase().contains("stanfordNER") ? "stanford" : "off";
            } else {
                System.exit(1);
            }
        }

        final GeoRecognizer recognizer = args[0].equalsIgnoreCase("off") ? new MockRecognizer() : new StanfordNERecognizer();

        final GeoResolver resolver;
        if (args[1].equalsIgnoreCase("setcover"))
            resolver = new SpatialHierarchySetResolver();
        else if (args[1].equalsIgnoreCase("yr-pre"))
            resolver = new PreliminaryContextBoundGeoResolver();
        else if (args[1].equalsIgnoreCase("yr-inh"))
            resolver = new ContextBoundInhHypothesisResolver();
        else if (args[1].equalsIgnoreCase("yr-near"))
            resolver = new ContextBoundNearbyHypothesisResolver();
        else if (args[1].equalsIgnoreCase("yr"))
            resolver = new ContextBoundResolver();
        else if (args[1].equalsIgnoreCase("adaptive")) {
            final int wb;
            final Matcher matcher = Pattern.compile("\\.wb(\\d+)\\.", Pattern.CASE_INSENSITIVE).matcher(modelFile);
            if (matcher.find())
                wb = Integer.valueOf(matcher.group(1));
            else {
                System.out.println("unable to extract window breadth from model file name");
                System.exit(1);
                wb = 0;
            }

            resolver = new LiebermanAdaptiveGeoResolver(modelFile, wb, Integer.MAX_VALUE);
        } else {
            if (args.length < 5) {
                System.out.println("fused resolver requires a score threshold");
                System.exit(1);
            } else if (!args[4].matches("\\d*(\\.\\d+)?")) {
                System.out.println("score threshold for fused resolver must be a number: '" + args[4] + "'");
                System.exit(1);
            }

            resolver = new CHFResolver(Double.valueOf(args[4]));
        }

        final GeoTagResultMatcher.MatcherType matcherType;
        if (args[3].equalsIgnoreCase("distance"))
            matcherType = GeoTagResultMatcher.MatcherType.DISTANCE;
        else if (args[3].equalsIgnoreCase("bbox"))
            matcherType = GeoTagResultMatcher.MatcherType.BOUNDING_BOX;
        else
            matcherType = GeoTagResultMatcher.MatcherType.OPTIMISTIC;


        try {
            final Dataset<Article, DatasetSummary> dataset = ArgUtil.getDataset(args[2]);
//            final GeoTagResultMatcher matcher = new GeoTagResultMatcher(matcherType, DistanceUnit.km.of(161));
            final GeoTagResultMatcher matcher = new GeoTagResultMatcher(matcherType, DistanceUnit.mi.of(10));

            final GeoTaggingExperiment experiment = new GeoTaggingExperiment(recognizer, resolver, true);

            experiment.evaluate(dataset, matcher);
        } catch (IllegalArgumentException e) {
            System.out.println(datasetHelp);
            System.exit(1);
        } catch (DatasetException e) {
            e.printStackTrace();
        }
    }
}
