package tr.geonames;

import tr.BoundingBox;
import tr.util.Config;
import tr.util.db.BatchExecutor;
import tr.util.db.IdNotFoundException;
import tr.util.db.SQLiteConnector;
import tr.util.redis.RedisHash;
import tr.util.redis.RedisKey;
import tr.util.redis.RedisSet;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 11/11/2016
 * Time: 1:16 PM
 */
public class GeoNamesRepository {

    private final Logger logger = LogManager.getLogger(getClass());

    private static final BiMap<String, Long> continentMap = HashBiMap.create();
    private static final BiMap<String, Long> countryMap = HashBiMap.create();
    private static final BiMap<String, Long> admin1Map = HashBiMap.create();
    private static final BiMap<String, Long> admin2Map = HashBiMap.create();

    private final GeoNamesAPI geoNamesAPI = new GeoNamesAPI();

    static {
        try {
            final List<String> countries = Files.readAllLines(Paths.get("data/gazetteer/country.csv"));
            for (String country : countries) {
                final String[] tokens = country.split(",");
                countryMap.put(tokens[0], Long.valueOf(tokens[1]));
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }

        try {
            final List<String> admin1s = Files.readAllLines(Paths.get("data/gazetteer/admin1.csv"));
            for (String admin1 : admin1s) {
                final String[] tokens = admin1.split(",");
                admin1Map.put(tokens[0], Long.valueOf(tokens[1]));
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }

        try {
            final List<String> admin2s = Files.readAllLines(Paths.get("data/gazetteer/admin2.csv"));
            for (String admin2 : admin2s) {
                final String[] tokens = admin2.split(",");
                admin2Map.put(tokens[0], Long.valueOf(tokens[1]));
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }

        try {
            final List<String> continents = Files.readAllLines(Paths.get("data/gazetteer/continents.csv"));
            for (String continent : continents) {
                final String[] tokens = continent.split(",");
                continentMap.put(tokens[0], Long.valueOf(tokens[1]));
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final SQLiteConnector connector = new SQLiteConnector(Config.Gazetteer.GEONAMES_SQLITE);
    private final ResultSetToEntryMapper resultSetToEntryMapper = new ResultSetToEntryMapper();

    final GeoNamesEntry earthEntry;

    public GeoNamesRepository() {
        this(false);
    }

    public GeoNamesRepository(boolean isImport) {
        if (!isImport) {
            try {
                earthEntry = load(6295630L, entry -> {});
                earthEntry.setLevel(GeoNamesLevel.EARTH);
            } catch (IdNotFoundException e) {
                throw new ExceptionInInitializerError("earth entry not found in geonames: " + e.getMessage());
            }
        } else {
            earthEntry = null;
        }
    }

    public void createSchema() {
        try {
            final Connection conn = connector.openWritableConnection();
            final Statement statement = conn.createStatement();
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS geonames " +
                    "(geonameId INT PRIMARY KEY," +
                    "name VARCHAR, alternateNames VARCHAR, latitude REAL, longitude REAL," +
                    "featureCode VARCHAR, featureClass VARCHAR, countryCode VARCHAR," +
                    "admin1Code VARCHAR, admin2Code VARCHAR, admin3Code VARCHAR, admin4Code VARCHAR," +
                    "population BIGINT, modificationDate VARCHAR)");

            statement.executeUpdate("CREATE INDEX IF NOT EXISTS geonames_name_idx ON geonames(name COLLATE NOCASE)");
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public BatchExecutor startBatch(int batchSize) {
        return new BatchExecutor(
                connector::openParallelWritableConnection,
                batchSize,
                "INSERT INTO geonames VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
    }

    public GeoNamesEntry load(final Long geonameId) throws IdNotFoundException {
        return load(geonameId, entry -> entry.setLevel(findLevel(entry)));
    }

    private GeoNamesEntry load(final Long geonameId, Consumer<GeoNamesEntry> postloadConsumer) throws IdNotFoundException {
        try(final Connection connection = connector.openReadOnlyConnection()) {
            final PreparedStatement pstmt = connection.prepareStatement("SELECT * FROM geonames WHERE geonameId = ?");
            pstmt.setLong(1, geonameId);

            final ResultSet rs = pstmt.executeQuery();

            GeoNamesEntry entry = null;
            if (rs.next())
                entry = resultSetToEntryMapper.transform(rs);

            pstmt.close();

            if (entry != null) {
                postloadConsumer.accept(entry);
                return entry;
            }
        } catch (SQLException e) {
            logger.error("unable to load", e);
        }

        throw new IdNotFoundException();
    }

    public long count(final String toponymText) {
        return newRedisSet(toponymText).size();
    }

    public List<GeoNamesEntry> load(final String toponymText) {
        final List<GeoNamesEntry> entries = new ArrayList<>();

        try(final Connection connection = connector.openReadOnlyConnection()) {
            RedisSet<Long> geonamesRedis = newRedisSet(toponymText);

            final PreparedStatement pstmt = connection.prepareStatement("SELECT * FROM geonames WHERE geonameId = ?");

            for (Long geonameId : geonamesRedis) {
                pstmt.setLong(1, geonameId);
                final ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    final GeoNamesEntry entry = resultSetToEntryMapper.transform(rs);
                    entry.setLevel(findLevel(entry));
                    entries.add(entry);
                }
            }

            pstmt.close();
        } catch (SQLException e) {
            logger.error("unable to load", e);
        }

        return entries;
    }

    private RedisSet<Long> newRedisSet(String toponymText) {
        RedisSet<Long> geonamesRedis = RedisSet.newLongSet(toponymText.toLowerCase());
        if (geonamesRedis.isEmpty()) {
            geonamesRedis = RedisSet.newLongSet(toponymText.toLowerCase().replaceAll("\\.", ""));
        }
        return geonamesRedis;
    }

    private boolean isContinent(Long geonameId) {
        return continentMap.inverse().containsKey(geonameId);
    }

    public boolean isCountry(Long geonameId) {
        return countryMap.inverse().containsKey(geonameId);
    }

    private boolean isAdmin1(Long geonameId) {
        return admin1Map.inverse().containsKey(geonameId);
    }

    private boolean isAdmin2(Long geonameId) {
        return admin2Map.inverse().containsKey(geonameId);
    }

    public Optional<GeoNamesEntry> loadCountryByCode(String countryCode) {
//        final RedisHash<Long> countryHash = RedisHash.newLongSet(COUNTRY_HASH_KEY);
        return Optional.ofNullable(countryMap.get(countryCode)).map(countryId -> {
            try {
                return load(countryId);
            } catch (IdNotFoundException e) {
                return null;
            }
        });
    }

    public Optional<GeoNamesEntry> loadAdmin1ByCode(String adminCode1) {
//        final RedisHash<Long> adminHash1 = RedisHash.newLongSet(ADMIN1_HASH_KEY);
        return Optional.ofNullable(admin1Map.get(adminCode1)).map(adminId -> {
            try {
                return load(adminId);
            } catch (IdNotFoundException e) {
                return null;
            }
        });
    }

    public Optional<GeoNamesEntry> loadAdmin2ByCode(String adminCode2) {
//        final RedisHash<Long> adminHash2 = RedisHash.newLongSet(ADMIN2_HASH_KEY);
        return Optional.ofNullable(admin2Map.get(adminCode2)).map(adminId -> {
            try {
                return load(adminId);
            } catch (IdNotFoundException e) {
                return null;
            }
        });
    }

    private GeoNamesLevel findLevel(GeoNamesEntry entry) {
        if (earthEntry.equals(entry))
            return GeoNamesLevel.EARTH;
        if (isContinent(entry.getGeonameId()))
            return GeoNamesLevel.CONTINENT;
        else if (isCountry(entry.getGeonameId()))
            return GeoNamesLevel.COUNTRY;
        else if (isAdmin1(entry.getGeonameId()))
            return GeoNamesLevel.ADMIN1;
        else if (isAdmin2(entry.getGeonameId()))
            return GeoNamesLevel.ADMIN2;
        else
            return GeoNamesLevel.LEAF;
    }

    public Optional<BoundingBox> getBoundingBox(Long geonameId) {
        final RedisHash<Double> bboxHash = RedisHash.newDoubleSet("bbox:" + geonameId);
        final Map<String, Double> box = bboxHash.getAll();

        if (box.isEmpty()) {
            try {
                final BoundingBox boundingBox = geoNamesAPI.getBoundingBox(geonameId);
                bboxHash.set(boundingBox.asMap());

                return Optional.of(boundingBox);
            } catch (IllegalStateException e) {
                bboxHash.set("0", 0D);
            } catch (Exception ignored) {
            }

            return Optional.empty();
        }

        if (box.containsKey("0"))
            return Optional.empty();

        return Optional.of(new BoundingBox(box.get("w"), box.get("e"), box.get("n"), box.get("s")));
    }

    public Optional<String> getWikipediaUrl(Long geonameId) {
        final RedisKey<String> wikiKey = RedisKey.newStringSet();

        String key = "wiki:" + geonameId;
        Optional<String> wikiUrl = wikiKey.get(key);

        if (!wikiUrl.isPresent()) {
            try {
                String wikipediaUrl = geoNamesAPI.getWikipediaUrl(geonameId);
                wikiKey.set(key, wikipediaUrl);

                return Optional.of(wikipediaUrl);
            } catch (IllegalStateException e) {
                wikiKey.set(key, "0");
            } catch (Exception ignored) {
            }

            return Optional.empty();
        } else {
            if (wikiUrl.get().equals("0"))
                return Optional.empty();

            return wikiUrl;
        }
    }

    private class ResultSetToEntryMapper {
        GeoNamesEntry transform(ResultSet rs) throws SQLException {
            final GeoNamesEntry entry = new GeoNamesEntry();

            entry.setGeonameId(rs.getLong("geonameId"));
            entry.setName(rs.getString("name"));
            entry.setAlternateNames(rs.getString("alternateNames"));
            entry.setLatitude(rs.getDouble("latitude"));
            entry.setLongitude(rs.getDouble("longitude"));
            entry.setFeatureCode(rs.getString("featureCode"));
            entry.setFeatureClass(rs.getString("featureClass"));
            entry.setAdmin1code(rs.getString("admin1Code"));
            entry.setAdmin2code(rs.getString("admin2Code"));
            entry.setCountryCode(rs.getString("countryCode"));
            entry.setPopulation(rs.getLong("population"));
            entry.setModificationDate(rs.getString("modificationDate"));

            return entry;
        }
    }
}
