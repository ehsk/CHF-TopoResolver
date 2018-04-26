package tr.util.math.statistics;

import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 4/12/2017
 * Time: 2:21 AM
 */
public class ConditionalSummaryStat<T> implements BiFunction<T, Double, Boolean> {
    private final SummaryStat summaryStat = new SummaryStat();
    private final Predicate<T> predicate;

    public ConditionalSummaryStat(Predicate<T> predicate) {
        this.predicate = predicate;
    }

    @Override
    public Boolean apply(T element, Double value) {
        if (!predicate.test(element))
            return false;

        summaryStat.accept(value);
        return true;
    }

    public long getCount() {
        return summaryStat.getCount();
    }

    public double getSum() {
        return summaryStat.getSum();
    }

    public double getAverage() {
        return summaryStat.getAverage();
    }

    public double getVariance() {
        return summaryStat.getVariance();
    }

    public double getMin() {
        return summaryStat.getMin();
    }

    public double getMax() {
        return summaryStat.getMax();
    }

    public double getStdev() {
        return summaryStat.getStdev();
    }

}
