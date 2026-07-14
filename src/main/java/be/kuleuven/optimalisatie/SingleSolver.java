package be.kuleuven.optimalisatie;

import be.kuleuven.optimalisatie.algorithm.FirstFitDecreasing;
import be.kuleuven.optimalisatie.probleminstance.DataLoader;
import be.kuleuven.optimalisatie.probleminstance.TrussProblem;
import be.kuleuven.optimalisatie.solution.Solution;

import java.util.List;

public class SingleSolver {
    public static void main(String[] args) {
        // Load the problem instance from the file

        List<TrussProblem> problems = DataLoader.loadAllProblems();

        // Create a solver and solve the problem

        FirstFitDecreasing solver = new FirstFitDecreasing(problems.getFirst());
        Solution solution = solver.solve();

        // For now, just print out the loaded problem details
        System.out.println("Loaded problem instance: " + problems.getFirst().getFileName());
        System.out.println("Total items: " + problems.getFirst().getTotalItems());
        System.out.println("Number of trapezoid types: " + problems.getFirst().getNumberOfTrapezoidTypes());
        System.out.println("Number of orders: " + problems.getFirst().getNumberOfOrders());

        System.out.println("Solution found with total bins used: " + solution.getTotalBins());
        System.out.println("Lower bound: " + solution.getLowerBound());
        System.out.println("Upper bound: " + solution.getUpperBound());
        System.out.println("Execution time (ms): " + solution.getExecutionTimeMs());
        for(int i = 0; i < solution.getPatterns().size(); i++) {
            System.out.println("Pattern " + (i + 1) + ": " + solution.getPatterns().get(i).getItems().size() + " items, used length: " + solution.getPatterns().get(i).getUsedLength());
        }

    }
}
