package be.kuleuven.optimalisatie.gui;

import be.kuleuven.optimalisatie.probleminstance.DataLoader;
import be.kuleuven.optimalisatie.probleminstance.Trapezoid;
import be.kuleuven.optimalisatie.probleminstance.TrussProblem;
import be.kuleuven.optimalisatie.solution.IterationData;
import be.kuleuven.optimalisatie.solution.Pattern;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SolutionViewer extends JFrame {

    private DefaultListModel<IterationData> iterationListModel;
    private JList<IterationData> iterationList;
    private JLabel lblMetadata;
    private JPanel dualsVisualPanel;
    private JPanel visualizationPanel;
    private List<IterationData> iterations;
    private String problemName;
    private List<TrussProblem> allProblems;
    private TrussProblem currentTrussProblem;

    public SolutionViewer() {
        setTitle("Optimalisatie Solution Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 800);
        setLocationRelativeTo(null);

        // Laad eenmalig alle orginele problemen in om items visueel te koppelen aan duale waarden
        allProblems = DataLoader.loadAllProblems();
        iterations = new ArrayList<>();
        initUI();
        loadSolutionFile();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Bestand");
        JMenuItem openItem = new JMenuItem("Open Oplossing (.txt)");
        openItem.addActionListener(e -> loadSolutionFile());
        fileMenu.add(openItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        iterationListModel = new DefaultListModel<>();
        iterationList = new JList<>(iterationListModel);
        iterationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        iterationList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                displayIteration(iterationList.getSelectedValue());
            }
        });

        JScrollPane listScrollPane = new JScrollPane(iterationList);
        listScrollPane.setPreferredSize(new Dimension(250, 0));
        listScrollPane.setBorder(BorderFactory.createTitledBorder("Oplossingsstappen"));

        JPanel rightPanel = new JPanel(new BorderLayout());

        lblMetadata = new JLabel("Laad een oplossing via Bestand > Open Oplossing.");
        lblMetadata.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        rightPanel.add(lblMetadata, BorderLayout.NORTH);

        // TAB 1: RMP Info (Visualisatie van individuele items met hun duale waarde)
        dualsVisualPanel = new JPanel();
        dualsVisualPanel.setLayout(new BoxLayout(dualsVisualPanel, BoxLayout.Y_AXIS));
        JScrollPane dualsScrollPane = new JScrollPane(dualsVisualPanel);
        dualsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        dualsScrollPane.setBorder(BorderFactory.createEmptyBorder());

        // TAB 2: Patroon Visualisatie (Count + patroon)
        visualizationPanel = new JPanel();
        visualizationPanel.setLayout(new BoxLayout(visualizationPanel, BoxLayout.Y_AXIS));
        JScrollPane visualizationScrollPane = new JScrollPane(visualizationPanel);
        visualizationScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        visualizationScrollPane.setBorder(BorderFactory.createEmptyBorder());

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("RMP Duale Waarden", dualsScrollPane);
        tabbedPane.addTab("Patroon Visualisatie", visualizationScrollPane);

        rightPanel.add(tabbedPane, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, rightPanel);
        splitPane.setDividerLocation(250);
        add(splitPane, BorderLayout.CENTER);
    }

    private void loadSolutionFile() {
        JFileChooser fileChooser = new JFileChooser("src/main/resources/solutions");
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            parseSolutionFile(selectedFile);
        }
    }

    private void parseSolutionFile(File file) {
        iterations.clear();
        iterationListModel.clear();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            IterationData currentIteration = null;
            Pattern currentPattern = null;
            boolean readingDuals = false;
            boolean readingPatterns = false;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("PROBLEM=")) {
                    problemName = line.substring(8);
                    // Match the correct TrussProblem to be able to draw individual items for duals
                    currentTrussProblem = allProblems.stream()
                            .filter(p -> p.getFileName().equals(problemName))
                            .findFirst()
                            .orElse(null);

                } else if (line.startsWith("ITERATION=")) {
                    int iterNum = Integer.parseInt(line.substring(10));
                    currentIteration = new IterationData(iterNum);
                    iterations.add(currentIteration);
                } else if (line.startsWith("LB=") && currentIteration != null) {
                    currentIteration.setLowerBound(Double.parseDouble(line.substring(3)));
                } else if (line.startsWith("UB=") && currentIteration != null) {
                    currentIteration.setUpperBound(Double.parseDouble(line.substring(3)));
                } else if (line.equals("DUALS_START")) {
                    readingDuals = true;
                } else if (line.equals("DUALS_END")) {
                    readingDuals = false;
                } else if (readingDuals && currentIteration != null) {
                    currentIteration.addDualValue(Double.parseDouble(line));
                } else if (line.equals("PATTERNS_START")) {
                    readingPatterns = true;
                } else if (line.equals("PATTERNS_END")) {
                    readingPatterns = false;
                    if (currentPattern != null && currentIteration != null) {
                        currentIteration.addPattern(currentPattern);
                        currentPattern = null;
                    }
                } else if (readingPatterns && line.startsWith("PATTERN=")) {
                    if (currentPattern != null && currentIteration != null) {
                        currentIteration.addPattern(currentPattern);
                    }
                    currentPattern = new Pattern();

                    String details = line.substring(8);
                    String[] parts = details.split(",");
                    currentPattern.setUsed(parts[0].equals("USED"));
                    if(parts.length > 1 && parts[1].startsWith("COUNT=")) {
                        currentPattern.setCount(Double.parseDouble(parts[1].substring(6)));
                    }
                } else if (readingPatterns && line.startsWith("ITEM=") && currentPattern != null) {
                    String[] parts = line.substring(5).split(",");
                    Trapezoid t = new Trapezoid(
                            Integer.parseInt(parts[0]),
                            Integer.parseInt(parts[1]),
                            Integer.parseInt(parts[2]),
                            Integer.parseInt(parts[3]),
                            Double.parseDouble(parts[4]),
                            Double.parseDouble(parts[5]),
                            Integer.parseInt(parts[6])
                    );

                    if (parts.length > 7 && Boolean.parseBoolean(parts[7])) t.flipHorizontally();
                    if (parts.length > 8 && Boolean.parseBoolean(parts[8])) t.flipVertically();

                    currentPattern.addItem(t);
                }
            }

            for (IterationData it : iterations) {
                iterationListModel.addElement(it);
            }
            if (!iterationListModel.isEmpty()) {
                iterationList.setSelectedIndex(0);
            }

        } catch (IOException | NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Fout bij inlezen bestand: " + e.getMessage(), "Fout", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void displayIteration(IterationData iteration) {
        if (iteration == null) return;

        lblMetadata.setText(String.format("<html><b>Probleem:</b> %s<br><b>Iteratie:</b> %d<br><b>Lower Bound:</b> %.2f<br><b>Upper Bound:</b> %.2f</html>",
                problemName, iteration.getIterationNumber(), iteration.getLowerBound(), iteration.getUpperBound()));

        // --- DUAL VALUES VISUALISATIE ---
        dualsVisualPanel.removeAll();
        List<Double> dualValues = iteration.getDualValues();

        if (currentTrussProblem != null && !dualValues.isEmpty()) {
            for (int i = 0; i < dualValues.size(); i++) {
                // Voorkom fouten als the list met trapezoids groter of kleiner is dan de duals lijst
                if (i < currentTrussProblem.getTrapezoids().size()) {
                    Trapezoid t = currentTrussProblem.getTrapezoids().get(i);
                    double piValue = dualValues.get(i);
                    dualsVisualPanel.add(new DualValueRowPanel(t, piValue));
                }
            }
        } else if (dualValues.isEmpty()) {
            JLabel emptyLabel = new JLabel("Geen duale waarden gevonden (bijvoorbeeld in FFD iteratie 0).");
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            dualsVisualPanel.add(emptyLabel);
        }

        dualsVisualPanel.revalidate();
        dualsVisualPanel.repaint();

        // --- PATRONEN VISUALISATIE ---
        visualizationPanel.removeAll();
        for (Pattern p : iteration.getPatterns()) {
            visualizationPanel.add(new PatternDrawingPanel(p));
        }

        visualizationPanel.revalidate();
        visualizationPanel.repaint();
    }
    
    private static class DualValueRowPanel extends JPanel {
        private final Trapezoid trapezoid;
        private final double piValue;

        public DualValueRowPanel(Trapezoid trapezoid, double piValue) {
            this.trapezoid = trapezoid;
            this.piValue = piValue;

            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(1000, 70));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

            JPanel leftDrawingPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int marginX = 20;
                    int marginY = 15;
                    int shapeHeight = 40;

                    // Vaste schaal voor de items links, we gebruiken 1200 als referentiebreedte op scherm
                    double scale = 500.0 / 4200.0;

                    int L = trapezoid.getTotalLength();
                    int p1 = trapezoid.getP1();
                    int p2 = trapezoid.getP2();

                    int[] xPts = new int[4];
                    int[] yPts = {0, 0, shapeHeight, shapeHeight};

                    // Basisweergave voor duals is origineel (ongeflitpt)
                    if (trapezoid.getShapeIndicator() == 0) {
                        xPts[0] = 0;           xPts[1] = L - p2;
                        xPts[2] = L;           xPts[3] = p1;
                    } else {
                        xPts[0] = 0;           xPts[1] = L;
                        xPts[2] = L - p2;      xPts[3] = p1;
                    }

                    int[] drawXPts = new int[4];
                    int[] drawYPts = new int[4];
                    for (int pt = 0; pt < 4; pt++) {
                        drawXPts[pt] = marginX + (int)(xPts[pt] * scale);
                        drawYPts[pt] = marginY + yPts[pt];
                    }

                    g2d.setColor(new Color(135, 206, 250));
                    g2d.fillPolygon(drawXPts, drawYPts, 4);

                    g2d.setColor(Color.DARK_GRAY);
                    g2d.setStroke(new BasicStroke(1.2f));
                    g2d.drawPolygon(drawXPts, drawYPts, 4);

                    g2d.setColor(Color.BLACK);
                    g2d.drawString(String.format("L: %d | p1: %d | p2: %d | Vorm: %d", L, p1, p2, trapezoid.getShapeIndicator()), marginX, marginY - 5);
                }
            };

            leftDrawingPanel.setPreferredSize(new Dimension(600, 70));

            JLabel rightLabel = new JLabel(String.format("Duale Waarde (\u03C0): %.4f", piValue));
            rightLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            rightLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            rightLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 40));

            add(leftDrawingPanel, BorderLayout.CENTER);
            add(rightLabel, BorderLayout.EAST);
        }
    }

    private static class PatternDrawingPanel extends JPanel {
        private final Pattern pattern;

        public PatternDrawingPanel(Pattern pattern) {
            this.pattern = pattern;
            setPreferredSize(new Dimension(1000, 110));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));

            if (!pattern.isUsed()) {
                setBackground(new Color(245, 245, 245));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int panelWidth = getWidth();
            int marginX = 20;
            int marginY = 35;
            int shapeHeight = 40;
            int binLength = 4200;

            int drawableWidth = panelWidth - (marginX * 2);
            double scale = (double) drawableWidth / binLength;

            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
            String status = pattern.isUsed() ? "[GEBRUIKT IN RMP]" : "[ONGEBRUIKT]";
            g2d.drawString(String.format("%s | Count (Xj): %.4f | Totale lengte: %d / %d",
                    status, pattern.getCount(), pattern.getUsedLength(), binLength), marginX, marginY - 15);

            g2d.setColor(Color.LIGHT_GRAY);
            g2d.drawRect(marginX, marginY, drawableWidth, shapeHeight);

            int currentXOffset = 0;

            for (int i = 0; i < pattern.getItems().size(); i++) {
                Trapezoid t = pattern.getItems().get(i);

                int L = t.getTotalLength();
                int p1 = t.getP1();
                int p2 = t.getP2();

                int[] xPts = new int[4];
                int[] yPts = {0, 0, shapeHeight, shapeHeight};

                boolean flipH = t.isFlippedHorizontally();
                boolean flipV = t.isFlippedVertically();

                if (t.getShapeIndicator() == 0) {
                    if (!flipH && !flipV) { // \ \
                        xPts[0] = 0;           xPts[1] = L - p2;
                        xPts[2] = L;           xPts[3] = p1;
                    } else if (!flipH && flipV) { // //
                        xPts[0] = p1;          xPts[1] = L;
                        xPts[2] = L - p2;      xPts[3] = 0;
                    } else if (flipH && !flipV) { // //
                        xPts[0] = p2;          xPts[1] = L;
                        xPts[2] = L - p1;      xPts[3] = 0;
                    } else { // flipH && flipV -> \ \
                        xPts[0] = 0;           xPts[1] = L - p1;
                        xPts[2] = L;           xPts[3] = p2;
                    }
                } else { // Vorm 1
                    if (!flipH && !flipV) { // \ /
                        xPts[0] = 0;           xPts[1] = L;
                        xPts[2] = L - p2;      xPts[3] = p1;
                    } else if (!flipH && flipV) { // \ /
                        xPts[0] = 0;           xPts[1] = L;
                        xPts[2] = L - p2;      xPts[3] = p1;
                    } else if (flipH && !flipV) { // / \
                        xPts[0] = p2;          xPts[1] = L - p1;
                        xPts[2] = L;           xPts[3] = 0;
                    } else { // flipH && flipV -> / \
                        xPts[0] = p2;          xPts[1] = L - p1;
                        xPts[2] = L;           xPts[3] = 0;
                    }
                }

                int[] drawXPts = new int[4];
                int[] drawYPts = new int[4];
                for (int pt = 0; pt < 4; pt++) {
                    drawXPts[pt] = marginX + (int)((currentXOffset + xPts[pt]) * scale);
                    drawYPts[pt] = marginY + yPts[pt];
                }

                g2d.setColor(new Color(135, 206, 250, 200));
                g2d.fillPolygon(drawXPts, drawYPts, 4);

                g2d.setColor(Color.DARK_GRAY);
                g2d.setStroke(new BasicStroke(1.2f));
                g2d.drawPolygon(drawXPts, drawYPts, 4);

                if (i < pattern.getItems().size() - 1) {
                    Trapezoid nextT = pattern.getItems().get(i + 1);

                    int rightProj = t.isFlippedHorizontally() ? t.getP1() : t.getP2();
                    int leftProjNext = nextT.isFlippedHorizontally() ? nextT.getP2() : nextT.getP1();

                    int overlap = Math.min(rightProj, leftProjNext);
                    currentXOffset += (L - overlap);
                }
            }
        }
    }
}