package com.trafficsim.gui.panels;

import com.trafficsim.config.SimulationConfig;
import com.trafficsim.controller.SimulationState;
import com.trafficsim.gui.util.GuiConstants;
import com.trafficsim.gui.util.SimulationAdapter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Panel de control: botones Iniciar / Pausar / Detener.
 */
public class ControlPanel extends JPanel {

    private final SimulationAdapter adapter;
    private final ConfigPanel       configPanel;

    private JButton btnStart;
    private JButton btnPause;
    private JButton btnStop;

    public interface ControlListener {
        void onStart();
        void onPause();
        void onResume();
        void onStop();
    }

    private ControlListener controlListener;

    public ControlPanel(SimulationAdapter adapter, ConfigPanel configPanel) {
        this.adapter     = adapter;
        this.configPanel = configPanel;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, GuiConstants.INTER_BORDER),
            new EmptyBorder(18, 20, 18, 20)
        ));
        buildUI();
    }

    private void buildUI() {
        JLabel title = new JLabel("CONTROL");
        title.setFont(GuiConstants.FONT_MONO_BOLD);
        title.setForeground(GuiConstants.ACCENT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(title);
        add(Box.createRigidArea(new Dimension(0, 14)));

        btnStart = makeButton("▶  Iniciar",  GuiConstants.ACCENT);
        btnPause = makeButton("⏸  Pausar",  GuiConstants.TEXT);
        btnStop  = makeButton("■  Detener", GuiConstants.SEM_RED);

        btnPause.setEnabled(false);
        btnStop.setEnabled(false);

        btnStart.addActionListener(e -> handleStart());
        btnPause.addActionListener(e -> handlePause());
        btnStop.addActionListener(e  -> handleStop());

        add(btnStart);
        add(Box.createRigidArea(new Dimension(0, 8)));
        add(btnPause);
        add(Box.createRigidArea(new Dimension(0, 8)));
        add(btnStop);
    }

    // ── Acciones ─────────────────────────────────────────────────────────────

    private void handleStart() {
        SimulationConfig config = configPanel.buildConfig();
        adapter.initialize(config);
        setButtonsForCalculating();
        if (controlListener != null) controlListener.onStart();
    }

    private void handlePause() {
        if (adapter.isPaused()) {
            adapter.resume();
            btnPause.setText("⏸  Pausar");
            if (controlListener != null) controlListener.onResume();
        } else {
            adapter.pause();
            btnPause.setText("⏵  Reanudar");
            if (controlListener != null) controlListener.onPause();
        }
    }

    private void handleStop() {
        adapter.stop();
        setButtonsForStopped();
        if (controlListener != null) controlListener.onStop();
    }

    // ── Sincronización con estado del controlador ────────────────────────────

    public void syncWithState(SimulationState state) {
        SwingUtilities.invokeLater(() -> {
            switch (state) {
                case CALCULATING: setButtonsForCalculating(); break;
                case IDLE:        setButtonsForIdle();        break;
                case RUNNING:     setButtonsForRunning();     break;
                case FINISHED:    setButtonsForStopped();     break;
                default: break;
            }
        });
    }

    private void setButtonsForCalculating() {
        btnStart.setEnabled(false);
        btnPause.setEnabled(false);
        btnStop.setEnabled(false);
        configPanel.setEnabled(false);
    }

    private void setButtonsForIdle() {
        // Rutas calculadas → arrancamos automáticamente
        adapter.start();
        setButtonsForRunning();
    }

    private void setButtonsForRunning() {
        btnStart.setEnabled(false);
        btnPause.setEnabled(true);
        btnStop.setEnabled(true);
        btnPause.setText("⏸  Pausar");
        configPanel.setEnabled(false);
    }

    private void setButtonsForStopped() {
        btnStart.setEnabled(true);
        btnPause.setEnabled(false);
        btnStop.setEnabled(false);
        btnPause.setText("⏸  Pausar");
        configPanel.setEnabled(true);
    }

    // ── Fábrica de botones ───────────────────────────────────────────────────

    private JButton makeButton(String text, java.awt.Color color) {
        JButton btn = new JButton(text);
        btn.setFont(GuiConstants.FONT_MONO_11);
        btn.setForeground(color);
        btn.setBackground(GuiConstants.PANEL_BG);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GuiConstants.INTER_BORDER),
            new EmptyBorder(10, 14, 10, 14)
        ));
        btn.setFocusPainted(false);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(220, 40));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (btn.isEnabled()) {
                    btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(color),
                        new EmptyBorder(10, 14, 10, 14)
                    ));
                }
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GuiConstants.INTER_BORDER),
                    new EmptyBorder(10, 14, 10, 14)
                ));
            }
        });

        return btn;
    }

    public void setControlListener(ControlListener listener) {
        this.controlListener = listener;
    }
}