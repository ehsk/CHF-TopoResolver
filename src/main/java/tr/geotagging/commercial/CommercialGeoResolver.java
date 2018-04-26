package tr.geotagging.commercial;

import tr.Article;
import tr.TaggedWord;
import tr.geotagging.resolution.ArticleBucket;
import tr.geotagging.resolution.DefaultGeoResolver;

import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 8/10/2017
 * Time: 6:16 PM
 */
public abstract class CommercialGeoResolver extends DefaultGeoResolver<CommercialGeoResolver.CommercialArticleBucket> {
    @Override
    protected CommercialArticleBucket newArticleBucket(Article article, List<TaggedWord> taggedWords) {
        return new CommercialArticleBucket(article, taggedWords);
    }

    protected class CommercialArticleBucket extends ArticleBucket {
        public final Article article;

        CommercialArticleBucket(Article article, List<TaggedWord> taggedWords) {
            super(taggedWord -> new LinkedList<>(), taggedWords);
            this.article = article;
        }
    }
}
