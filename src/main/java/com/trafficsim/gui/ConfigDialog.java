package com.trafficsim.gui;

import com.trafficsim.config.SimulationConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class ConfigDialog extends JDialog {

    private static final Color BG_DARK    = new Color(10, 13, 20);
    private static final Color BG_PANEL   = new Color(16, 20, 32);
    private static final Color BG_FIELD   = new Color(22, 28, 45);
    private static final Color ACCENT     = new Color(0, 200, 150);
    private static final Color TEXT_MAIN  = new Color(220, 230, 245);
    private static final Color TEXT_DIM   = new Color(90, 110, 140);
    private static final Color BORDER_COL = new Color(35, 45, 70);

    private final SimulationConfig config;
    private boolean confirmed = false;

    // Campos del formulario
    private JSpinner spnVehicles;
    private JSpinner spnGreen;
    private JSpinner spnYellow;
    private JSpinner spnRed;
    private JCheckBox chkRandomRoutes;

    public ConfigDialog(Frame parent, SimulationConfig config) {
        super(parent, "Configuración de Simulación", true);
        this.config = config;
        buildUI();
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void buildUI() {
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(0, 0));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_DARK);
        header.setBorder(new EmptyBorder(24, 28, 12, 28));

        JLabel title = new JLabel("PARÁMETROS DE SIMULACIÓN");
        title.setFont(new Font("Monospaced", Font.BOLD, 14));
        title.setForeground(ACCENT);
        header.add(title, BorderLayout.NORTH);

        JLabel subtitle = new JLabel("Configura el comportamiento del simulador de tráfico");
        subtitle.setFont(new Font("Monospaced", Font.PLAIN, 11));
        subtitle.setForeground(TEXT_DIM);
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));
        header.add(subtitle, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);

        // Cuerpo del formulario
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(BG_DARK);
        body.setBorder(new EmptyBorder(0, 28, 16, 28));

        // Sección: Vehículos
        body.add(buildSectionLabel("VEHÍCULOS"));
        body.add(Box.createVerticalStrut(8));
        spnVehicles = buildSpinner(config.getVehicleCount(), 5, 200, 5);
        body.add(buildFieldRow("Cantidad de vehículos", spnVehicles,
                "Entre 5 y 200 vehículos en la simulación"));
        body.add(Box.createVerticalStrut(6));

        chkRandomRoutes = new JCheckBox("Generar rutas aleatorias automáticamente");
        chkRandomRoutes.setSelected(true);
        chkRandomRoutes.setFont(new Font("Monospaced", Font.PLAIN, 11));
        chkRandomRoutes.setForeground(TEXT_MAIN);
        chkRandomRoutes.setBackground(BG_DARK);
        chkRandomRoutes.setFocusPainted(false);
        chkRandomRoutes.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(chkRandomRoutes);
        body.add(Box.createVerticalStrut(20));

        // Sección: Semáforos
        body.add(buildSectionLabel("SEMÁFOROS  (milisegundos)"));
        body.add(Box.createVerticalStrut(8));

        spnGreen  = buildSpinner(config.getGreenMs(),  500, 15000, 500);
        spnYellow = buildSpinner(config.getYellowMs(), 500, 10000, 500);
        spnRed    = buildSpinner(config.getRedMs(),    500, 15000, 500);

        body.add(buildFieldRow("Duración verde  (ms)", spnGreen,  "Tiempo en luz verde"));
        body.add(Box.createVerticalStrut(6));
        body.add(buildFieldRow("Duración amarillo (ms)", spnYellow, "Tiempo en luz amarilla"));
        body.add(Box.createVerticalStrut(6));
        body.add(buildFieldRow("Duración rojo  (ms)", spnRed,   "Tiempo en luz roja"));
        body.add(Box.createVerticalStrut(24));

        add(body, BorderLayout.CENTER);

        // Footer con botones
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JLabel buildSectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Monospaced", Font.BOLD, 10));
        lbl.setForeground(TEXT_DIM);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JPanel buildFieldRow(String label, JSpinner spinner, String tooltip) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setBackground(BG_PANEL);
        row.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_COL, 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Monospaced", Font.PLAIN, 12));
        lbl.setForeground(TEXT_MAIN);

        JLabel tip = new JLabel(tooltip);
        tip.setFont(new Font("Monospaced", Font.PLAIN, 10));
        tip.setForeground(TEXT_DIM);

        JPanel left = new JPanel(new BorderLayout(0, 2));
        left.setBackground(BG_PANEL);
        left.add(lbl, BorderLayout.NORTH);
        left.add(tip, BorderLayout.SOUTH);

        // Estilo del spinner
        spinner.setPreferredSize(new Dimension(100, 30));
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
        editor.getTextField().setBackground(BG_FIELD);
        editor.getTextField().setForeground(TEXT_MAIN);
        editor.getTextField().setFont(new Font("Monospaced", Font.BOLD, 13));
        editor.getTextField().setBorder(new EmptyBorder(2, 8, 2, 8));
        editor.getTextField().setCaretColor(ACCENT);
        spinner.setBorder(new LineBorder(BORDER_COL, 1, true));

        row.add(left,   BorderLayout.CENTER);
        row.add(spinner, BorderLayout.EAST);
        return row;
    }

    private JSpinner buildSpinner(int value, int min, int max, int step) {
        JSpinner spinner = new JSpinner(
                new SpinnerNumberModel(value, min, max, step));
        return spinner;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 16));
        footer.setBackground(BG_DARK);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COL),
                new EmptyBorder(0, 28, 0, 28)
        ));

        JButton btnCancel = buildButton("Cancelar", new Color(40, 48, 70), TEXT_DIM);
        JButton btnOk     = buildButton("Iniciar simulación →", ACCENT, BG_DARK);

        btnCancel.addActionListener(e -> dispose());
        btnOk.addActionListener(e -> {
            applyConfig();
            confirmed = true;
            dispose();
        });

        footer.add(btnCancel);
        footer.add(btnOk);
        return footer;
    }

    private JButton buildButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Monospaced", Font.BOLD, 12));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));

        Color hover = bg.brighter();
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(hover);
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(bg);
            }
        });
        return btn;
    }

    private void applyConfig() {
        config.setVehicleCount((int) spnVehicles.getValue());
        config.setGreenMs((int)  spnGreen.getValue());
        config.setYellowMs((int) spnYellow.getValue());
        config.setRedMs((int)    spnRed.getValue());
    }

    // Muestra el diálogo y devuelve true si el usuario confirmó
    public static boolean show(Frame parent, SimulationConfig config) {
        ConfigDialog dialog = new ConfigDialog(parent, config);
        dialog.setVisible(true);
        return dialog.confirmed;
    }
}