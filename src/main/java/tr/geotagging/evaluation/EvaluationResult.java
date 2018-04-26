package tr.geotagging.evaluation;

import tr.Toponym;
import tr.util.geo.GeoUtil;
import tr.util.math.MathUtil;
import tr.util.math.statistics.SampledSummaryStat;

import java.util.ArrayList;
import java.util.List;

public class EvaluationResult {
    private int correct = 0;
    private int approxCorrect = 0;

    private int incorrect = 0;

    private int notFound = 0;
    private int nonCoordinated = 0;

    private int falseFound = 0;

    private double actualCorrects;

    private SampledSummaryStat errorDistance = new SampledSummaryStat();

    private final List<Toponym> corrects = new ArrayList<>();
    private final List<Toponym> approxCorrects = new ArrayList<>();
    private final List<Toponym> incorrects = new ArrayList<>();
    private final List<Toponym> falseFounds = new ArrayList<>();

    public void acceptCorrect(Toponym toponym) {
        this.corrects.add(toponym);
        this.correct++;
    }

    public void acceptApproxCorrect(Toponym toponym) {
        this.approxCorrects.add(toponym);
        this.approxCorrect++;
    }

    public void acceptFalseFound(Toponym toponym) {
        this.falseFounds.add(toponym);
        this.falseFound++;
    }

    public void acceptIncorrect(Toponym toponym) {
        this.incorrects.add(toponym);
    }

    public void acceptErrorDistance(Toponym predicted, Toponym groundTruth) {
        errorDistance.accept(GeoUtil.distance(predicted.toCoordinate(), groundTruth.toCoordinate()).toKilometres());
    }

    public void incrIncorrect() {
        this.incorrect++;
    }

    public void incrNonCoordinated() {
        this.nonCoordinated++;
    }

    public void incrNotFound() {
        this.notFound++;
    }

    public void add(EvaluationResult r) {
        this.correct += r.correct;
        this.approxCorrect += r.approxCorrect;

        this.incorrect += r.incorrect;

        this.notFound += r.notFound;
        this.nonCoordinated += r.nonCoordinated;

        this.falseFound += r.falseFound;
        this.errorDistance.acceptAll(r.errorDistance.getSamples());
    }

    public void setActualCorrects(double actualCorrects) {
        this.actualCorrects = actualCorrects;
    }

    public int getCorrect() {
        return correct;
    }

    public int getApproxCorrect() {
        return approxCorrect;
    }

    public int getIncorrect() {
        return incorrect;
    }

    public int getNotFound() {
        return notFound;
    }

    public int getNonCoordinated() {
        return nonCoordinated;
    }

    public int getFalseFound() {
        return falseFound;
    }

    public List<Toponym> getCorrects() {
        return corrects;
    }

    public List<Toponym> getApproxCorrects() {
        return approxCorrects;
    }

    public List<Toponym> getIncorrects() {
        return incorrects;
    }

    public List<Toponym> getFalseFounds() {
        return falseFounds;
    }

    public int getTotal() {
        return getTotalCorrect() + incorrect + falseFound;
    }

    public int getTotalCorrect() {
        return correct + approxCorrect;
    }

    public int getTotalNotFound() {
        return notFound + nonCoordinated;
    }

    public double getPrecision() {
        return MathUtil.safeDivide(getTotalCorrect(), getTotalCorrect() + incorrect);
    }

    public double getRecall() {
        return MathUtil.safeDivide(getTotalCorrect(), actualCorrects);
    }

    public double getMedianErrorDistance() {
        return errorDistance.getMedian();
    }

    public double getMeanErrorDistance() {
        return errorDistance.getAverage();
    }
}