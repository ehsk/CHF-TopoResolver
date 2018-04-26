package tr.util.math;

/**
 * Created with IntelliJ IDEA.
 * User: ehsan
 * Date: 4/12/2017
 * Time: 3:12 AM
 */
public interface MathUtil {
    static double safeDivide(double a, double b) {
        return b == 0 ? 0 : (a / b);
    }

    static double safeDivide(long a, long b) {
        return b == 0 ? 0 : ((double) a / b);
    }

    static double percentOf(double a, double b) {
        return 100 * a / b;
    }

    static double fMeasure(double beta, double precision, double recall) {
        return MathUtil.safeDivide(
                (1 + Math.pow(beta, 2)) * precision * recall,
                Math.pow(beta, 2) * precision + recall);
    }

    static double f1Measure(double precision, double recall) {
        return fMeasure(1, precision, recall);
    }
}
