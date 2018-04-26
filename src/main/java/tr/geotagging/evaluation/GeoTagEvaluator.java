package tr.geotagging.evaluation;

import com.google.common.collect.ArrayListMultimap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tr.TaggedWord;
import tr.Toponym;
import tr.util.Distance;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 7/28/2017
 * Time: 4:33 AM
 */
public class GeoTagEvaluator {
    private final Logger logger = LogManager.getLogger(getClass());

    private final GeoTagResultMatcher resultMatcher;

    public GeoTagEvaluator(GeoTagResultMatcher resultMatcher) {
        this.resultMatcher = resultMatcher;
    }

    public GeoTagEvaluator(Distance threshold) {
        this(new GeoTagResultMatcher(GeoTagResultMatcher.MatcherType.DISTANCE, threshold));
    }

    public EvaluationResult measure(List<Toponym> expectedToponyms, List<Toponym> extractedToponyms) {
        final Map<Integer, Toponym> toposByOffset = expectedToponyms.stream().collect(Collectors.toMap(TaggedWord::getStart, v -> v));

        final ArrayListMultimap<Integer, Toponym> extractedToposByOffset = ArrayListMultimap.create();
        for (Toponym extracted : extractedToponyms) {
            extractedToposByOffset.put(extracted.getStart(), extracted);
        }

        final EvaluationResult result = new EvaluationResult();

        for (Toponym groundTruth : expectedToponyms) {
            if (!groundTruth.hasCoordinate())
                continue;

            final List<Toponym> extractedList = extractedToposByOffset.get(groundTruth.getStart());
            if (!extractedList.isEmpty()) {
                int incorrect = 0, nonCoordinated = 0;
                boolean correctFound = false;
                for (Toponym extracted : extractedList) {
                    if (extracted.getGeonameId() == null && !extracted.hasCoordinate()) {
                        nonCoordinated++;
                        continue;
                    }

                    boolean isCorrectIntrepretation = resultMatcher.check(extracted, groundTruth);
                    result.acceptErrorDistance(extracted, groundTruth);

                    if (isCorrectIntrepretation) {
                        if (extracted.getPhrase().equalsIgnoreCase(groundTruth.getPhrase()))
                            result.acceptCorrect(extracted);
                        else
                            result.acceptApproxCorrect(extracted);
                        correctFound = true;
                        break;
                    } else {
                        result.acceptIncorrect(extracted);
                        incorrect++;
                    }
                }

                if (!correctFound) {
                    if (incorrect > 0)
                        result.incrIncorrect();
                    else if (nonCoordinated > 0)
                        result.incrNonCoordinated();
                }
            } else
                result.incrNotFound();

        }


        extractedToposByOffset.asMap().forEach((start, extractedList) -> {
            final Toponym groundTruth = toposByOffset.get(start);

            if (groundTruth == null) {
                extractedList.stream().findFirst().ifPresent(extracted -> {
                    logger.warn(String.format("POTENTIAL toponym at %d (%s -> %s)", start, extracted.getPhrase(),
                            String.join(",", extractedList.stream().map(t ->
                                    Optional.ofNullable(t.getGeonameId())
                                            .map(Object::toString)
                                            .orElse(String.format("(%.5f,%.5f)", t.getLatitude(), t.getLongitude()))).collect(Collectors.toList()))));
                    result.acceptFalseFound(extracted);
                });
            }
        });

        return result;
    }

}
