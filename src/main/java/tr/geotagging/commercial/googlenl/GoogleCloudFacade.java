package tr.geotagging.commercial.googlenl;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.language.v1.*;
import com.google.cloud.storage.StorageOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tr.Article;
import tr.Toponym;
import tr.util.Config;
import tr.util.geo.GeoCoordinate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 8/10/2017
 * Time: 11:43 PM
 */
class GoogleCloudFacade {
    private final Logger logger = LogManager.getLogger(GoogleCloudFacade.class);

    private final WikipediaRepository wikipediaRepository = new WikipediaRepository();

    GoogleCloudFacade() {
        try {
            StorageOptions.newBuilder()
                    .setCredentials(ServiceAccountCredentials.fromStream(getClass().getResourceAsStream("/" + Config.GoogleCloud.KEY_FILE)))
                    .build()
                    .getService();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    List<Toponym> extractGeoTags(Article article) {
        try (LanguageServiceClient languageServiceClient = LanguageServiceClient.create()) {
            Document document = Document.newBuilder().setContent(article.getText()).setType(Document.Type.PLAIN_TEXT).setLanguage("en").build();
            final AnalyzeEntitiesResponse response = languageServiceClient.analyzeEntities(document, EncodingType.UTF8);

            final List<Toponym> toponyms = new ArrayList<>();

            for (Entity entity : response.getEntitiesList()) {
                if (entity.getType() == Entity.Type.LOCATION) {
                    final String wikipediaUrl = entity.getMetadataMap().get("wikipedia_url");
                    Optional<GeoCoordinate> coordinate = wikipediaRepository.getCoordinate(wikipediaUrl);

                    for (EntityMention mention : entity.getMentionsList()) {
                        final int start = mention.getText().getBeginOffset();
                        final String phrase = mention.getText().getContent();
                        Toponym.ToponymBuilder toponymBuilder = new Toponym.ToponymBuilder()
                                .withPhrase(phrase)
                                .withStart(start)
                                .withEnd(start + phrase.length())
                                .withScore(entity.getSalience());

                        coordinate.ifPresent(c ->
                                toponymBuilder
                                        .withLatitude(c.getLatitude())
                                        .withLongitude(c.getLongitude())
                                        .withName(c.getName())
                        );

                        toponyms.add(toponymBuilder.build());
                    }
                }
            }

            return toponyms;
        } catch (Exception e) {
            logger.error("GoogleCloud error occurred in analyzing entities of article " + article.getArticleId(), e);
        }

        return new ArrayList<>();
    }
}
