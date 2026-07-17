package be.kuleuven.optimalisatie.algorithm;

import be.kuleuven.optimalisatie.probleminstance.Trapezoid;
import be.kuleuven.optimalisatie.probleminstance.TrussProblem;
import be.kuleuven.optimalisatie.solution.Pattern;
import be.kuleuven.optimalisatie.solution.Solution;

import java.util.ArrayList;
import java.util.List;

public class FirstFitDecreasing {
    private final TrussProblem problem;
    private final int binLength = 4200;

    public FirstFitDecreasing(TrussProblem problem) {
        this.problem = problem;
    }

    public Solution solve() {
        long startTime = System.currentTimeMillis();

        List<Trapezoid> individualItems = new ArrayList<>();
        for (Trapezoid type : problem.getTrapezoids()) {
            for (int i = 0; i < type.getNumberOfItems(); i++) {
                // nieuwe trapezium voor enkele items te hebben
                individualItems.add(new Trapezoid(
                        1,
                        type.getTotalLength(),
                        type.getP1(),
                        type.getP2(),
                        type.getAngle1(),
                        type.getAngle2(),
                        type.getShapeIndicator()
                ));
            }
        }

        individualItems.sort((t1, t2) -> Integer.compare(t2.getTotalLength(), t1.getTotalLength()));

        List<Pattern> originalPatterns = new ArrayList<>();

        // onthoud type nul shape ziet er standaard uit:
        //          __________
        //           \              \
        //             \_________\

        // onthoud type een shape ziet er standaard uit:
        //          __________
        //          \             /
        //            \______/


        for (int i = 0; i < individualItems.size(); i++) {
            // we nemen het item en proberen het in een bestaand patroon te plaatsen.
            // door telkens te nesten
            if (originalPatterns.isEmpty()) {
                Pattern newPattern = new Pattern(true);
                newPattern.addItem(individualItems.get(i));
                originalPatterns.add(newPattern);
            } else {
                boolean itemPlaced = false;
                for (Pattern pattern : originalPatterns) {
                    int patternLength = pattern.getUsedLength();
                    int itemLength = individualItems.get(i).getTotalLength();
                    int itemTypeNewItem = individualItems.get(i).getShapeIndicator();
                    int rightProjectionLengthNewItem = individualItems.get(i).getRightProjectionLength();
                    int leftProjectionLengthNewItem = individualItems.get(i).getLeftProjectionLength();
                    Trapezoid lastItemInPattern = pattern.getItems().getLast();
                    int itemTypeLastItem = lastItemInPattern.getShapeIndicator();
                    int projectionLengthLastItem = lastItemInPattern.getRightProjectionLength();
                    boolean isFlippedHorizontally = lastItemInPattern.isFlippedHorizontally();
                    boolean isFlippedVertically = lastItemInPattern.isFlippedVertically();

                    // eerst bekijken welke projectie van nieuwe trapezium het dichtst bij de projectie van laatste trapezium in patroon ligt
                    // vervolgens berekenen of het past in het patroon
                    // zo niet nieuw patroon maken
                    // als het past, dan moeten we item toevoegen maar in juiste oriëntatie

                    // we kennen lengte van rechtse projectie, en de twee projecties van nieuwe trapezium
                    // check welke projectie dichter bij de rechtse projectie van laatste trapezium
                    int distanceLeftProjection = Math.abs(leftProjectionLengthNewItem - projectionLengthLastItem);
                    int distanceRightProjection = Math.abs(rightProjectionLengthNewItem - projectionLengthLastItem);

                    // eerste geval de trapezium is volledig symmetrisch of de linkse projectie komt het dichtst bij de rechtse projectie van laatste trapezium
                    if ((leftProjectionLengthNewItem == rightProjectionLengthNewItem) || (distanceLeftProjection < distanceRightProjection)) {
                        // berekenen of het past in het patroon
                        if (patternLength + itemLength - Math.min(leftProjectionLengthNewItem, projectionLengthLastItem) <= binLength) {
                            // past
                            // bepaal eerst de oriëntatie van laatste item
                            if (itemTypeLastItem == 0) {
                                if ((isFlippedHorizontally && isFlippedVertically) || (!isFlippedHorizontally && !isFlippedVertically)) {
                                    // Orientatie van projectie is \ dus in beide gevallen originele orientatie van nieuwe trapezium behouden
                                    pattern.addItem(individualItems.get(i));
                                    itemPlaced = true;
                                } else {
                                    // nu is orientatie / dus horizontaal spiegelen van nieuwe trapezium
                                    individualItems.get(i).flipHorizontally();
                                    pattern.addItem(individualItems.get(i));
                                    itemPlaced = true;
                                }
                            } else {
                                if ((!isFlippedHorizontally && !isFlippedVertically) || (!isFlippedHorizontally && isFlippedVertically)) {
                                    // orientatie van projectie is / dus in beide gevallen horizontaal spiegelen van nieuwe trapezium
                                    individualItems.get(i).flipHorizontally();
                                    pattern.addItem(individualItems.get(i));
                                    itemPlaced = true;
                                } else {
                                    // nu is orientatie \ dus originele orientatie van nieuwe trapezium behouden
                                    pattern.addItem(individualItems.get(i));
                                    itemPlaced = true;
                                }
                            }
                            break;
                        }
                    }

                    if (distanceRightProjection < distanceLeftProjection) {
                        // rechter projectie is dichter bij de rechter projectie van laatste trapezium
                        // berekenen of deze past in het patroon
                        if (patternLength+ itemLength - Math.min(rightProjectionLengthNewItem, projectionLengthLastItem) <= binLength) {
                            // past
                            // bepaal eerst de oriëntatie van laatste item
                            if (itemTypeLastItem == 0) {
                                if ((isFlippedHorizontally && isFlippedVertically) || (!isFlippedHorizontally && !isFlippedVertically)) {
                                    // Orientatie van projectie is \ dus in geval dat nieuw item type 1 is enkel verticaal spiegelen
                                    // in geval van type 0, horizontaal en verticaal spiegelen
                                    if (itemTypeNewItem == 0) {
                                        individualItems.get(i).flipHorizontally();
                                        individualItems.get(i).flipVertically();
                                    } else {
                                        individualItems.get(i).flipVertically();
                                    }
                                    pattern.addItem(individualItems.get(i));
                                    itemPlaced = true;
                                } else {
                                    // nu is orientatie /
                                    // in geval dat nieuw item type 0 is enkel verticaal spiegelen
                                    // in geval van type 1, horizontaal en verticaal spiegelen
                                    if (itemTypeNewItem == 0) {
                                        individualItems.get(i).flipVertically();
                                    } else {
                                        individualItems.get(i).flipHorizontally();
                                        individualItems.get(i).flipVertically();
                                    }
                                    pattern.addItem(individualItems.get(i));
                                    itemPlaced = true;
                                }
                            } else {
                                if (!isFlippedHorizontally) {
                                    // orientatie van projectie is /
                                    // type 0 moet enkel verticaal spiegelen, type 1 horizontaal en verticaal spiegelen
                                    if (itemTypeNewItem == 0) {
                                        individualItems.get(i).flipVertically();
                                    } else {
                                        individualItems.get(i).flipHorizontally();
                                        individualItems.get(i).flipVertically();
                                    }
                                    pattern.addItem(individualItems.get(i));
                                    itemPlaced = true;
                                } else {
                                    // nu is orientatie \
                                    // type 0 moet horizontaal en verticaal spiegelen, type 1 enkel verticaal spiegelen
                                    if (itemTypeNewItem == 0) {
                                        individualItems.get(i).flipHorizontally();
                                        individualItems.get(i).flipVertically();
                                    } else {
                                        individualItems.get(i).flipVertically();
                                    }
                                    pattern.addItem(individualItems.get(i));
                                    itemPlaced = true;
                                }
                            }
                            break;
                        }
                    }
                }
                if (!itemPlaced) {
                    Pattern newPattern = new Pattern(true);
                    newPattern.addItem(individualItems.get(i));
                    originalPatterns.add(newPattern);
                }
            }
        }

        // Merge identieke patronen

        List<Pattern> mergedPatterns = new ArrayList<>();
        int totalBins = 0;

        for (Pattern p : originalPatterns) {
            boolean foundDuplicate = false;
            for (Pattern m : mergedPatterns) {
                if (m.getItems().size() == p.getItems().size()) {
                    boolean identical = true;
                    // elk item in het patroon vergelijken met het item in het andere patroon
                    // als er een verschil is, dan zijn de patronen niet identiek
                    for (int idx = 0; idx < p.getItems().size(); idx++) {
                        Trapezoid a = p.getItems().get(idx);
                        Trapezoid b = m.getItems().get(idx);
                        if (a.getTotalLength() != b.getTotalLength() ||
                                a.getP1() != b.getP1() ||
                                a.getP2() != b.getP2() ||
                                a.getShapeIndicator() != b.getShapeIndicator() ||
                                a.isFlippedHorizontally() != b.isFlippedHorizontally() ||
                                a.isFlippedVertically() != b.isFlippedVertically()) {
                            identical = false;
                            break;
                        }
                    }
                    if (identical) {
                        // verhoog aantal geselcteerde patronen met het aantal van het huidige patroon
                        m.setCount(m.getCount() + p.getCount());
                        foundDuplicate = true;
                        break;
                    }
                }
            }
            if (!foundDuplicate) {
                // het patroon bestaat nog niet in de lijst van samengevoegde patronen, dus voeg het toe want het is uniek
                Pattern copy = new Pattern(true);
                for (Trapezoid t : p.getItems()) {
                    // om inconsistenties te vermijden, diepe kopie
                    Trapezoid clone = new Trapezoid(1, t.getTotalLength(), t.getP1(), t.getP2(),
                            t.getAngle1(), t.getAngle2(), t.getShapeIndicator());
                    if (t.isFlippedHorizontally()) clone.flipHorizontally();
                    if (t.isFlippedVertically()) clone.flipVertically();
                    copy.addItem(clone);
                }
                copy.setCount(p.getCount());
                mergedPatterns.add(copy);
            }
        }

        for (Pattern m : mergedPatterns) {
            totalBins += (int) m.getCount();
        }

        Solution solution = new Solution(problem.getFileName());
        for (Pattern m : mergedPatterns) {
            solution.addPattern(m);
        }

        long endTime = System.currentTimeMillis();
        solution.setExecutionTimeMs(endTime - startTime);
        solution.setLowerBound(totalBins);
        solution.setUpperBound(totalBins);

        return solution;
    }
}