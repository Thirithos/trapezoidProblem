package be.kuleuven.optimalisatie;

import com.gurobi.gurobi.*;

public class Main {
    public static void main(String[] args) {
        try {
            // Maak een nieuwe Gurobi omgeving
            GRBEnv env = new GRBEnv(true);
            env.set("logFile", "gurobi.log");
            env.start();

            // Maak een leeg model
            GRBModel model = new GRBModel(env);

            // Voeg een variabele toe
            GRBVar x = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "x");
            GRBVar y = model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "y");

            // Stel een doelstelling in
            GRBLinExpr expr = new GRBLinExpr();
            expr.addTerm(1.0, x);
            expr.addTerm(2.0, y);
            model.setObjective(expr, GRB.MAXIMIZE);

            GRBLinExpr lhs = new GRBLinExpr();
            lhs.addTerm(1.0, x);
            lhs.addTerm(-3.0, y);
            GRBLinExpr rhs = new GRBLinExpr();
            rhs.addConstant(0.0);

            model.addConstr(lhs, GRB.EQUAL, rhs, "c0");


            // Optimaliseer het model
            model.optimize();

            // Print de oplossing
            System.out.println("Optimal value X: " + x.get(GRB.DoubleAttr.X));
            System.out.println("Optimal value Y: " + y.get(GRB.DoubleAttr.X));

            

            // Opruimen
            model.dispose();
            env.dispose();
        } catch (GRBException e) {
            System.err.println("Error code: " + e.getErrorCode() + ". " + e.getMessage());
        }
    }
}
