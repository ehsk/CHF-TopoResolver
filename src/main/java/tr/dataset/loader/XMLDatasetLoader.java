package tr.dataset.loader;

import com.google.common.base.Stopwatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import tr.dataset.DatasetEntry;
import tr.dataset.DatasetException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 5/18/2017
 * Time: 12:38 PM
 */
public abstract class XMLDatasetLoader<T extends DatasetEntry, S> implements DatasetLoader<T, S> {
    protected final Logger logger = LogManager.getLogger(getClass());

    protected abstract AbstractXMLDatasetHandler createHandler(Consumer<T> consumer);

    @Override
    public S forEach(String datasetFile, Consumer<T> consumer) throws DatasetException {
        try {
            logger.info("[dataset loading] {} starts", datasetFile);
            final Stopwatch stopwatch = Stopwatch.createStarted();
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            final AbstractXMLDatasetHandler handler = createHandler(consumer);
            parser.parse(datasetFile, handler);
            stopwatch.stop();
            logger.info("[dataset loaded] at {} s", stopwatch.elapsed(TimeUnit.SECONDS));
            return handler.getSummary();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new DatasetException("error in loading dataset", e);
        }
    }

    protected class AbstractXMLDatasetHandler extends DefaultHandler {
        S summary;

        public S getSummary() {
            return summary;
        }
    }
}
