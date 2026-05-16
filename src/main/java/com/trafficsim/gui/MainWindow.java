package com.trafficsim.gui;

import com.trafficsim.controller.SimulationState;
import com.trafficsim.gui.panels.CityCanvasPanel;
import com.trafficsim.gui.panels.ConfigPanel;
import com.trafficsim.gui.panels.ControlPanel;
import com.trafficsim.gui.panels.LegendPanel;
import com.trafficsim.gui.panels.LogPanel;
import com.trafficsim.gui.panels.StatsPanel;
import com.trafficsim.gui.util.GuiConstants;
import com.trafficsim.gui.util.SimulationAdapter;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;

/**
 * Ventana principal del simulador.
 * Orquesta paneles, timer de refresco y listener del adaptador.
 */
public class MainWindow extends JFrame {

    private final SimulationAdapter adapter = new SimulationAdapter();

    // Paneles
    private CityCanvasPanel cityCanvas;
    private ControlPanel    controlPanel;
    private ConfigPanel     configPanel;
    private StatsPanel      statsPanel;
    private LogPanel        logPanel;

    // Header
    private JPanel statusDot;
    private JLabel statusText;

    // Timer de refresco
    private Timer repaintTimer;

    public MainWindow() {
        setTitle("Urban Traffic Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(GuiConstants.BG);

        buildUI();
        registerAdapterListener();
        startRepaintTimer();

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    // ── Construcción de UI ───────────────────────────────────────────────────

    private void buildUI() {
        add(buildHeader(),    BorderLayout.NORTH);
        add(buildMainPanel(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(8, 11, 16));
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, GuiConstants.INTER_BORDER),
            new EmptyBorder(14, 28, 14, 28)
        ));

        JLabel logo = new JLabel("URBAN TRAFFIC SIM");
        logo.setFont(GuiConstants.FONT_TITLE);
        logo.setForeground(GuiConstants.ACCENT);
        header.add(logo, BorderLayout.WEST);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        statusPanel.setOpaque(false);

        statusDot = new JPanel();
        statusDot.setPreferredSize(new Dimension(8, 8));

        statusText = new JLabel("DETENIDO");
        statusText.setFont(GuiConstants.FONT_MONO_11);
        statusText.setForeground(GuiConstants.TEXT);

        setStatus(GuiConstants.SEM_RED, "DETENIDO");

        statusPanel.add(statusDot);
        statusPanel.add(statusText);
        header.add(statusPanel, BorderLayout.EAST);

        return header;
    }

    private JPanel buildMainPanel() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(GuiConstants.BG);

        cityCanvas = new CityCanvasPanel(adapter);
        JPanel canvasWrap = new JPanel(new GridBagLayout());
        canvasWrap.setBackground(new Color(8, 11, 16));
        canvasWrap.setBorder(new EmptyBorder(28, 28, 28, 28));
        canvasWrap.add(cityCanvas);
        main.add(canvasWrap, BorderLayout.CENTER);

        main.add(buildSidePanel(), BorderLayout.EAST);

        return main;
    }

    private JPanel buildSidePanel() {
        configPanel  = new ConfigPanel();
        controlPanel = new ControlPanel(adapter, configPanel);
        statsPanel   = new StatsPanel(adapter);
        logPanel     = new LogPanel();

        controlPanel.setControlListener(new ControlPanel.ControlListener() {
            @Override public void onStart()  { /* estado llega por adapter listener */ }
            @Override public void onPause()  { setStatus(GuiConstants.SEM_YELLOW, "PAUSADO");  }
            @Override public void onResume() { setStatus(GuiConstants.SEM_GREEN,  "EN CURSO"); }
            @Override public void onStop()   {
                setStatus(GuiConstants.SEM_RED, "DETENIDO");
                statsPanel.reset();
                logPanel.clear();
            }
        });

        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(GuiConstants.PANEL_BG);
        side.setPreferredSize(new Dimension(GuiConstants.SIDE_W, 0));
        side.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, GuiConstants.INTER_BORDER));

        side.add(controlPanel);
        side.add(configPanel);
        side.add(statsPanel);
        side.add(logPanel);
        side.add(new LegendPanel());

        return side;
    }

    // ── Listener del adaptador ───────────────────────────────────────────────

    private void registerAdapterListener() {
        adapter.setGuiListener(new SimulationAdapter.GuiListener() {

            @Override
            public void onStateChanged(SimulationState state) {
                SwingUtilities.invokeLater(() -> {
                    controlPanel.syncWithState(state);
                    switch (state) {
                        case CALCULATING:
                            setStatus(new Color(251, 191, 36), "CALCULANDO..."); break;
                        case RUNNING:
                            setStatus(GuiConstants.SEM_GREEN, "EN CURSO"); break;
                        case PAUSED:
                            setStatus(GuiConstants.SEM_YELLOW, "PAUSADO"); break;
                        case FINISHED:
                            setStatus(GuiConstants.SEM_RED, "FINALIZADO"); break;
                        default: break;
                    }
                });
            }

            @Override
            public void onVehicleArrived(int vehicleId, long travelTimeMs) {
                logPanel.addEntry(vehicleId,
                    String.format("llegó al destino en %d ms ✓", travelTimeMs));
            }

            @Override
            public void onAllVehiclesArrived() {
                SwingUtilities.invokeLater(() -> {
                    setStatus(GuiConstants.SEM_GREEN, "COMPLETADO");
                    logPanel.addEntry(-1, String.format(
                        "✔ Todos llegaron | Speedup: %.2fx | Seq: %d ms | Par: %d ms",
                        adapter.getSpeedup(),
                        adapter.getSequentialRouteTime(),
                        adapter.getParallelRouteTime()));
                });
            }
        });
    }

    // ── Timer de refresco ────────────────────────────────────────────────────

    private void startRepaintTimer() {
        repaintTimer = new Timer(GuiConstants.REPAINT_INTERVAL_MS, e -> {
            cityCanvas.repaint();
            statsPanel.refresh();
        });
        repaintTimer.start();
    }

    // ── Helper de estado del header ──────────────────────────────────────────

    private void setStatus(Color color, String text) {
        statusDot.setBackground(color);
        statusDot.setBorder(BorderFactory.createLineBorder(color, 1));
        statusText.setText(text);
        statusText.setForeground(color);
    }

    @Override
    public void dispose() {
        if (repaintTimer != null) repaintTimer.stop();
        adapter.stop();
        super.dispose();
    }
}