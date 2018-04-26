package tr.geotagging.recognition;

import tr.Article;
import tr.TaggedWord;
import tr.util.nlp.StanfordNER;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 5/28/2017
 * Time: 11:21 AM
 */
public class StanfordNERecognizer implements GeoRecognizer {

    private final StanfordNER stanfordNER = new StanfordNER();

    @Override
    public List<TaggedWord> extract(Article article) {
        return stanfordNER.tag(article.getText());
    }
}
