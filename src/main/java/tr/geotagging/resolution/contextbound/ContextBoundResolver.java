package tr.geotagging.resolution.contextbound;

import tr.Article;
import tr.NamedEntityTag;
import tr.TaggedWord;
import tr.Toponym;
import tr.geonames.GeoNamesEntry;
import tr.geonames.GeoNamesLevel;
import tr.geonames.GeoNamesUtil;
import tr.geotagging.resolution.ArticleBucket;
import tr.geotagging.resolution.DefaultGeoResolver;
import tr.geotagging.resolution.GeoCandidateEntry;
import tr.util.Config;
import tr.util.DistanceFunction;
import tr.util.math.MathUtil;
import tr.util.math.statistics.SummaryStat;
import tr.util.tuple.Tuple2;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of the following paper:
 * Geotagging Named Entities in News and Online Documents
 * By Jiangwei Yu and Davood Rafiei
 *
 * @author ehsan
 *         Date: 12/18/2016
 *         Time: 4:48 PM
 * @link http://dl.acm.org/citation.cfm?id=2983795
 */
public class ContextBoundResolver extends DefaultGeoResolver<ContextBoundResolver.YRArticleBucket> {
    protected final int k;
    private final DistanceFunction<TaggedWord> dm = new IndexDistanceFunction();

    private final Logger logger = LogManager.getLogger(getClass());


    ContextBoundResolver(int k) {
        this.k = k;
    }

    public ContextBoundResolver() {
        this(Integer.MAX_VALUE);
    }

    @Override
    protected YRArticleBucket newArticleBucket(Article article, List<TaggedWord> taggedWords) {
        return new YRArticleBucket(taggedWords);
    }

    @Override
    protected List<Toponym> extract(YRArticleBucket articleBucket) {
        final Map<TaggedWord, Toponym> disambiguatedMap = disambiguateLocations(articleBucket);
        articleBucket.setDisambiguatedMap(disambiguatedMap);

        return detectGeoTags(articleBucket);
    }

    protected List<Toponym> detectGeoTags(final YRArticleBucket articleBucket) {

        final Map<TaggedWord, List<Toponym>> geotags = new HashMap<>();

        YRArticleBucket bucket = articleBucket;

        int revisedCount = 0;
        for (int i = 1; i <= Config.CBH.MAX_ITERATIONS; i++) {
            for (GeoNamesLevel level : GeoNamesLevel.getHierarchyLevels()) {

                final Table<String, Long, Double> inheritProbModel = calcInheritProbModel(level, bucket);
                final Table<String, GeoCandidateEntry, Double> nearProbModel = calcNearProbModel(level, bucket);

                for (TaggedWord taggedWord : bucket.getRecognizedToponyms()) {

                    final List<GeoCandidateEntry> candidateEntries = bucket.getCandidates(taggedWord);

                    Tuple2<Double, List<GeoNamesEntry>> selected = new Tuple2<>(0D, new ArrayList<>());

                    if (candidateEntries.size() == 1) {
                        selected = new Tuple2<>(1D, Stream.of(candidateEntries.get(0).getGeoNamesCandid()).collect(Collectors.toList()));
                    } else {
                        final double H = calcKLDivergence(level, taggedWord, nearProbModel, candidateEntries);

                        for (GeoCandidateEntry candidateEntry : candidateEntries) {
                            final Optional<GeoNamesEntry> constituentTerm = candidateEntry.getEntryAt(level);
                            if (!constituentTerm.isPresent())
                                continue;

                            double J = 1 - H / Math.log(candidateEntries.size());
                            double Ph = inheritProbModel.get(taggedWord.getPhrase().toLowerCase(), constituentTerm.get().getGeonameId());
                            double Pn = nearProbModel.get(taggedWord.getPhrase().toLowerCase(), candidateEntry);
                            double p = J * Pn + (1 - J) * Ph;
                            if (selected.get_1() < p) {
                                selected = new Tuple2<>(p, Stream.of(candidateEntry.getGeoNamesCandid()).collect(Collectors.toList()));
                            } else if (selected.get_1() == p && p > 0) {
                                selected.get_2().add(candidateEntry.getGeoNamesCandid());
                            }
                        }
                    }

                    if (!selected.get_2().isEmpty()) {
                        final Double score = selected.get_1();
                        geotags.put(taggedWord, selected.get_2().stream()
                                .sorted(Comparator.comparingLong(GeoNamesEntry::getPopulation).reversed())
                                .findFirst()
                                .map(ge -> Stream.of(new Toponym(taggedWord, ge, score)).collect(Collectors.toList()))
                                .get());
                    }
                }
            }

            final Set<TaggedWord> changedList = new HashSet<>();

            final int iter = i;
            final Map<TaggedWord, Toponym> disambiguatedMap = bucket.disambiguatedMap;
            geotags.forEach((taggedWord, estimatedTopos) -> {
                Toponym oldEst = disambiguatedMap.get(taggedWord);

                if (oldEst == null ||
                        estimatedTopos.stream().noneMatch(t -> t.getGeonameId().equals(oldEst.getGeonameId()))) {
                    changedList.add(taggedWord);
                    logger.info("At {} - {}/{} modified from {} to {}", iter,
                            taggedWord.getPhrase(), taggedWord.getIndex(),
                            oldEst == null ? "-" : disambiguatedMap.get(taggedWord).getGeonameId(),
                            estimatedTopos.get(0).getGeonameId());
                }
            });

            revisedCount += changedList.size();

            if (changedList.isEmpty())
                break;
            else
                logger.info("At {} - {} of {} revised", i, changedList.size(), geotags.size());

            bucket = bucket.clone();

            final Map<TaggedWord, Toponym> newDisambiguatedMap = new HashMap<>();
            geotags.forEach((taggedWord, estimatedTopos) -> newDisambiguatedMap.put(taggedWord, estimatedTopos.get(0)));
            bucket.setDisambiguatedMap(newDisambiguatedMap);
        }

        if (revisedCount > 0)
            logger.info("{} of {} totally revised", revisedCount, geotags.size());

        return geotags.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }

    protected double calcKLDivergence(GeoNamesLevel level, TaggedWord taggedWord, Table<String, GeoCandidateEntry, Double> nearProbModel, List<GeoCandidateEntry> candidateEntries) {
        return candidateEntries.stream()
                .filter(candidateEntry -> candidateEntry.getHierarchyMap().containsKey(level))
                .mapToDouble(candidateEntry -> nearProbModel.get(taggedWord.getPhrase().toLowerCase(), candidateEntry))
                .filter(Pn -> Pn != 0)
                .reduce(0.0, (result, Pn) -> result - Pn * Math.log(Pn));
    }

    private Table<String, GeoCandidateEntry, Double> calcNearProbModel(GeoNamesLevel geoNamesLevel, YRArticleBucket articleBucket) {

        Table<String, GeoCandidateEntry, Double> nearProbModel = HashBasedTable.create();

        for (TaggedWord taggedWord : articleBucket.getRecognizedToponyms()) {

            if (nearProbModel.containsRow(taggedWord.getPhrase().toLowerCase()))
                continue;

            final List<GeoCandidateEntry> candidates = articleBucket.getCandidates(taggedWord);
            SummaryStat inverseDEStat = new SummaryStat();

            for (GeoCandidateEntry candidate : candidates) {
                final Optional<GeoNamesEntry> geoNamesEntry = candidate.getEntryAt(geoNamesLevel);
                if (!geoNamesEntry.isPresent())
                    continue;

                final Set<TaggedWord> allMentions = articleBucket.getSurfaceAndIndirectMentions(geoNamesEntry.get());

                SummaryStat DEStat = new SummaryStat();

                for (TaggedWord mi : articleBucket.getSurfaceMentions(taggedWord.getPhrase().toLowerCase())) {
                    for (TaggedWord mj : allMentions) {
                        if (mj.equals(mi))
                            continue;

                        DEStat.accept(dm.applyAsDouble(mi, mj));
                    }
                }

                final double inverseDE = DEStat.getCount() > 0 ? (1.0 / DEStat.getMin()) : 0;
                inverseDEStat.accept(inverseDE);
                nearProbModel.put(taggedWord.getPhrase().toLowerCase(), candidate, inverseDE);
            }

            for (GeoCandidateEntry candidate : candidates) {
                final Double inverseDE = Optional.ofNullable(nearProbModel.get(taggedWord.getPhrase().toLowerCase(), candidate)).orElse(0D);
                nearProbModel.put(taggedWord.getPhrase().toLowerCase(), candidate, MathUtil.safeDivide(inverseDE, inverseDEStat.getSum()));
            }
        }

        return nearProbModel;
    }

    private Map<TaggedWord, Toponym> disambiguateLocations(final YRArticleBucket articleBucket) {
        final Map<TaggedWord, Toponym> disambiguatedMap = new HashMap<>();

        final Map<String, Collection<GeoCandidateEntry>> flatCandidateMap = articleBucket.getCandidateMap().asMap();

        for (Map.Entry<String, Collection<GeoCandidateEntry>> flatEntry : flatCandidateMap.entrySet()) {
            final Set<TaggedWord> MS = articleBucket.getSurfaceMentions(flatEntry.getKey());

            final Collection<GeoCandidateEntry> candidateEntries = flatEntry.getValue();

            if (candidateEntries.size() == 1) {
                for (TaggedWord word : MS) {
                    candidateEntries
                            .stream()
                            .findFirst()
                            .ifPresent(candidateEntry ->
                                    disambiguatedMap.put(word, new Toponym(word, candidateEntry.getGeoNamesCandid())));
                }
                continue;
            }

            Tuple2<Double, List<GeoNamesEntry>> selected = new Tuple2<>(0D, new ArrayList<>());

            for (GeoCandidateEntry candidateEntry : candidateEntries) {
                double confidence = 0;

                for (GeoNamesEntry c : candidateEntry) {
                    if (!GeoNamesLevel.isHierarchyLevel(c.getLevel()) && c.getLevel() != GeoNamesLevel.CONTINENT)
                        continue;

                    final Set<TaggedWord> MC = articleBucket.getSurfaceMentions(c);

                    for (TaggedWord mc : MC) {
                        double score = 0;
                        for (TaggedWord ms : MS) {
                            if (ms.equals(mc))
                                continue;

                            if (c.getLevel() != GeoNamesLevel.COUNTRY && c.getLevel() != GeoNamesLevel.CONTINENT)
                                if  (ms.getPhrase().equalsIgnoreCase(mc.getPhrase()))
                                    continue;

                            score = Math.max(score, 1.0 / dm.applyAsDouble(mc, ms));
                        }

                        confidence += score;
                    }
                }

                if (selected.get_1() < confidence) {
                    selected = new Tuple2<>(confidence, Stream.of(candidateEntry.getGeoNamesCandid()).collect(Collectors.toList()));
                } else if (selected.get_1() == confidence) {
                    selected.get_2().add(candidateEntry.getGeoNamesCandid());
                }
            }

            if (!selected.get_2().isEmpty()) {
                selected.get_2().sort(Comparator.comparingLong(GeoNamesEntry::getPopulation).reversed());
                final Double score = selected.get_1();

                for (TaggedWord word : MS) {
                    selected.get_2()
                            .stream()
                            .findFirst()
                            .ifPresent(geoNamesEntry ->
                                    disambiguatedMap.put(word, new Toponym(word, geoNamesEntry, score)));
                }
            }
        }

        return disambiguatedMap;
    }

    private Table<String, Long, Double> calcInheritProbModel(GeoNamesLevel geoNamesLevel, YRArticleBucket articleBucket) {

        Table<String, Long, Double> inheritModel = HashBasedTable.create();

        for (TaggedWord taggedWord : articleBucket.getRecognizedToponyms()) {

            LongSummaryStatistics totalStats = new LongSummaryStatistics();
            final Map<Long, Long> freqMap = new HashMap<>();

            for (GeoCandidateEntry candidateEntry : articleBucket.getCandidates(taggedWord)) {
                final Optional<GeoNamesEntry> constituency = candidateEntry.getEntryAt(geoNamesLevel);

                if (!constituency.isPresent())
                    continue;

                if (freqMap.containsKey(constituency.get().getGeonameId()))
                    continue;

                final Set<TaggedWord> allMentions = articleBucket.getSurfaceAndIndirectMentions(constituency.get());
                allMentions.removeIf(w -> w.equals(taggedWord));

                final long tf = allMentions.size();
                freqMap.put(constituency.get().getGeonameId(), tf);

                totalStats.accept(tf);
            }

            freqMap.forEach((constituentId, tf) ->
                    inheritModel.put(taggedWord.getPhrase().toLowerCase(), constituentId,
                            MathUtil.safeDivide(tf, totalStats.getSum())));
        }

        return inheritModel;
    }

    @Override
    public String toString() {
        return "ContextBoundHypotheses";
    }

    class YRArticleBucket extends ArticleBucket implements Cloneable {
        private final Table<GeoNamesLevel, String, Set<TaggedWord>> indirectMentionTable = HashBasedTable.create();
        Map<TaggedWord, Toponym> disambiguatedMap;

        YRArticleBucket(List<TaggedWord> taggedWords) {
            super(k, taggedWords);
        }

        private YRArticleBucket(Function<TaggedWord, List<GeoNamesEntry>> candidateLoader,
                                List<TaggedWord> taggedWords,
                                List<TaggedWord> recognizedToponyms,
                                ArrayListMultimap<String, GeoCandidateEntry> candidateMap,
                                Table<NamedEntityTag, String, Set<TaggedWord>> surfaceMentionMap) {
            super(candidateLoader, taggedWords, recognizedToponyms, candidateMap, surfaceMentionMap);
        }

        void setDisambiguatedMap(Map<TaggedWord, Toponym> disambiguatedMap) {
            this.disambiguatedMap = disambiguatedMap;

            for (Map.Entry<TaggedWord, Toponym> disambiguatedEntry : disambiguatedMap.entrySet()) {
                this.addIndirect(
                        disambiguatedEntry.getKey(),
                        disambiguatedEntry.getValue(),
                        getCandidates(disambiguatedEntry.getKey()));
            }
        }

        Set<TaggedWord> getSurfaceAndIndirectMentions(GeoNamesEntry geoNamesEntry) {
            return Stream.concat(
                    Stream.concat(Stream.of(geoNamesEntry.getName()), GeoNamesUtil.getAlternateNames(geoNamesEntry).stream())
                            .flatMap(geoName ->
                                    Optional.ofNullable(indirectMentionTable.get(geoNamesEntry.getLevel(), geoName.toLowerCase()))
                                            .orElse(Collections.emptySet())
                                            .stream()),
                    getSurfaceMentions(geoNamesEntry).stream())
                    .collect(Collectors.toSet());
        }

        private void addToIndirectTable(GeoNamesEntry geoNamesEntry, TaggedWord mention) {
            Stream.concat(Stream.of(geoNamesEntry.getName()), GeoNamesUtil.getAlternateNames(geoNamesEntry).stream())
                    .forEach(geoName -> {
                        Set<TaggedWord> mentions = indirectMentionTable.get(geoNamesEntry.getLevel(), geoName.toLowerCase());

                        if (mentions == null)
                            indirectMentionTable.put(geoNamesEntry.getLevel(), geoName.toLowerCase(), mentions = new HashSet<>());

                        mentions.add(mention);
                    });
        }

        void addIndirect(TaggedWord taggedWord, Toponym toponym, List<GeoCandidateEntry> candidateEntries) {
            candidateEntries
                    .stream()
                    .filter(c -> c.getGeoNamesCandid().getGeonameId().equals(toponym.getGeonameId()))
                    .findFirst()
                    .ifPresent(candidateEntry -> {

                        if (GeoNamesLevel.isHierarchyLevel(candidateEntry.getGeoNamesCandid().getLevel())) {
                            addToIndirectTable(candidateEntry.getGeoNamesCandid(), taggedWord);
                        }

                        candidateEntry.getHierarchyMap().forEach(
                                (hierarchyLevel, hierarchyEntry) -> addToIndirectTable(hierarchyEntry, taggedWord)
                        );
                    });
        }

        @Override
        protected YRArticleBucket clone() {
            return new YRArticleBucket(getCandidateLoader(), getTaggedWords(), getRecognizedToponyms(), getCandidateMap(), getSurfaceMentionMap());
        }
    }

    class IndexDistanceFunction implements DistanceFunction<TaggedWord> {

        @Override
        public double applyAsDouble(TaggedWord w1, TaggedWord w2) {
            return Math.abs(w1.getIndex() - w2.getIndex());
        }
    }


}
