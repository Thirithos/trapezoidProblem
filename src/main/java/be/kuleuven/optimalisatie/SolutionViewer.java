package be.kuleuven.optimalisatie;

import javax.swing.SwingUtilities;

public class SolutionViewer {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            be.kuleuven.optimalisatie.gui.SolutionViewer viewer = new be.kuleuven.optimalisatie.gui.SolutionViewer();
            viewer.setVisible(true);
        });
    }
}
