package tr.dataset;

import tr.Article;
import tr.dataset.loader.TRNewsDatasetLoader;

/**
 * {@code AnnotatedDatasets} class represents
 * the dataset provided alongside with the code.
 * The dataset, named <strong>TR-News</strong>, is collected
 * based on News articles (with 118 articles) and is located in the data folder.
 * We also provide a small subset of TR-News for testing purposes.
 * <p>
 *     For each dataset, a name, file path,
 *     and a {@link tr.dataset.loader.DatasetLoader loader} class should be specified.
 * </p>
 *
 * @see tr.dataset.loader.DatasetLoader
 * @see TRNewsDatasetLoader
 */
public class AnnotatedDatasets {
    public static final Dataset<Article, DatasetSummary> TRNews =
            new Dataset<>("TR-News", "data/tr-news/TR-News.xml", new TRNewsDatasetLoader());
    public static final Dataset<Article, DatasetSummary> SAMPLE =
            new Dataset<>("Sample", "data/tr-news/sample.xml", new TRNewsDatasetLoader());
}
