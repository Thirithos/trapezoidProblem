package be.kuleuven.optimalisatie.solution;

import java.util.ArrayList;
import java.util.List;

public class IterationData {
    private final int iterationNumber;
    private double lowerBound;
    private double upperBound;
    private final List<Double> dualValues;
    private final List<Pattern> patterns;
    private String title;

    public IterationData(int iterationNumber, String title) {
        this.iterationNumber = iterationNumber;
        this.dualValues = new ArrayList<>();
        this.patterns = new ArrayList<>();
        this.title = title;
    }

    public int getIterationNumber() {
        return iterationNumber;
    }

    public double getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(double lowerBound) {
        this.lowerBound = lowerBound;
    }

    public double getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(double upperBound) {
        this.upperBound = upperBound;
    }

    public void addDualValue(double dualValue) {
        this.dualValues.add(dualValue);
    }

    public List<Double> getDualValues() {
        return dualValues;
    }

    public void addPattern(Pattern pattern) {
        this.patterns.add(pattern);
    }

    public List<Pattern> getPatterns() {
        return patterns;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @Override
    public String toString() {
        if (title != null && !title.isEmpty()) {
            return title;
        }

        return iterationNumber == 0 ? "Initiele Oplossing (FFD)" : "CG Iteratie " + iterationNumber;
    }
}