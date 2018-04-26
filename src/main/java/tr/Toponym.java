package tr;

import tr.geonames.GeoNamable;
import tr.geonames.GeoNamesEntry;
import tr.util.geo.GeoCoordinate;

import java.util.Objects;
import java.util.Optional;

/**
 * This class illustrates a toponym representation in a text.
 * It is a subclass of {@link TaggedWord}, so while
 * it inherits textual information from {@link TaggedWord},
 * it comprises the geographical aspects of the toponym as well.
 * These fields are derived from <a href="http://geonames.org">GeoNames</a>.
 * Please refer to GeoNames <a href="http://download.geonames.org/export/dump/readme.txt">documentation</a>
 * to find the descriptions.
 * <p>
 * A toponym can only be built through {@code ToponymBuilder}.
 * </p>
 *
 * @see TaggedWord
 * @see ToponymBuilder
 */
public class Toponym extends TaggedWord implements GeoNamable {
    private final Double latitude, longitude;
    private final String name;

    private final Long geonameId;

    private final Long countryGeonameId;
    private final String country;

    private final Long admin1GeonameId;
    private final String admin1;

    private final Long admin2GeonameId;

    private final String featureClass;
    private final String featureCode;

    private double score = 0;

    private Toponym(String phrase,
                    int index, int start, int end,
                    Double latitude, Double longitude,
                    String name,
                    Long geonameId,
                    Long countryGeonameId, String country,
                    Long admin1GeonameId, String admin1,
                    Long admin2GeonameId,
                    String featureClass, String featureCode,
                    double score) {
        super(phrase, NamedEntityTag.LOCATION, index, start, end);

        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        this.geonameId = geonameId;
        this.countryGeonameId = countryGeonameId;
        this.country = country;
        this.admin1GeonameId = admin1GeonameId;
        this.admin1 = admin1;
        this.admin2GeonameId = admin2GeonameId;
        this.featureClass = featureClass;
        this.featureCode = featureCode;

        this.score = score;
    }

    public Toponym(TaggedWord taggedWord) {
        super(taggedWord.getPhrase(), NamedEntityTag.LOCATION, taggedWord.getIndex(), taggedWord.getStart(), taggedWord.getEnd());

        this.name = getPhrase();
        this.latitude = 0d;
        this.longitude = 0d;
        this.geonameId = null;
        this.countryGeonameId = null;
        this.country = null;
        this.admin1GeonameId = null;
        this.admin1 = null;
        this.admin2GeonameId = null;
        this.featureClass = null;
        this.featureCode = null;
    }

    public Toponym(TaggedWord taggedWord, GeoNamesEntry geoNamesEntry) {
        this(taggedWord.getPhrase(), taggedWord.getIndex(), taggedWord.getStart(), taggedWord.getEnd(), geoNamesEntry);
    }

    public Toponym(TaggedWord taggedWord, GeoNamesEntry geoNamesEntry, double score) {
        this(taggedWord.getPhrase(), taggedWord.getIndex(), taggedWord.getStart(), taggedWord.getEnd(), geoNamesEntry);

        this.score = score;
    }

    public Toponym(String phrase, int index, int start, int end, GeoNamesEntry geoNamesEntry) {
        super(phrase, NamedEntityTag.LOCATION, index, start, end);

        this.name = geoNamesEntry.getName();
        this.geonameId = geoNamesEntry.getGeonameId();
        this.latitude = geoNamesEntry.getLatitude();
        this.longitude = geoNamesEntry.getLongitude();
        this.featureClass = geoNamesEntry.getFeatureClass();
        this.featureCode = geoNamesEntry.getFeatureCode();

        this.countryGeonameId = null;
        this.country = null;
        this.admin1GeonameId = null;
        this.admin1 = null;
        this.admin2GeonameId = null;
    }

    /**
     * Checks whether two toponym objects are equal.
     * @param o the second toponym.
     * @return {@code true} if the coordinates (i.e., latitude and longitude) are the same.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Toponym toponym = (Toponym) o;
        return super.equals(toponym) &&
                Double.compare(toponym.latitude, latitude) == 0 &&
                Double.compare(toponym.longitude, longitude) == 0;
    }

    /**
     * Returns a hash code value based on {@link TaggedWord#hashCode()}
     * and the coordinates.
     * @return a hash code for this toponym.
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), latitude, longitude);
    }

    @Override
    public String toString() {
        return String.format("%s (%f,%f)", getPhrase(), latitude, longitude);
    }

    /**
     * Checks if this toponym has coordinates.
     * Because there may be toponyms with no coordinates in the dataset.
     * This case happens if the toponym does not exist in <a href="http://geonames.org">GeoNames</a>.
     * @return {@code true} if the toponym has non-null latitude and longitude.
     */
    public boolean hasCoordinate() {
        return latitude != null && longitude != null;
    }

    /**
     * Converts the toponym object to a {@link GeoCoordinate} object.
     * @return {@code GeoCoordiante} object corresponding to the toponym.
     */
    @Override
    public GeoCoordinate toCoordinate() {
        return new GeoCoordinate(latitude, longitude, name);
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getName() {
        return name;
    }

    @Override
    public Long getGeonameId() {
        return geonameId;
    }

    public Long getCountryGeonameId() {
        return countryGeonameId;
    }

    public String getCountry() {
        return country;
    }

    public Long getAdmin1GeonameId() {
        return admin1GeonameId;
    }

    public String getAdmin1() {
        return admin1;
    }

    public Long getAdmin2GeonameId() {
        return admin2GeonameId;
    }

    public String getFeatureClass() {
        return featureClass;
    }

    public String getFeatureCode() {
        return featureCode;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    /**
     * A Builder class for {@link Toponym} object.
     */
    public static class ToponymBuilder {
        private String phrase, name;
        private String startText, endText;
        private int index, start, end;
        private String latText, lngText;
        private Double latitude, longitude;
        private Long geonameId;
        private Long countryGeonameId, admin1GeonameId, admin2GeonameId;
        private String country, admin1;
        private String featureClass, featureCode;
        private double score;

        public ToponymBuilder() {
        }

        public ToponymBuilder(Toponym src) {
            this.phrase = src.getPhrase();
            this.name = src.getName();
            this.index = src.getIndex();
            this.start = src.getStart();
            this.end = src.getEnd();
            this.latitude = src.getLatitude();
            this.longitude = src.getLongitude();
            this.geonameId = src.getGeonameId();
            this.countryGeonameId = src.getCountryGeonameId();
            this.admin1GeonameId = src.getAdmin1GeonameId();
            this.admin2GeonameId = src.getAdmin2GeonameId();
            this.country = src.getCountry();
            this.admin1 = src.getAdmin1();
            this.featureClass = src.getFeatureClass();
            this.featureCode = src.getFeatureCode();
            this.score = src.getScore();
        }



        public ToponymBuilder withPhrase(String phrase) {
            this.phrase = Optional.ofNullable(this.phrase).orElse("") + phrase;
            return this;
        }

        public ToponymBuilder withName(String name) {
            this.name = Optional.ofNullable(this.name).orElse("") + name;
            return this;
        }

        public ToponymBuilder withScore(double score) {
            this.score = score;
            return this;
        }

        public ToponymBuilder withFeatureClass(String featureClass) {
            this.featureClass = Optional.ofNullable(this.featureClass).orElse("") + featureClass;
            return this;
        }

        public ToponymBuilder withFeatureCode(String featureCode) {
            this.featureCode = Optional.ofNullable(this.featureCode).orElse("") + featureCode;
            return this;
        }

        public ToponymBuilder withCountry(String country) {
            this.country = Optional.ofNullable(this.country).orElse("") + country;
            return this;
        }

        public ToponymBuilder withAdmin1(String admin1) {
            this.admin1 = Optional.ofNullable(this.admin1).orElse("") + admin1;
            return this;
        }

        public ToponymBuilder withGeonameId(Long geonameId) {
            this.geonameId = geonameId;
            return this;
        }

        public ToponymBuilder withCountryGeonameId(Long countryGeonameId) {
            this.countryGeonameId = countryGeonameId;
            return this;
        }

        public ToponymBuilder withAdmin1GeonameId(Long admin1GeonameId) {
            this.admin1GeonameId = admin1GeonameId;
            return this;
        }

        public ToponymBuilder withAdmin2GeonameId(Long admin2GeonameId) {
            this.admin2GeonameId = admin2GeonameId;
            return this;
        }

        public ToponymBuilder withLatitude(String latitude) {
            this.latText = Optional.ofNullable(this.latText).orElse("") + latitude;
            return this;
        }

        public ToponymBuilder withLatitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public ToponymBuilder withLongitude(String longitude) {
            this.lngText = Optional.ofNullable(this.lngText).orElse("") + longitude;
            return this;
        }

        public ToponymBuilder withLongitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public ToponymBuilder withIndex(int index) {
            this.index = index;
            return this;
        }

        public ToponymBuilder withStart(String start) {
            this.startText = Optional.ofNullable(this.startText).orElse("") + start;
            return this;
        }

        public ToponymBuilder withStart(int start) {
            this.start = start;
            return this;
        }

        public ToponymBuilder withEnd(String end) {
            this.endText = Optional.ofNullable(this.endText).orElse("") + end;
            return this;
        }

        public ToponymBuilder withEnd(int end) {
            this.end = end;
            return this;
        }

        public ToponymBuilder trimPhrase() {
            Optional.ofNullable(this.phrase).map(String::trim).ifPresent(p -> this.phrase = p);
            return this;
        }

        public ToponymBuilder trimName() {
            Optional.ofNullable(this.name).map(String::trim).ifPresent(n -> this.name = n);
            return this;
        }

        public ToponymBuilder trimFeatureClass() {
            Optional.ofNullable(this.featureClass).map(String::trim).ifPresent(f -> this.featureClass = f);
            return this;
        }

        public ToponymBuilder trimFeatureCode() {
            Optional.ofNullable(this.featureCode).map(String::trim).ifPresent(f -> this.featureCode = f);
            return this;
        }

        public ToponymBuilder trimCountry() {
            Optional.ofNullable(this.country).map(String::trim).ifPresent(c -> this.country = c);
            return this;
        }

        public ToponymBuilder trimAdmin1() {
            Optional.ofNullable(this.admin1).map(String::trim).ifPresent(a1 -> this.admin1 = a1);
            return this;
        }

        public ToponymBuilder setStart() {
            Optional.ofNullable(this.startText).map(String::trim).ifPresent(start -> this.start = Integer.valueOf(start));
            return this;
        }

        public ToponymBuilder setEnd() {
            Optional.ofNullable(this.endText).map(String::trim).ifPresent(end -> this.end = Integer.valueOf(end));
            return this;
        }

        public ToponymBuilder setLatitude() {
            Optional.ofNullable(this.latText).map(String::trim).ifPresent(latitude -> this.latitude = Double.valueOf(latitude));
            return this;
        }

        public ToponymBuilder setLongitude() {
            Optional.ofNullable(this.lngText).map(String::trim).ifPresent(longitude -> this.longitude = Double.valueOf(longitude));
            return this;
        }

        public Toponym build() {
            return new Toponym(phrase, index, start, end, latitude, longitude, name,
                    geonameId, countryGeonameId, country, admin1GeonameId, admin1,
                    admin2GeonameId, featureClass, featureCode, score);
        }


    }
}
