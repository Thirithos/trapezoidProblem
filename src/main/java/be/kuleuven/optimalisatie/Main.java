package be.kuleuven.optimalisatie;

import be.kuleuven.optimalisatie.algorithm.FirstFitDecreasing;
import be.kuleuven.optimalisatie.algorithm.RMP;
import be.kuleuven.optimalisatie.algorithm.SubProblem;
import be.kuleuven.optimalisatie.gui.SolutionViewer;
import be.kuleuven.optimalisatie.probleminstance.DataLoader;
import be.kuleuven.optimalisatie.probleminstance.TrussProblem;
import be.kuleuven.optimalisatie.solution.Pattern;
import be.kuleuven.optimalisatie.solution.Solution;
import be.kuleuven.optimalisatie.solution.SolutionWriter;

import com.gurobi.gurobi.*;

import javax.swing.SwingUtilities;
import java.util.HashMap;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<TrussProblem> problems = DataLoader.loadAllProblems();
        if (problems.isEmpty()) {
            System.err.println("Geen problemen gevonden om op te lossen. Controleer of de txt-bestanden in src/main/resources/Truss staan.");
            return;
        }

        for (int i =0 ; i<20 ; i++) {
            TrussProblem currentProblem = problems.get(i);
            System.out.println("Probleem geladen: " + currentProblem.getFileName());

            String outputPath = "src/main/resources/solutions/" + currentProblem.getFileName() + "_solution.txt";
            SolutionWriter.initializeFile(outputPath, currentProblem.getFileName());

            System.out.println("Starten van First Fit Decreasing algoritme...");
            FirstFitDecreasing ffd = new FirstFitDecreasing(currentProblem);
            Solution initialSolution = ffd.solve();

            for (Pattern pattern : initialSolution.getPatterns()) {
                System.out.println("Patroon " + initialSolution.getPatterns().indexOf(pattern) + ": " + pattern.getItems().size() + " items, Count: " + pattern.getCount() + ", Gebruikt: " + pattern.getUsedLength() + "/4200");
                if (pattern.getUsedLength() > 4200) {
                    System.err.println("Waarschuwing: Patroon overschrijdt de maximale lengte van 4200. Gebruikt: " + pattern.getUsedLength());
                }
            }

            System.out.println("LB= " + initialSolution.getLowerBound() + ", UB= " + initialSolution.getUpperBound());

            initialSolution.getPatterns().forEach(p -> p.setUsed(true));

            SolutionWriter.appendIteration(
                    outputPath,
                    0,
                    initialSolution.getLowerBound(),
                    initialSolution.getUpperBound(),
                    initialSolution.getDualValues(),                    // duale waarden
                    initialSolution.getPatterns()
            );

            System.out.println("Initiële oplossing succesvol weggeschreven naar: " + outputPath);

            GRBEnv env;
            RMP rmp;

            try {
                env = new GRBEnv(true);
                env.set("LogFile", "gurobi.log");
                env.start();

                rmp = new RMP(currentProblem, initialSolution.getPatterns(), env);
                rmp.addPatterns(initialSolution.getPatterns());
                int iterationNumber = 1;
                Solution RMPSolution = rmp.solve(iterationNumber);

                System.out.println("RMP Oplossing Iteratie 1: LB= " + RMPSolution.getLowerBound() + ", UB= " + RMPSolution.getUpperBound());

                SolutionWriter.appendIteration(
                        outputPath,
                        iterationNumber,
                        RMPSolution.getLowerBound(),
                        RMPSolution.getUpperBound(),
                        RMPSolution.getDualValues(),
                        RMPSolution.getPatterns()
                );

                Solution RMPSolutionAfterNewPattern = RMPSolution;
                while (true) {
                    iterationNumber++;
                    SubProblem columnGenration = new SubProblem(currentProblem, RMPSolutionAfterNewPattern.getPatterns(), env);
                    Pattern newPattern = columnGenration.solve(RMPSolutionAfterNewPattern.getDualValues());

                    if(newPattern == null) break;

                    rmp.addPattern(newPattern);
                    RMPSolutionAfterNewPattern = rmp.solve(iterationNumber);

                    System.out.println("RMP Oplossing Iteratie "+ iterationNumber +": LB= " + RMPSolutionAfterNewPattern.getLowerBound() + ", UB= " + RMPSolutionAfterNewPattern.getUpperBound());

                    SolutionWriter.appendIteration(
                            outputPath,
                            iterationNumber,
                            RMPSolutionAfterNewPattern.getLowerBound(),
                            RMPSolutionAfterNewPattern.getUpperBound(),
                            RMPSolutionAfterNewPattern.getDualValues(),
                            RMPSolutionAfterNewPattern.getPatterns()
                    );

                }
                env.dispose();
            } catch (GRBException e) {
                throw new RuntimeException(e);
            }

        }

        System.out.println("Starten van de Solution Viewer GUI...");
        SwingUtilities.invokeLater(() -> {
            SolutionViewer viewer = new SolutionViewer();
            viewer.setVisible(true);
        });
    }
}