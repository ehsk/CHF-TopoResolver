package tr.util.math.statistics;

import java.util.List;
import java.util.function.DoubleConsumer;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 4/12/2017
 * Time: 2:21 AM
 */
public class SummaryStat implements DoubleConsumer {
    private long count = 0;
    private double sum = 0;
    private double average = 0;
    private double variance = 0;
    private double min = Double.MAX_VALUE;
    private double max = Double.MIN_VALUE;

    public SummaryStat() {
    }

    public SummaryStat(List<Double> initList) {
        acceptAll(initList);
    }

    @Override
    public void accept(double value) {
        ++count;
        sum += value;
        min = Math.min(min, value);
        max = Math.max(max, value);

        final double diff = value - average;
        average += diff / count;
        variance += diff * (value - average);
    }

    public void acceptAll(List<Double> values) {
        for (Double value : values) {
            accept(value);
        }
    }

    public long getCount() {
        return count;
    }

    public double getSum() {
        return sum;
    }

    public double getAverage() {
        return average;
    }

    public double getVariance() {
        return count <= 1 ? 0 : variance / (count - 1);
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStdev() {
        return count <= 1 ? 0 : Math.sqrt(variance / (count - 1));
    }


}
