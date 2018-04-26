package tr.geotagging.resolution;

import tr.Article;
import tr.TaggedWord;
import tr.Toponym;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 5/28/2017
 * Time: 11:21 AM
 */
public interface GeoResolver {
    List<Toponym> resolve(final Article article, final List<TaggedWord> taggedWords);
}
