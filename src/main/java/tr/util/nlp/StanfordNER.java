package tr.util.nlp;

import tr.NamedEntityTag;
import tr.TaggedWord;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: user
 * Date: 10/12/2016
 * Time: 10:46 PM
 */
public class StanfordNER {
    private static final String serializedClassifier = "edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz";
    private static final AbstractSequenceClassifier<CoreLabel> classifier;

    static {
        try {
            classifier = CRFClassifier.getClassifier(serializedClassifier);
        } catch (IOException | ClassNotFoundException  e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public List<TaggedWord> tag(String text) {
        List<TaggedWord> allTaggedWords = new ArrayList<>();

        final List<List<CoreLabel>> labeledText = classifier.classify(text);

        for (List<CoreLabel> sentence : labeledText) {
            final List<TaggedWord> taggedWords = sentence.stream()
                    .map(word -> new TaggedWord(word.word(), word.get(CoreAnnotations.AnswerAnnotation.class),
                            Integer.valueOf(word.get(CoreAnnotations.PositionAnnotation.class)),
                            word.beginPosition(),
                            word.endPosition()))
                    .collect(Collectors.toList());

            TaggedWord prevWord = null;
            for (TaggedWord tw : taggedWords) {
                if (prevWord != null) {
                    if (prevWord.getTag() != NamedEntityTag.OTHER &&
                            (prevWord.getEnd() + 1 == tw.getStart() || prevWord.getEnd() == tw.getStart()) &&
                            prevWord.getTag().equals(tw.getTag())) {
                        prevWord = new TaggedWord(
                                prevWord.getPhrase() +
                                        Collections.nCopies(tw.getStart() - prevWord.getEnd(), " ").stream().collect(Collectors.joining()) +
                                        tw.getPhrase(),
                                tw.getTag(), prevWord.getIndex(), prevWord.getStart(), tw.getEnd());
                        continue;
                    } else {
                        allTaggedWords.add(prevWord);
                    }
                }

                prevWord = tw;
            }

            if (prevWord != null)
                allTaggedWords.add(prevWord);
        }

        return TaggedWordUtil.dropCommonTerms(allTaggedWords);
    }

}
