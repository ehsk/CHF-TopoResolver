package tr.geotagging.resolution.contextfusion;

import tr.Article;
import tr.TaggedWord;
import tr.Toponym;
import tr.geotagging.resolution.spatialset.SpatialHierarchySetResolver;
import tr.geotagging.resolution.contextbound.ContextBoundResolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tr.util.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 6/23/2017
 * Time: 6:09 PM
 */
public class CHFResolver extends SpatialHierarchySetResolver {

    private final Logger logger = LogManager.getLogger(getClass());

    private final double scoreThreshold;

    private final ContextBoundResolver contextBoundResolver = new ContextBoundResolver();

    public CHFResolver() {
        this(Config.CHF.DEFAULT_THRESHOLD);
    }

    public CHFResolver(double scoreThreshold) {
        this.scoreThreshold = scoreThreshold;
        logger.debug("resolution is set up with threshold {}", this.scoreThreshold);
    }

    @Override
    public List<Toponym> resolve(Article article, List<TaggedWord> taggedWords) {
        final List<Toponym> setcoverResolvedToponyms = super.resolve(article, taggedWords);
        final Map<Integer, Toponym> setcoverResolvedMap = setcoverResolvedToponyms.stream().collect(Collectors.toMap(Toponym::getStart, v -> v));

        final List<Toponym> yrResolvedToponyms = contextBoundResolver.resolve(article, taggedWords)
                .stream()
                .filter(t -> t.getScore() >= scoreThreshold)
                .collect(Collectors.toList());
        final Map<Integer, Toponym> yrResolvedMap = yrResolvedToponyms.stream().collect(Collectors.toMap(Toponym::getStart, v -> v));

        final List<Toponym> fusedToponyms = new ArrayList<>();

        int disagreeCount = 0, onlyYR = 0;

        for (Toponym setcoverToponym : setcoverResolvedToponyms) {
            final Toponym yrToponym = yrResolvedMap.get(setcoverToponym.getStart());
            if (yrToponym == null || setcoverToponym.equals(yrToponym)) {
                fusedToponyms.add(setcoverToponym);
            } else {
                fusedToponyms.add(yrToponym);
                disagreeCount++;
            }
        }

        for (Toponym yrToponym : yrResolvedToponyms) {
            if (!setcoverResolvedMap.containsKey(yrToponym.getStart())) {
                fusedToponyms.add(yrToponym);
                onlyYR++;
            }
        }

        logger.info(String.format("[Article %s] %d topos = %d from YR (%d/disagree + %d/onlyYR) and %d from setcover",
                article.getArticleId(), fusedToponyms.size(),
                disagreeCount + onlyYR, disagreeCount, onlyYR,
                fusedToponyms.size() - disagreeCount - onlyYR));

        return fusedToponyms;
    }

    @Override
    public String toString() {
        return "Fused-" + scoreThreshold + "-Resolver";
    }
}
