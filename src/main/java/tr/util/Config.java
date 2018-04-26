package tr.util;

import java.io.IOException;
import java.util.Properties;

/**
 * {@code Config} class handles the parameters that should be read
 * from configuration file.
 * A {@code config.properties} must exist in the resources directory.
 * <p>
 * The parameters are divided into a number of sections:
 * <strong>Gazetteer</strong>, <strong>Data</strong>, <strong>Redis</strong>,
 * <strong>ContextBound</strong>, <strong>Fusion</strong>,
 * <strong>Adaptive</strong>, and <strong>Eval</strong>.
 * The parameters are explained in the sample config file provided in resources.
 * </p>
 */
public class Config {
    public interface Parameter {
        String GEONAMES_SQLITE = "gazetteer.geonames.sqlite";
        String GEONAMES_DB = "gazetteer.geonames.db";
        String GEONAMES_API_USERNAMES = "gazetteer.geonames.api.usernames";
        String OPENCALAIS_ACCESS_TOKEN = "opencalais.access_token";
        String YAHOO_YDN_CONSUMER_KEY = "yahoo.ydn.consumer_key";
        String YAHOO_YDN_CONSUMER_SECRET = "yahoo.ydn.consumer_secret";
        String YAHOO_YDN_AUTH_CODE = "yahoo.ydn.authorization_code";
        String GOOGLE_CLOUD_KEY_FILE = "google.cloud.key_file";
        String REDIS_HOST = "redis.host";
        String REDIS_PORT = "redis.port";
        String REDIS_TIMEOUT = "redis.timeout";
        String REDIS_MAX_CONNECTIONS = "redis.max_connections";
        String DATA_LARGE_CITY_POPULATION = "data.large_city.population";
        String CBH_MAX_ITERATIONS = "resolvers.cbh.max_iterations";
        String ADAPTIVE_DEFAULT_Wb = "resolvers.adaptive.default.window_breadth";
        String ADAPTIVE_DEFAULT_N_FEATURES = "resolvers.adaptive.default.n_features";
        String ADAPTIVE_DEFAULT_N_TREES = "resolvers.adaptive.default.n_trees";
        String CHF_DEFAULT_THRESHOLD = "resolvers.chf.default_threshold";
        String EVAL_DEFAULT_DISTANCE = "eval.default_distance";
        String EVAL_DEFAULT_N_FOLD = "eval.default.n_folds";
    }

    public interface Gazetteer {
        String GEONAMES_SQLITE = Config.get(Parameter.GEONAMES_SQLITE);
        String GEONAMES_DB = Config.get(Parameter.GEONAMES_DB);
        String ADJECTIVAL_FILE = "data/gazetteer/adjectival.txt";
        String ABBREVIATION_FILE = "data/gazetteer/abbreviation.txt";
        String[] GEONAMES_API_USERNAMES = Config.getAndSplit(Parameter.GEONAMES_API_USERNAMES, "\\|");
    }

    public interface OpenCalais {
        String ACCESS_TOKEN = Config.get(Parameter.OPENCALAIS_ACCESS_TOKEN);
    }

    public interface YahooYDN {
        String CONSUMER_KEY = Config.get(Parameter.YAHOO_YDN_CONSUMER_KEY);
        String CONSUMER_SECRET = Config.get(Parameter.YAHOO_YDN_CONSUMER_SECRET);
        String AUTH_CODE = Config.get(Parameter.YAHOO_YDN_AUTH_CODE);
    }

    public interface GoogleCloud {
        String KEY_FILE = Config.get(Parameter.GOOGLE_CLOUD_KEY_FILE);
    }

    public interface Data {
        int LARGE_CITY_POPULATION = Config.getInt(Parameter.DATA_LARGE_CITY_POPULATION);
    }

    public static class Redis {
        public static String HOST = Config.get(Parameter.REDIS_HOST);
        public static Integer PORT = Config.getInt(Parameter.REDIS_PORT);
        public static Integer TIMEOUT = Config.getInt(Parameter.REDIS_TIMEOUT);
        public static Integer MAX_CONNECTIONS = Config.getInt(Parameter.REDIS_MAX_CONNECTIONS);
    }

    public interface CBH {
        Integer MAX_ITERATIONS = Config.getInt(Parameter.CBH_MAX_ITERATIONS);
    }

    public interface Adaptive {
        int DEFAULT_Wb = Config.getInt(Parameter.ADAPTIVE_DEFAULT_Wb);
        int DEFAULT_N_FEATURES = Config.getInt(Parameter.ADAPTIVE_DEFAULT_N_FEATURES);
        int DEFAULT_N_TREES = Config.getInt(Parameter.ADAPTIVE_DEFAULT_N_TREES);
    }

    public interface CHF {
        double DEFAULT_THRESHOLD = Config.getDouble(Parameter.CHF_DEFAULT_THRESHOLD);
    }

    public interface Eval {
        double DEFAULT_DISTANCE = Config.getDouble(Parameter.EVAL_DEFAULT_DISTANCE);
        int DEFUALT_N_FOLDS = Config.getInt(Parameter.EVAL_DEFAULT_N_FOLD);
    }

    private static final Properties properties = new Properties();

    static {
        try {
            properties.load(Config.class.getClassLoader().getResourceAsStream("config.properties"));
        } catch (IOException e) {
            throw new ExceptionInInitializerError("unable to load config file: " + e.getMessage());
        }
    }

    public static String get(String parameter) {
        return properties.getProperty(parameter);
    }

    public static String[] getAndSplit(String parameter, String delimiter) {
        return get(parameter).split(delimiter);
    }

    public static String getOrDefault(String parameter, String defaultValue) {
        return properties.getProperty(parameter, defaultValue);
    }

    public static Integer getInt(String parameter) {
        return Integer.valueOf(get(parameter));
    }

    public static Double getDouble(String parameter) {
        return Double.valueOf(get(parameter));
    }
}
