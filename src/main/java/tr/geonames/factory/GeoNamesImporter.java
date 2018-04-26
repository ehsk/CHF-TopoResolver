package tr.geonames.factory;

import com.google.common.base.Stopwatch;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tr.geonames.GeoNamesEntry;
import tr.geonames.GeoNamesRepository;
import tr.util.Config;
import tr.util.db.BatchExecutor;
import tr.util.redis.PipelinedSet;
import tr.util.redis.RedisDAO;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 4/2/2018
 * Time: 11:08 AM
 */
public class GeoNamesImporter {

    private static final Logger logger = LogManager.getLogger(GeoNamesImporter.class);

    public static void main(String[] args) {
        ArgumentParser argParser = ArgumentParsers.newFor("GeoNamesImporter")
                .build()
                .defaultHelp(true)
                .description("Builds a SQLite database as well as Redis key-values by importing data from GeoNames");
        argParser.addArgument("--geonames")
                .required(true)
                .help("GeoNames file (allCountries.zip file should be downloaded from GeoNames and extracted)");
        argParser.addArgument("--adj")
                .setDefault(Config.Gazetteer.ADJECTIVAL_FILE)
                .help("Adjectival names for countries: A tab separated file where the first name is the actual country name");
        argParser.addArgument("--abbr")
                .setDefault(Config.Gazetteer.ABBREVIATION_FILE)
                .help("Abbreviations file for some U.S. states and countries: A tab separated file containing a name and the corresponding GeoNames Id");
        argParser.addArgument("--redis_host")
                .setDefault(Config.Redis.HOST)
                .help("Redis host");
        argParser.addArgument("--redis_port")
                .type(Integer.class)
                .setDefault(Config.Redis.PORT)
                .help("Redis port");

        final Namespace ns = argParser.parseArgsOrFail(args);

        Config.Redis.PORT = ns.getInt("redis_port");
        Config.Redis.HOST = ns.getString("redis_host");

        String geoNamesFile = ns.getString("geonames");
        if (Files.notExists(Paths.get(geoNamesFile)))
            throw new IllegalArgumentException(
                    String.format("GeoNames file cannot be found in the given path: '%s'. " +
                            "Modify the argument or download allCountries.zip from GeoNames website and extract it",
                            geoNamesFile));

        String abbreviationsFile = ns.getString("abbr");
        if (Files.notExists(Paths.get(abbreviationsFile)))
            throw new IllegalArgumentException(
                    String.format("Abbreviations file cannot be found in the given path: '%s'. " +
                                    "By default, it is located in '%s'",
                            abbreviationsFile, Config.Gazetteer.ABBREVIATION_FILE));

        String adjectivalFile = ns.getString("adj");
        if (Files.notExists(Paths.get(adjectivalFile)))
            throw new IllegalArgumentException(
                    String.format("Adjectival names cannot be found in the given path: '%s'. " +
                                    "By default, it is located in '%s'",
                            adjectivalFile, Config.Gazetteer.ADJECTIVAL_FILE));

        importGeoNames(geoNamesFile);
        importAbbreviations(abbreviationsFile);
        importAdjectivalNames(adjectivalFile);
    }

    private static void importAbbreviations(String abbrvFile) {
        try(final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(abbrvFile), "UTF-8"))) {
            final Stopwatch stopwatch = Stopwatch.createStarted();

            for (String line; (line = reader.readLine()) != null;) {

                if (line.isEmpty())
                    continue;

                final String[] entries = line.split("\t");
                final String location = entries[0].trim();
                final Long geonameId = Long.valueOf(entries[1].trim());

                RedisDAO.pipeline(pipeline -> {
                    new PipelinedSet<Long>(location.toLowerCase(), pipeline).add(geonameId);
                    return 0;
                });
            }

            stopwatch.stop();
            logger.info("Abbreviations successfully imported in {} s", stopwatch.elapsed(TimeUnit.SECONDS));
        } catch (IOException e) {
            logger.error("something went wrong during parsing", e);
        }
    }

    private static void importAdjectivalNames(String adjFile) {

        try(final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(adjFile), "UTF-8"))) {
            final Stopwatch stopwatch = Stopwatch.createStarted();

            final GeoNamesRepository geoNamesRepository = new GeoNamesRepository();

            for (String line; (line = reader.readLine()) != null;) {

                final String[] entries = line.split("\t|,| or |/");
                final String countryName = entries[0].trim();

                final List<GeoNamesEntry> countries = geoNamesRepository.load(countryName)
                        .stream()
                        .filter(e -> geoNamesRepository.isCountry(e.getGeonameId()))
                        .collect(Collectors.toList());

                if (countries.isEmpty()) {
                    logger.error("Could not find country: <{}>", countryName);
                    continue;
                } else if (countries.size() > 1) {
                    logger.warn("More than one country found: <{}> {}", countryName, countries.stream().map(e -> String.valueOf(e.getGeonameId())).collect(Collectors.joining(",")));
                    continue;
                }

                final GeoNamesEntry country = countries.get(0);

                for (int i = 1; i < entries.length; i++) {
                    String adjectival = entries[i].trim();

                    if (adjectival.matches("\".*\""))
                        adjectival = adjectival.substring(1, adjectival.length() - 1);

                    if (adjectival.isEmpty() || adjectival.equalsIgnoreCase("none"))
                        continue;

                    final String key = adjectival.toLowerCase();
                    logger.debug("{},{},{},{}", country.getCountryCode(), country.getName(), country.getGeonameId(), adjectival);
                    RedisDAO.pipeline(pipeline -> {
                        new PipelinedSet<Long>(key, pipeline).add(country.getGeonameId());
                        return 0;
                    });
                }
            }

            stopwatch.stop();
            logger.info("Adjectival names successfully imported in {} s", stopwatch.elapsed(TimeUnit.SECONDS));
        } catch (IOException e) {
            logger.error("something went wrong during parsing", e);
        }
    }

    private static void importGeoNames(String geoNamesFile) {
        final GeoNamesRepository repository = new GeoNamesRepository(true);

        try(final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(geoNamesFile), "UTF-8"))) {
            final Stopwatch stopwatch = Stopwatch.createStarted();

            repository.createSchema();
            final BatchExecutor batchExecutor = repository.startBatch(5000);

            int i = 0;
            for (String line; (line = reader.readLine()) != null;) {
                i += 1;

                if (i % 100000 == 0) {
                    logger.info(String.format("  at line %d", i));
                }

                final String[] entries = line.split("\\t");
                final Long geonameId = Long.valueOf(entries[0]);
                final String name = entries[1];
                final String alternateNames = entries[3];
                final Double latitude = Double.valueOf(entries[4]);
                final Double longitude = Double.valueOf(entries[5]);
                final String featureCode = entries[6];
                final String featureClass = entries[7];
                final String countryCode = entries[8];
                final String admin1Code = entries[10];
                final String admin2Code = entries[11];
                final String admin3Code = entries[12];
                final String admin4Code = entries[13];
                final Long population = Long.valueOf(entries[14]);
                final String modificationDate = entries[18];

                batchExecutor.addBatch(p -> {
                    try {
                        p.setLong(1, geonameId);
                        p.setString(2, name);
                        p.setString(3, alternateNames);
                        p.setDouble(4, latitude);
                        p.setDouble(5, longitude);
                        p.setString(6, featureCode);
                        p.setString(7, featureClass);
                        p.setString(8, countryCode);
                        p.setString(9, admin1Code);
                        p.setString(10, admin2Code);
                        p.setString(11, admin3Code);
                        p.setString(12, admin4Code);
                        p.setLong(13, population);
                        p.setString(14, modificationDate);
                    } catch (SQLException ignored) {
                    }
                });

                RedisDAO.pipeline(pipeline -> {
                    new PipelinedSet<Long>(name.toLowerCase(), pipeline).add(geonameId);
                    if (alternateNames != null && !alternateNames.isEmpty()) {
                        for (String altName : alternateNames.split(",")) {
                            if (!altName.equals(name))
                                new PipelinedSet<Long>(altName.toLowerCase(), pipeline).add(geonameId);
                        }
                    }

                    return 0;
                });
            }

            batchExecutor.finalizeBatch();
            
            stopwatch.stop();
            logger.info("GeoNames successfully imported in {} s", stopwatch.elapsed(TimeUnit.SECONDS));
        } catch (IOException | SQLException e) {
            logger.error("something went wrong during parsing", e);
        }
    }
}
