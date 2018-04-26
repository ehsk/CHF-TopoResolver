package tr.geotagging.resolution.lieberman;

import tr.Article;
import tr.TaggedWord;
import tr.Toponym;
import tr.geotagging.resolution.ArticleBucket;
import tr.geotagging.resolution.DefaultGeoResolver;
import tr.util.Config;
import tr.util.ml.WekaUtil;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import hr.irb.fastRandomForest.FastRandomForest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import weka.core.Instance;

import java.util.ArrayList;
import java.util.List;

import static tr.geotagging.resolution.lieberman.LiebermanAdaptiveModelBuilder.Model.CLUST_stanfordNER_wb80;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 6/25/2017
 * Time: 12:19 AM
 */
public class LiebermanAdaptiveGeoResolver extends DefaultGeoResolver<ArticleBucket> {

    private static final Logger logger = LogManager.getLogger(LiebermanAdaptiveGeoResolver.class);

    private final String modelFile;
    private final int windowBreadth, windowDepth;
    private final FastRandomForest randomForest;

    public LiebermanAdaptiveGeoResolver(String modelFile, int windowBreadth, int windowDepth) {
        this.modelFile = modelFile;
        this.windowBreadth = windowBreadth;
        this.windowDepth = windowDepth;

        try {
            this.randomForest = WekaUtil.loadClassificationModel(modelFile);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(String.format("Unable to load model file '%s': %s", modelFile, e.getMessage()));
        }
    }

    public LiebermanAdaptiveGeoResolver(String modelFile, int windowBreadth) {
        this(modelFile, windowBreadth, Integer.MAX_VALUE);
    }

    public LiebermanAdaptiveGeoResolver(String modelFile) {
        this(modelFile, Config.Adaptive.DEFAULT_Wb);
    }

    public LiebermanAdaptiveGeoResolver() {
        this(CLUST_stanfordNER_wb80);
    }

    protected ArticleBucket newArticleBucket(Article article, List<TaggedWord> taggedWords) {
        return new ArticleBucket(windowDepth, taggedWords);
    }

    @Override
    protected List<Toponym> extract(ArticleBucket articleBucket) {
        final LiebermanAdaptiveFeaturesBuilder adaptiveFeaturesBuilder = new LiebermanAdaptiveFeaturesBuilder(windowBreadth, windowDepth);
        final Multimap<Toponym, Instance> testInstances = adaptiveFeaturesBuilder.toUnlabeledInstances(articleBucket.getTaggedWords());

        final List<Toponym> extractedToponyms = new ArrayList<>();

        testInstances.asMap().forEach((recognizedToponym, candidateInstances) ->
                candidateInstances.stream()
                        .filter(testInstance -> {
                            try {
                                return randomForest.classifyInstance(testInstance) == 1;
                            } catch (Exception e) {
                                logger.error("word %s cannot classify instance\n", recognizedToponym.getPhrase(), e);
                                return false;
                            }
                        })
                        .findAny()
                        .ifPresent(instance ->
                                extractedToponyms.add(recognizedToponym)
                        )
        );

        return extractedToponyms;
    }

    @Override
    public String toString() {
        return Files.getNameWithoutExtension(modelFile);
    }
}
