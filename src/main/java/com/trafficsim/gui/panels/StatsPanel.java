package com.trafficsim.gui.panels;

import com.trafficsim.gui.util.GuiConstants;
import com.trafficsim.gui.util.SimulationAdapter;
import com.trafficsim.threads.VehicleThread;
import com.trafficsim.threads.VehicleThread.VehicleState;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;

/**
 * Panel de estadísticas actualizado en tiempo real (~60 fps).
 */
public class StatsPanel extends JPanel {

    private final SimulationAdapter adapter;

    private JLabel lblMoving;
    private JLabel lblArrived;
    private JLabel lblWaiting;
    private JLabel lblAvgTime;
    private JLabel lblSpeedup;

    public StatsPanel(SimulationAdapter adapter) {
        this.adapter = adapter;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, GuiConstants.INTER_BORDER),
            new EmptyBorder(18, 20, 18, 20)
        ));
        buildUI();
    }

    private void buildUI() {
        JLabel title = new JLabel("ESTADÍSTICAS");
        title.setFont(GuiConstants.FONT_MONO_BOLD);
        title.setForeground(GuiConstants.ACCENT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(title);
        add(Box.createRigidArea(new Dimension(0, 14)));

        JPanel grid = new JPanel(new GridLayout(3, 2, 8, 8));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(220, 180));

        lblMoving  = new JLabel("0");
        lblArrived = new JLabel("0");
        lblWaiting = new JLabel("0");
        lblAvgTime = new JLabel("—");
        lblSpeedup = new JLabel("—");

        grid.add(makeStatBox("En ruta",    lblMoving,  GuiConstants.ACCENT));
        grid.add(makeStatBox("Llegaron",   lblArrived, GuiConstants.SEM_GREEN));
        grid.add(makeStatBox("Esperando",  lblWaiting, GuiConstants.VEH_WAIT));
        grid.add(makeStatBox("Prom. (ms)", lblAvgTime, GuiConstants.TEXT_BRIGHT));
        grid.add(makeStatBox("Speedup A*", lblSpeedup, new Color(167, 139, 250)));

        add(grid);
    }

    private JPanel makeStatBox(String label, JLabel valueLabel, Color color) {
        JPanel box = new JPanel(new BorderLayout(0, 4));
        box.setBackground(new Color(8, 11, 16));
        box.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GuiConstants.INTER_BORDER),
            new EmptyBorder(8, 10, 8, 10)
        ));

        JLabel lbl = new JLabel(label);
        lbl.setFont(GuiConstants.FONT_MONO_SM);
        lbl.setForeground(GuiConstants.TEXT);

        valueLabel.setFont(GuiConstants.FONT_STAT_VAL);
        valueLabel.setForeground(color);

        box.add(lbl,        BorderLayout.NORTH);
        box.add(valueLabel, BorderLayout.CENTER);

        return box;
    }

    /** Llamado cada ~16 ms desde el timer de MainWindow. */
    public void refresh() {
        List<VehicleThread> vehicles = adapter.getVehicles();

        long moving  = vehicles.stream()
            .filter(v -> v.getVeichleState() == VehicleState.MOVING).count();
        long waiting = vehicles.stream()
            .filter(v -> v.getVeichleState() == VehicleState.WAITING_LIGHT
                      || v.getVeichleState() == VehicleState.WAITING_LOCK).count();

        int    arrived = adapter.getArrivedCount();
        double avgTime = adapter.getAverageTravelTime();
        double speedup = adapter.getSpeedup();

        lblMoving.setText(String.valueOf(moving));
        lblArrived.setText(String.valueOf(arrived));
        lblWaiting.setText(String.valueOf(waiting));
        lblAvgTime.setText(avgTime > 0 ? String.format("%.0f", avgTime) : "—");
        lblSpeedup.setText(speedup > 0 ? String.format("%.2fx", speedup) : "—");
    }

    public void reset() {
        lblMoving.setText("0");
        lblArrived.setText("0");
        lblWaiting.setText("0");
        lblAvgTime.setText("—");
        lblSpeedup.setText("—");
    }
}