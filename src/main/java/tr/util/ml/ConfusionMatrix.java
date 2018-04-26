package tr.util.ml;

import tr.util.math.MathUtil;

import java.util.function.BiConsumer;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 4/12/2017
 * Time: 2:43 AM
 */
public class ConfusionMatrix implements BiConsumer<Boolean, Boolean> {
    private final long[][] matrix = new long[][] {{0, 0}, {0, 0}};

    @Override
    public void accept(Boolean isActualPositive, Boolean isPredictedPositive) {
        matrix[isActualPositive ? 1 : 0][isPredictedPositive ? 1 : 0]++;
    }

    public long getTP() {
        return matrix[1][1];
    }

    public long getTN() {
        return matrix[0][0];
    }

    public long getFP() {
        return matrix[0][1];
    }

    public long getFN() {
        return matrix[1][0];
    }

    public long getPredictedPositive() {
        return getTP() + getFP();
    }

    public long getPredictedNegative() {
        return getTN() + getFN();
    }

    public long getActualPositive() {
        return getTP() + getFN();
    }

    public long getActualNegative() {
        return getTN() + getFP();
    }

    public long getTotal() {
        return getPredictedNegative() + getPredictedPositive();
    }

    public double getPrecision() {
        return MathUtil.safeDivide(getTP(), getPredictedPositive());
    }

    public double getRecall() {
        return MathUtil.safeDivide(getTP(), getActualPositive());
    }

    public double getAccuracy() {
        return MathUtil.safeDivide(getTN() + getTP(), getTotal());
    }

    public double getFalsePositiveRate() {
        return MathUtil.safeDivide(getFP(), getActualNegative());
    }

    public double getFMeasure(double beta) {
        return MathUtil.fMeasure(beta, getPrecision(), getRecall());
    }

    public double getF1Measure() {
        return getFMeasure(1);
    }
}
