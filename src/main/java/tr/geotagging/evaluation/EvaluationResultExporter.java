package tr.geotagging.evaluation;

import tr.Article;
import tr.Toponym;
import tr.dataset.DatasetSummary;
import tr.dataset.stats.DatasetAnalyzer;
import tr.util.math.MathUtil;
import tr.util.math.statistics.SampledSummaryStat;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 6/26/2017
 * Time: 6:19 PM
 */
public class EvaluationResultExporter {

    private final BufferedWriter writer;

    private final DatasetAnalyzer analyzer = new DatasetAnalyzer();

    public EvaluationResultExporter(String output) throws IOException {
        this.writer = new BufferedWriter(new FileWriter(output));
    }

    public void close() {
        try {
            this.writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exportFooter(EvaluationResult resultSummary, DatasetSummary datasetSummary) {
        try {
            writer.write("\t<performance>");
            writer.newLine();

            final double precision = resultSummary.getPrecision();
            final double recall = resultSummary.getTotalCorrect() / datasetSummary.getNumberOfToponymsWithGazetteer();
            final double f1Measure = MathUtil.f1Measure(precision, recall);

            writer.write(String.format("\t\t<precision>%.5f</precision>", precision));
            writer.newLine();

            writer.write(String.format("\t\t<recall>%.5f</recall>", recall));
            writer.newLine();

            writer.write(String.format("\t\t<f1measure>%.5f</f1measure>", f1Measure));
            writer.newLine();

            writer.write(String.format("\t\t<correct>%d</correct>", resultSummary.getTotalCorrect()));
            writer.newLine();

            writer.write(String.format("\t\t<incorrect>%d</incorrect>", resultSummary.getIncorrect()));
            writer.newLine();

            writer.write(String.format("\t\t<notFound>%d</notFound>", resultSummary.getTotalNotFound()));
            writer.newLine();

            writer.write(String.format("\t\t<falseFound>%d</falseFound>", resultSummary.getFalseFound()));
            writer.newLine();

            writer.write("\t</performance>");
            writer.newLine();

            writer.newLine();
            writer.write("</experiment>");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exportHeader(String recognizerName, String resolverName, String dataset) {
        try {
            writer.write(String.format("<experiment recognizer=\"%s\" resolver=\"%s\" dataset=\"%s\">",
                    recognizerName, resolverName, dataset));
            writer.newLine();
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exportArticle(Article article, EvaluationResult result) {
        try {
            writer.write(String.format("\t<article docid=\"%s\">", article.getArticleId()));
            writer.newLine();

            writer.write("\t\t<statistics>");
            writer.newLine();

            final long taggedToponyms = article.getToponyms().stream().filter(t -> t.getGeonameId() != null).count();
            final double precision = MathUtil.safeDivide(result.getTotalCorrect(), result.getTotalCorrect() + result.getIncorrect());
            final double recall = MathUtil.safeDivide(result.getTotalCorrect(), taggedToponyms);
            final double f1Measure = MathUtil.f1Measure(precision, recall);
            writer.write(String.format("\t\t\t<summary docid=\"%s\" toponymsCount=\"%d\" taggedCount=\"%d\" " +
                            "resolvedCount=\"%d\" correct=\"%d\" incorrect=\"%d\" notFound=\"%d\" falseFound=\"%d\" " +
                            "precision=\"%.3f\" recall=\"%.3f\" f1Measure=\"%.3f\" />",
                    article.getArticleId(),
                    article.getToponyms().size(), taggedToponyms,
                    result.getTotal(),
                    result.getTotalCorrect(), result.getIncorrect(), result.getTotalNotFound(), result.getFalseFound(),
                    precision, recall, f1Measure));
            writer.newLine();

            final SampledSummaryStat candidatesSummary = analyzer.countCandidates(article);
            writer.write(String.format("\t\t\t<candidates docid=\"%s\" total=\"%.0f\" mean=\"%.3f\" median=\"%.1f\" stdev=\"%.3f\" />",
                    article.getArticleId(),
                    candidatesSummary.getSum(),
                    candidatesSummary.getAverage(), candidatesSummary.getMedian(), candidatesSummary.getStdev()));
            writer.newLine();

            final SampledSummaryStat actualAllPairDistance = analyzer.calcAllPairDistance(article);
            final SampledSummaryStat predictedAllPairDistance = analyzer.calcAllPairDistance(
                    Lists.newArrayList(Iterables.concat(result.getCorrects(), result.getApproxCorrects(),
                            result.getIncorrects(), result.getFalseFounds()))
            );

            writer.write(String.format("\t\t\t<geoDistance docid=\"%s\" total=\"%.0f\" mean=\"%.3f\" median=\"%.3f\" stdev=\"%.3f\" " +
                            "predicted-mean=\"%.3f\" predicted-median=\"%.3f\" predicted-stdev=\"%.3f\" />",
                    article.getArticleId(),
                    actualAllPairDistance.getSum(),
                    actualAllPairDistance.getAverage(), actualAllPairDistance.getMedian(), actualAllPairDistance.getStdev(),
                    predictedAllPairDistance.getAverage(), predictedAllPairDistance.getMedian(), predictedAllPairDistance.getStdev()));
            writer.newLine();

            writer.write("\t\t</statistics>");
            writer.newLine();

            writer.write("\t\t<toponyms>");
            writer.newLine();

            exportToponyms(result.getCorrects(), "correct");
            exportToponyms(result.getApproxCorrects(), "approxCorrect");
            exportToponyms(result.getIncorrects(), "incorrect");
            exportToponyms(result.getFalseFounds(), "falseFound");

            writer.write("\t\t</toponyms>");
            writer.newLine();

            writer.write("\t</article>");
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportToponyms(List<Toponym> toponyms, String status) throws IOException {
        for (Toponym cr : toponyms) {
            if (cr.getGeonameId() != null) {
                writer.write(String.format("\t\t\t<toponym status=\"%s\" geonameid=\"%d\" start=\"%d\" end=\"%d\">", status, cr.getGeonameId(), cr.getStart(), cr.getEnd()));
            } else {
                writer.write(String.format("\t\t\t<toponym status=\"%s\" start=\"%d\" end=\"%d\">", status, cr.getStart(), cr.getEnd()));
            }
            writer.newLine();

            writer.write(String.format("\t\t\t\t<phrase>%s</phrase>", cr.getPhrase()));
            writer.newLine();

            writer.write(String.format("\t\t\t\t<lat>%.6f</lat>", cr.getLatitude()));
            writer.newLine();

            writer.write(String.format("\t\t\t\t<lon>%.6f</lon>", cr.getLongitude()));
            writer.newLine();

            writer.write(String.format("\t\t\t\t<name>%s</name>", cr.getName()));
            writer.newLine();

            writer.write(String.format("\t\t\t\t<score>%.5f</score>", cr.getScore()));
            writer.newLine();

            if (cr.getCountryGeonameId() != null) {
                writer.write(String.format("\t\t\t\t<country geonameid=\"%d\">%s</country>", cr.getCountryGeonameId(), cr.getCountry()));
                writer.newLine();
            } else if (cr.getCountry() != null) {
                writer.write(String.format("\t\t\t\t<country>%s</country>", cr.getCountry()));
                writer.newLine();
            }

            if (cr.getAdmin1GeonameId() != null) {
                writer.write(String.format("\t\t\t\t<admin1 geonameid=\"%d\">%s</admin1>", cr.getAdmin1GeonameId(), cr.getAdmin1()));
                writer.newLine();
            } else if (cr.getAdmin1() != null) {
                writer.write(String.format("\t\t\t\t<admin1>%s</admin1>", cr.getAdmin1()));
                writer.newLine();
            }

            if (cr.getFeatureClass() != null) {
                writer.write(String.format("\t\t\t\t<fclass>%s</fclass>", cr.getFeatureClass()));
                writer.newLine();
            }

            if (cr.getFeatureCode() != null) {
                writer.write(String.format("\t\t\t\t<fcode>%s</fcode>", cr.getFeatureCode()));
                writer.newLine();
            }

            writer.write("\t\t\t</toponym>");
            writer.newLine();
        }
    }
}
