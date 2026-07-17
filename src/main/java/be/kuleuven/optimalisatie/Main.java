package be.kuleuven.optimalisatie;

import be.kuleuven.optimalisatie.algorithm.*;
import be.kuleuven.optimalisatie.gui.SolutionViewer;
import be.kuleuven.optimalisatie.probleminstance.DataLoader;
import be.kuleuven.optimalisatie.probleminstance.TrussProblem;
import be.kuleuven.optimalisatie.solution.Pattern;
import be.kuleuven.optimalisatie.solution.Solution;
import be.kuleuven.optimalisatie.solution.SolutionWriter;

import com.gurobi.gurobi.*;

import javax.swing.SwingUtilities;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<TrussProblem> problems = DataLoader.loadAllProblems();
        if (problems.isEmpty()) {
            System.err.println("Geen problemen gevonden om op te lossen. Controleer of de txt-bestanden in src/main/resources/Truss staan.");
            return;
        }

        for (int i =problems.size()-1 ; i<problems.size() ; i++) {
            TrussProblem currentProblem = problems.get(i);
            System.out.println("Probleem geladen: " + currentProblem.getFileName());

            String outputPath = "src/main/resources/solutions/" + currentProblem.getFileName() + "_solution.txt";
            SolutionWriter.initializeFile(outputPath, currentProblem.getFileName());

            System.out.println("Starten van First Fit Decreasing algoritme...");
            long iterStart = System.currentTimeMillis();
            FirstFitDecreasing ffd = new FirstFitDecreasing(currentProblem);
            Solution initialSolution = ffd.solve();

            for (Pattern pattern : initialSolution.getPatterns()) {
                System.out.println("Patroon " + initialSolution.getPatterns().indexOf(pattern) + ": " + pattern.getItems().size() + " items, Count: " + pattern.getCount() + ", Gebruikt: " + pattern.getUsedLength() + "/4200");
            }
            long iterDuration = System.currentTimeMillis() - iterStart;

            System.out.println("LB= " + initialSolution.getLowerBound() + ", UB= " + initialSolution.getUpperBound());

            initialSolution.getPatterns().forEach(p -> p.setUsed(true));

            SolutionWriter.appendIteration(
                    outputPath,
                    "Initial solution after FFD",
                    0,
                    initialSolution.getLowerBound(),
                    initialSolution.getUpperBound(),
                    initialSolution.getDualValues(),                    // duale waarden
                    initialSolution.getPatterns(),
                    iterDuration
            );

            System.out.println("Initiële oplossing succesvol weggeschreven naar: " + outputPath);

            GRBEnv env;
            RMP rmp;

            try {
                env = new GRBEnv(true);
                env.set("LogFile", "gurobi.log");
                env.start();

                iterStart = System.currentTimeMillis();
                rmp = new RMP(currentProblem, env);
                rmp.addPatterns(initialSolution.getPatterns());
                int iterationNumber = 1;
                Solution RMPSolution = rmp.solve(iterationNumber);

                iterDuration = System.currentTimeMillis() - iterStart;

                System.out.println("RMP Oplossing Iteratie 1: LB= " + RMPSolution.getLowerBound() + ", UB= " + RMPSolution.getUpperBound());

                SolutionWriter.appendIteration(
                        outputPath,
                        "RMP after FFD: ",
                        iterationNumber,
                        RMPSolution.getLowerBound(),
                        RMPSolution.getUpperBound(),
                        RMPSolution.getDualValues(),
                        RMPSolution.getPatterns(),
                        iterDuration
                );

                Solution RMPSolutionAfterNewPattern = RMPSolution;
                while (true) {
                    iterationNumber++;
                    SubProblem columnGenration = new SubProblem(currentProblem, env);
                    iterStart = System.currentTimeMillis();
                    Pattern newPattern = columnGenration.solve(RMPSolutionAfterNewPattern.getDualValues(), iterationNumber);

                    if(newPattern == null) break;

                    rmp.addPattern(newPattern);
                    RMPSolutionAfterNewPattern = rmp.solve(iterationNumber);
                    iterDuration = System.currentTimeMillis() - iterStart;

                    System.out.println("RMP Oplossing Iteratie "+ iterationNumber +": LB= " + RMPSolutionAfterNewPattern.getLowerBound() + ", UB= " + RMPSolutionAfterNewPattern.getUpperBound());

                    SolutionWriter.appendIteration(
                            outputPath,
                            "RMP Iteration: " + iterationNumber,
                            iterationNumber,
                            RMPSolutionAfterNewPattern.getLowerBound(),
                            RMPSolutionAfterNewPattern.getUpperBound(),
                            RMPSolutionAfterNewPattern.getDualValues(),
                            RMPSolutionAfterNewPattern.getPatterns(),
                            iterDuration
                    );
                }

                DivingHeuristic divingHeuristic = new ILPHeuristic(rmp.getModel());
                iterStart = System.currentTimeMillis();
                Solution finalSolution = divingHeuristic.solve(RMPSolutionAfterNewPattern);
                iterDuration = System.currentTimeMillis() - iterStart;

                System.out.println("Finale Oplossing na Diving Heuristic: LB= " + finalSolution.getLowerBound() + ", UB= " + finalSolution.getUpperBound());
                SolutionWriter.appendIteration(
                        outputPath,
                        "ILP solution",
                        iterationNumber,
                        finalSolution.getLowerBound(),
                        finalSolution.getUpperBound(),
                        finalSolution.getDualValues(),
                        finalSolution.getPatterns(),
                        iterDuration
                );

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