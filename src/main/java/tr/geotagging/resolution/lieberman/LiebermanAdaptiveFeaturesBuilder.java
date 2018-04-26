package tr.geotagging.resolution.lieberman;

import tr.TaggedWord;
import tr.Toponym;
import tr.geonames.GeoNamesEntry;
import tr.geonames.GeoNamesRepository;
import tr.util.StringUtil;
import tr.util.nlp.TaggedWordUtil;
import tr.util.geo.GeoCoordinate;
import tr.util.geo.GeoUtil;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.AtomicLongMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;


/**
 * Implementation of the following paper:
 * Adaptive Context Features for Toponym Resolution in Streaming News
 * By Michael D. Lieberman and Hanan Samet
 *
 * @author ehsan
 * Date: 11/23/2016
 * Time: 1:57 PM
 * @link http://dl.acm.org/citation.cfm?id=2348381
 */
class LiebermanAdaptiveFeaturesBuilder {

    private final Logger logger = LogManager.getLogger(getClass());


    private enum AdaptiveFeature {
        //        toponym,
//        interpretation,
//        latitude,
//        longitude,
        interps,
        population,
        altNames,
        proximity,
        siblingCountryLevel,
        siblingAdmin1Level,
        siblingAdmin2Level,
        klass
    }

    private final static int NUM_OF_FEATURES = AdaptiveFeature.values().length;

    private final GeoNamesRepository geoNamesRepository = new GeoNamesRepository();

    private final int wb, wd;

    LiebermanAdaptiveFeaturesBuilder(int wb, int wd) {
        this.wb = wb;
        this.wd = wd;
    }

    Accumulator accumulator() {
        return new Accumulator(this);
    }

    Multimap<Toponym, Instance> toUnlabeledInstances(final List<TaggedWord> taggedWords) {
        final HashMultimap<Toponym, Instance> unlabeledInstances = HashMultimap.create();

        final Table<String, Long, AdaptiveValue> instanceTable = compute(taggedWords);

        final Instances dataset = new Instances("test", getWekaAttributes(), 0);
        dataset.setClassIndex(dataset.numAttributes() - 1);

        HashMultimap<String, TaggedWord> taggedWordsByPhrase = HashMultimap.create();

        taggedWords.forEach(taggedWord -> taggedWordsByPhrase.put(taggedWord.getPhrase().toLowerCase(), taggedWord));

        for (String toponymPhrase : instanceTable.rowKeySet()) {
            final Set<TaggedWord> usages = taggedWordsByPhrase.get(toponymPhrase);

            instanceTable.row(toponymPhrase).forEach((geonameId, adaptiveValue) -> {
                for (TaggedWord usage : usages) {
                    double[] vals = new double[NUM_OF_FEATURES];

//                    vals[AdaptiveFeature.latitude.ordinal()] = adaptiveValue.latitude;
//                    vals[AdaptiveFeature.longitude.ordinal()] = adaptiveValue.longitude;
                    vals[AdaptiveFeature.interps.ordinal()] = adaptiveValue.interps;
                    vals[AdaptiveFeature.population.ordinal()] = adaptiveValue.population;
                    vals[AdaptiveFeature.altNames.ordinal()] = adaptiveValue.altNames;
                    vals[AdaptiveFeature.proximity.ordinal()] = adaptiveValue.proximity;
                    vals[AdaptiveFeature.siblingCountryLevel.ordinal()] = adaptiveValue.siblingCountryLevel;
                    vals[AdaptiveFeature.siblingAdmin1Level.ordinal()] = adaptiveValue.siblingAdmin1Level;
                    vals[AdaptiveFeature.siblingAdmin2Level.ordinal()] = adaptiveValue.siblingAdmin2Level;
                    vals[AdaptiveFeature.klass.ordinal()] = 0;

                    final DenseInstance instance = new DenseInstance(1.0, vals);
                    instance.setDataset(dataset);

                    final Toponym toponym = new Toponym(usage, geoNamesRepository.load(geonameId));

                    unlabeledInstances.put(toponym, instance);
                }
            });
        }

        return unlabeledInstances;
    }

    private ArrayList<Attribute> getWekaAttributes() {
        return new ArrayList<>(Arrays.asList(
//                new Attribute(AdaptiveFeature.latitude.name()),
//                new Attribute(AdaptiveFeature.longitude.name()),
                new Attribute(AdaptiveFeature.interps.name()),
                new Attribute(AdaptiveFeature.population.name()),
                new Attribute(AdaptiveFeature.altNames.name()),
                new Attribute(AdaptiveFeature.proximity.name()),
                new Attribute(AdaptiveFeature.siblingCountryLevel.name()),
                new Attribute(AdaptiveFeature.siblingAdmin1Level.name()),
                new Attribute(AdaptiveFeature.siblingAdmin2Level.name()),
                new Attribute("class", Arrays.asList("0", "1"))));
    }

    private Set<AdaptiveValue> toLabeledInstances(final List<TaggedWord> taggedWords, final List<Toponym> groundTruthToponyms) {
        final Set<AdaptiveValue> labeledInstances = new HashSet<>();

        final Table<String, Long, AdaptiveValue> instanceTable = compute(taggedWords);

        final Map<String, Long> groundTruthByPhrase = new HashMap<>();

        groundTruthToponyms.forEach(toponym -> groundTruthByPhrase.put(toponym.getPhrase().toLowerCase(), toponym.getGeonameId()));

        for (String toponymPhrase : instanceTable.rowKeySet()) {
            instanceTable.row(toponymPhrase).forEach((geonameId, adaptiveValue) -> {
                adaptiveValue.klass = Objects.equals(groundTruthByPhrase.get(toponymPhrase), geonameId);
                labeledInstances.add(adaptiveValue);
            });
        }

        return labeledInstances;
    }

    private Table<String, Long, AdaptiveValue> compute(final List<TaggedWord> taggedWords) {
        final List<TaggedWord> recognizedToponyms = TaggedWordUtil.filterLocations(taggedWords);

        final HashMultimap<InstanceKey, AdaptiveValue> docMultimap = HashMultimap.create();

        // line 2 of the pseudo-code explained in the paper
        for (TaggedWord t : recognizedToponyms) {
            final List<GeoNamesEntry> tInterpretations = geoNamesRepository.load(t.getPhrase());
            if (tInterpretations.isEmpty())
                continue;

            // line 4 of the pseudo-code explained in the paper
            final List<TaggedWord> window = createWindow(taggedWords, recognizedToponyms, t);

            final Map<InstanceKey, ProximityArg> proximityArgMap = new HashMap<>();
            final AtomicLongMap<InstanceKey> countryLevelMap = AtomicLongMap.create();
            final AtomicLongMap<InstanceKey> admin1LevelMap = AtomicLongMap.create();
            final AtomicLongMap<InstanceKey> admin2LevelMap = AtomicLongMap.create();

            // line 5 of the pseudo-code explained in the paper
            for (TaggedWord o : window) {

                // line 6 of the pseudo-code explained in the paper
                final List<GeoNamesEntry> oInterpretations = geoNamesRepository
                        .load(o.getPhrase())
                        .stream()
                        .sorted(Comparator
                                .<GeoNamesEntry>comparingInt(p -> Optional.ofNullable(p.getAlternateNames()).orElse("").split(",").length)
                                .reversed()
                                .thenComparing(GeoNamesEntry::getPopulation)
                                .reversed())
                        .limit(wd)
                        .collect(Collectors.toList());

                if (!oInterpretations.isEmpty()) {
                    for (GeoNamesEntry lt : tInterpretations) {
                        final GeoCoordinate ct = lt.toCoordinate();
                        final InstanceKey key = new InstanceKey(t.getPhrase(), lt.getGeonameId());

                        DoubleSummaryStatistics distanceSummary = new DoubleSummaryStatistics();
                        for (GeoNamesEntry lo : oInterpretations) {
                            final GeoCoordinate co = lo.toCoordinate();
                            // line 8 of the pseudo-code explained in the paper
                            distanceSummary.accept(GeoUtil.distance(ct, co).toKilometres());

                            // lines 10-14 of the pseudo-code explained in the paper
                            // from here
                            if (StringUtil.hasText(lt.getCountryCode()) && Objects.equals(lo.getCountryCode(), lt.getCountryCode()))
                                countryLevelMap.addAndGet(key, 1);

                            if (StringUtil.hasText(lt.getAdmin1code()) && Objects.equals(lo.getAdmin1code(), lt.getAdmin1code()))
                                admin1LevelMap.addAndGet(key, 1);

                            if (StringUtil.hasText(lt.getAdmin2code()) && Objects.equals(lo.getAdmin2code(), lt.getAdmin2code()))
                                admin2LevelMap.addAndGet(key, 1);
                            // to here
                        }

                        // line 9 of the pseudo-code explained in the paper
                        final double dMin = distanceSummary.getMin();
                        if (Double.isFinite(dMin))
                            proximityArgMap.compute(key, (instanceKey, arg) -> (arg == null ? new ProximityArg() : arg).incr(dMin));
                    }
                }

            }

            final int interps = tInterpretations.size();

            // line 7 of the pseudo-code explained in the paper
            for (GeoNamesEntry interpretation : tInterpretations) {
                final InstanceKey key = new InstanceKey(t.getPhrase(), interpretation.getGeonameId());

//                if (proximityArgMap.isEmpty() &&
//                        countryLevelMap.isEmpty() && admin1LevelMap.isEmpty() && admin2LevelMap.isEmpty())
//                    continue;

                final AdaptiveValue val = new AdaptiveValue();

                // line 18 of the pseudo-code explained in the paper
                val.proximity = proximityArgMap.getOrDefault(key, new ProximityArg()).avg();
                val.siblingCountryLevel = countryLevelMap.get(key);
                val.siblingAdmin1Level = admin1LevelMap.get(key);
                val.siblingAdmin2Level = admin2LevelMap.get(key);
                val.interps = interps;
                val.population = interpretation.getPopulation();
//                val.latitude = interpretation.getLatitude();
//                val.longitude = interpretation.getLongitude();

                if (interpretation.getAlternateNames() != null)
                    val.altNames = interpretation.getAlternateNames().split(",").length;
                else
                    val.altNames = 0;

                docMultimap.put(new InstanceKey(t.getPhrase(), interpretation.getGeonameId()), val);
            }
        }

        return propagateFeatures(docMultimap);
    }

    private List<TaggedWord> createWindow(List<TaggedWord> taggedWords, List<TaggedWord> recognizedToponyms, TaggedWord t) {
        final int numOfTokens = taggedWords.size();
        final int index = taggedWords.indexOf(t);
        int lb, ub;
        if (index <= wb / 2) {
            lb = 0;
            ub = Math.min(numOfTokens, wb);
        } else if (index + wb / 2 >= numOfTokens) {
            lb = Math.max(0, numOfTokens - wb);
            ub = numOfTokens;
        } else {
            lb = index - wb / 2;
            ub = index + wb / 2;
        }

        final Map<String, TaggedWord> windowMap = new HashMap<>();
        recognizedToponyms.stream()
                .filter(oo -> {
                    final int oindex = taggedWords.indexOf(oo);
                    return oindex <= ub && oindex >= lb;
                })
                .filter(oo -> !t.getPhrase().equalsIgnoreCase(oo.getPhrase()))
                .forEach(taggedWord -> windowMap.put(taggedWord.getPhrase().toLowerCase(), taggedWord));

        return new ArrayList<>(windowMap.values());
    }

    private Table<String, Long, AdaptiveValue> propagateFeatures(HashMultimap<InstanceKey, AdaptiveValue> articleInstanceMap) {
        final Table<String, Long, AdaptiveValue> propagatedTable = HashBasedTable.create();

        articleInstanceMap.asMap().forEach((key, valueList) ->
                valueList.stream()
                        .findFirst()
                        .map(aValue -> {
                            DoubleSummaryStatistics proximity = new DoubleSummaryStatistics();
                            LongSummaryStatistics siblingCountryLevel = new LongSummaryStatistics();
                            LongSummaryStatistics siblingAdmin1Level = new LongSummaryStatistics();
                            LongSummaryStatistics siblingAdmin2Level = new LongSummaryStatistics();

                            for (AdaptiveValue val : valueList) {
                                proximity.accept(val.proximity);
                                siblingCountryLevel.accept(val.siblingCountryLevel);
                                siblingAdmin1Level.accept(val.siblingAdmin1Level);
                                siblingAdmin2Level.accept(val.siblingAdmin2Level);
                            }

                            final AdaptiveValue mergedInstance = new AdaptiveValue();
                            mergedInstance.altNames = aValue.altNames;
                            mergedInstance.interps = aValue.interps;
                            mergedInstance.population = aValue.population;
//                            mergedInstance.latitude = aValue.latitude;
//                            mergedInstance.longitude = aValue.longitude;

                            mergedInstance.proximity = proximity.getMin();
                            mergedInstance.siblingCountryLevel = siblingCountryLevel.getMax();
                            mergedInstance.siblingAdmin1Level = siblingAdmin1Level.getMax();
                            mergedInstance.siblingAdmin2Level = siblingAdmin2Level.getMax();

                            return mergedInstance;
                        })
                        .ifPresent(mergedInstance ->
                                propagatedTable.put(key.toponymPhrase.toLowerCase(), key.geonameId, mergedInstance)
                        )
        );

        return propagatedTable;
    }


    private class InstanceKey {
        final String toponymPhrase;
        final Long geonameId;

        InstanceKey(String toponymPhrase, Long geonameId) {
            this.toponymPhrase = toponymPhrase;
            this.geonameId = geonameId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InstanceKey that = (InstanceKey) o;
            return toponymPhrase.equalsIgnoreCase(that.toponymPhrase) &&
                    Objects.equals(geonameId, that.geonameId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(toponymPhrase.toLowerCase(), geonameId);
        }
    }

    public class AdaptiveValue {
//        double latitude, longitude;
        int interps;
        long population;
        int altNames;
        double proximity;
        long siblingCountryLevel;
        long siblingAdmin1Level;
        long siblingAdmin2Level;

        Boolean klass;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AdaptiveValue that = (AdaptiveValue) o;
            return //Double.compare(that.latitude, latitude) == 0 &&
                    //Double.compare(that.longitude, longitude) == 0 &&
                    Double.compare(that.proximity, proximity) == 0 &&
                    siblingCountryLevel == that.siblingCountryLevel &&
                    siblingAdmin1Level == that.siblingAdmin1Level &&
                    siblingAdmin2Level == that.siblingAdmin2Level &&
                    Objects.equals(klass, that.klass);
        }

        @Override
        public int hashCode() {
            if (klass == null)
                return Objects.hash(interps, altNames, population,
                        proximity, siblingCountryLevel, siblingAdmin1Level, siblingAdmin2Level);
            else
                return Objects.hash(klass, interps, altNames, population,
                        proximity, siblingCountryLevel, siblingAdmin1Level, siblingAdmin2Level);
        }
    }

    private class ProximityArg {
        double dMinSum = 0;
        int num = 0;

        int avg() {
            return num != 0 ? (int) Math.round(dMinSum / num) : 20000;
        }

        ProximityArg incr(double dMin) {
            dMinSum += dMin;
            num++;
            return this;
        }
    }

    public class Accumulator implements BiConsumer<List<TaggedWord>, List<Toponym>> {
        private final LiebermanAdaptiveFeaturesBuilder builder;

        private final Set<AdaptiveValue> values = new HashSet<>();

        private Accumulator(LiebermanAdaptiveFeaturesBuilder builder) {
            this.builder = builder;
        }

        @Override
        public void accept(List<TaggedWord> taggedWords, List<Toponym> toponyms) {
            final Set<AdaptiveValue> labeledInstances = this.builder.toLabeledInstances(taggedWords, toponyms);
            values.addAll(labeledInstances);
            logger.info("{} labeled instances created for {} topos", labeledInstances.size(), toponyms.size());
        }

        Instances toWekaDataset(String datasetName) {
            final Instances dataset = new Instances(datasetName, getWekaAttributes(), 0);
            dataset.setClassIndex(dataset.numAttributes() - 1);

            for (AdaptiveValue value : values) {
                double[] vals = new double[NUM_OF_FEATURES];
//                vals[AdaptiveFeature.toponym.ordinal()] = dataset.attribute(AdaptiveFeature.toponym.ordinal()).addStringValue(entry.getKey().toponym);
//                vals[AdaptiveFeature.interpretation.ordinal()] = entry.getKey().geonameId;
//                vals[AdaptiveFeature.latitude.ordinal()] = value.latitude;
//                vals[AdaptiveFeature.longitude.ordinal()] = value.longitude;
                vals[AdaptiveFeature.interps.ordinal()] = value.interps;
                vals[AdaptiveFeature.population.ordinal()] = value.population;
                vals[AdaptiveFeature.altNames.ordinal()] = value.altNames;
                vals[AdaptiveFeature.proximity.ordinal()] = value.proximity;
                vals[AdaptiveFeature.siblingCountryLevel.ordinal()] = value.siblingCountryLevel;
                vals[AdaptiveFeature.siblingAdmin1Level.ordinal()] = value.siblingAdmin1Level;
                vals[AdaptiveFeature.siblingAdmin2Level.ordinal()] = value.siblingAdmin2Level;
                vals[AdaptiveFeature.klass.ordinal()] = value.klass ? 1 : 0;

                dataset.add(new DenseInstance(1.0, vals));
            }

            return dataset;
        }
    }

}
