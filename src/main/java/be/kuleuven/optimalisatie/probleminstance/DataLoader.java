package be.kuleuven.optimalisatie.probleminstance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataLoader {

    public static List<TrussProblem> loadAllProblems() {
        List<TrussProblem> problems = new ArrayList<>();
        File locationData = new File("src/main/resources/Truss");

        // alle files in de map ophalen
        File[] files = locationData.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        if (files != null) {
            for (File file : files) {
                TrussProblem problem = loadProblem(file);
                if (problem != null) {
                    problems.add(problem);
                }
            }
        }
        return problems;
    }

    private static TrussProblem loadProblem(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String firstLine = br.readLine();
            if (firstLine == null) return null;

            // trim en split voor meerdere spaties, tussen de waarden
            String[] headerParts = firstLine.trim().split("\\s+");
            if (headerParts.length < 4) return null;

            int totalItems = Integer.parseInt(headerParts[0]);
            int numTypes = Integer.parseInt(headerParts[1]);
            int numOrders = Integer.parseInt(headerParts[2]);
            int seedIndex = Integer.parseInt(headerParts[3]);

            TrussProblem problem = new TrussProblem(file.getName(), totalItems, numTypes, numOrders, seedIndex);

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;


                // in datafile formaat is altijd:
                // 1 2463    9    3 85.0 88.0 1
                // aantal items, lengte, eerste projectie lengte, tweede projectie lengte, eerste hoek, tweede hoek, shape indicatie
                // dus shape indicatie geeft 1 als de "bases" van de projecties aan dezelfde kant liggen. _|_|_
                // als shape indicatie 0 geeft, liggen de bases aan tegenovergestelde kanten. ‾|_|_

                String[] parts = line.split("\\s+");
                if (parts.length >= 7) {
                    int numberOfItems = Integer.parseInt(parts[0]);
                    int length = Integer.parseInt(parts[1]);
                    int p1 = Integer.parseInt(parts[2]);
                    int p2 = Integer.parseInt(parts[3]);
                    double a1 = Double.parseDouble(parts[4]);
                    double a2 = Double.parseDouble(parts[5]);
                    int shape = Integer.parseInt(parts[6]);

                    problem.addTrapezoid(new Trapezoid(numberOfItems, length, p1, p2, a1, a2, shape));
                }
            }
            return problem;
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error reading file: " + file.getName() +" " + e.getMessage());
            return null;
        }
    }
}