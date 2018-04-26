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
public abstract class DefaultGeoResolver<A extends ArticleBucket> implements GeoResolver {

    protected abstract A newArticleBucket(Article article, List<TaggedWord> taggedWords);

    protected abstract List<Toponym> extract(A articleBucket);

    @Override
    public List<Toponym> resolve(Article article, List<TaggedWord> taggedWords) {
        final A articleBucket = newArticleBucket(article, taggedWords);
        return extract(articleBucket);
    }
}
