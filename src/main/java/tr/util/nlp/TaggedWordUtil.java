package tr.util.nlp;

import tr.NamedEntityTag;
import tr.TaggedWord;
import tr.util.nlp.NLPToolkit;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 12/14/2016
 * Time: 5:41 PM
 */
public class TaggedWordUtil {
    private TaggedWordUtil() {
    }

    public static List<TaggedWord> filterLocations(List<TaggedWord> taggedWords) {
        return taggedWords.stream()
                .filter(taggedWord -> taggedWord.getTag() == NamedEntityTag.LOCATION)
                .collect(Collectors.toList());
    }

    public static List<TaggedWord> dropCommonTerms(List<TaggedWord> taggedWords) {
        final List<TaggedWord> filteredList = new ArrayList<>();

        for (int i = 0, j = 0; i < taggedWords.size(); i++) {
            TaggedWord taggedWord = taggedWords.get(i);
            if (!taggedWord.getPhrase().matches("\\p{Punct}+") && !NLPToolkit.isStopword(taggedWord.getPhrase())) {
                taggedWord.setIndex(j++);
                filteredList.add(taggedWord);
            }
        }

        return filteredList;
    }

}
