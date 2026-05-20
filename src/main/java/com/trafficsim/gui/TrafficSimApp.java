package com.trafficsim.gui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

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