package com.trafficsim;

import com.trafficsim.gui.MainFrame;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Activa renderizado más limpio en pantallas HiDPI (opcional)
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(MainFrame::new);
    }
}