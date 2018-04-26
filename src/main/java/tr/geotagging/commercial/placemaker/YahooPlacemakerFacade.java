package tr.geotagging.commercial.placemaker;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;
import tr.Article;
import tr.Toponym;
import tr.util.Config;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 8/9/2017
 * Time: 1:28 PM
 */
public class YahooPlacemakerFacade {
    private static final Logger logger = LogManager.getLogger(YahooPlacemakerFacade.class);

    private static final String SERVICE_URL = "http://query.yahooapis.com/v1/yql";
//    private static final String AUTH_URL = "https://api.login.yahoo.com/oauth2/request_auth";
    private static final String TOKEN_URL = "https://api.login.placemaker.com/oauth2/get_token";
    private static final String ENCODED_AUTH;

    private static final String TOKEN_FILE = ".yahoo_token";

    private static final String PLACEMAKER_YQL = "SELECT * FROM geo.placemaker WHERE documentContent=\"%s\" AND documentType=\"text/plain\"";

    private static final Type type = new TypeToken<Map<String, String>>(){}.getType();

    private final Gson gson;
    private final HttpClient client = new HttpClient();

    private final Stopwatch expireWatch = Stopwatch.createUnstarted();
    private String accessToken;
    private String refreshToken;
    private long tokenExpiresIn = Long.MAX_VALUE;

    private static YahooPlacemakerFacade instance = null;
    private static final Object lock = new Object();

    static {
        try {
            ENCODED_AUTH = Base64.getEncoder().encodeToString((Config.YahooYDN.CONSUMER_KEY + ":" + Config.YahooYDN.CONSUMER_SECRET).getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static YahooPlacemakerFacade getInstance() {
        if (instance == null) {
            synchronized (lock) {
                instance = new YahooPlacemakerFacade();
            }
        }

        return instance;
    }

    private YahooPlacemakerFacade() {
        GsonBuilder builder = new GsonBuilder();
        gson = builder.create();

        if (Files.exists(Paths.get(TOKEN_FILE))) {
            try {
                final long tokenExpiredIn = loadToken();
                if (tokenExpiredIn > 10) {
                    expireWatch.start();
                    this.tokenExpiresIn = tokenExpiredIn;
                } else {
                    retrieveAccessToken(true);
                }
            } catch (IOException e) {
                throw new ExceptionInInitializerError(e);
            }
        } else {
            try {
                retrieveAccessToken(false);
            } catch (IOException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

    }

    private long loadToken() throws IOException {
        final Map<String, String> tokenMap = gson.fromJson(Files.newBufferedReader(Paths.get(TOKEN_FILE)), type);
        accessToken = tokenMap.get("access_token");
        refreshToken = tokenMap.get("refresh_token");
        final Instant expireAt = Instant.ofEpochSecond(Long.valueOf(tokenMap.get("expire_at")));
        return Duration.between(Instant.now(), expireAt).getSeconds();
    }

    private void retrieveAccessToken(boolean refresh) throws IOException {
        if (refresh) {
            logger.debug(String.format("Yahoo! OAuth: refreshing access token after %d seconds...", expireWatch.elapsed(TimeUnit.SECONDS)));
        } else {
            logger.debug("Yahoo! OAuth: acquiring access token");
        }

        expireWatch.reset();

        PostMethod method = new PostMethod(TOKEN_URL);

        try {
            method.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
            method.setRequestHeader("Authorization", "Basic " + ENCODED_AUTH);
            method.setRequestBody(new NameValuePair[] {
                    new NameValuePair("grant_type", refresh ? "refresh_token" : "authorization_code"),
                    new NameValuePair(refresh ? "refresh_token" : "code", refresh ? refreshToken : Config.YahooYDN.AUTH_CODE),
                    new NameValuePair("redirect_uri", "oob")
            });



            expireWatch.start();
            int responseCode = client.executeMethod(method);

            if (responseCode == HttpStatus.SC_OK) {
                final Map<String, String> resp = gson.fromJson(new InputStreamReader(method.getResponseBodyAsStream()), type);
                final Instant createdTimestamp = Instant.now();
                tokenExpiresIn = Long.valueOf(resp.getOrDefault("expires_in", String.valueOf(Long.MAX_VALUE)));
                accessToken = resp.get("access_token");
                refreshToken = resp.get("refresh_token");
                String yahooGuid = resp.get("xoauth_yahoo_guid");

                exportToken(createdTimestamp);

                logger.info(String.format("Yahoo! OAuth: access token acquired / refresh token %s / expired in %d s / guid %s", refreshToken, tokenExpiresIn, yahooGuid));
            } else {
                logger.error("Yahoo! OAuth: cannot acquire access token due to response error: " + responseCode);
                logger.error("Yahoo! OAuth: response body: " + method.getResponseBodyAsString());
                throw new IOException("Yahoo! OAuth: cannot acquire access token due to response error: " + responseCode);
            }

        } finally {
            method.releaseConnection();
        }
    }

    private void exportToken(Instant createdInstant) throws IOException {
        Map<String, String> exportedToken = new HashMap<>();
        exportedToken.put("access_token", accessToken);
        exportedToken.put("refresh_token", refreshToken);
        
        final long expireAt = LocalDateTime.ofInstant(createdInstant, ZoneOffset.UTC).plus(tokenExpiresIn, ChronoUnit.SECONDS).toEpochSecond(ZoneOffset.UTC);
        exportedToken.put("expire_at", String.valueOf(expireAt));

//        Files.deleteIfExists(Paths.get(TOKEN_FILE));
        Files.write(Paths.get(TOKEN_FILE), Lists.newArrayList(gson.toJson(exportedToken, type)));
    }

    private List<Toponym> post(Article article) throws IOException {

        if (expireWatch.elapsed(TimeUnit.SECONDS) >= tokenExpiresIn)
            retrieveAccessToken(true);

        String query = String.format(PLACEMAKER_YQL, URLEncoder.encode(article.getText(), "UTF-8"));

        for (int i = 0; ; i++) {

            GetMethod method = new GetMethod(SERVICE_URL);
            method.addRequestHeader("Authorization", "Bearer " + accessToken);
            method.setQueryString(new NameValuePair[] {
                    new NameValuePair("format", "xml"),
                    new NameValuePair("q", query)
            });

            try {
                final Stopwatch stopwatch = Stopwatch.createStarted();
                int returnCode = client.executeMethod(method);
                stopwatch.stop();

                logger.debug("YQL placemaker API called <{}> for {} in {}", i + 1, article.getArticleId(), stopwatch.elapsed(TimeUnit.SECONDS));

                if (returnCode == HttpStatus.SC_OK) {
                    return parse(method.getResponseBodyAsStream());
                    
                } else if (returnCode == 401) {
                    if (method.getResponseBodyAsString().contains("oauth_problem=\"token_expired\"")) {
                        if (i < 3) {
                            logger.warn("request failed for " + article.getArticleId() + " response " + returnCode + " token expired and try to renew token");
                            retrieveAccessToken(true);
                        } else {
                            logger.error("request failed for " + article.getArticleId() + " resp code: " + returnCode);
                            logger.error("response: " + method.getResponseBodyAsString());
                            throw new IOException("invalid resp received " + returnCode);
                        }
                    }
//                    if (i < 3) {
//                        logger.warn("request failed for " + article.getArticleId() + " response " + returnCode + " (retrying...)");
//                        try {
//                            TimeUnit.MILLISECONDS.sleep(750);
//                        } catch (InterruptedException ignored) {
//                        }
//                    } else {
//                        logger.error("request failed for " + article.getArticleId() + " giving up after " + i + " retries ");
//                        throw new IOException("request failed after too many retries");
//                    }
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

    private List<Toponym> parse(InputStream responseStream) throws IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser parser = factory.newSAXParser();
            final PlacemakerResponseHandler handler = new PlacemakerResponseHandler();
            parser.parse(responseStream, handler);

            return handler.getToponyms();
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("error occurred in parsing response: " + e.getMessage(), e);
        }
    }

    List<Toponym> extractGeoTags(final Article article) {
        try {
            final Stopwatch stopwatch = Stopwatch.createStarted();
            final List<Toponym> toponyms = post(article);
            stopwatch.stop();
            logger.debug("YahooYQL done for {} in {}", article.getArticleId(), stopwatch.elapsed(TimeUnit.SECONDS));
            return toponyms;
        } catch (IOException e) {
            logger.error(e);
        }

        return new LinkedList<>();
    }

}
