package com.trafficsim.gui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Punto de entrada de la GUI.
 *
 * Estructura de carpetas requerida en el proyecto:
 *
 *   src/main/java/com/trafficsim/gui/
 *   ├── TrafficSimApp.java
 *   ├── MainWindow.java
 *   ├── panels/
 *   │   ├── CityCanvasPanel.java
 *   │   ├── ControlPanel.java
 *   │   ├── ConfigPanel.java
 *   │   ├── StatsPanel.java
 *   │   ├── LogPanel.java
 *   │   └── LegendPanel.java
 *   ├── renderer/
 *   │   ├── CityRenderer.java
 *   │   ├── SemaphoreRenderer.java
 *   │   └── VehicleRenderer.java
 *   └── util/
 *       ├── GuiConstants.java
 *       └── SimulationAdapter.java
 */
public class TrafficSimApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("[GUI] Look & Feel no disponible: " + e.getMessage());
            }
            new MainWindow().setVisible(true);
        });
    }
}