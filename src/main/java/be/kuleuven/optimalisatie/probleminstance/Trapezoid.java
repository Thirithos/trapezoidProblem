package be.kuleuven.optimalisatie.probleminstance;

public class Trapezoid {
    private final int numberOfItems;
    private final int totalLength;
    private final int p1;
    private final int p2;
    private final double angle1;
    private final double angle2;
    private final int shapeIndicator;

    // in datafile formaat is altijd:
    // 1 2463    9    3 85.0 88.0 1
    // aantal items, totale lengte (met projecties), eerste projectie lengte, tweede projectie lengte, eerste hoek, tweede hoek, shape indicatie
    // dus shape indicatie geeft 1 als de "bases" van de projecties aan dezelfde kant liggen. _|_|_   ,  p1 | totale lengte - p1 - p2 | p2
    // als shape indicatie 0 geeft, liggen de bases aan tegenovergestelde kanten. ‾|_|_

    public Trapezoid(int numberOfItems, int totalLength, int p1, int p2, double angle1, double angle2, int shapeIndicator) {
        this.numberOfItems = numberOfItems;
        this.totalLength = totalLength;
        this.p1 = p1;
        this.p2 = p2;
        this.angle1 = angle1;
        this.angle2 = angle2;
        this.shapeIndicator = shapeIndicator;
    }

    public int getNumberOfItems() {
        return numberOfItems;
    }

    public int getTotalLength() {
        return totalLength;
    }

    public int getP1() {
        return p1;
    }

    public int getP2() {
        return p2;
    }

    public double getAngle1() {
        return angle1;
    }

    public double getAngle2() {
        return angle2;
    }

    public int getShapeIndicator() {
        return shapeIndicator;
    }
}