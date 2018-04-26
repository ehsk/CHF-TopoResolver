package tr.geotagging.recognition;

import tr.Article;
import tr.NamedEntityTag;
import tr.TaggedWord;
import tr.geonames.GeoNamesEntry;
import tr.geonames.GeoNamesRepository;
import tr.util.StringUtil;
import tr.util.nlp.TaggedWordUtil;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.PTBTokenizer;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 5/28/2017
 * Time: 3:43 PM
 */
public class MockRecognizer implements GeoRecognizer {

    private final GeoNamesRepository geoNamesRepository = new GeoNamesRepository();

    @Override
    public List<TaggedWord> extract(Article article) {
        final List<TaggedWord> taggedWords = new ArrayList<>();

        taggedWords.addAll(article.getToponyms().stream().map(t -> {
            String toponymPhrase = t.getPhrase();

            final List<GeoNamesEntry> interpretations = geoNamesRepository.load(toponymPhrase);
            if (interpretations.isEmpty()) {
                if (StringUtil.hasText(t.getName()))
                    toponymPhrase = t.getName();
                else if (t.getGeonameId() != null) {
                    toponymPhrase = geoNamesRepository.load(t.getGeonameId()).getName();
                }
            }

            return new TaggedWord(toponymPhrase, NamedEntityTag.LOCATION, 0, t.getStart(), t.getEnd());
        }).collect(Collectors.toList()));
        final List<Integer[]> toponymOffsets = article.getToponyms().stream().map(t -> new Integer[] {t.getStart(), t.getEnd()}).collect(Collectors.toList());

        int index = 0;
        final List<Word> words = PTBTokenizer.factory().getTokenizer(new StringReader(article.getText())).tokenize();
        for (Word word : words) {
            final String phrase = article.getText().substring(word.beginPosition(), word.endPosition());
            boolean isToponym = false;
            for (Integer[] offsets : toponymOffsets) {
                if (word.beginPosition() >= offsets[0] && word.endPosition() <= offsets[1]) {
                    isToponym = true;
                    break;
                }
            }

            if (isToponym)
                continue;

            taggedWords.add(new TaggedWord(phrase, NamedEntityTag.OTHER, index++, word.beginPosition(), word.endPosition()));
        }

        taggedWords.sort(Comparator.comparingInt(TaggedWord::getStart));

        return TaggedWordUtil.dropCommonTerms(taggedWords);
    }
}
