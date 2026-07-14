package be.kuleuven.optimalisatie.solution;

import be.kuleuven.optimalisatie.probleminstance.Trapezoid;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SolutionWriter {

    public static void initializeFile(String outputPath, String problemName) {
        File file = new File(outputPath);
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            writer.write("PROBLEM=" + problemName);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Fout bij het aanmaken van logbestand: " + e.getMessage());
        }
    }


    public static void appendIteration(String outputPath, int iteration, double lb, double ub,
                                       Map<Integer, Double> duals, List<Pattern> allPatterns) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath, true))) {
            writer.write("ITERATION=" + iteration);
            writer.newLine();
            writer.write("LB=" + lb);
            writer.newLine();
            writer.write("UB=" + ub);
            writer.newLine();

            writer.write("DUALS_START");
            writer.newLine();
            if (duals != null) {
                for (Map.Entry<Integer, Double> entry : duals.entrySet()) {
                    writer.write(entry.getKey() + ":" + entry.getValue());
                    writer.newLine();
                }
            }
            writer.write("DUALS_END");
            writer.newLine();

            writer.write("PATTERNS_START");
            writer.newLine();
            for (Pattern p : allPatterns) {
                writer.write("PATTERN=" + (p.isUsed() ? "USED" : "UNUSED") + ",COUNT=" + p.getCount());
                writer.newLine();
                for (Trapezoid t : p.getItems()) {
                    String itemStr = String.format("%d,%d,%d,%d,%s,%s,%d,%b,%b",
                            t.getNumberOfItems(), t.getTotalLength(), t.getP1(), t.getP2(),
                            t.getAngle1(), t.getAngle2(), t.getShapeIndicator(),
                            t.isFlippedHorizontally(), t.isFlippedVertically());
                    writer.write("ITEM=" + itemStr);
                    writer.newLine();
                }
            }
            writer.write("PATTERNS_END");
            writer.newLine();

        } catch (IOException e) {
            System.err.println("Fout bij het appenden van iteratie " + iteration + ": " + e.getMessage());
        }
    }
}