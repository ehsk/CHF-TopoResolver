package tr.geotagging.commercial.placemaker;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import tr.Toponym;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 8/10/2017
 * Time: 9:54 PM
 */
public class PlacemakerResponseHandler extends DefaultHandler {
    private boolean isPlaceTag, isWoeIdTag, isTypeTag, isNameTag, isLatTag, isLonTag;
    private boolean isReferenceTag, isStartTag, isEndTag, isTextTag;
    private Toponym.ToponymBuilder toponymBuilder;

    private final List<Toponym> toponyms = new ArrayList<>();

    public List<Toponym> getToponyms() {
        return toponyms;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equalsIgnoreCase("match")) {
            this.toponymBuilder = new Toponym.ToponymBuilder();
        } else if (qName.equalsIgnoreCase("place")) {
            isPlaceTag = true;
        } else if (qName.equalsIgnoreCase("woeId")) {
            isWoeIdTag = true;
        } else if (qName.equalsIgnoreCase("type")) {
            isTypeTag = true;
        } else if (qName.equalsIgnoreCase("name")) {
            isNameTag = true;
        } else if (qName.equalsIgnoreCase("latitude")) {
            isLatTag = true;
        } else if (qName.equalsIgnoreCase("longitude")) {
            isLonTag = true;
        } else if (qName.equalsIgnoreCase("reference")) {
            isReferenceTag = true;
            final Toponym.ToponymBuilder newBuilder = new Toponym.ToponymBuilder();
            
            if (this.toponymBuilder != null) {
                final Toponym t = toponymBuilder.build();
                newBuilder.withFeatureCode(t.getFeatureCode()).withFeatureClass(t.getFeatureClass())
                        .withName(t.getName())
                        .withLatitude(t.getLatitude()).withLongitude(t.getLongitude());
            }

            this.toponymBuilder = newBuilder;
        } else if (qName.equalsIgnoreCase("start")) {
            isStartTag = true;
        } else if (qName.equalsIgnoreCase("end")) {
            isEndTag = true;
        } else if (qName.equalsIgnoreCase("text")) {
            isTextTag = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase("place")) {
            isPlaceTag = false;
        } else if (qName.equalsIgnoreCase("woeId")) {
            toponymBuilder.trimFeatureCode();
            isWoeIdTag = false;
        } else if (qName.equalsIgnoreCase("type")) {
            toponymBuilder.trimFeatureClass();
            isTypeTag = false;
        } else if (qName.equalsIgnoreCase("name")) {
            toponymBuilder.trimName();
            isNameTag = false;
        } else if (qName.equalsIgnoreCase("latitude")) {
            toponymBuilder.setLatitude();
            isLatTag = false;
        } else if (qName.equalsIgnoreCase("longitude")) {
            toponymBuilder.setLongitude();
            isLonTag = false;
        } else if (qName.equalsIgnoreCase("reference")) {
            isReferenceTag = false;
            toponyms.add(toponymBuilder.build());
        } else if (qName.equalsIgnoreCase("start")) {
            toponymBuilder.setStart();
            isStartTag = false;
        } else if (qName.equalsIgnoreCase("end")) {
            toponymBuilder.setEnd();
            isEndTag = false;
        } else if (qName.equalsIgnoreCase("text")) {
            toponymBuilder.trimPhrase();
            isTextTag = false;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        final String text = new String(ch, start, length);

        if (isPlaceTag && isWoeIdTag)
            toponymBuilder.withFeatureCode(text);

        if (isPlaceTag && isTypeTag)
            toponymBuilder.withFeatureClass(text);

        if (isPlaceTag && isNameTag)
            toponymBuilder.withName(text);

        if (isPlaceTag && isLatTag)
            toponymBuilder.withLatitude(text);

        if (isPlaceTag && isLonTag)
            toponymBuilder.withLongitude(text);

        if (isReferenceTag && isStartTag)
            toponymBuilder.withStart(text);

        if (isReferenceTag && isEndTag)
            toponymBuilder.withEnd(text);

        if (isReferenceTag && isTextTag)
            toponymBuilder.withPhrase(text);
    }
}
