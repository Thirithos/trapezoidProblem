package be.kuleuven.optimalisatie.algorithm;

import be.kuleuven.optimalisatie.solution.Pattern;
import be.kuleuven.optimalisatie.solution.Solution;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

import java.util.List;

public class ILPHeuristic implements  DivingHeuristic{
    // Het is een heuristiek want we voegen geen extra patronen toe,
    // we fixeren elke variabele naar een integere waarde en lossen het probleem op als een ILP.
    GRBModel model;

    public ILPHeuristic(GRBModel model) {
        this.model = model;
    }

    @Override
    public Solution solve(Solution solution) {
        // zelfde model als in RMP, maar dan alle variabelen gefixeerd naar een integere waarde
        Solution sol = new Solution(solution.getProblemName());

        try {
            for (GRBVar var: model.getVars()) {
                var.set(GRB.CharAttr.VType, GRB.INTEGER);
            }
            model.update();
            model.optimize();

            double objVal = model.get(GRB.DoubleAttr.ObjVal);
            sol.setLowerBound(solution.getLowerBound());
            sol.setUpperBound(objVal);

            // dit zorgt dat in de nieuwe oplossing
            List<Pattern> lpPatterns = solution.getPatterns();
            GRBVar[] vars = model.getVars();
            for (int j = 0; j < vars.length; j++) {
                double val = vars[j].get(GRB.DoubleAttr.X);
                if (val > 0.1) {
                    Pattern p = lpPatterns.get(j);
                    p.setUsed(true);
                    p.setCount(val);
                    sol.addPattern(p, (int) val);
                } else {
                    Pattern p = lpPatterns.get(j);
                    p.setUsed(false);
                    p.setCount(0);
                    sol.addPattern(p, 0);
                }
            }
        } catch (GRBException e) {
            e.printStackTrace();
        }
        return sol;
    }
}
