package tr.geonames;

import tr.BoundingBox;
import tr.util.Config;
import com.google.common.base.Stopwatch;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 8/9/2017
 * Time: 12:04 AM
 */
class GeoNamesAPI {
    private final Logger logger = LogManager.getLogger(getClass());

    private static final Pattern errorPattern = Pattern.compile("<status message=\".*\" value=\"(\\d+)\"/>");
    private static final Pattern bboxPattern = Pattern.compile("<bbox>\\R*(.*)\\R*</bbox>", Pattern.DOTALL);
    private static final Pattern wikiPattern = Pattern.compile("<alternateName lang=\"link\">(https?://(.*)\\.wikipedia\\.org/wiki/.*)</alternateName>");

    private static final Pattern westPattern = Pattern.compile("<west>(-?[0-9]+(\\.[0-9]+)?)</west>");
    private static final Pattern eastPattern = Pattern.compile("<east>(-?[0-9]+(\\.[0-9]+)?)</east>");
    private static final Pattern northPattern = Pattern.compile("<north>(-?[0-9]+(\\.[0-9]+)?)</north>");
    private static final Pattern southPattern = Pattern.compile("<south>(-?[0-9]+(\\.[0-9]+)?)</south>");

    private static final int NO_RESULT_FOUND = 15;

    private static final int DAILY_LIMIT_EXCEEDED = 18;
    private static final int HOURLY_LIMIT_EXCEEDED = 19;
    private static final int WEEKLY_LIMIT_EXCEEDED = 20;
    private static final int SERVER_OVERLOADED_EXCEPTION = 22;

    private final HttpClient client;
    private final String[] userNames = Config.Gazetteer.GEONAMES_API_USERNAMES;
    private int currentUserNameIndex;

    GeoNamesAPI() {
        client = new HttpClient();
        currentUserNameIndex = 0;
    }

    private void get(Long geonameId, Consumer<String> responseConsumer) throws IOException {
        for (int i = 0; ; i++) {
            GetMethod method = new GetMethod("http://api.geonames.org/get");
            method.setQueryString(new NameValuePair[] {
                    new NameValuePair("username", userNames[currentUserNameIndex]),
                    new NameValuePair("geonameId", String.valueOf(geonameId))
            });

            final Stopwatch geoNamesRTT = Stopwatch.createStarted();
            final int returnCode = client.executeMethod(method);
            geoNamesRTT.stop();
            logger.debug("GeoNames API called <{}> for {} in {}", i + 1, geonameId, geoNamesRTT.elapsed(TimeUnit.SECONDS));

            if (returnCode == HttpStatus.SC_OK) {
                final String responseText = method.getResponseBodyAsString();
                final Matcher errorMatcher = errorPattern.matcher(responseText);
                if (errorMatcher.find()) {
                    final Integer errorCode = Integer.valueOf(errorMatcher.group(1));
                    if (errorCode == NO_RESULT_FOUND) {
                        throw new IllegalArgumentException("geonameId not found: " + geonameId);
                    } else if (errorCode == HOURLY_LIMIT_EXCEEDED || errorCode == DAILY_LIMIT_EXCEEDED || errorCode == WEEKLY_LIMIT_EXCEEDED) {
                        if (i < 10) {
                            currentUserNameIndex = (currentUserNameIndex+1) % userNames.length;
                            logger.warn("request failed for " + geonameId + " limit exceeded: " + returnCode + "username set to " + userNames[currentUserNameIndex] + " (retrying...)");
                        } else {
                            logger.error("request failed for " + geonameId + " limit exceeded: " + returnCode + "giving up (username is " + userNames[currentUserNameIndex] + ")");
                            throw new IOException("request failed due to exceeding limit (" + returnCode + ")");
                        }
                    } else if (errorCode == SERVER_OVERLOADED_EXCEPTION) {
                        if (i < 3) {
                            logger.warn("request failed for " + geonameId + " response " + returnCode + " (retrying...)");
                            try {
                                TimeUnit.MILLISECONDS.sleep(750);
                            } catch (InterruptedException ignored) {
                            }
                        } else {
                            logger.error("request failed for " + geonameId + " giving up after " + i + " retries ");
                            throw new IOException("request failed after too many retries");
                        }
                    }
                } else {
                    responseConsumer.accept(responseText);
                    break;
                }
            } else {
                logger.error("request failed for " + geonameId + " error code returned " + returnCode);
                throw new IOException("request failed due to " + returnCode + " error");
            }
        }
    }

    BoundingBox getBoundingBox(Long geonameId) throws Exception {
        final BoundingBox[] box = new BoundingBox[] {null};

        get(geonameId, responseText -> {
            final Matcher bboxMatcher = bboxPattern.matcher(responseText);
            if (bboxMatcher.find()) {
                final String bboxContent = bboxMatcher.group(1).replaceAll("\\R", "");

                final Matcher westMatcher = westPattern.matcher(bboxContent);
                westMatcher.find();
                final Double west = Double.valueOf(westMatcher.group(1));

                final Matcher eastMatcher = eastPattern.matcher(bboxContent);
                eastMatcher.find();
                final Double east = Double.valueOf(eastMatcher.group(1));

                final Matcher northMatcher = northPattern.matcher(bboxContent);
                northMatcher.find();
                final Double north = Double.valueOf(northMatcher.group(1));

                final Matcher southMatcher = southPattern.matcher(bboxContent);
                southMatcher.find();
                final Double south = Double.valueOf(southMatcher.group(1));

                box[0] = new BoundingBox(west, east, north, south);
            }
        });

        if (box[0] == null)
            throw new IllegalStateException("no bounding box found: " + geonameId);

        return box[0];
    }

    String getWikipediaUrl(Long geonameId) throws Exception {
        String[] wikipediaUrl = new String[] {null};

        get(geonameId, responseText -> {
            final Matcher wikiMatcher = wikiPattern.matcher(responseText);

            while (wikiMatcher.find()) {
                wikipediaUrl[0] = wikiMatcher.group(1);

                final String countryCode = wikiMatcher.group(2);
                if (countryCode.equalsIgnoreCase("en"))
                    break;
            }
        });

        if (wikipediaUrl[0] == null)
            throw new IllegalStateException("no wikipedia url found: " + geonameId);

        return wikipediaUrl[0];
    }
}
