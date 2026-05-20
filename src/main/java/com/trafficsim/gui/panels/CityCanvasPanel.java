package com.trafficsim.gui.panels;

import com.trafficsim.gui.renderer.CityRenderer;
import com.trafficsim.gui.renderer.SemaphoreRenderer;
import com.trafficsim.gui.renderer.VehicleRenderer;
import com.trafficsim.gui.util.GuiConstants;
import com.trafficsim.gui.util.SimulationAdapter;
import com.trafficsim.threads.TrafficLightThread;
import com.trafficsim.threads.VehicleThread;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

/**
 * Panel canvas de la ciudad.
 * Orquesta los tres renderers (ciudad → semáforos → vehículos).
 */
public class CityCanvasPanel extends JPanel {

    private final SimulationAdapter adapter;

    private final CityRenderer      cityRenderer      = new CityRenderer();
    private final SemaphoreRenderer semaphoreRenderer = new SemaphoreRenderer();
    private final VehicleRenderer   vehicleRenderer   = new VehicleRenderer();

    public CityCanvasPanel(SimulationAdapter adapter) {
        this.adapter = adapter;
        setPreferredSize(new Dimension(GuiConstants.CANVAS_SIZE, GuiConstants.CANVAS_SIZE));
        setBackground(new Color(8, 11, 16));
        setBorder(BorderFactory.createLineBorder(GuiConstants.INTER_BORDER));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Ciudad (capa base)
        cityRenderer.draw(g2);

        // 2. Semáforos
        List<TrafficLightThread> lights = adapter.getLightThreads();
        if (!lights.isEmpty()) {
            semaphoreRenderer.draw(g2, lights);
        }

        // 3. Vehículos (capa superior)
        List<VehicleThread> vehicles = adapter.getVehicles();
        if (!vehicles.isEmpty()) {
            vehicleRenderer.draw(g2, vehicles);
        }
    }
}