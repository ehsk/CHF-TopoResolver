package tr.dataset;

import tr.Article;
import tr.util.math.statistics.SummaryStat;

/**
 * Created with IntelliJ IDEA.
 * User: user
 * Date: 10/18/2016
 * Time: 8:41 PM
 */
public class DatasetSummary {
    private final int numberOfArticles;
    private final int numberOfAnnotatedArticles;

    private final double numberOfToponyms;
    private final double numberOfToponymsWithGazetteer;

    private final double maxToponymsPerArticle;
    private final double minToponymsPerArticle;
    private final double averageToponymsPerArticle;
    private final double stdevToponymsPerArticle;

    private final double maxToponymsWithGazPerArticle;
    private final double averageToponymsWithGazPerArticle;
    private final double stdevToponymsWithGazPerArticle;

    public DatasetSummary(int numberOfArticles, int numberOfAnnotatedArticles,
                          double numberOfToponyms, double numberOfToponymsWithGazetteer,
                          double maxToponymsPerArticle, double minToponymsPerArticle, double averageToponymsPerArticle, double stdevToponymsPerArticle,
                          double maxToponymsWithGazPerArticle, double averageToponymsWithGazPerArticle, double stdevToponymsWithGazPerArticle) {
        this.numberOfArticles = numberOfArticles;
        this.numberOfAnnotatedArticles = numberOfAnnotatedArticles;
        this.numberOfToponyms = numberOfToponyms;
        this.numberOfToponymsWithGazetteer = numberOfToponymsWithGazetteer;
        this.maxToponymsPerArticle = maxToponymsPerArticle;
        this.minToponymsPerArticle = minToponymsPerArticle;
        this.averageToponymsPerArticle = averageToponymsPerArticle;
        this.stdevToponymsPerArticle = stdevToponymsPerArticle;
        this.maxToponymsWithGazPerArticle = maxToponymsWithGazPerArticle;
        this.averageToponymsWithGazPerArticle = averageToponymsWithGazPerArticle;
        this.stdevToponymsWithGazPerArticle = stdevToponymsWithGazPerArticle;
    }

    public int getNumberOfArticles() {
        return numberOfArticles;
    }

    public int getNumberOfAnnotatedArticles() {
        return numberOfAnnotatedArticles;
    }

    public double getNumberOfToponyms() {
        return numberOfToponyms;
    }

    public double getNumberOfToponymsWithGazetteer() {
        return numberOfToponymsWithGazetteer;
    }

    public double getMaxToponymsPerArticle() {
        return maxToponymsPerArticle;
    }

    public double getMinToponymsPerArticle() {
        return minToponymsPerArticle;
    }

    public double getAverageToponymsPerArticle() {
        return averageToponymsPerArticle;
    }

    public double getStdevToponymsPerArticle() {
        return stdevToponymsPerArticle;
    }

    public double getMaxToponymsWithGazPerArticle() {
        return maxToponymsWithGazPerArticle;
    }

    public double getAverageToponymsWithGazPerArticle() {
        return averageToponymsWithGazPerArticle;
    }

    public double getStdevToponymsWithGazPerArticle() {
        return stdevToponymsWithGazPerArticle;
    }

    @Override
    public String toString() {
        String summaryString = "";

        summaryString += String.format("n_articles %d | n_annotated_articles %d\n",
                numberOfArticles, numberOfAnnotatedArticles);

        summaryString += String.format("n_topos %.0f | topos_with_gaz %.0f\n", numberOfToponyms, numberOfToponymsWithGazetteer);

        summaryString += String.format("min_topos_per_article %.0f | max_topos_per_article %.0f" +
                        " | mean_topos_per_article %.3f | stdev_topos_per_article %.3f\n",
                minToponymsPerArticle, maxToponymsPerArticle, averageToponymsPerArticle, stdevToponymsPerArticle);

        summaryString += String.format("max_topos_w_gaz_per_article %.0f" +
                        " | mean_topos_w_gaz_per_article %.3f | stdev_topos_w_gaz_per_article %.3f\n",
                maxToponymsWithGazPerArticle, averageToponymsWithGazPerArticle, stdevToponymsWithGazPerArticle);

        return summaryString;
    }

    public static class DatasetSummaryBuilder {
        private int numberOfArticles = 0;
        private int numberOfAnnotatedArticles = 0;

        private SummaryStat toposPerArticle = new SummaryStat();
        private SummaryStat toposWithGazetteerPerArticle = new SummaryStat();

        public void recordArticleSummary(final Article article) {
            numberOfArticles++;

            if (article.isAnnotated()) {
                numberOfAnnotatedArticles++;

                toposPerArticle.accept(article.getToponyms().size());
                toposWithGazetteerPerArticle.accept(article.getTaggedToponyms().size());
            }
        }

        public DatasetSummary build() {
            return new DatasetSummary(numberOfArticles, numberOfAnnotatedArticles,
                    toposPerArticle.getSum(), toposWithGazetteerPerArticle.getSum(),
                    toposPerArticle.getMax(), toposPerArticle.getMin(), toposPerArticle.getAverage(), toposPerArticle.getStdev(),
                    toposWithGazetteerPerArticle.getMax(), toposWithGazetteerPerArticle.getAverage(), toposWithGazetteerPerArticle.getStdev());
        }

    }
}

