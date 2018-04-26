package tr.dataset.stats;

import tr.Article;
import tr.Toponym;
import tr.dataset.Dataset;
import tr.dataset.DatasetException;
import tr.dataset.DatasetSummary;
import tr.geonames.GeoNamesEntry;
import tr.geonames.GeoNamesLevel;
import tr.geonames.GeoNamesRepository;
import tr.geotagging.evaluation.GeoTagResultMatcher;
import tr.util.ArgUtil;
import tr.util.Config;
import tr.util.DistanceUnit;
import tr.util.nlp.NLPToolkit;
import tr.util.db.IdNotFoundException;
import tr.util.geo.GeoCoordinate;
import tr.util.geo.GeoUtil;
import tr.util.math.MathUtil;
import tr.util.math.statistics.SampledSummaryStat;
import tr.util.math.statistics.SummaryStat;
import com.google.common.base.Strings;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.AtomicLongMap;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 7/18/2017
 * Time: 9:41 PM
 */
public class DatasetAnalyzer {

    private final Logger logger = LogManager.getLogger(getClass());

    private final GeoNamesRepository geoNamesRepository = new GeoNamesRepository();

    private final GeoTagResultMatcher matcher = new GeoTagResultMatcher(GeoTagResultMatcher.MatcherType.OPTIMISTIC, DistanceUnit.mi.of(1));

    private static final long NO_GAZ_ID = -100L;
    private static final long NO_COUNTRY_ID = -1L;

    private Long getCountryId(Toponym toponym) {
        if (toponym.getGeonameId() == null)
            return NO_GAZ_ID;
        else if (toponym.getCountryGeonameId() == null)
            return NO_COUNTRY_ID;
        else
            return toponym.getCountryGeonameId();
    }

    private String loadCountryCode(Long countryId) {
        if (countryId.equals(NO_GAZ_ID))
            return "<NO_GAZ>";
        else if (countryId.equals(NO_COUNTRY_ID))
            return "<NO_COUNTRY>";
        else
            return geoNamesRepository.load(countryId).getCountryCode();
    }

    public String build(Dataset<Article, DatasetSummary> dataset) {
        final AtomicLongMap<String> articleFreqPerSource = AtomicLongMap.create();
        final AtomicLongMap<String> toponymFreqPerSource = AtomicLongMap.create();
        final AtomicLongMap<String> distinctTopoFreqPerSource = AtomicLongMap.create();
        final Table<String, Long, Long> countryFreqPerSource = HashBasedTable.create();

        SampledSummaryStat toposPerArticle = new SampledSummaryStat();
        SampledSummaryStat toposWithGazetteerPerArticle = new SampledSummaryStat();

        final AtomicLongMap<String> topoType = AtomicLongMap.create();

        final Map<String, SummaryStat> meanCandidatesPerSource = new HashMap<>();
        final AtomicLongMap<String> totalCandidatesPerSource = AtomicLongMap.create();
        final Map<String, SummaryStat> meanGeoDistancePerSource = new HashMap<>();

        final Map<Long, SummaryStat> toposPerCountry = new HashMap<>();

        SummaryStat articleWordCount = new SummaryStat();

        SummaryStat nonExistingTopos = new SummaryStat();

        SummaryStat wikiTopos = new SummaryStat();

        SummaryStat distinctToposPerArticle = new SummaryStat();

        SampledSummaryStat candidatesPerTopoPerArticle = new SampledSummaryStat();
        SampledSummaryStat candidatesPerArticle = new SampledSummaryStat();

        SummaryStat meanGeoDistancePerArticle = new SummaryStat();

        final Set<Long> distinctToponyms = new HashSet<>();

        try {
            final DatasetSummary summary = dataset.forEach(article -> {
                if (!article.isAnnotated())
                    return;

                toposPerArticle.accept(article.getToponyms().size());
                toposWithGazetteerPerArticle.accept(article.getTaggedToponyms().size());

                articleFreqPerSource.addAndGet(article.getSource(), 1);

                articleWordCount.accept(NLPToolkit.tokenize(article.getText()).size());

                final Set<Long> distinctTopos = article.getToponyms().stream().map(Toponym::getGeonameId).collect(Collectors.toSet());
                distinctToponyms.addAll(distinctTopos);
                distinctToposPerArticle.accept(distinctTopos.size());

                distinctTopoFreqPerSource.addAndGet(article.getSource(), distinctTopos.size());
                toponymFreqPerSource.addAndGet(article.getSource(), article.getToponyms().size());

                logger.info("[{}] topos: {} | tagged_topos: {} | distinct_topos: {}", article.getArticleId(),
                        article.getToponyms().size(), article.getTaggedToponyms().size(), distinctTopos.size());

                for (Toponym toponym : article.getTaggedToponyms()) {
                    try {
                        GeoNamesEntry geoNamesEntry = null;

                        if (toponym.getGeonameId() != null)
                            geoNamesEntry = geoNamesRepository.load(toponym.getGeonameId());


                        Optional<GeoNamesEntry> potentialCandid = geoNamesRepository.load(toponym.getPhrase()).stream()
                                .filter(c -> matcher.check(c, toponym))
                                .min(Comparator.comparingDouble(e -> GeoUtil.distance(e.toCoordinate(), toponym.toCoordinate()).toMetres()));

                        if (potentialCandid.isPresent()) {
                            if (geoNamesEntry == null)
                                geoNamesEntry = potentialCandid.get();
                            else if (!potentialCandid.get().getGeonameId().equals(geoNamesEntry.getGeonameId()))
                                logger.warn("[{}] unable to estimate: {} -> {}", article.getArticleId(), geoNamesEntry.getGeonameId(), potentialCandid.get().getGeonameId());
                        } else {
                            nonExistingTopos.accept(1);
                            logger.info("[{}] >>> {} | {}", article.getArticleId(), toponym.getPhrase(), toponym.getGeonameId());
                            continue;
                        }

                        if (geoNamesEntry.getLevel() != GeoNamesLevel.LEAF)
                            topoType.addAndGet(geoNamesEntry.getLevel().name(), 1);
                        else {
                            if (geoNamesEntry.getFeatureClass().startsWith("PPL")) {
                                if (geoNamesEntry.getPopulation() >= Config.Data.LARGE_CITY_POPULATION)
                                    topoType.addAndGet("CITY_LARGE", 1);
                                else
                                    topoType.addAndGet("CITY_SMALL", 1);
                            } else
                                topoType.addAndGet("MISC", 1);
                        }

                        Optional<String> wikiUrl = geoNamesRepository.getWikipediaUrl(geoNamesEntry.getGeonameId());
                        if (wikiUrl.isPresent())
                            wikiTopos.accept(1);

                    } catch (IdNotFoundException e) {
                        logger.error(String.format("[Article %s] geoNameId not found: %d %s", article.getArticleId(), toponym.getGeonameId(), toponym));
                    }
                }

                SampledSummaryStat candidatesSummary = countCandidates(article);

                candidatesPerTopoPerArticle.accept(candidatesSummary.getMedian());
                candidatesPerArticle.accept(candidatesSummary.getSum());

                SummaryStat perSourceStat = meanCandidatesPerSource.get(article.getSource());
                if (perSourceStat == null)
                    meanCandidatesPerSource.put(article.getSource(), perSourceStat = new SummaryStat());
                perSourceStat.accept(candidatesSummary.getMedian());
                totalCandidatesPerSource.addAndGet(article.getSource(), (long) candidatesSummary.getSum());

                AtomicLongMap<Long> perCountry = AtomicLongMap.create();
                for (Toponym toponym : article.getToponyms()) {
                    perCountry.addAndGet(getCountryId(toponym), 1);
                }

                perCountry.asMap().forEach((countryId, count) -> {
                    SummaryStat stat = toposPerCountry.get(countryId);
                    if (stat == null)
                        toposPerCountry.put(countryId, stat = new SummaryStat());

                    stat.accept(count);

                    countryFreqPerSource.put(article.getSource(), countryId,
                            Optional.ofNullable(countryFreqPerSource.get(article.getSource(), countryId)).orElse(0L) + count);
                });

                SampledSummaryStat allPairDistanceSummary = calcAllPairDistance(article);

                meanGeoDistancePerArticle.accept(allPairDistanceSummary.getAverage());

                SummaryStat geoDistancePerSource = meanGeoDistancePerSource.get(article.getSource());
                if (geoDistancePerSource == null)
                    meanGeoDistancePerSource.put(article.getSource(), geoDistancePerSource = new SummaryStat());
                geoDistancePerSource.accept(allPairDistanceSummary.getAverage());
            });

            String analysis =
                    String.format("dataset %s summary:\n", dataset.getName()) +
                            summary +
                            "-----\n" +

                            String.format("topos_wiki_linked %.0f (%.2f%%)\n", wikiTopos.getSum(), MathUtil.percentOf(wikiTopos.getSum(), summary.getNumberOfToponymsWithGazetteer())) +
                            String.format("topos_gaz_not_exists %.0f (%.2f%%)\n", nonExistingTopos.getSum(), MathUtil.percentOf(nonExistingTopos.getSum(), summary.getNumberOfToponymsWithGazetteer())) +

                            "-----\n" +

                            String.format("median_topos_per_article %.0f | ", toposPerArticle.getMedian()) +
                            String.format("median_topos_with_gazetteer_per_article %.0f\n", toposWithGazetteerPerArticle.getMedian()) +

                            String.format("d_topos %d\n", distinctToponyms.size()) +

                            String.format("max_d_topos_per_article %.0f | ", distinctToposPerArticle.getMax()) +
                            String.format("mean_d_topos_per_article %.1f | ", distinctToposPerArticle.getAverage()) +
                            String.format("stdev_d_topos_per_article %.1f\n", distinctToposPerArticle.getStdev()) +

                            String.format("min_wc %.0f | ", articleWordCount.getMin()) +
                            String.format("max_wc %.0f | ", articleWordCount.getMax()) +
                            String.format("mean_wc %.1f | ", articleWordCount.getAverage()) +
                            String.format("stdev_wc %.1f\n", articleWordCount.getStdev()) +

                            String.format("MEDIAN of median_candidates_per_topo_per_article %.1f\n", candidatesPerTopoPerArticle.getMedian()) +
                            String.format("MEAN of median_candidates_per_topo_per_article %.1f\n", candidatesPerTopoPerArticle.getAverage()) +
                            String.format("STDEV of median_candidates_per_topo_per_article %.1f\n", candidatesPerTopoPerArticle.getStdev()) +
                            String.format("total_candidates %.0f\n", candidatesPerArticle.getSum()) +
                            String.format("median_candidates_per_article %.1f | ", candidatesPerArticle.getMedian()) +
                            String.format("mean_candidates_per_article %.1f | ", candidatesPerArticle.getAverage()) +
                            String.format("stdev_candidates_per_article %.1f | ", candidatesPerArticle.getStdev()) +
                            String.format("min_candidates_per_article %.0f | ", candidatesPerArticle.getMin()) +
                            String.format("max_candidates_per_article %.0f\n", candidatesPerArticle.getMax()) +

                            String.format("MEAN of mean_geo_distance_per_article %.2f\n", meanGeoDistancePerArticle.getAverage());

            analysis += String.format("\n%-10s %-5s\n", "Type", "Freq");
            analysis += "------------------\n";
            analysis += topoType.asMap().keySet().stream().sorted().map(type ->
                    String.format("%-10s %-5d", type, topoType.get(type))
            ).collect(Collectors.joining("\n"));
            analysis += "\n";

            analysis += String.format("\n%-15s %-5s %s\n", "country", "Freq", "#Articles");
            analysis += "-------------------------------\n";
            analysis += toposPerCountry.entrySet().stream()
                    .sorted(Comparator.<Map.Entry<Long, SummaryStat>>comparingDouble(value -> value.getValue().getSum()).reversed())
                    .map(entry ->
                            String.format("%-15s %-5.0f %d", loadCountryCode(entry.getKey()), entry.getValue().getSum(), entry.getValue().getCount()))
                    .collect(Collectors.joining("\n"));


            analysis += String.format("\n\nn_sources %d\n", articleFreqPerSource.size());
            analysis += String.format("\n%-30s %-10s %-8s %-8s %-8s %-8s %-8s %s\n", "Source", "#Articles", "#Topos", "#D_Topos", "Med_Cand", "Tot_Cand", "Avg_Dist", "Country");
            analysis += Strings.repeat("-", 130) + "\n";
            analysis += articleFreqPerSource.asMap().entrySet().stream()
                    .sorted(Comparator.<Map.Entry<String, Long>>comparingLong(e -> toponymFreqPerSource.get(e.getKey())).reversed())
                    .map(entry -> {
                        final String src = entry.getKey();
                        return String.format("%-30s %-10d %-8d %-8d %-8.1f %-8d %-8.1f %s", src, entry.getValue(),
                                toponymFreqPerSource.get(src), distinctTopoFreqPerSource.get(src),
                                meanCandidatesPerSource.get(src).getAverage(), totalCandidatesPerSource.get(src),
                                meanGeoDistancePerSource.get(src).getAverage(),
                                countryFreqPerSource.row(src).entrySet().stream()
                                        .sorted(Comparator.<Map.Entry<Long, Long>>comparingLong(Map.Entry::getValue).reversed())
                                        .limit(3)
                                        .map(e -> loadCountryCode(e.getKey()) + " (" + e.getValue() + ")")
                                        .collect(Collectors.joining(", "))
                        );
                    }).collect(Collectors.joining("\n"));

            return analysis;
        } catch (DatasetException e) {
            logger.error("error in building analysis of dataset {}: {}", dataset.getName(), e.getMessage());
            return "";
        }
    }

    public SampledSummaryStat calcAllPairDistance(Article article) {
        return calcAllPairDistance(article.getToponyms());
    }

    public SampledSummaryStat calcAllPairDistance(List<Toponym> toponyms) {
        SampledSummaryStat allPairDistanceSummary = new SampledSummaryStat();

        for (int i = 0; i < toponyms.size(); i++) {
            Toponym t = toponyms.get(i);

            if (!t.hasCoordinate())
                continue;

            for (int j = i + 1; j < toponyms.size(); j++) {
                Toponym s = toponyms.get(j);

                if (!s.hasCoordinate())
                    continue;

                final GeoCoordinate tt = t.toCoordinate();
                final GeoCoordinate ss = s.toCoordinate();

                if (s.getGeonameId() != null)
                    if (s.getGeonameId().equals(t.getGeonameId()))
                        continue;
                else if (ss.equals(tt))
                    continue;

                final double distance = GeoUtil.distance(tt, ss).toKilometres();
                allPairDistanceSummary.accept(distance);
            }
        }

        return allPairDistanceSummary;
    }

    public SampledSummaryStat countCandidates(Article article) {
        final Set<String> toponymPhrases = article.getTaggedToponyms().stream().map(t -> t.getPhrase().toLowerCase()).collect(Collectors.toSet());
        SampledSummaryStat candidatesSummary = new SampledSummaryStat();
        for (String toponymPhrase : toponymPhrases) {
            candidatesSummary.accept(geoNamesRepository.count(toponymPhrase));
        }

        return candidatesSummary;
    }

    public static void main(String[] args) {
        ArgumentParser argParser = ArgumentParsers.newFor("DatasetAnalyzer")
                .build()
                .defaultHelp(true)
                .description("Analyze a dataset");
        argParser.addArgument("-d", "--data")
                .choices(ArgUtil.getDataOptions()).setDefault("tr-news")
                .help("Specify dataset on which the experiment runs");

        final Namespace ns = argParser.parseArgsOrFail(args);

        final Dataset<Article, DatasetSummary> dataset = ArgUtil.getDataset(ns.getString("data"));
        final DatasetAnalyzer analyzer = new DatasetAnalyzer();
        System.out.println(analyzer.build(dataset));
    }
}
