package tr.dataset.loader;

import tr.Article;
import tr.Toponym;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import tr.dataset.DatasetSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 5/18/2017
 * Time: 1:18 PM
 */
public class LiebermanStandardDatasetLoader extends XMLDatasetLoader<Article, DatasetSummary> {
    @Override
    protected AbstractXMLDatasetHandler createHandler(Consumer<Article> consumer) {
        return new LiebermanStandardHandler(consumer, getPageSize());
    }

    protected int getPageSize() {
        return 100;
    }

    protected String getGazetteerIdAttribute() {
        return "geonameid";
    }

    class LiebermanStandardHandler extends AbstractXMLDatasetHandler {
        final DatasetSummary.DatasetSummaryBuilder builder = new DatasetSummary.DatasetSummaryBuilder();
        final Consumer<Article> consumer;
        final List<Article> articlePage = new ArrayList<>();
        final int pageSize;

        private boolean isTextTag;
        private boolean isStartTag, isEndTag, isPhraseTag, isAnnotated;
        private boolean isCountryTag, isAdmin1Tag;
        private boolean isFClassTag, isFCodeTag;
        private boolean isNameTag;
        private boolean isLatTag, isLonTag;
        private boolean isDomainTag;
        protected Article article;
        Toponym.ToponymBuilder toponymBuilder;

        LiebermanStandardHandler(Consumer<Article> consumer, int pageSize) {
            this.consumer = consumer;
            this.pageSize = pageSize;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equalsIgnoreCase("article")) {
                article = new Article();
                article.setAnnotated(true);
                article.setArticleId(attributes.getValue("docid"));
            } else if (qName.equalsIgnoreCase("text")) {
                isTextTag = true;
            } else if (qName.equalsIgnoreCase("annotated")) {
                isAnnotated = true;
            } else if (qName.equalsIgnoreCase("toponyms")) {
                attributes.getValue("count");
            } else if (qName.equalsIgnoreCase("toponym")) {
                toponymBuilder = new Toponym.ToponymBuilder();
            } else if (qName.equals("start")) {
                isStartTag = true;
            } else if (qName.equals("end")) {
                isEndTag = true;
            } else if (qName.equals("phrase")) {
                isPhraseTag = true;
            } else if (qName.equals("gaztag")) {
                toponymBuilder.withGeonameId(Long.valueOf(attributes.getValue(getGazetteerIdAttribute())));
            } else if (qName.equals("fclass")) {
                isFClassTag = true;
            } else if (qName.equals("fcode")) {
                isFCodeTag = true;
            } else if (qName.equals("name")) {
                isNameTag = true;
            } else if (qName.equals("lat")) {
                isLatTag = true;
            } else if (qName.equals("lon")) {
                isLonTag = true;
            } else if (qName.equals("country")) {
                toponymBuilder.withCountryGeonameId(Long.valueOf(attributes.getValue(getGazetteerIdAttribute())));
                isCountryTag = true;
            } else if (qName.equals("admin1")) {
                toponymBuilder.withAdmin1GeonameId(Long.valueOf(attributes.getValue(getGazetteerIdAttribute())));
                isAdmin1Tag = true;
            } else if (qName.equals("admin2")) {
                toponymBuilder.withAdmin2GeonameId(Long.valueOf(attributes.getValue(getGazetteerIdAttribute())));
            } else if (qName.equals("domain")) {
                isDomainTag = true;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equalsIgnoreCase("toponym")) {
                article.getToponyms().add(toponymBuilder.build());
            } else if (qName.equalsIgnoreCase("article")) {
                builder.recordArticleSummary(article);
                articlePage.add(article);
                if (articlePage.size() >= pageSize) {
                    articlePage.forEach(consumer);
                    articlePage.clear();
                }

                article = null;
            } else if (qName.equals("text")) {
                article.setText(article.getText());
                isTextTag = false;
            } else if (qName.equals("phrase")) {
                toponymBuilder.trimPhrase();
                isPhraseTag = false;
            } else if (qName.equals("fclass")) {
                toponymBuilder.trimFeatureClass();
                isFClassTag = false;
            } else if (qName.equals("fcode")) {
                toponymBuilder.trimFeatureCode();
                isFCodeTag = false;
            } else if (qName.equals("name")) {
                toponymBuilder.trimName();
                isNameTag = false;
            } else if (qName.equals("domain")) {
                if (article.getSource() != null)
                    article.setSource(article.getSource().trim());
                isDomainTag = false;
            }  else if (qName.equals("lat")) {
                toponymBuilder.setLatitude();
                isLatTag = false;
            } else if (qName.equals("lon")) {
                toponymBuilder.setLongitude();
                isLonTag = false;
            } else if (qName.equals("start")) {
                toponymBuilder.setStart();
                isStartTag = false;
            } else if (qName.equals("end")) {
                toponymBuilder.setEnd();
                isEndTag = false;
            } else if (qName.equals("country")) {
                toponymBuilder.trimCountry();
                isCountryTag = false;
            } else if (qName.equals("admin1")) {
                toponymBuilder.trimAdmin1();
                isAdmin1Tag = false;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            final String text = new String(ch, start, length);

            if (isTextTag) {
                article.setText(Optional.ofNullable(article.getText()).orElse("") + text);
            }

            if (isAnnotated) {
                article.setAnnotated(text.equalsIgnoreCase("y"));
                isAnnotated = false;
            }

            if (isStartTag) {
                toponymBuilder.withStart(text);
            }

            if (isEndTag) {
                toponymBuilder.withEnd(text);
            }

            if (isPhraseTag) {
                toponymBuilder.withPhrase(text);
            }

            if (isFClassTag) {
                toponymBuilder.withFeatureClass(text);
            }

            if (isFCodeTag) {
                toponymBuilder.withFeatureCode(text);
            }

            if (isLatTag) {
                toponymBuilder.withLatitude(text);
            }

            if (isLonTag) {
                toponymBuilder.withLongitude(text);
            }

            if (isCountryTag) {
                toponymBuilder.withCountry(text);
            }

            if (isAdmin1Tag) {
                toponymBuilder.withAdmin1(text);
            }

            if (isNameTag) {
                toponymBuilder.withName(text);
            }

            if (isDomainTag) {
                article.setSource(Optional.ofNullable(article.getSource()).orElse("") + text);
            }
        }

        @Override
        public void endDocument() {
            if (!articlePage.isEmpty())
                articlePage.forEach(consumer);

            this.summary = builder.build();

        }
    }
}
