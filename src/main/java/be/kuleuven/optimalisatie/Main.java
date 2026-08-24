package be.kuleuven.optimalisatie;

import be.kuleuven.optimalisatie.algorithm.*;
import be.kuleuven.optimalisatie.probleminstance.DataLoader;
import be.kuleuven.optimalisatie.probleminstance.TrussProblem;
import be.kuleuven.optimalisatie.solution.Pattern;
import be.kuleuven.optimalisatie.solution.Solution;
import be.kuleuven.optimalisatie.solution.SolutionWriter;
import be.kuleuven.optimalisatie.solution.SolutionReader;

import com.gurobi.gurobi.*;

import java.io.File;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<TrussProblem> problems = DataLoader.loadAllProblems();

        for (int i = 70; i < problems.size(); i++) {
            TrussProblem currentProblem = problems.get(i);
            System.out.println("Oplossen van instantie " + (i + 1) + "/" + problems.size() + ": " + currentProblem.getFileName());

            String outputPath = "src/main/resources/Solutions/" + currentProblem.getFileName().substring(0,9) + "_solution.txt";
            File file = new File(outputPath);

            try {
                GRBEnv env = new GRBEnv(true);
                env.set("LogFile", "gurobi.log");
                env.start();

                RMP rmp = new RMP(currentProblem, env);
                int iterationNumber;
                Solution RMPSolutionAfterNewPattern;

                if (file.exists()) {
                    System.out.println("Bestaand logbestand gevonden. Inlezen van de laatste opgeslagen status...");
                    SolutionReader.ParsedState lastState = SolutionReader.readLastIteration(outputPath);

                    if (lastState.stepName != null && lastState.stepName.contains("ILP")) {
                        System.out.println("instantie is al opgelost. Overslaan...");
                        env.dispose();
                        continue;
                    }

                    rmp.addPatterns(lastState.patterns);
                    iterationNumber = lastState.iteration;

                    // verderdoen
                    RMPSolutionAfterNewPattern = rmp.solve(iterationNumber);
                } else {
                    // bestand bestaat niet
                    SolutionWriter.initializeFile(outputPath, currentProblem.getFileName());

                    long iterStart = System.currentTimeMillis();
                    FirstFitDecreasing ffd = new FirstFitDecreasing(currentProblem);
                    Solution initialSolution = ffd.solve();

                    /*
                    for (Pattern pattern : initialSolution.getPatterns()) {
                        System.out.println("Patroon " + initialSolution.getPatterns().indexOf(pattern) + ": " + pattern.getItems().size() + " items, Count: " + pattern.getCount() + ", Gebruikt: " + pattern.getUsedLength() + "/4200");
                    }
                    */

                    long iterDuration = System.currentTimeMillis() - iterStart;
                    System.out.println("tijd FFD: " + iterDuration + " ms");

                    for (Pattern pattern : initialSolution.getPatterns()) {
                        pattern.setUsed(true);
                    }

                    SolutionWriter.appendIteration(
                            outputPath,
                            "Initial solution after FFD",
                            0,
                            initialSolution.getLowerBound(),
                            initialSolution.getUpperBound(),
                            initialSolution.getDualValues(),
                            initialSolution.getPatterns(),
                            iterDuration
                    );

                    rmp.addPatterns(initialSolution.getPatterns());
                    iterationNumber = 1;

                    iterStart = System.currentTimeMillis();
                    RMPSolutionAfterNewPattern = rmp.solve(iterationNumber);
                    iterDuration = System.currentTimeMillis() - iterStart;

                    System.out.println("RMP Oplossing Iteratie 1: LB= " + RMPSolutionAfterNewPattern.getLowerBound() + ", UB= " + RMPSolutionAfterNewPattern.getUpperBound() +" tijd: " + iterDuration + " ms");

                    SolutionWriter.appendIteration(
                            outputPath,
                            "RMP after FFD: ",
                            iterationNumber,
                            RMPSolutionAfterNewPattern.getLowerBound(),
                            RMPSolutionAfterNewPattern.getUpperBound(),
                            RMPSolutionAfterNewPattern.getDualValues(),
                            RMPSolutionAfterNewPattern.getPatterns(),
                            iterDuration
                    );
                }

                // blijven herhalen tot geen nieuwe patronen meer gevonden worden
                while (true) {
                    iterationNumber++;
                    SubProblem columnGeneration = new SubProblem(currentProblem, env);

                    long iterStart1 = System.currentTimeMillis();
                    Pattern newPattern = columnGeneration.solve(RMPSolutionAfterNewPattern.getDualValues(), iterationNumber);
                    long subProblemDuration = System.currentTimeMillis() - iterStart1;
                    System.out.println("Subprobleem Oplossing Iteratie "+ iterationNumber+ ": tijd: " + subProblemDuration + " ms");

                    if(newPattern == null) {
                        SolutionWriter.appendIteration(
                                outputPath,
                                "Subproblem Timeout or No New Pattern Found",
                                iterationNumber,
                                RMPSolutionAfterNewPattern.getLowerBound(),
                                RMPSolutionAfterNewPattern.getUpperBound(),
                                RMPSolutionAfterNewPattern.getDualValues(),
                                RMPSolutionAfterNewPattern.getPatterns(),
                                subProblemDuration
                        );
                        break;
                    }

                    long iterStart2 = System.currentTimeMillis();
                    rmp.addPattern(newPattern);
                    RMPSolutionAfterNewPattern = rmp.solve(iterationNumber);
                    long rmpDuration = System.currentTimeMillis() - iterStart2;

                    long totalIterationDuration = subProblemDuration + rmpDuration;

                    System.out.println("RMP Oplossing Iteratie "+ iterationNumber +": LB= " + RMPSolutionAfterNewPattern.getLowerBound() + ", UB= " + RMPSolutionAfterNewPattern.getUpperBound()+ " tijd: " + rmpDuration + " ms");

                    SolutionWriter.appendIteration(
                            outputPath,
                            "RMP Iteration: " + iterationNumber,
                            iterationNumber,
                            RMPSolutionAfterNewPattern.getLowerBound(),
                            RMPSolutionAfterNewPattern.getUpperBound(),
                            RMPSolutionAfterNewPattern.getDualValues(),
                            RMPSolutionAfterNewPattern.getPatterns(),
                            totalIterationDuration
                    );
                }

                ILPHeuristic divingHeuristic = new ILPHeuristic(rmp.getModel());
                long iterStartILP = System.currentTimeMillis();
                Solution finalSolution = divingHeuristic.solve(RMPSolutionAfterNewPattern);
                long iterDurationILP = System.currentTimeMillis() - iterStartILP;

                System.out.println("Finale Oplossing na Diving Heuristic: LB= " + finalSolution.getLowerBound() + ", UB= " + finalSolution.getUpperBound());
                SolutionWriter.appendIteration(
                        outputPath,
                        "ILP solution",
                        iterationNumber,
                        finalSolution.getLowerBound(),
                        finalSolution.getUpperBound(),
                        finalSolution.getDualValues(),
                        finalSolution.getPatterns(),
                        iterDurationILP
                );

                env.dispose();
            } catch (GRBException e) {
                throw new RuntimeException(e);
            }
        }
    }
}