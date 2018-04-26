package tr.geotagging.resolution;

import tr.NamedEntityTag;
import tr.TaggedWord;
import tr.geonames.GeoNamesEntry;
import tr.geonames.GeoNamesRepository;
import tr.geonames.GeoNamesUtil;
import tr.util.nlp.TaggedWordUtil;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 5/28/2017
 * Time: 11:38 AM
 */
public class ArticleBucket {
    private final Function<TaggedWord, List<GeoNamesEntry>> candidateLoader;

    private final List<TaggedWord> taggedWords;
    private final List<TaggedWord> recognizedToponyms;

    private final ArrayListMultimap<String, GeoCandidateEntry> candidateMap;
    private final Table<NamedEntityTag, String, Set<TaggedWord>> surfaceMentionMap;

    public ArticleBucket(Function<TaggedWord, List<GeoNamesEntry>> candidateLoader, List<TaggedWord> taggedWords) {
        this.candidateLoader = candidateLoader;
        this.taggedWords = taggedWords;

        this.recognizedToponyms = TaggedWordUtil.filterLocations(taggedWords);
        this.surfaceMentionMap =  HashBasedTable.create();

        taggedWords.forEach(taggedWord -> {
            Set<TaggedWord> mentions = surfaceMentionMap.get(taggedWord.getTag(), makeKey(taggedWord));
            if (mentions == null)
                surfaceMentionMap.put(taggedWord.getTag(), makeKey(taggedWord), mentions = new HashSet<>());

            mentions.add(taggedWord);
        });
        candidateMap = findCandidates();
    }

    public ArticleBucket(int k, List<TaggedWord> taggedWords) {
        this(taggedWord ->
                new GeoNamesRepository().load(taggedWord.getPhrase())
                        .stream()
                        .sorted(Comparator.comparing(GeoNamesEntry::getPopulation).reversed())
                        .limit(k)
                        .collect(Collectors.toList()),
                taggedWords);
    }

    protected ArticleBucket(Function<TaggedWord, List<GeoNamesEntry>> candidateLoader,
                            List<TaggedWord> taggedWords,
                            List<TaggedWord> recognizedToponyms,
                            ArrayListMultimap<String, GeoCandidateEntry> candidateMap,
                            Table<NamedEntityTag, String, Set<TaggedWord>> surfaceMentionMap) {
        this.candidateLoader = candidateLoader;
        this.taggedWords = taggedWords;
        this.recognizedToponyms = recognizedToponyms;
        this.candidateMap = candidateMap;
        this.surfaceMentionMap = surfaceMentionMap;
    }

    private String makeKey(TaggedWord taggedWord) {
        return taggedWord.getPhrase().toLowerCase();
    }

    private String makeKey(GeoNamesEntry geoNamesEntry) {
        return geoNamesEntry.getName().toLowerCase();
    }

    private ArrayListMultimap<String, GeoCandidateEntry> findCandidates() {
        ArrayListMultimap<String, GeoCandidateEntry> candidateMap = ArrayListMultimap.create();

        for (TaggedWord s : recognizedToponyms) {
            if (candidateMap.containsKey(makeKey(s)))
                continue;

            candidateLoader.apply(s)
                    .forEach(candidate ->
                            candidateMap.put(makeKey(s),
                                    new GeoCandidateEntry(candidate, GeoNamesUtil.getGeoNamesHierarchy(candidate))));
        }

        return candidateMap;
    }

    public List<GeoCandidateEntry> getCandidates(TaggedWord taggedWord) {
        return this.candidateMap.get(makeKey(taggedWord));
    }

    public Set<TaggedWord> getSurfaceMentions(String phrase) {
        return surfaceMentionMap.get(NamedEntityTag.LOCATION, phrase);
    }

    public Set<TaggedWord> getSurfaceMentions(GeoNamesEntry geoNamesEntry) {
        final Set<TaggedWord> mentions = new HashSet<>();

        Optional.ofNullable(surfaceMentionMap.get(NamedEntityTag.LOCATION, makeKey(geoNamesEntry))).ifPresent(mentions::addAll);

        for (String altName : GeoNamesUtil.getAlternateNames(geoNamesEntry)) {
            Optional.ofNullable(surfaceMentionMap.get(NamedEntityTag.LOCATION, altName.toLowerCase())).ifPresent(mentions::addAll);
        }

        return mentions;
    }

    protected Table<NamedEntityTag, String, Set<TaggedWord>> getSurfaceMentionMap() {
        return surfaceMentionMap;
    }

    public ArrayListMultimap<String, GeoCandidateEntry> getCandidateMap() {
        return candidateMap;
    }

    public List<TaggedWord> getTaggedWords() {
        return taggedWords;
    }

    public List<TaggedWord> getRecognizedToponyms() {
        return recognizedToponyms;
    }

    protected Function<TaggedWord, List<GeoNamesEntry>> getCandidateLoader() {
        return candidateLoader;
    }
}
