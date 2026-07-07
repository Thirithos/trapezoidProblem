package be.kuleuven.optimalisatie.probleminstance;

import java.util.ArrayList;
import java.util.List;

public class TrussProblem {
    private final String fileName;
    private final int totalItems;
    private final int numberOfTrapezoidTypes;
    private final int numberOfOrders;
    //private final int seedIndex;
    private final List<Trapezoid> trapezoids;


    // bevat wat extra informatie over de probleem instantie zoals, totaal aantal items, en orders.
    // de seedindex is niet relevant is voor het generen van de probleem instantie
    public TrussProblem(String fileName, int totalItems, int numberOfTrapezoidTypes, int numberOfOrders, int seedIndex) {
        this.fileName = fileName;
        this.totalItems = totalItems;
        this.numberOfTrapezoidTypes = numberOfTrapezoidTypes;
        this.numberOfOrders = numberOfOrders;
        //this.seedIndex = seedIndex;
        this.trapezoids = new ArrayList<>();
    }

    public void addTrapezoid(Trapezoid trapezoid) {
        this.trapezoids.add(trapezoid);
    }

    public String getFileName() {
        return fileName;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getNumberOfTrapezoidTypes() {
        return numberOfTrapezoidTypes;
    }

    public int getNumberOfOrders() {
        return numberOfOrders;
    }

    /*
    public int getSeedIndex() {
        return seedIndex;
    }
    */

    public List<Trapezoid> getTrapezoids() {
        return trapezoids;
    }

    @Override
    public String toString() {
        return fileName;
    }
}
