package com.trafficsim.gui.panels;

import com.trafficsim.config.SimulationConfig;
import com.trafficsim.gui.util.GuiConstants;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

/**
 * Panel de configuración.
 * buildConfig() construye un SimulationConfig listo para el adaptador.
 */
public class ConfigPanel extends JPanel {

    private JSlider sldVehicles;
    private JSlider sldGreenMs;
    private JSlider sldYellowMs;
    private JSlider sldRedMs;
    private JSlider sldSpeed;   // ms por celda: mayor = más lento

    public ConfigPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, GuiConstants.INTER_BORDER),
            new EmptyBorder(18, 20, 18, 20)
        ));
        buildUI();
    }

    private void buildUI() {
        JLabel title = new JLabel("CONFIGURACIÓN");
        title.setFont(GuiConstants.FONT_MONO_BOLD);
        title.setForeground(GuiConstants.ACCENT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(title);
        add(Box.createRigidArea(new Dimension(0, 14)));

        sldVehicles = addSlider("Vehículos",          5,   60,  30);
        sldGreenMs  = addSlider("Verde (s)",           1,   15,   5);
        sldYellowMs = addSlider("Amarillo (s)",        1,    5,   2);
        sldRedMs    = addSlider("Rojo (s)",            1,   15,   6);

        // Velocidad: el valor es ms/celda.
        // 200 ms = muy rápido, 2000 ms = muy lento (bueno para demos)
        // El label muestra el valor invertido como "velocidad" para que
        // sea intuitivo: slider a la derecha = más lento = más visible
        sldSpeed    = addSlider("Velocidad vehículos (ms/celda)", 200, 2000, 800);
    }

    private JSlider addSlider(String label, int min, int max, int initial) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(220, 58));

        JPanel labelRow = new JPanel(new BorderLayout());
        labelRow.setOpaque(false);

        JLabel nameLabel = new JLabel(label);
        nameLabel.setFont(GuiConstants.FONT_MONO_MD);
        nameLabel.setForeground(GuiConstants.TEXT);

        JLabel valueLabel = new JLabel(String.valueOf(initial));
        valueLabel.setFont(GuiConstants.FONT_MONO_MD);
        valueLabel.setForeground(GuiConstants.ACCENT);

        labelRow.add(nameLabel,  BorderLayout.WEST);
        labelRow.add(valueLabel, BorderLayout.EAST);

        JSlider slider = new JSlider(min, max, initial);
        slider.setOpaque(false);
        slider.setForeground(GuiConstants.ACCENT);
        slider.addChangeListener(e -> valueLabel.setText(String.valueOf(slider.getValue())));

        row.add(labelRow, BorderLayout.NORTH);
        row.add(slider,   BorderLayout.CENTER);

        add(row);
        add(Box.createRigidArea(new Dimension(0, 10)));

        return slider;
    }

    /**
     * Construye SimulationConfig con los valores actuales de todos los sliders,
     * incluyendo la velocidad de los vehículos.
     */
    public SimulationConfig buildConfig() {
        return new SimulationConfig(
            sldGreenMs.getValue()  * 1000,   // verde en ms
            sldYellowMs.getValue() * 1000,   // amarillo en ms
            sldRedMs.getValue()    * 1000,   // rojo en ms
            sldVehicles.getValue(),          // cantidad de vehículos
            sldSpeed.getValue()              // ms por celda (velocidad visual)
        );
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        sldVehicles.setEnabled(enabled);
        sldGreenMs.setEnabled(enabled);
        sldYellowMs.setEnabled(enabled);
        sldRedMs.setEnabled(enabled);
        sldSpeed.setEnabled(enabled);
    }
}