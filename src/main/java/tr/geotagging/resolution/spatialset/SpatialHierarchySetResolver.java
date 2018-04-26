package tr.geotagging.resolution.spatialset;

import tr.Article;
import tr.TaggedWord;
import tr.Toponym;
import tr.geonames.GeoNamesEntry;
import tr.geotagging.resolution.ArticleBucket;
import tr.geotagging.resolution.DefaultGeoResolver;
import tr.geotagging.resolution.GeoCandidateEntry;
import tr.util.tuple.Tuple3;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 5/26/2017
 * Time: 12:30 PM
 */
public class SpatialHierarchySetResolver extends DefaultGeoResolver<ArticleBucket> {

    private final Logger logger = LogManager.getLogger(getClass());

    @Override
    protected ArticleBucket newArticleBucket(Article article, List<TaggedWord> taggedWords) {
        return new ArticleBucket(Integer.MAX_VALUE, taggedWords);
    }

    @Override
    protected List<Toponym> extract(ArticleBucket articleBucket) {
        Set<String> universe = new HashSet<>();

        final Set<Long> childGeonames = new HashSet<>();
        final Map<Long, ToponymSet> pivotIdToSet = new HashMap<>();
        final Multimap<String, ToponymSet> toponymToSets = HashMultimap.create();

        for (TaggedWord taggedWord : articleBucket.getRecognizedToponyms()) {
            final String toponymText = taggedWord.getPhrase().toLowerCase();

            if (universe.contains(toponymText))
                continue;

            universe.add(toponymText);

            final int startingSize = pivotIdToSet.size();
            final List<GeoCandidateEntry> candidates = articleBucket.getCandidates(taggedWord);

            for (GeoCandidateEntry geoCandidateEntry : candidates) {

                if (childGeonames.contains(geoCandidateEntry.getGeoNamesCandid().getGeonameId()))
                    continue;

                final GeoNamesEntry pivot = geoCandidateEntry.getParentEntry();
                final ToponymSet existingSet = pivotIdToSet.get(pivot.getGeonameId());

                if (existingSet == null) {
                    final ToponymSet newSet = new ToponymSet(toponymText, geoCandidateEntry);
                    pivotIdToSet.put(newSet.getPivot().getElement().entry.getGeonameId(), newSet);

                    if (newSet.getPivot().getParent() != null) {
                        final ToponymSet pivotParentSet = pivotIdToSet.get(newSet.getPivot().getParent().getElement().entry.getGeonameId());

                        if (pivotParentSet != null) {
                            pivotParentSet.find(newSet.getPivot().getElement().entry).ifPresent(n -> {
                                newSet.getPivot().getElement().toponyms.addAll(n.toponyms);
                                n.toponyms.forEach(topo -> toponymToSets.get(topo).add(newSet));
                            });
                        }
                    }

                    toponymToSets.put(toponymText, newSet);
                } else {
                    existingSet.addChild(toponymText, geoCandidateEntry);
                    toponymToSets.put(toponymText, existingSet);
                }

                childGeonames.add(geoCandidateEntry.getGeoNamesCandid().getGeonameId());


                final ToponymSet pivotSet = pivotIdToSet.get(geoCandidateEntry.getGeoNamesCandid().getGeonameId());
                if (pivotSet != null) {
                    pivotSet.getPivot().getElement().toponyms.add(toponymText);
                    toponymToSets.get(toponymText).add(pivotSet);
                }
                
            }

            final int finishingSize = pivotIdToSet.size();
            logger.debug("toponym {} with {} candidates added {} sets. size: {}", toponymText, candidates.size(), (finishingSize - startingSize), finishingSize);
        }

//        final List<Element> edmonton = collection.stream().filter(element -> !element.tree.filter(n -> n.getElement().toponyms.contains("pyongyang")).isEmpty()).collect(Collectors.toList());

        final Map<String, GeoNamesEntry> foundToponyms = new HashMap<>();

        while (foundToponyms.size() < universe.size()) {
            Tuple3<Double, Long, ToponymSet> bestSet = new Tuple3<>(Double.MAX_VALUE, Long.MIN_VALUE,null);

            final Set<String> I = foundToponyms.keySet();

            for (ToponymSet toponymSet : pivotIdToSet.values()) {

                final Set<String> Si = toponymSet.toponyms();

                if (I.stream().anyMatch(i -> Si.contains(i) && !toponymSet.contains(foundToponyms.get(i))))
                    continue;

                final double effectiveness = toponymSet.getCost() / Sets.difference(Si, I).size();
                if (bestSet.get_1() > effectiveness)
                    bestSet = new Tuple3<>(effectiveness, toponymSet.getPopulation(), toponymSet);
                else if (bestSet.get_1() == effectiveness)
                    if (bestSet.get_2() < toponymSet.getPopulation())
                        bestSet = new Tuple3<>(effectiveness, toponymSet.getPopulation(), toponymSet);
            }

            if (bestSet.get_3() != null) {
                for (String topo : bestSet.get_3().toponyms()) {
                    if (I.contains(topo))
                        continue;

                    bestSet.get_3().pickPopulatedNode(topo).ifPresent(e -> foundToponyms.put(topo, e.entry));
                }
            } else
                break;
        }


        final List<Toponym> geotags = new ArrayList<>();
        foundToponyms.forEach((toponymText, matchedEntry) ->
                articleBucket.getSurfaceMentions(toponymText).forEach(
                        taggedWord -> geotags.add(new Toponym(taggedWord, matchedEntry))
                )
        );

        return geotags;
    }

    @Override
    public String toString() {
        return "SpatialHierarchySet";
    }
}
