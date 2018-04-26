package tr.geotagging.evaluation;

import tr.geonames.GeoNamable;
import tr.geonames.GeoNamesEntry;
import tr.geonames.GeoNamesRepository;
import tr.util.Distance;
import tr.util.geo.GeoUtil;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 8/9/2017
 * Time: 2:22 AM
 */
public class GeoTagResultMatcher {
    public enum MatcherType {
        DISTANCE, BOUNDING_BOX, OPTIMISTIC
    }

    private final MatcherType matcherType;
    private final Distance threshold;
    private final BiFunction<GeoNamable, GeoNamable, Boolean> matchFunction;

    public GeoTagResultMatcher(MatcherType matcherType, Distance threshold) {
        this.matcherType = matcherType;
        this.threshold = threshold;
        this.matchFunction = buildFunction();
    }

    public GeoTagResultMatcher(Distance threshold) {
        this(MatcherType.DISTANCE, threshold);
    }

    private BiFunction<GeoNamable, GeoNamable, Boolean> buildFunction() {
        switch (matcherType) {
            case DISTANCE:
                return distance(threshold);
            case BOUNDING_BOX:
                return boundingBox(threshold);
            case OPTIMISTIC:
                return optimistic(threshold);
            default:
                throw new IllegalStateException("Unknown enum value!");
        }
    }

    public<G extends GeoNamable, H extends GeoNamable> boolean check(G extracted, H groundTruth) {
        return matchFunction.apply(extracted, groundTruth);
    }

    private final GeoNamesRepository geoNamesRepository = new GeoNamesRepository();

    private BiFunction<GeoNamable, GeoNamable, Boolean> distance(Distance threshold) {
        return (extracted, groundTruth) -> {
            if (extracted.getGeonameId() == null)
                return matchDistance(extracted, groundTruth, threshold);
            else
                return matchGeoNames(extracted, groundTruth, threshold);
        };
    }

    private BiFunction<GeoNamable, GeoNamable, Boolean> boundingBox(Distance threshold) {
        return (extracted, groundTruth) ->
                matchBoundingBox(extracted, groundTruth)
                        .orElseGet(() -> distance(threshold).apply(extracted, groundTruth));
    }

    private BiFunction<GeoNamable, GeoNamable, Boolean> optimistic(Distance threshold) {
        return (extracted, groundTruth) -> {
            final Boolean distanceResult = distance(threshold).apply(extracted, groundTruth);

            if (!distanceResult) {
                return matchBoundingBox(extracted, groundTruth).orElse(false);
            } else {
                return true;
            }

        };
    }

    private boolean matchGeoNames(GeoNamable resolved, GeoNamable groundTruth, Distance threshold) {
        boolean isCorrect = groundTruth.getGeonameId() != null && groundTruth.getGeonameId().equals(resolved.getGeonameId());

        if (!isCorrect) {
            final GeoNamesEntry resolvedGeoNamesEntry = geoNamesRepository.load(resolved.getGeonameId());

            final Distance distance = GeoUtil.distance(groundTruth.toCoordinate(), resolvedGeoNamesEntry.toCoordinate());
            return distance.le(threshold);
        }

        return true;
    }

    private boolean matchDistance(GeoNamable resolved, GeoNamable groundTruth, Distance threshold) {
        return GeoUtil.distance(groundTruth.toCoordinate(), resolved.toCoordinate()).le(threshold);
    }

    private Optional<Boolean> matchBoundingBox(GeoNamable resolved, GeoNamable groundTruth) {
        if (groundTruth.getGeonameId() == null)
                return Optional.empty();

        return geoNamesRepository.getBoundingBox(groundTruth.getGeonameId()).map(bbox -> bbox.contains(resolved));
    }
}
