package tr;

import tr.dataset.DatasetEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Article implements DatasetEntry {

    private String text;
    private boolean annotated;
    private String articleId;
    private String source;
    private List<Toponym> toponyms = new ArrayList<>();

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isAnnotated() {
        return annotated;
    }

    public void setAnnotated(boolean annotated) {
        this.annotated = annotated;
    }

    public String getArticleId() {
        return articleId;
    }

    public void setArticleId(String articleId) {
        this.articleId = articleId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<Toponym> getToponyms() {
        return toponyms;
    }

    public void setToponyms(List<Toponym> toponyms) {
        this.toponyms = toponyms;
    }

    public List<Toponym> getTaggedToponyms() {
        return toponyms.stream().filter(Toponym::hasCoordinate).collect(Collectors.toList());
    }
}
