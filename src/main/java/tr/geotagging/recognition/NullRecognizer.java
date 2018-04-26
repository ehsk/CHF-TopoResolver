package tr.geotagging.recognition;

import tr.Article;
import tr.TaggedWord;

import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 6/26/2017
 * Time: 1:45 PM
 */
public class NullRecognizer implements GeoRecognizer {
    @Override
    public List<TaggedWord> extract(Article article) {
        return Collections.emptyList();
    }
}
