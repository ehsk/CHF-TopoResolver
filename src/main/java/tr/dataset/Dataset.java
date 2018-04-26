package tr.dataset;

import tr.dataset.loader.DatasetLoader;

import java.util.function.Consumer;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 5/18/2017
 * Time: 12:12 PM
 */
public class Dataset<T extends DatasetEntry, S> {
    private final String name;
    private final String filePath;
    private final DatasetLoader<T, S> loader;

    public Dataset(String name, String filePath, DatasetLoader<T, S> loader) {
        this.name = name;
        this.filePath = filePath;
        this.loader = loader;
    }

    public Dataset(String filePath, DatasetLoader<T, S> loader) {
        this("", filePath, loader);
    }

    public String getName() {
        return name;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getContainingDir() {
        final int separatorIndex = filePath.lastIndexOf("/");
        if (separatorIndex >= 0)
            return filePath.substring(0, separatorIndex);
        else
            return ".";
    }

    public S forEach(Consumer<T> consumer) throws DatasetException {
        return loader.forEach(filePath, consumer);
    }

    @Override
    public String toString() {
        return getName();
    }
}
