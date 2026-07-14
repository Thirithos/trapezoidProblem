package be.kuleuven.optimalisatie.solution;

import java.util.ArrayList;
import java.util.List;

public class Solution {
    private final String problemName;
    private final List<Pattern> patterns;
    private double lowerBound;
    private double upperBound;
    private long executionTimeMs;

    public Solution(String problemName) {
        this.problemName = problemName;
        this.patterns = new ArrayList<>();
    }

    public void addPattern(Pattern pattern) {
        this.patterns.add(pattern);
    }

    public String getProblemName() {
        return problemName;
    }

    public List<Pattern> getPatterns() {
        return patterns;
    }

    public int getTotalBins() {
        return patterns.size();
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

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
}