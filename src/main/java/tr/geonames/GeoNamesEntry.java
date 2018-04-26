package tr.geonames;

import tr.util.geo.GeoCoordinate;

import java.util.Objects;

public class GeoNamesEntry implements GeoNamable {

	private Long geonameId;
	private String name;
	private String alternateNames;
	private Double latitude, longitude;
	private String featureCode, featureClass;
	private String countryCode;
	private String admin1code, admin2code, admin3Code, admin4Code;
	private Long population;
	private String modificationDate;

	private GeoNamesLevel level;

	@Override
	public String toString() {
		return String.format("%s (%d)", name, geonameId);
	}

	@Override
    public GeoCoordinate toCoordinate() {
	    return new GeoCoordinate(latitude, longitude, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeoNamesEntry that = (GeoNamesEntry) o;
        return Objects.equals(geonameId, that.geonameId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(geonameId);
    }

    @Override
    public Long getGeonameId() {
		return geonameId;
	}

	public void setGeonameId(Long geonamesId) {
		this.geonameId = geonamesId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAlternateNames() {
		return alternateNames;
	}

	public void setAlternateNames(String alternateNames) {
		this.alternateNames = alternateNames;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public String getFeatureCode() {
		return featureCode;
	}

	public void setFeatureCode(String featureCode) {
		this.featureCode = featureCode;
	}

	public String getFeatureClass() {
		return featureClass;
	}

	public void setFeatureClass(String featureClass) {
		this.featureClass = featureClass;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getAdmin1code() {
		return admin1code;
	}

	public void setAdmin1code(String admin1code) {
		this.admin1code = admin1code;
	}

	public String getAdmin2code() {
		return admin2code;
	}

	public void setAdmin2code(String admin2code) {
		this.admin2code = admin2code;
	}

	public String getAdmin3Code() {
		return admin3Code;
	}

	public void setAdmin3Code(String admin3Code) {
		this.admin3Code = admin3Code;
	}

	public String getAdmin4Code() {
		return admin4Code;
	}

	public void setAdmin4Code(String admin4Code) {
		this.admin4Code = admin4Code;
	}

	public Long getPopulation() {
		return population;
	}

	public void setPopulation(Long population) {
		this.population = population;
	}

	public String getModificationDate() {
		return modificationDate;
	}

	public void setModificationDate(String modificationDate) {
		this.modificationDate = modificationDate;
	}

    public GeoNamesLevel getLevel() {
        return level;
    }

    public void setLevel(GeoNamesLevel level) {
        this.level = level;
    }
}
