package com.trafficsim.gui.panels;

import com.trafficsim.gui.util.GuiConstants;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Leyenda estática de colores usados en el canvas.
 */
public class LegendPanel extends JPanel {

    public LegendPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, GuiConstants.INTER_BORDER),
            new EmptyBorder(14, 20, 14, 20)
        ));
        buildUI();
    }

    private void buildUI() {
        JLabel title = new JLabel("LEYENDA");
        title.setFont(GuiConstants.FONT_MONO_BOLD);
        title.setForeground(GuiConstants.ACCENT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(title);
        add(Box.createRigidArea(new Dimension(0, 10)));

        addItem("Semáforo verde",     GuiConstants.SEM_GREEN);
        addItem("Semáforo amarillo",  GuiConstants.SEM_YELLOW);
        addItem("Semáforo rojo",      GuiConstants.SEM_RED);
        addItem("Vehículo en ruta",   GuiConstants.ACCENT);
        addItem("Vehículo esperando", GuiConstants.VEH_WAIT);
    }

    private void addItem(String text, Color color) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(220, 22));

        JPanel dot = new JPanel();
        dot.setPreferredSize(new Dimension(10, 10));
        dot.setBackground(color);
        dot.setBorder(BorderFactory.createLineBorder(color.darker()));

        JLabel label = new JLabel(text);
        label.setFont(GuiConstants.FONT_MONO_MD);
        label.setForeground(GuiConstants.TEXT);

        row.add(dot);
        row.add(label);
        add(row);
    }
}