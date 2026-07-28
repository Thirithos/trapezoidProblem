package be.kuleuven.optimalisatie.algorithm;

import be.kuleuven.optimalisatie.probleminstance.Trapezoid;
import be.kuleuven.optimalisatie.probleminstance.TrussProblem;
import be.kuleuven.optimalisatie.solution.Pattern;
import com.gurobi.gurobi.*;

import java.util.List;

public class SubProblem {
    private final TrussProblem problem;
    private final int binLength = 4200;

    private GRBEnv env;
    private GRBModel model;

    private int itemTypeCount;

    public SubProblem(TrussProblem problem, GRBEnv env) throws GRBException {
        this.problem = problem;
        this.env = env;
        this.itemTypeCount = problem.getNumberOfTrapezoidTypes();
    }

    public Pattern solve(List<Double> dualValues, int iteration) {
        int minLength = Integer.MAX_VALUE;
        int projectionLeft = Integer.MAX_VALUE;
        int projectionRight = Integer.MAX_VALUE;
        boolean projectionIsLeft = false;

        // bepaal de min lengte van trapezoiden die mogelijks aanleiding geven tot een nieuw patroon
        for (int i = 0; i < itemTypeCount;i++) {
            Trapezoid t = problem.getTrapezoids().get(i);

            if (dualValues.get(i) > 0 && t.getTotalLength() < minLength) {
                minLength = t.getTotalLength();
                projectionLeft = t.getP1();
                projectionRight = t.getP2();
                if (projectionLeft > projectionRight) {
                    projectionIsLeft = true;
                }
            }
        }

        // er is geen enkele item met duale waarde > 0
        if (minLength == Integer.MAX_VALUE) {
            return null;
        }

        // minLength kan niet integer.MAX_VALUE zijn.
        // dus maxItems is altijd groter of gelijk aan 1, want minLength is altijd <= binLength
        // om nesting in rekening te brengen, kunnen er meer items in een bin passen.
        // de boolean projectionIsLeft bepaalt welke projectie de geneste snede is.
        // dus als de projectionRight kleiner is dan projectionLeft, dan is de geneste projectie de linkse projectie aan de start (bespaart meeste ruimte)
        // en dus is de boolean projectionIsLeft true.
        int addedLength = minLength;
        int maxItems = 1;
        while (binLength > addedLength) {
            if (projectionIsLeft) {
                addedLength += minLength - projectionLeft;
                projectionIsLeft = false;
            } else {
                addedLength += minLength - projectionRight;
                projectionIsLeft = true;
            }
            if (binLength >= addedLength) {
                maxItems++;
            }
        }

        System.out.println("--> Maximaal aantal items dat in een bin kan passen (met nesting): " + maxItems);



        Pattern pattern;
        try {
            pattern = solve(maxItems, dualValues, itemTypeCount, iteration);
        } catch (GRBException e) {
            e.printStackTrace();
            return null;
        }

        int optimalStatus = -1;
        try {
            optimalStatus = model.get(GRB.IntAttr.Status);
        } catch (GRBException e) {
            e.printStackTrace();
        } finally {
            model.dispose();
        }

        if (optimalStatus == GRB.Status.OPTIMAL) {
            if (pattern == null) {
                System.out.println("--> Geen nieuw patroon meer gevonden (w <= 1). Optimum bereikt!");
            }
            return pattern;
        }

        System.out.println("--> Subprobleem kon niet optimaal worden opgelost. Status: " + optimalStatus);
        return null;
    }

    private Pattern solve(int maxItems, List<Double> dualValues, int numItemTypes, int iteration) throws GRBException {
        try {
            model = new GRBModel(env);
            model.set(GRB.StringAttr.ModelName, "PricingProblem");
            model.set(GRB.IntParam.OutputFlag, 0);
            model.set(GRB.IntParam.Symmetry, 2);

            // Beslissingsvariabelen: y[i][flippedVertical][k] = 1 als itemtype i, met verticale flip flippedVertical (0 of 1), op positie k in het patroon wordt geplaatst
            GRBVar[][][] y = new GRBVar[numItemTypes][2][maxItems];
            for (int i = 0; i < numItemTypes; i++) {
                for (int flippedVertical = 0; flippedVertical <= 1; flippedVertical++) {
                    for (int k = 0; k < maxItems; k++) {
                        y[i][flippedVertical][k] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "y_" + i + "_" + flippedVertical + "_" + k);
                    }
                }
            }

            // Beslissingsvariabelen: o[k] = overlap tussen item op positie k en item op positie k+1
            GRBVar[] o = new GRBVar[maxItems - 1];
            for (int k = 0; k < maxItems - 1; k++) {
                o[k] = model.addVar(0.0, binLength, 0.0, GRB.CONTINUOUS, "o_" + k);
            }

            // Objective functie: max w = pi[i] * y[i][flippedVertical][k] dus de totale winst van duale waarden (pi[i]) van gebruikte items (sum(y[i][flippedVertical][k])) in het nieuwe patroon
            GRBLinExpr objective = new GRBLinExpr();
            for (int i = 0; i < numItemTypes; i++) {
                double pi = dualValues.get(i);
                for (int flippedVertical = 0; flippedVertical <= 1; flippedVertical++) {
                    for (int k = 0; k < maxItems; k++) {
                        objective.addTerm(pi, y[i][flippedVertical][k]);
                    }
                }
            }
            model.setObjective(objective, GRB.MAXIMIZE);

            // Projecties per (itemtype, flippedVertical) als lookup tabel
            double[][] leftProj = new double[numItemTypes][2];
            double[][] rightProj = new double[numItemTypes][2];
            for (int i = 0; i < numItemTypes; i++) {
                Trapezoid type = problem.getTrapezoids().get(i);
                leftProj[i][0] = type.getP1();
                rightProj[i][0] = type.getP2();
                leftProj[i][1] = type.getP2();
                rightProj[i][1] = type.getP1();
            }

            // Beperking:
            // sum(y[i][flippedVertical][k]) <= demand[i] voor alle i, dus het aantal gekozen items van type i mag niet groter zijn dan de vraag naar dat itemtype.
            for (int i = 0; i < numItemTypes; i++) {
                int demand = problem.getTrapezoids().get(i).getNumberOfItems();
                GRBLinExpr sumY_i = new GRBLinExpr();
                for (int flippedVertical = 0; flippedVertical <= 1; flippedVertical++) {
                    for (int k = 0; k < maxItems; k++) {
                        sumY_i.addTerm(1.0, y[i][flippedVertical][k]);
                    }
                }
                model.addConstr(sumY_i, GRB.LESS_EQUAL, demand, "Demand_" + i);
            }

            // Beperking:
            // sum(y[i][flippedVertical][k]) <= 1 voor alle i en flippedVertical, dus er kan maximaal 1 item in een slot (positie) worden geplaatst.
            // Beperking:
            // sum(y[i][flippedVertical][k]) <= sum(y[i][flippedVertical][k-1]) voor alle i en flippedVertical, dus er kan geen item in een slot (positie) worden geplaatst als het vorige slot leeg is.
            GRBLinExpr[] slotUsage = new GRBLinExpr[maxItems];
            for (int k = 0; k < maxItems; k++) {
                slotUsage[k] = new GRBLinExpr();
                for (int i = 0; i < numItemTypes; i++) {
                    for (int flippedVertical = 0; flippedVertical <= 1; flippedVertical++) {
                        slotUsage[k].addTerm(1.0, y[i][flippedVertical][k]);
                    }
                }
                model.addConstr(slotUsage[k], GRB.LESS_EQUAL, 1.0, "MaxOnePerSlot_" + k);
                if (k > 0) {
                    model.addConstr(slotUsage[k], GRB.LESS_EQUAL, slotUsage[k - 1], "Sequential_" + k);
                }
            }

            // Beperking:
            // o[k] <= sum(rightProj[i][flippedVertical] * y[i][flippedVertical][k]) voor alle i en flippedVertical, dus de overlap tussen item op positie k en
            // item op positie k+1 is kleiner of gelijk aan de rechterprojectie van het item op positie k.
            // Beperking:
            // o[k] <= sum(leftProj[i][flippedVertical] * y[i][flippedVertical][k + 1]) voor alle i en flippedVertical, dus de overlap tussen item op
            // positie k en item op positie k+1 is kleiner of gelijk aan de linkerprojectie van het item op positie k+1.

            // deze twee beperkingen zorgen dus dat overlap tussen twee items bounded is tussen de linker en rechterpojecties van de items
            for (int k = 0; k < maxItems - 1; k++) {
                GRBLinExpr rightProjK = new GRBLinExpr();
                GRBLinExpr leftProjK1 = new GRBLinExpr();

                for (int i = 0; i < numItemTypes; i++) {
                    for (int flippedVertical = 0; flippedVertical <= 1; flippedVertical++) {
                        rightProjK.addTerm(rightProj[i][flippedVertical], y[i][flippedVertical][k]);
                        leftProjK1.addTerm(leftProj[i][flippedVertical], y[i][flippedVertical][k + 1]);
                    }
                }

                model.addConstr(o[k], GRB.LESS_EQUAL, rightProjK, "Overlap_RightLimit_" + k);
                model.addConstr(o[k], GRB.LESS_EQUAL, leftProjK1, "Overlap_LeftLimit_" + k);
            }

            // Beperking:
            // sum(len[i] * y[i][flippedVertical][k]) - sum(o[k]) <= binLength, dus de totale lengte van alle gekozen items minus de overlap tussen opeenvolgende
            // items moet kleiner of gelijk zijn aan de maximale lengte van de bin.
            GRBLinExpr totalLengthExpr = new GRBLinExpr();
            for (int i = 0; i < numItemTypes; i++) {
                double len = problem.getTrapezoids().get(i).getTotalLength();
                for (int flippedVertical = 0; flippedVertical <= 1; flippedVertical++) {
                    for (int k = 0; k < maxItems; k++) {
                        totalLengthExpr.addTerm(len, y[i][flippedVertical][k]);
                    }
                }
            }
            for (int k = 0; k < maxItems - 1; k++) {
                totalLengthExpr.addTerm(-1.0, o[k]);
            }
            model.addConstr(totalLengthExpr, GRB.LESS_EQUAL, binLength, "Max_Bin_Length");
            model.update();
            //model.write("src/main/resources/ModelsDebug/" + problem.getFileName().replace(".txt","") + "/sub_iter_" + iteration + ".lp");
            model.optimize();

            Pattern newPattern = null;

            int solCount = model.get(GRB.IntAttr.SolCount);
            if (solCount > 0) {
                double objVal = model.get(GRB.DoubleAttr.ObjVal);

                if (objVal > 1.0 + 1e-5) {
                    System.out.println("--> Nieuw patroon gevonden! Doelfunctiewaarde (w): " + objVal);

                    newPattern = new Pattern(false);

                    Trapezoid lastItemAdded = null;
                    for (int k = 0; k < maxItems; k++) {
                        boolean slotFilled = false;
                        for (int i = 0; i < numItemTypes && !slotFilled; i++) {
                            for (int flippedVertical = 0; flippedVertical <= 1; flippedVertical++) {
                                if (y[i][flippedVertical][k].get(GRB.DoubleAttr.X) > 0.5) {
                                    Trapezoid type = problem.getTrapezoids().get(i);
                                    Trapezoid item = new Trapezoid(
                                            1,
                                            type.getTotalLength(),
                                            type.getP1(),
                                            type.getP2(),
                                            type.getAngle1(),
                                            type.getAngle2(),
                                            type.getShapeIndicator()
                                    );
                                    boolean verticalFlip = (flippedVertical == 1);
                                    if (verticalFlip) {
                                        item.flipVertically();
                                    }
                                    // horizontaal flippen heeft hier geen invloed op de totale lengte zoals paper zegt maar voor te visualiseren wel belangrijk
                                    if (needsHorizontalFlip(verticalFlip, type.getShapeIndicator(), lastItemAdded)) {
                                        item.flipHorizontally();
                                    }
                                    newPattern.addItem(item);
                                    lastItemAdded = item;
                                    slotFilled = true;
                                    break;
                                }
                            }
                        }
                        if (!slotFilled) {
                            break;
                        }
                    }

                    if (newPattern.getUsedLength() > binLength) {
                        System.err.println("--> WAARSCHUWING: gereconstrueerd patroon overschrijdt binLength (" +
                                newPattern.getUsedLength() + " > " + binLength + ").");
                    }
                }

            }

            return newPattern;
        } catch (GRBException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean needsHorizontalFlip(boolean currentNeedsVerticalFlip, int currentShapeIndicator, Trapezoid lastItem) {
        // om de juiste nesting te bekomen wat het LP model verwacht
        if (lastItem == null) {
            // Eerste item van het patroon
            return false;
        }

        boolean lastFlippedHorizontally = lastItem.isFlippedHorizontally();
        boolean lastFlippedVertically = lastItem.isFlippedVertically();
        int lastShape = lastItem.getShapeIndicator();

        if (!currentNeedsVerticalFlip) {
            if (lastShape == 0) {
                return lastFlippedHorizontally != lastFlippedVertically;
                // als beide true zijn dan is orientatie \      in beide gevallen voor de nieuwe item type is er geen horizontale flip nodig,
                // omdat nieuwe item type niet geflipt is en dus altijd \ is
            } else {
                return !lastFlippedHorizontally;
                //     zolang laatste item niet horizontaal geflipt is dan is de project / en dus moet er horizontaal geflipt worden in beide gevallen
            }
        } else {
            if (lastShape == 0) {
                // iets complexer want er zijn meerdere mogelijkheden  shape 0  \\
                if (lastFlippedHorizontally == lastFlippedVertically) {
                    // als er zowel horizontaal en verticaal geflipt zijn dan is projectie \\
                    return currentShapeIndicator == 0;
                    // er is slechts 1 flip gedaan in nieuwe shape:
                    // bij shape 0 is dit // dus horizontale flip nodig om \ te bekomen bij de linkerprojectie
                    // bij shape 1 is dit \/, is dus al genest
                } else {
                    // in de andere gevallen is projectie /
                    // want als de vorige maar 1 flip heeft gedaan in shape 0 dan is projectie //
                    // er wordt slechts 1 flip gedaan in nieuwe shape
                    // bij shape 0 is dit //
                    // bij shape 1 is dit \/ dus nog eens horizontaal flippen is nodig om / te bekomen bij de linkerprojectie
                    return currentShapeIndicator != 0;
                }
            } else {
                if (!lastFlippedHorizontally) {
                    // als er geen horizontale flip is gedaan bij vorige dan is de shape \/
                    // shape 0 wordt verticaal geflipt en dus is het // er moet niet horizontaal geflipt worden
                    // shape 1 wordt verticaal geflipt en dus is het \/, er kan niet worden genest
                    // dus horizontale flip is nodig
                    return currentShapeIndicator != 0;
                } else {
                    // er is geflipt bij vorige item dus shape is /\
                    // bij shape 0 wordt verticaal geflipt en dus is het // geen nesting, dus horizontale flip is nodig
                    // bij shape 1 wordt verticaal geflipt en dus is het \/
                    // er kan genest worden, dus horizontale flip is niet nodig
                    return currentShapeIndicator == 0;
                }
            }
        }
    }
}