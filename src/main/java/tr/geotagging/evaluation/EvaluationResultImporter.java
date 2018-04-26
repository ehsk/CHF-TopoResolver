package tr.geotagging.evaluation;

import tr.Article;
import tr.Toponym;
import tr.dataset.DatasetException;
import tr.util.ArgUtil;
import tr.util.StringUtil;
import tr.util.geo.GeoCoordinate;
import tr.util.geo.GeoUtil;
import tr.util.math.statistics.SampledSummaryStat;
import com.google.common.base.Stopwatch;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 2/18/2018
 * Time: 8:51 PM
 */
public class EvaluationResultImporter {
    private static final Logger logger = LogManager.getLogger(EvaluationResultImporter.class);

    private static ArgumentParser buildArgParser() {
        ArgumentParser argParser = ArgumentParsers.newFor("EvaluationResultImporter")
                .build()
                .defaultHelp(true)
                .description("Import an saved evaluation xml file");
        argParser.addArgument("-f", "--file")
                .required(true)
                .help("evaluation xml file");
        return argParser;
    }

    private static class EvalXmlHandler extends DefaultHandler {
        final Map<String, Article> articles = new HashMap<>();
        Article article = null;
        boolean isLatTag = false;
        boolean isLonTag = false;
        boolean isToponymTag = false;
        String lat = "", lon = "";
        Toponym toponym;

        SampledSummaryStat summaryStat = new SampledSummaryStat();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if (qName.equalsIgnoreCase("experiment")) {
                final String dataset = attributes.getValue("dataset");

                try {
                    ArgUtil.getDataset(dataset).forEach(article -> {
                        if (article.isAnnotated())
                            articles.put(article.getArticleId(), article);
                    });
                } catch (DatasetException e) {
                    logger.error("Error occurred during reading dataset", e);
                    return;
                }

                logger.info(String.format("%d articles loaded", articles.size()));
            } else if (qName.equalsIgnoreCase("article")) {
                article = articles.get(attributes.getValue("docid"));
            } else if (qName.equalsIgnoreCase("toponym")) {
                String status = attributes.getValue("status");
                if (status.equalsIgnoreCase("correct") || status.equalsIgnoreCase("approxCorrect") || status.equalsIgnoreCase("incorrect")) {
                    int toponymStart = Integer.valueOf(attributes.getValue("start"));
                    int toponymEnd = Integer.valueOf(attributes.getValue("end"));
                    for (Toponym topo : article.getTaggedToponyms()) {
                        if (topo.getStart() == toponymStart) {
                            if (topo.getEnd() != toponymEnd)
                                logger.warn(String.format("Not match entirely: [%s] start=%d data_end=%d file_end=%d %s",
                                        article.getArticleId(), toponymStart, topo.getEnd(), toponymEnd, topo.getPhrase()));
                            toponym = topo;
                        }
                    }
                }
                isToponymTag = true;
            } else if (qName.equalsIgnoreCase("lat")) {
                isLatTag = true;
            } else if (qName.equalsIgnoreCase("lon")) {
                isLonTag = true;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if (qName.equalsIgnoreCase("article")) {
                article = null;
            } else if (qName.equalsIgnoreCase("toponym")) {

                if (toponym != null)
                    summaryStat.accept(GeoUtil.distance(toponym.toCoordinate(),
                            new GeoCoordinate(Double.valueOf(lat), Double.valueOf(lon))).toKilometres());

                isToponymTag = false;
                lat = "";
                lon = "";
                toponym = null;
            } else if (qName.equalsIgnoreCase("lat")) {
                isLatTag = false;
            } else if (qName.equalsIgnoreCase("lon")) {
                isLonTag = false;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            final String text = new String(ch, start, length);

            if (StringUtil.hasText(text)) {
                if (isLatTag) {
                    lat += text;
                }

                if (isLonTag) {
                    lon += text;
                }
            }
        }

        SampledSummaryStat getSummaryStat() {
            return summaryStat;
        }
    }

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
        final ArgumentParser argParser = buildArgParser();

        final Namespace ns = argParser.parseArgsOrFail(args);
        logger.info("Args: " + ns);

        final String file = ns.getString("file");

        logger.info("[eval file loading] {} starts", file);
        final Stopwatch stopwatch = Stopwatch.createStarted();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        final EvalXmlHandler handler = new EvalXmlHandler();
        parser.parse(file, handler);
        stopwatch.stop();
        logger.info("[file processed] at {} s", stopwatch.elapsed(TimeUnit.SECONDS));
        final SampledSummaryStat summaryStat = handler.getSummaryStat();
        logger.info("Error Distance: median {} - mean {}", summaryStat.getMedian(), summaryStat.getAverage());
    }
}
