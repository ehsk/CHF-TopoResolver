package tr.util.ml;

import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 7/17/2017
 * Time: 11:47 PM
 */
public class WekaUtil {
    private WekaUtil() {
    }

    public static void saveDataset(Instances dataset, String fileName) throws IOException {
        final ArffSaver saver = new ArffSaver();
        saver.setInstances(dataset);

        saver.setFile(new File(fileName));
        saver.writeBatch();
    }

    public static Instances loadDataset(String datasetFile) throws Exception {
        ConverterUtils.DataSource source = new ConverterUtils.DataSource(datasetFile);
        final Instances dataset = source.getDataSet();
        dataset.setClassIndex(dataset.numAttributes() - 1);

        return dataset;
    }

    public static void saveClassificationModel(Classifier classifier, String modelFile) throws Exception {
        SerializationHelper.write(modelFile, classifier);
    }

    public static<T extends Classifier> T loadClassificationModel(String modelFile) throws Exception {
        return (T) SerializationHelper.read(modelFile);
    }
}
