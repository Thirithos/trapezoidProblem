package be.kuleuven.optimalisatie.algorithm;

import be.kuleuven.optimalisatie.probleminstance.Trapezoid;
import be.kuleuven.optimalisatie.probleminstance.TrussProblem;
import be.kuleuven.optimalisatie.solution.Pattern;
import be.kuleuven.optimalisatie.solution.Solution;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.gurobi.gurobi.*;

public class RMP  {
    private final TrussProblem problem;
    private final int binLength = 4200;
    private List<Pattern> patterns;

    private GRBEnv env;
    private GRBModel model;
    private GRBConstr[] demandConstraints;
    private GRBLinExpr[] expressions;

    private int itemTypeCount;

    public RMP(TrussProblem problem, List<Pattern> patterns, GRBEnv env) throws GRBException {
        this.problem = problem;
        this.patterns = new ArrayList<>();
        this.env = env;
        this.itemTypeCount = problem.getNumberOfTrapezoidTypes();
    }


    public void addPattern(Pattern pattern) {
        patterns.add(pattern);
    }

    public void addPatterns(List<Pattern> patterns) {
        for (Pattern pattern : patterns) {
            addPattern(pattern);
        }
    }

    public Solution solve(int iterationNumber) {
        Solution solution = new Solution(problem.getFileName());

        try {
            // Initialisatie van model
            this.model = new GRBModel(env);
            this.model.set(GRB.StringAttr.ModelName, "RMP_iter_" + iterationNumber);

            this.demandConstraints = new GRBConstr[itemTypeCount+1];
            this.expressions = new GRBLinExpr[itemTypeCount+1];

            for (int i = 1; i <= itemTypeCount; i++) {
                this.expressions[i] = new GRBLinExpr();
            }

            GRBVar[] x = new GRBVar[patterns.size()];
            GRBLinExpr objective = new GRBLinExpr();

            //  de beslissingsvariabelen (xj)
            for (int j = 0; j < patterns.size(); j++) {
                // groter dan 0, tot infinity, continue en draagt bij aan objective functie dus 1.0
                x[j] = model.addVar(0.0, GRB.INFINITY, 1.0, GRB.CONTINUOUS, "Pattern_" + (j+1));
                objective.addTerm(1.0, x[j]);
            }

            // direct ook de objective functie
            model.setObjective(objective, GRB.MINIMIZE);
            model.update();

            // constraints
            // sum(a_ij * x_j) >= d_i
            for (int i = 1; i <= itemTypeCount; i++) {
                Trapezoid currentType = problem.getTrapezoids().get(i - 1);
                int demand = currentType.getNumberOfItems();

                for (int j = 0; j < patterns.size(); j++) {
                    Pattern pattern = patterns.get(j);
                    int a_ij = pattern.countOccurrencesInPattern(currentType);

                    if (a_ij > 0) {
                        expressions[i].addTerm(a_ij, x[j]);
                    }
                }

                demandConstraints[i] = model.addConstr(expressions[i], GRB.GREATER_EQUAL, demand, "Demand_Type_" + i);
            }

            model.update();

            String modelName = problem.getFileName().substring(0, problem.getFileName().length() - 4);

            Path dir = Paths.get("src", "main", "resources", "ModelsDebug", modelName);
            Files.createDirectories(dir);

            model.write("src/main/resources/ModelsDebug/"+  modelName + "/iteration_"+ iterationNumber +"_debug_RMP.lp");

            model.optimize();

            int optimStatus = model.get(GRB.IntAttr.Status);
            if (optimStatus == GRB.Status.OPTIMAL) {
                double objVal = model.get(GRB.DoubleAttr.ObjVal);
                solution.setLowerBound(objVal);

                System.out.println("RESULTATEN RMP");
                System.out.println("Objective Value (Z): " + objVal);
                System.out.println();

                System.out.println("GEBRUIKTE PATRONEN (Variabelen xj)");
                for (int j = 0; j < patterns.size(); j++) {
                    double variableValue = x[j].get(GRB.DoubleAttr.X);
                    Pattern p = patterns.get(j);

                    p.setUsed(true);
                    p.setCount(variableValue);
                    System.out.println(x[j].get(GRB.StringAttr.VarName) + " = " + variableValue);

                    solution.addPattern(p);
                }
                System.out.println();

                System.out.println("DUALE WAARDEN (Pi) PER ITEM TYPE");
                List<Double> dualValues = new ArrayList<>();
                for (int i = 1; i <= itemTypeCount; i++) {
                    double dualValue = demandConstraints[i].get(GRB.DoubleAttr.Pi);
                    System.out.println(demandConstraints[i].get(GRB.StringAttr.ConstrName) + " = " + dualValue);
                    dualValues.add(dualValue);
                }
                solution.setDualValues(dualValues);

            } else {
                System.err.println("Geen optimale oplossing gevonden. Status code: " + optimStatus);
            }

        } catch (GRBException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return solution;
    }
}
