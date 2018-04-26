package tr.util.math.statistics;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 7/27/2017
 * Time: 1:03 AM
 */
public class SampledSummaryStat extends SummaryStat {
    private final List<Double> samples;

    public SampledSummaryStat() {
        this.samples = new ArrayList<>();
    }

    public SampledSummaryStat(List<Double> initList) {
        super(initList);
        this.samples = initList;
    }

    @Override
    public void accept(double value) {
        super.accept(value);
        samples.add(value);
    }

    public double getMedian() {
        if (samples.isEmpty())
            return 0;
        
        final List<Double> sortedSamples = samples.stream().sorted().collect(Collectors.toList());
        final int sampleSize = sortedSamples.size();
        if (sampleSize % 2 == 0)
            return 0.5 * (sortedSamples.get(sampleSize / 2) + sortedSamples.get(sampleSize / 2 - 1));
        else
            return sortedSamples.get(sampleSize / 2);
    }

    public List<Double> getSamples() {
        return samples;
    }
}
