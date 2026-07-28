package be.kuleuven.optimalisatie.solution;

import be.kuleuven.optimalisatie.probleminstance.Trapezoid;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SolutionReader {

    public static class ParsedState {
        public String stepName;
        public int iteration;
        public double lb;
        public double ub;
        public long iterationTime;
        public List<Double> duals = new ArrayList<>();
        public List<Pattern> patterns = new ArrayList<>();
    }

    public static ParsedState readLastIteration(String filepath) {
        ParsedState latestState = null;
        ParsedState currentState = null;

        boolean readingDuals = false;
        boolean readingPatterns = false;
        Pattern currentPattern = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("PROBLEM=")) {
                    continue;
                }

                if (line.startsWith("STEP=")) {
                    // Als we aan een nieuwe stap beginnen, slaan we de vorige (indien voltooid) op als de laatste
                    if (currentState != null) {
                        latestState = currentState;
                    }
                    currentState = new ParsedState();
                    currentState.stepName = line.substring(5);
                    continue;
                }

                if (currentState == null) {
                    continue; // Beveiliging voor het geval de file een vreemde opmaak heeft
                }

                if (line.startsWith("ITERATION=")) {
                    currentState.iteration = Integer.parseInt(line.substring(10));
                } else if (line.startsWith("LB=")) {
                    currentState.lb = Double.parseDouble(line.substring(3));
                } else if (line.startsWith("UB=")) {
                    currentState.ub = Double.parseDouble(line.substring(3));
                } else if (line.startsWith("ITERATION_TIME=")) {
                    currentState.iterationTime = Long.parseLong(line.substring(15));
                } else if (line.equals("DUALS_START")) {
                    readingDuals = true;
                } else if (line.equals("DUALS_END")) {
                    readingDuals = false;
                } else if (readingDuals) {
                    currentState.duals.add(Double.parseDouble(line));
                } else if (line.equals("PATTERNS_START")) {
                    readingPatterns = true;
                } else if (line.equals("PATTERNS_END")) {
                    readingPatterns = false;
                } else if (readingPatterns) {

                    if (line.startsWith("PATTERN=")) {
                        // Formaat: PATTERN=USED,COUNT=1.0
                        String[] parts = line.substring(8).split(",");
                        boolean isUsed = parts[0].equals("USED");
                        double count = 1.0;
                        if (parts.length > 1 && parts[1].startsWith("COUNT=")) {
                            count = Double.parseDouble(parts[1].substring(6));
                        }

                        currentPattern = new Pattern(false);
                        currentPattern.setUsed(isUsed);
                        currentPattern.setCount(count);
                        currentState.patterns.add(currentPattern);

                    } else if (line.startsWith("ITEM=") && currentPattern != null) {
                        // Formaat: ITEM=numberOfItems,totalLength,p1,p2,angle1,angle2,shapeIndicator,flippedH,flippedV
                        String[] itemData = line.substring(5).split(",");

                        int demand = Integer.parseInt(itemData[0]);
                        int totalLength = Integer.parseInt(itemData[1]);
                        int p1 = Integer.parseInt(itemData[2]);
                        int p2 = Integer.parseInt(itemData[3]);
                        double angle1 = Double.parseDouble(itemData[4]);
                        double angle2 = Double.parseDouble(itemData[5]);
                        int shapeIndicator = Integer.parseInt(itemData[6]);
                        boolean flippedH = Boolean.parseBoolean(itemData[7]);
                        boolean flippedV = Boolean.parseBoolean(itemData[8]);

                        Trapezoid item = new Trapezoid(demand, totalLength, p1, p2, angle1, angle2, shapeIndicator);
                        if (flippedH) {
                            item.flipHorizontally();
                        }
                        if (flippedV) {
                            item.flipVertically();
                        }

                        currentPattern.addItem(item);
                    }
                }
            }

            // Aan het einde van de file moeten we de allerlaatste iteratie die in opbouw was opslaan
            if (currentState != null) {
                latestState = currentState;
            }

        } catch (IOException | NumberFormatException e) {
            System.err.println("Fout bij het inlezen van de solution file: " + e.getMessage());
        }

        return latestState;
    }
}