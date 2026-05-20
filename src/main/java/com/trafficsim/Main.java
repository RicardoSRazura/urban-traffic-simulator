package com.trafficsim;

import javax.swing.*;

import com.trafficsim.gui.MainWindow;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            try {

                UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
                );

            } catch (Exception e) {

                e.printStackTrace();
            }

            MainWindow window = new MainWindow();

            window.setVisible(true);
        });
    }
}