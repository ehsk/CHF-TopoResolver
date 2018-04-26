package tr.geotagging.recognition;

import tr.Article;
import tr.TaggedWord;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 5/28/2017
 * Time: 11:21 AM
 */
public interface GeoRecognizer {
    List<TaggedWord> extract(final Article article);
}
