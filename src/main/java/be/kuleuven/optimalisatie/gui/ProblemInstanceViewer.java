package be.kuleuven.optimalisatie.gui;

import be.kuleuven.optimalisatie.probleminstance.DataLoader;
import be.kuleuven.optimalisatie.probleminstance.TrussProblem;
import be.kuleuven.optimalisatie.probleminstance.Trapezoid;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ProblemInstanceViewer extends JFrame {
    private JList<TrussProblem> problemList;
    private DefaultListModel<TrussProblem> listModel;
    private JTable itemsTable;
    private DefaultTableModel itemsTableModel;
    private JTable binsTable;
    private DefaultTableModel binsTableModel;
    private JLabel lblMetadata;
    private JPanel visualizationPanel;

    public ProblemInstanceViewer() {
        setTitle("Problem Instance Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        initUI();
        loadData();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        listModel = new DefaultListModel<>();
        problemList = new JList<>(listModel);
        problemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        problemList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                displayProblemDetails(problemList.getSelectedValue());
            }
        });

        JScrollPane listScrollPane = new JScrollPane(problemList);
        listScrollPane.setPreferredSize(new Dimension(200, 0));
        listScrollPane.setBorder(BorderFactory.createTitledBorder("Datasets"));

        JPanel rightPanel = new JPanel(new BorderLayout());

        lblMetadata = new JLabel("Select a dataset.");
        lblMetadata.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        rightPanel.add(lblMetadata, BorderLayout.NORTH);

        JPanel dataTabPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;



        String[] binColumnNames = {"Length (mm)", "Height (H)"};
        binsTableModel = new DefaultTableModel(binColumnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        binsTable = new JTable(binsTableModel);
        JScrollPane binsScrollPane = new JScrollPane(binsTable);
        binsScrollPane.setBorder(BorderFactory.createTitledBorder("Bin"));
        dataTabPanel.add(binsScrollPane);

        String[] itemColumnNames = {"Quantity (Items)", "Total Length", "P1", "P2", "Angle 1", "Angle 2", "Shape Indicator"};
        itemsTableModel = new DefaultTableModel(itemColumnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        itemsTable = new JTable(itemsTableModel);
        JScrollPane itemsScrollPane = new JScrollPane(itemsTable);
        itemsScrollPane.setBorder(BorderFactory.createTitledBorder("Items / Trapezoids"));
        dataTabPanel.add(itemsScrollPane);


        visualizationPanel = new JPanel();
        visualizationPanel.setLayout(new BoxLayout(visualizationPanel, BoxLayout.Y_AXIS));
        JScrollPane visualizationScrollPane = new JScrollPane(visualizationPanel);
        visualizationScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        visualizationScrollPane.setBorder(BorderFactory.createEmptyBorder());

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Data", dataTabPanel);
        tabbedPane.addTab("Visualization", visualizationScrollPane);

        rightPanel.add(tabbedPane, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, rightPanel);
        splitPane.setDividerLocation(150);
        add(splitPane, BorderLayout.CENTER);

        gbc.weighty = 0.1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        dataTabPanel.add(binsScrollPane, gbc);

        gbc.weighty = 0.9;
        gbc.gridy = 1;
        dataTabPanel.add(itemsScrollPane, gbc);
    }

    private void loadData() {
        List<TrussProblem> problems = DataLoader.loadAllProblems();
        for (TrussProblem p : problems) {
            listModel.addElement(p);
        }
    }

    private void displayProblemDetails(TrussProblem problem) {
        if (problem == null) return;

        String metadataText = String.format("<html><b>File:</b> %s<br>" +
                        "<b>Total Items:</b> %d<br>" +
                        "<b>Trapezoid Types:</b> %d<br>" +
                        "<b>Orders:</b> %d<br>",
                problem.getFileName(), problem.getTotalItems(), problem.getNumberOfTrapezoidTypes(),
                problem.getNumberOfOrders());
        lblMetadata.setText(metadataText);

        binsTableModel.setRowCount(0);
        binsTableModel.addRow(new Object[]{"4200", "2"});

        itemsTableModel.setRowCount(0);
        visualizationPanel.removeAll();

        int maxLength = 1;
        for (Trapezoid t : problem.getTrapezoids()) {
            if (t.getTotalLength() > maxLength) {
                maxLength = t.getTotalLength();
            }
        }

        for (Trapezoid t : problem.getTrapezoids()) {
            Object[] row = {
                    t.getNumberOfItems(),
                    t.getTotalLength(),
                    t.getP1(),
                    t.getP2(),
                    t.getAngle1(),
                    t.getAngle2(),
                    t.getShapeIndicator()
            };
            itemsTableModel.addRow(row);

            visualizationPanel.add(new TrapezoidDrawingPanel(t, maxLength));
        }

        visualizationPanel.revalidate();
        visualizationPanel.repaint();
    }

    private static class TrapezoidDrawingPanel extends JPanel {
        private final Trapezoid trapezoid;
        private final int maxDatasetLength;

        public TrapezoidDrawingPanel(Trapezoid trapezoid, int maxDatasetLength) {
            this.trapezoid = trapezoid;
            this.maxDatasetLength = maxDatasetLength;
            setPreferredSize(new Dimension(800, 80));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int panelWidth = getWidth();
            int marginX = 20;
            int marginY = 25;
            int shapeHeight = 40;

            int drawableWidth = panelWidth - (marginX * 2);

            double scale = (double) drawableWidth / Math.max(maxDatasetLength, 1);

            int scaledLength = (int) (trapezoid.getTotalLength() * scale);
            int scaledP1 = (int) (trapezoid.getP1() * scale);
            int scaledP2 = (int) (trapezoid.getP2() * scale);

            int[] xPoints;
            int[] yPoints = { marginY + shapeHeight, marginY + shapeHeight, marginY, marginY };

            if (trapezoid.getShapeIndicator() == 0) {
                xPoints = new int[]{
                        marginX,
                        marginX + scaledLength,
                        marginX + scaledLength - scaledP2,
                        marginX + scaledP1
                };
            } else {
                xPoints = new int[]{
                        marginX,
                        marginX + scaledLength - scaledP2,
                        marginX + scaledLength,
                        marginX + scaledP1
                };
            }

            g2d.setColor(new Color(135, 206, 250)); // Light Sky Blue
            g2d.fillPolygon(xPoints, yPoints, 4);

            g2d.setColor(Color.DARK_GRAY);
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawPolygon(xPoints, yPoints, 4);

            g2d.setColor(Color.BLACK);
            String infoText = String.format("Qty: %d  |  Length: %d  |  P1: %d  |  P2: %d  |  Shape: %d",
                    trapezoid.getNumberOfItems(), trapezoid.getTotalLength(),
                    trapezoid.getP1(), trapezoid.getP2(), trapezoid.getShapeIndicator());
            g2d.drawString(infoText, marginX, marginY - 5);
        }
    }
}