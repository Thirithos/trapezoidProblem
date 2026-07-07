package be.kuleuven.optimalisatie;

import javax.swing.SwingUtilities;

public class ProblemInstanceViewer {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            be.kuleuven.optimalisatie.gui.ProblemInstanceViewer gui = new be.kuleuven.optimalisatie.gui.ProblemInstanceViewer();
            gui.setVisible(true);
        });
    }
}