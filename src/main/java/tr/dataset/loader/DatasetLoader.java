package tr.dataset.loader;

import tr.dataset.DatasetEntry;
import tr.dataset.DatasetException;

import java.util.function.Consumer;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 5/18/2017
 * Time: 12:15 PM
 */
public interface DatasetLoader<T extends DatasetEntry, S> {
    S forEach(String datasetFile, Consumer<T> consumer) throws DatasetException;
}
