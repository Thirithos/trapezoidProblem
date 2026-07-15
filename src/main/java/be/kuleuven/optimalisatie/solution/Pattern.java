package be.kuleuven.optimalisatie.solution;

import be.kuleuven.optimalisatie.probleminstance.Trapezoid;

import java.util.ArrayList;
import java.util.List;

public class Pattern {
    private int usedLength;
    private boolean lengthCalculated = false;
    private final ArrayList<Trapezoid> items;
    private boolean isUsed;
    private double count;


    public Pattern(List<Trapezoid> items, boolean isUsed) {
        this.items = new ArrayList<>(items);
        this.isUsed = isUsed;
        this.count = 1;
    }

    public Pattern(boolean isUsed) {
        this.items = new ArrayList<>();
        this.isUsed = isUsed;
        this.count = 1;
    }

    public Pattern() {
        this.items = new ArrayList<>();
        this.isUsed = false;
        this.count = 1;
    }

    public void addItem(Trapezoid item) {
        items.add(item);
        lengthCalculated = false;
    }

    public List<Trapezoid> getItems() {
        return items;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean used) {
        this.isUsed = used;
    }

    public double getCount() {
        return count;
    }

    public void setCount(double count) {
        this.count = count;
    }

    public int getUsedLength() {
        if(!lengthCalculated) {

            if (items.isEmpty()) {
                usedLength = 0;
                return usedLength;
            }

            lengthCalculated = true;
            Trapezoid itemFirst = items.getFirst();
            int length = itemFirst.getTotalLength();
            for (int i = 0; i < items.size() - 1; i++) {
                itemFirst = items.get(i);
                Trapezoid itemSecond = items.get(i+1);

                // algoritme zal het al hebben genest, dus logica is hier eenvoudiger.
                //  totale lengte van de twee l1 + l2 - min(projectieItem1, projectieItem2)
                int l2 = itemSecond.getTotalLength();
                int lengthProjectionFirstItem = itemFirst.getRightProjectionLength();
                int lengthProjectionSecondItem = itemSecond.getLeftProjectionLength();
                length += l2 - Math.min(lengthProjectionFirstItem, lengthProjectionSecondItem);
            }
            usedLength = length;
        }
        return usedLength;
    }

    public int countOccurrencesInPattern(Trapezoid type) {
        int count = 0;
        for (Trapezoid itemInPattern : items) {
            // We vergelijken de eigenschappen om te zien of het om exact hetzelfde type gaat
            if (itemInPattern.getTotalLength() == type.getTotalLength() &&
                    itemInPattern.getP1() == type.getP1() &&
                    itemInPattern.getP2() == type.getP2() &&
                    itemInPattern.getShapeIndicator() == type.getShapeIndicator()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Pattern{");
        sb.append("usedLength=").append(getUsedLength());
        sb.append(", isUsed=").append(isUsed);
        sb.append(", count=").append(count);
        sb.append(", items=[");
        for (Trapezoid item : items) {
            sb.append(item.toString()).append(", ");
        }
        if (!items.isEmpty()) {
            sb.setLength(sb.length() - 2);
        }
        sb.append("]}");
        return sb.toString();
    }
}