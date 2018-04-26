package tr.geotagging.commercial.googlenl;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Literal;
import tr.util.geo.GeoCoordinate;
import tr.util.geo.GeoUtil;

import java.util.Optional;

//import org.apache.jena.query.*;
//import org.apache.jena.rdf.model.Literal;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 8/11/2017
 * Time: 2:01 AM
 */
class Dbpedia {
    private static final String DBPEDIA_ENDPOINT = "http://dbpedia.org/sparql";

    private Dbpedia() {
    }

    private static final String COORDINATE_QUERY =
            "PREFIX dbp: <http://dbpedia.org/property/>\n" +
            "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n" +
            "SELECT ?latd ?latm ?lats ?latns ?longd ?longm ?longs ?longew ?geolat ?geolong\n" +
            "WHERE {\n" +
            "OPTIONAL {<http://dbpedia.org/resource/%1$s> dbp:latd ?latd .}\n" +
            "OPTIONAL {<http://dbpedia.org/resource/%1$s> dbp:latm ?latm .}\n" +
            "OPTIONAL {<http://dbpedia.org/resource/%1$s> dbp:lats ?lats .}\n" +
            "OPTIONAL {<http://dbpedia.org/resource/%1$s> dbp:latns ?latns .}\n" +
            "OPTIONAL {<http://dbpedia.org/resource/%1$s> dbp:longd ?longd .}\n" +
            "OPTIONAL {<http://dbpedia.org/resource/%1$s> dbp:longm ?longm .}\n" +
            "OPTIONAL {<http://dbpedia.org/resource/%1$s> dbp:longs ?longs .}\n" +
            "OPTIONAL {<http://dbpedia.org/resource/%1$s> dbp:longew ?longew .}\n" +
            "OPTIONAL {<http://dbpedia.org/resource/%1$s> geo:lat ?geolat .}\n" +
            "OPTIONAL {<http://dbpedia.org/resource/%1$s> geo:long ?geolong .}\n" +
            "}";

    static Optional<GeoCoordinate> getCoordinate(String resource) {
        Query query = QueryFactory.create(String.format(COORDINATE_QUERY, resource));

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(DBPEDIA_ENDPOINT, query)) {
            ResultSet results = qexec.execSelect();
            if (results.hasNext()) {
                QuerySolution soln = results.nextSolution();

                if (soln.contains("latd") && soln.contains("longd") && soln.contains("latns") && soln.contains("longew")) {
                    double latitude = GeoUtil.toDecimalDegree(
                            soln.getLiteral("latd").getInt(),
                            Optional.ofNullable(soln.getLiteral("latm")).map(Literal::getInt).orElse(0),
                            Optional.ofNullable(soln.getLiteral("lats")).map(Literal::getInt).orElse(0));
                    if (soln.getLiteral("latns").getString().equalsIgnoreCase("S"))
                        latitude *= -1;

                    double longitude = GeoUtil.toDecimalDegree(
                            soln.getLiteral("longd").getInt(),
                            Optional.ofNullable(soln.getLiteral("longm")).map(Literal::getInt).orElse(0),
                            Optional.ofNullable(soln.getLiteral("longs")).map(Literal::getInt).orElse(0));
                    if (soln.getLiteral("longew").getString().equalsIgnoreCase("W"))
                        longitude *= -1;

                    return Optional.of(new GeoCoordinate(latitude, longitude, resource));
                } else if (soln.contains("geolat") && soln.contains("geolong")) {
                    return Optional.of(new GeoCoordinate(
                            soln.getLiteral("geolat").getDouble(),
                            soln.getLiteral("geolong").getDouble(),
                            resource));
                }
            }
        }

        return Optional.empty();
    }
}
