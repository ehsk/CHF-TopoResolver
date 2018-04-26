package tr.geotagging.commercial.opencalais;

import com.tr.tms.abstractionLayer.CalaisModel;
import com.tr.tms.abstractionLayer.CalaisModelCreator;
import com.tr.tms.abstractionLayer.CalaisObject;
import com.tr.tms.abstractionLayer.engine.Engine;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tr.Article;
import tr.Toponym;
import tr.util.Config;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 12/18/2016
 * Time: 8:39 PM
 */
public class OpenCalaisFacade {

    private static final Logger logger = LogManager.getLogger(OpenCalaisFacade.class);

    private static final String CALAIS_URL = "https://api.thomsonreuters.com/permid/calais";
    private final HttpClient client;
    private String accessToken = Config.OpenCalais.ACCESS_TOKEN;

    public OpenCalaisFacade() {
        client = new HttpClient();
        client.getParams().setParameter("http.useragent", "Calais Rest Client");
    }

    private PostMethod createPostMethod(String accessToken) {
        PostMethod method = new PostMethod(CALAIS_URL);

        method.setRequestHeader("X-AG-Access-Token", accessToken);
        method.setRequestHeader("Content-Type", "text/raw");
        method.setRequestHeader("x-calais-contentClass", "news");
        method.setRequestHeader("omitOutputtingOriginalText", "true");
        //method.setRequestHeader("x-calais-selectiveTags", "country");

        return method;
    }

    private CalaisModel post(Article article) throws IOException {

        for (int i = 0; ; i++) {
            final PostMethod method = createPostMethod(accessToken);
            method.setRequestEntity(new StringRequestEntity(article.getText(), null, null));

            try {

                final Instant start = Instant.now();
                int returnCode = client.executeMethod(method);
                final Instant respReceived = Instant.now();
                logger.debug("opencalais API called <{}> for {} in {}", i + 1, article.getArticleId(), Duration.between(start, respReceived).getSeconds());

                if (returnCode == HttpStatus.SC_OK) {
                    return CalaisModelCreator.readInputStream(method.getResponseBodyAsStream(), Engine.Format.RDF);
                } else if (returnCode == 429 || returnCode == 500 || returnCode == 503) {
                    if (i < 3) {
                        logger.warn("request failed for " + article.getArticleId() + " response " + returnCode + " (retrying...)");
                        try {
                            TimeUnit.MILLISECONDS.sleep(750);
                        } catch (InterruptedException ignored) {
                        }
                    } else {
                        logger.error("request failed for " + article.getArticleId() + " giving up after " + i + " retries ");
                        throw new IOException("request failed after too many retries");
                    }
                } else {
                    logger.error("request failed for " + article.getArticleId() + " resp code: " + returnCode);
                    logger.error("response: " + method.getResponseBodyAsString());
                    throw new IOException("invalid resp received " + returnCode);
                }
            } finally {
                method.releaseConnection();
            }
        }
    }

    List<Toponym> extractGeoTags(final Article article) {
        try {
            final Instant start = Instant.now();
            final CalaisModel model = post(article);
            final List<Toponym> calaisToponyms = extractGeoTags(model);
            final Instant end = Instant.now();
            logger.debug("opencalais done for {} in {}", article.getArticleId(), Duration.between(start, end).getSeconds());
            return calaisToponyms;
        } catch (IOException e) {
            logger.error(e);
        }

        return new LinkedList<>();
    }

    private List<Toponym> extractGeoTags(CalaisModel model) {
        final Set<String> geoTypes = model.getAllTypes().stream().filter(type -> type.matches("(.*)/em/e/(City|Country|Continent|ProvinceOrState)/?(.*)$")).collect(Collectors.toSet());

        final List<Toponym> extractedToponyms = new ArrayList<>();
        for (String geoType : geoTypes) {
            final String granularity;
            if (geoType.matches("(.*)/em/e/City/?(.*)$"))
                granularity = "City";
            else if (geoType.matches("(.*)/em/e/Country/?(.*)$"))
                granularity = "Country";
            else if (geoType.matches("(.*)/em/e/Continent/?(.*)$"))
                granularity = "Continent";
            else
                granularity = "ProvinceOrState";

            final List<CalaisObject> geoObjects = model.getCalaisObjectByType(geoType);
            for (CalaisObject geoObject : geoObjects) {
                final Map<String, List<CalaisObject>> geoSubject = geoObject.getBackReferencesByFieldName("http://s.opencalais.com/1/pred/subject");

                final List<CalaisObject> geoRefs = geoSubject.get("http://s.opencalais.com/1/type/er/Geo/" + granularity);
                if (geoRefs == null)
                    continue;

                final CalaisObject geoRef = geoRefs.get(0);
                final Map<String, List<String>> geoLiterals = geoRef.getLiterals();

                final String toponymName = geoLiterals.get("http://s.opencalais.com/1/pred/name").get(0);
                final String shortname = geoLiterals.get("http://s.opencalais.com/1/pred/shortname").get(0);

                final List<String> lats = geoLiterals.get("http://s.opencalais.com/1/pred/latitude");
                final Double latitude = lats != null ? Double.valueOf(lats.get(0)) : null;

                final List<String> longs = geoLiterals.get("http://s.opencalais.com/1/pred/longitude");
                final Double longitude = longs != null ? Double.valueOf(longs.get(0)) : null;

                final List<String> stateInfo = geoLiterals.get("http://s.opencalais.com/1/pred/containedbystate");
                final String state = stateInfo != null ? stateInfo.get(0) : null;

                final List<String> countryInfo = geoLiterals.get("http://s.opencalais.com/1/pred/containedbycountry");
                final String country = countryInfo != null ? countryInfo.get(0) : null;

                final List<CalaisObject> relevanceInfo = geoSubject.get("http://s.opencalais.com/1/type/sys/RelevanceInfo");
                double score = 0;
                if (!relevanceInfo.isEmpty())
                    score = Double.valueOf(relevanceInfo.get(0).getLiterals().get("http://s.opencalais.com/1/pred/relevance").get(0));

                final List<CalaisObject> instances = geoSubject.get("http://s.opencalais.com/1/type/sys/InstanceInfo");
                for (CalaisObject instance : instances) {
                    final Map<String, List<String>> literals = instance.getLiterals();
                    final Integer start = Integer.valueOf(literals.get("http://s.opencalais.com/1/pred/offset").get(0));
                    final Integer length = Integer.valueOf(literals.get("http://s.opencalais.com/1/pred/length").get(0));
                    final String phrase = literals.get("http://s.opencalais.com/1/pred/exact").get(0);

                    extractedToponyms.add(new Toponym.ToponymBuilder()
                            .withPhrase(phrase).withName(shortname)
                            .withStart(start).withEnd(start + length)
                            .withLatitude(latitude).withLongitude(longitude)
                            .withCountry(country).withAdmin1(state)
                            .withScore(score)
                            .build());
                }
            }
        }
        return extractedToponyms;
    }


}
