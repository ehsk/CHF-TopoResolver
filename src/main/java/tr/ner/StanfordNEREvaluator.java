package tr.ner;

import tr.Article;
import tr.NamedEntityTag;
import tr.TaggedWord;
import tr.Toponym;
import tr.dataset.Dataset;
import tr.dataset.DatasetException;
import tr.dataset.DatasetSummary;
import tr.util.ml.ConfusionMatrix;
import tr.util.nlp.TaggedWordUtil;
import com.google.common.base.Stopwatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tr.util.nlp.StanfordNER;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: user
 * Date: 10/12/2016
 * Time: 10:57 PM
 */
public class StanfordNEREvaluator {

    private static final Logger logger = LogManager.getLogger(StanfordNEREvaluator.class);

    private final StanfordNER tagger = new StanfordNER();

    void evaluateText(String text) {
        logger.info("text evaluated");
        final List<TaggedWord> taggedWords = tagger.tag(text);
        for (TaggedWord taggedWord : taggedWords) {
            logger.debug("at {}-{} {}/{}", taggedWord.getStart(), taggedWord.getEnd(), taggedWord.getPhrase(), taggedWord.getTag());
        }
    }

    public void evaluate(final Dataset<Article, DatasetSummary> dataset) throws DatasetException {
        final Stopwatch stopwatch = Stopwatch.createStarted();

        final ConfusionMatrix matrix = new ConfusionMatrix();

        final Map<NamedEntityTag, Integer> missedCountMap = new HashMap<>();

        final DatasetSummary summary = dataset.forEach(article -> {
            final ArrayList<Toponym> annotatedToponyms = new ArrayList<>(article.getToponyms());

            final List<TaggedWord> taggedWords = tagger.tag(article.getText());

            final List<TaggedWord> extractedToponyms = TaggedWordUtil.filterLocations(taggedWords);

            for (TaggedWord extractedToponym : extractedToponyms) {

                final Optional<Toponym> foundToponym = annotatedToponyms.stream().filter(extractedToponym::match).findFirst();

                if (foundToponym.isPresent()) {
                    matrix.accept(true, true);
                    annotatedToponyms.remove(foundToponym.get());
                } else {
                    matrix.accept(false, true);
                }
            }

            annotatedToponyms.forEach(t -> matrix.accept(true, false));

            final int prevTotalMissedPerTag = missedCountMap.values().stream().mapToInt(Integer::intValue).sum();

            int k = 0;
            for (Toponym toponym : annotatedToponyms) {

                boolean isToponymContainer = false;
                for (; k < taggedWords.size(); k++) {
                    TaggedWord taggedWord = taggedWords.get(k);

                    if (taggedWord.getEnd() < toponym.getStart())
                        continue;

                    final String toponymPhrase = toponym.getPhrase().replaceAll("’", "'");

                    if (toponymPhrase.startsWith(taggedWord.getPhrase())
                            && toponym.getStart() == taggedWord.getStart()) {
                        logger.debug("[evaluate] [{}] missed word {} / {} at {} tagged as {}", article.getArticleId(), taggedWord.getPhrase(), toponymPhrase, taggedWord.getStart(), taggedWord.getTag());
                        missedCountMap.put(taggedWord.getTag(), missedCountMap.getOrDefault(taggedWord.getTag(), 0) + 1);
                        isToponymContainer = true;
                    } else if (isToponymContainer) {
                        if (toponymPhrase.contains(taggedWord.getPhrase()))
                            logger.debug("[evaluate] [{}] missed word {} / {} at {} tagged as {}", article.getArticleId(), taggedWord.getPhrase(), toponymPhrase, taggedWord.getStart(), taggedWord.getTag());
                        else
                            break;
                    } else if (taggedWord.getPhrase().contains(toponymPhrase)
                            && taggedWord.getStart() + taggedWord.getPhrase().indexOf(toponymPhrase) == toponym.getStart()) {
                        logger.debug("[evaluate] [{}] missed word {} / {} at {} tagged as {}", article.getArticleId(), taggedWord.getPhrase(), toponymPhrase, taggedWord.getStart(), taggedWord.getTag());
                        missedCountMap.put(taggedWord.getTag(), missedCountMap.getOrDefault(taggedWord.getTag(), 0) + 1);
                        break;
                    }

                }

            }

            final int totalMissedPerTag = missedCountMap.values().stream().mapToInt(Integer::intValue).sum();
            if (totalMissedPerTag - prevTotalMissedPerTag != annotatedToponyms.size())
                logger.warn("[evaluate] [{}] {} missed toponyms and numbers don't add up {} <> {}", article.getArticleId(), annotatedToponyms.size(), totalMissedPerTag, matrix.getFN());
        });

        stopwatch.stop();

        logger.info("[dataset summary] {}", summary);
        logger.info(String.format("[evaluate] correct: %d | incorrect: %d | missed: %d | total: %d", matrix.getTP(), matrix.getFP(), matrix.getFN(), matrix.getTotal()));
        logger.info(String.format("[evaluate] precision: %.3f | recall: %.3f | f1-measure: %.3f", matrix.getPrecision(), matrix.getRecall(), matrix.getF1Measure()));
        logger.info("[evaluate] missed location tags: " + missedCountMap.entrySet().stream().map(e -> e.getKey() + "->" + e.getValue()).collect(Collectors.joining(",")));
        logger.info("evaluation done in {} s", stopwatch.elapsed(TimeUnit.SECONDS));
    }
}
