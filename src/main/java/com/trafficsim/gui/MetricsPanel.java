package com.trafficsim.gui;

import com.trafficsim.controller.SimulationController;
import com.trafficsim.metrics.MetricsCollector;
import com.trafficsim.threads.VehicleThread;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class MetricsPanel extends JPanel {

    private static final Color BG         = new Color(12, 15, 24);
    private static final Color BG_CARD    = new Color(18, 23, 38);
    private static final Color ACCENT     = new Color(0, 200, 150);
    private static final Color TEXT_MAIN  = new Color(210, 225, 245);
    private static final Color TEXT_DIM   = new Color(80, 100, 130);
    private static final Color BORDER_COL = new Color(30, 40, 65);
    private static final Color C_GREEN    = new Color(0, 210, 120);
    private static final Color C_YELLOW   = new Color(255, 195, 0);
    private static final Color C_RED      = new Color(240, 80, 80);

    private final SimulationController controller;

    // ---- KPI cards ----
    private JLabel valState;
    private JLabel valArrived;
    private JLabel valFirst;
    private JLabel valAvg;
    private JLabel valWait;

    // ---- Comparación A* ----
    private JLabel valSeq;
    private JLabel valPar;
    private JLabel valSpeedup;

    // ---- Tabla de vehículos ----
    private DefaultTableModel tableModel;
    private JTable table;

    // Timer de actualización de métricas (independiente del repaint de la grid)
    private final Timer updateTimer;

    public MetricsPanel(SimulationController controller) {
        this.controller = controller;
        setBackground(BG);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(16, 12, 16, 12));
        setPreferredSize(new Dimension(260, 0));

        buildKpiSection();
        add(Box.createVerticalStrut(14));
        buildSeparator();
        add(Box.createVerticalStrut(14));
        buildAStarSection();
        add(Box.createVerticalStrut(14));
        buildSeparator();
        add(Box.createVerticalStrut(14));
        buildTableSection();

        // Actualiza métricas cada 300ms
        updateTimer = new Timer(300, e -> refresh());
        updateTimer.start();
    }

    // -----------------------------------------------------------------------
    // KPI Cards — Estado, Llegados, Primero, Promedio
    // -----------------------------------------------------------------------
    private void buildKpiSection() {
        add(buildSectionTitle("ESTADO EN TIEMPO REAL"));
        add(Box.createVerticalStrut(8));

        valState   = buildKpiCard("Estado",         "IDLE",  TEXT_DIM);
        valArrived = buildKpiCard("Llegaron",        "0 / 0", TEXT_MAIN);
        valFirst   = buildKpiCard("1° en llegar",   "—",     C_GREEN);
        valAvg     = buildKpiCard("Tiempo promedio","—",     TEXT_MAIN);
        valWait    = buildKpiCard("Espera promedio", "—",     C_YELLOW);
    }

    // -----------------------------------------------------------------------
    // Sección comparación A*
    // -----------------------------------------------------------------------
    private void buildAStarSection() {
        add(buildSectionTitle("COMPARACIÓN A*"));
        add(Box.createVerticalStrut(8));

        valSeq     = buildKpiCard("Secuencial", "—", TEXT_MAIN);
        valPar     = buildKpiCard("Paralelo",   "—", C_GREEN);
        valSpeedup = buildKpiCard("Speedup",    "—", ACCENT);
    }

    // -----------------------------------------------------------------------
    // Tabla de vehículos
    // -----------------------------------------------------------------------
    private void buildTableSection() {
        add(buildSectionTitle("VEHÍCULOS"));
        add(Box.createVerticalStrut(8));

        String[] cols = {"#", "Pasos", "Tiempo", "Estado"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_MAIN);
        table.setFont(new Font("Monospaced", Font.PLAIN, 11));
        table.setRowHeight(22);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(new Color(30, 40, 70));
        table.setSelectionForeground(TEXT_MAIN);

        // Columnas con ancho fijo
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(1).setPreferredWidth(45);
        table.getColumnModel().getColumn(2).setPreferredWidth(75);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);

        // Renderer con colores por estado
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                                                           boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setBackground(row % 2 == 0 ? BG_CARD : new Color(22, 28, 48));
                setForeground(TEXT_MAIN);
                setBorder(new EmptyBorder(0, 6, 0, 6));
                setFont(new Font("Monospaced", Font.PLAIN, 11));

                // Colorear columna Estado
                if (col == 3 && val != null) {
                    String s = val.toString();
                    if (s.equals("LLEGÓ"))    setForeground(C_GREEN);
                    else if (s.equals("ESPERA")) setForeground(C_YELLOW);
                    else if (s.equals("MOVIENDO")) setForeground(C_GREEN.brighter());
                }
                return this;
            }
        });

        // Header de la tabla
        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(20, 26, 45));
        header.setForeground(TEXT_DIM);
        header.setFont(new Font("Monospaced", Font.BOLD, 10));
        header.setBorder(new LineBorder(BORDER_COL, 1));
        header.setReorderingAllowed(false);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(BG_CARD);
        scroll.setBorder(new LineBorder(BORDER_COL, 1, true));
        scroll.getViewport().setBackground(BG_CARD);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        add(scroll);
    }

    // -----------------------------------------------------------------------
    // Helpers de construcción
    // -----------------------------------------------------------------------
    private JLabel buildSectionTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Monospaced", Font.BOLD, 10));
        lbl.setForeground(TEXT_DIM);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    // Crea una tarjeta KPI y la agrega al panel, devuelve el JLabel del valor
    private JLabel buildKpiCard(String label, String initial, Color valueColor) {
        JPanel card = new JPanel(new BorderLayout(8, 0));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COL, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblKey = new JLabel(label);
        lblKey.setFont(new Font("Monospaced", Font.PLAIN, 11));
        lblKey.setForeground(TEXT_DIM);

        JLabel lblVal = new JLabel(initial);
        lblVal.setFont(new Font("Monospaced", Font.BOLD, 12));
        lblVal.setForeground(valueColor);
        lblVal.setHorizontalAlignment(SwingConstants.RIGHT);

        card.add(lblKey, BorderLayout.WEST);
        card.add(lblVal, BorderLayout.EAST);

        add(card);
        add(Box.createVerticalStrut(4));

        return lblVal;
    }

    private void buildSeparator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_COL);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(sep);
    }

    // -----------------------------------------------------------------------
    // Actualización de datos — llamada por el Timer cada 300ms
    // -----------------------------------------------------------------------
    public void refresh() {
        MetricsCollector m  = controller.getMetrics();
        List<VehicleThread> vehicles = controller.getVehicles();
        int total   = vehicles.size();
        int arrived = controller.getArrivedCount();

        // KPIs
        valState.setText(controller.getState().name());
        valArrived.setText(arrived + " / " + total);

        int firstId = m.getFirstArrival();
        valFirst.setText(firstId >= 0 ? "Vehículo #" + firstId : "—");

        double avg = m.getAverageTravelTime();
        valAvg.setText(avg > 0 ? String.format("%.0f ms", avg) : "—");

        // Espera promedio de todos los vehículos
        double avgWait = vehicles.stream()
                .mapToLong(v -> m.getWaitTime(v.getVehicleId()))
                .average().orElse(0);
        valWait.setText(avgWait > 0 ? String.format("%.0f ms", avgWait) : "—");

        // A*
        long seqMs  = m.getSequentialRouteTime();
        long parMs  = m.getParallelRouteTime();
        double spd  = m.getSpeedup();
        valSeq.setText(seqMs > 0 ? seqMs + " ms" : "—");
        valPar.setText(parMs > 0 ? parMs + " ms" : "—");
        valSpeedup.setText(spd > 0 ? String.format("%.2fx", spd) : "—");
        // Color del speedup según qué tan bueno es
        if      (spd >= 3.0) valSpeedup.setForeground(C_GREEN);
        else if (spd >= 1.5) valSpeedup.setForeground(C_YELLOW);
        else                 valSpeedup.setForeground(C_RED);

        // Tabla de vehículos
        refreshTable(vehicles, m);
    }

    private void refreshTable(List<VehicleThread> vehicles, MetricsCollector m) {
        tableModel.setRowCount(0); // limpia y reconstruye

        for (VehicleThread v : vehicles) {
            int id = v.getVehicleId();
            int steps = v.getRoute().size() - 1;

            long travelMs = m.getTravelTime(id);
            String timeStr = travelMs > 0
                    ? travelMs + " ms" : "—";

            String stateStr = switch (v.getVehicleState()) {
                case ARRIVED                     -> "LLEGÓ";
                case WAITING_LIGHT, WAITING_LOCK -> "ESPERA";
                case MOVING                      -> "MOVIENDO";
            };

            tableModel.addRow(new Object[]{"#" + id, steps, timeStr, stateStr});
        }
    }

    public void stopTimer() { updateTimer.stop(); }
}