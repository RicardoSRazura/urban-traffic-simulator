package com.trafficsim.gui;

import com.trafficsim.config.SimulationConfig;
import com.trafficsim.controller.SimulationController;
import com.trafficsim.controller.SimulationState;
import com.trafficsim.metrics.MetricsCollector;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class MainFrame extends JFrame implements SimulationController.SimulationListener {

    private static final Color BG_MAIN    = new Color(8, 11, 18);
    private static final Color BG_TOOLBAR = new Color(12, 15, 26);
    private static final Color ACCENT     = new Color(0, 200, 150);
    private static final Color TEXT_DIM   = new Color(80, 100, 130);
    private static final Color BORDER_COL = new Color(28, 36, 60);

    private final SimulationConfig config;
    private SimulationController controller;

    private GridPanel    gridPanel;
    private MetricsPanel metricsPanel;

    // Botones de la toolbar
    private JButton btnInit;
    private JButton btnStart;
    private JButton btnPause;
    private JButton btnStop;
    private JButton btnReset;
    private JButton btnConfig;

    // Label de estado en la barra inferior
    private JLabel lblStatusText;

    public MainFrame() {
        this.config = new SimulationConfig();
        initController();
        buildUI();
        applyWindowSettings();
    }

    private void initController() {
        controller = new SimulationController(config);
        controller.setListener(this);
    }

    // -----------------------------------------------------------------------
    // Construcción de la UI
    // -----------------------------------------------------------------------
    private void buildUI() {
        getContentPane().setBackground(BG_MAIN);
        setLayout(new BorderLayout(0, 0));

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // ---- Toolbar superior ----
    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        bar.setBackground(BG_TOOLBAR);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL),
                new EmptyBorder(0, 12, 0, 12)
        ));

        // Logo / título
        JLabel logo = new JLabel("◈ TRAFFIC SIM");
        logo.setFont(new Font("Monospaced", Font.BOLD, 14));
        logo.setForeground(ACCENT);
        logo.setBorder(new EmptyBorder(0, 0, 0, 20));
        bar.add(logo);

        // Botones
        btnConfig = buildToolBtn("⚙ Config",    new Color(40, 50, 80));
        btnInit   = buildToolBtn("⬡ Inicializar", new Color(30, 70, 120));
        btnStart  = buildToolBtn("▶ Iniciar",     new Color(20, 100, 60));
        btnPause  = buildToolBtn("⏸ Pausar",      new Color(120, 90, 10));
        btnStop   = buildToolBtn("■ Detener",     new Color(110, 30, 30));
        btnReset  = buildToolBtn("↺ Reiniciar",   new Color(60, 40, 110));

        btnStart.setEnabled(false);
        btnPause.setEnabled(false);
        btnStop.setEnabled(false);
        btnReset.setEnabled(false);

        bar.add(btnConfig);
        bar.add(makeDivider());
        bar.add(btnInit);
        bar.add(btnStart);
        bar.add(btnPause);
        bar.add(btnStop);
        bar.add(makeDivider());
        bar.add(btnReset);

        // Listeners
        btnConfig.addActionListener(e -> onConfig());
        btnInit.addActionListener(e  -> onInit());
        btnStart.addActionListener(e -> onStart());
        btnPause.addActionListener(e -> onPauseResume());
        btnStop.addActionListener(e  -> onStop());
        btnReset.addActionListener(e -> onReset());

        return bar;
    }

    // ---- Centro: cuadrícula + métricas ----
    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 0));
        center.setBackground(BG_MAIN);

        gridPanel    = new GridPanel(controller);
        metricsPanel = new MetricsPanel(controller);

        // Borde sutil entre grid y métricas
        metricsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER_COL),
                new EmptyBorder(16, 12, 16, 12)
        ));

        center.add(gridPanel,    BorderLayout.CENTER);
        center.add(metricsPanel, BorderLayout.EAST);
        return center;
    }

    // ---- Barra de estado inferior ----
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(6, 8, 14));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COL),
                new EmptyBorder(5, 16, 5, 16)
        ));

        lblStatusText = new JLabel("Listo. Configura los parámetros e inicializa la simulación.");
        lblStatusText.setFont(new Font("Monospaced", Font.PLAIN, 11));
        lblStatusText.setForeground(TEXT_DIM);
        bar.add(lblStatusText, BorderLayout.WEST);

        // Leyenda de colores
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        legend.setBackground(new Color(6, 8, 14));
        legend.add(legendDot(new Color(50, 180, 255),  "Moviendo"));
        legend.add(legendDot(new Color(255, 140, 40),  "Esperando"));
        legend.add(legendDot(new Color(80, 220, 100),  "Llegó"));
        legend.add(legendDot(new Color(0, 210, 120),   "Semáforo verde"));
        legend.add(legendDot(new Color(255, 195, 0),   "Amarillo"));
        legend.add(legendDot(new Color(240, 60, 60),   "Rojo"));
        bar.add(legend, BorderLayout.EAST);

        return bar;
    }

    // -----------------------------------------------------------------------
    // Acciones de botones
    // -----------------------------------------------------------------------
    private void onConfig() {
        boolean ok = ConfigDialog.show(this, config);
        if (ok) {
            setStatus("Configuración aplicada. Haz clic en Inicializar para preparar la simulación.");
        }
    }

    private void onInit() {
        btnInit.setEnabled(false);
        btnConfig.setEnabled(false);
        setStatus("Calculando rutas con A*...");
        controller.initialize();
    }

    private void onStart() {
        controller.start();
        btnStart.setEnabled(false);
        btnPause.setEnabled(true);
        btnStop.setEnabled(true);
        setStatus("Simulación en curso — " + controller.getVehicles().size() + " vehículos activos.");
    }

    private void onPauseResume() {
        if (controller.isPaused()) {
            controller.resume();
            btnPause.setText("⏸ Pausar");
            gridPanel.startTimer();
            setStatus("Simulación reanudada.");
        } else {
            controller.pause();
            btnPause.setText("▶ Reanudar");
            setStatus("Simulación pausada.");
        }
    }

    private void onStop() {
        controller.stop();
        btnPause.setEnabled(false);
        btnStop.setEnabled(false);
        btnReset.setEnabled(true);
        setStatus("Simulación detenida.");
    }

    private void onReset() {
        gridPanel.stopTimer();
        metricsPanel.stopTimer();
        controller.reset();

        // Reconstruir paneles para limpiar el estado visual
        getContentPane().remove(((BorderLayout) getContentPane().getLayout())
                .getLayoutComponent(BorderLayout.CENTER));

        controller = new SimulationController(config);
        controller.setListener(this);

        gridPanel    = new GridPanel(controller);
        metricsPanel = new MetricsPanel(controller);

        JPanel center = buildCenter();
        getContentPane().add(center, BorderLayout.CENTER);
        revalidate();
        repaint();

        // Resetear botones
        btnConfig.setEnabled(true);
        btnInit.setEnabled(true);
        btnStart.setEnabled(false);
        btnPause.setEnabled(false);
        btnPause.setText("⏸ Pausar");
        btnStop.setEnabled(false);
        btnReset.setEnabled(false);

        setStatus("Reiniciado. Configura e inicializa para una nueva simulación.");
    }

    // -----------------------------------------------------------------------
    // SimulationListener — callbacks desde otros hilos
    // SIEMPRE usar SwingUtilities.invokeLater para tocar la GUI
    // -----------------------------------------------------------------------
    @Override
    public void onStateChanged(SimulationState newState) {
        SwingUtilities.invokeLater(() -> {
            metricsPanel.refresh();

            if (newState == SimulationState.IDLE && !btnInit.isEnabled()) {
                // Terminó de calcular rutas
                btnStart.setEnabled(true);
                btnReset.setEnabled(true);
                setStatus("Rutas calculadas. " +
                        controller.getVehicles().size() + " vehículos listos.");
            }

            if (newState == SimulationState.FINISHED) {
                btnPause.setEnabled(false);
                btnStop.setEnabled(false);
                btnReset.setEnabled(true);
                gridPanel.stopTimer();
                metricsPanel.refresh();
                showFinalDialog();
            }
        });
    }

    @Override
    public void onVehicleArrived(int vehicleId, long travelTimeMs) {
        SwingUtilities.invokeLater(() -> metricsPanel.refresh());
    }

    @Override
    public void onAllVehiclesArrived() {
        // Manejado en onStateChanged(FINISHED)
    }

    // -----------------------------------------------------------------------
    // Diálogo final con resumen de métricas
    // -----------------------------------------------------------------------
    private void showFinalDialog() {
        MetricsCollector m = controller.getMetrics();

        String msg = String.format("""
            <html><body style='font-family:monospace; font-size:12px; padding:8px'>
            <b style='color:#00C896'>Simulación completada</b><br><br>
            <table cellpadding='4'>
              <tr><td>Vehículos totales</td>
                  <td><b>%d</b></td></tr>
              <tr><td>Primero en llegar</td>
                  <td><b>Vehículo #%d</b></td></tr>
              <tr><td>Tiempo promedio</td>
                  <td><b>%.0f ms</b></td></tr>
              <tr><td colspan='2'><hr></td></tr>
              <tr><td>A* secuencial</td>
                  <td><b>%d ms</b></td></tr>
              <tr><td>A* paralelo</td>
                  <td><b>%d ms</b></td></tr>
              <tr><td>Speedup</td>
                  <td><b style='color:#00C896'>%.2fx</b></td></tr>
            </table>
            </body></html>""",
                controller.getVehicles().size(),
                m.getFirstArrival(),
                m.getAverageTravelTime(),
                m.getSequentialRouteTime(),
                m.getParallelRouteTime(),
                m.getSpeedup()
        );

        JOptionPane.showMessageDialog(this, msg,
                "Reporte Final", JOptionPane.PLAIN_MESSAGE);
    }

    // -----------------------------------------------------------------------
    // Helpers de UI
    // -----------------------------------------------------------------------
    private JButton buildToolBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Monospaced", Font.BOLD, 11));
        btn.setForeground(new Color(200, 215, 235));
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setBorder(new EmptyBorder(7, 14, 7, 14));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Color hover = bg.brighter();
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(hover);
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(bg);
            }
        });
        return btn;
    }

    private JSeparator makeDivider() {
        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setForeground(BORDER_COL);
        sep.setPreferredSize(new Dimension(1, 28));
        return sep;
    }

    private JPanel legendDot(Color color, String label) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setBackground(new Color(6, 8, 14));

        JLabel dot = new JLabel("●");
        dot.setForeground(color);
        dot.setFont(new Font("Monospaced", Font.PLAIN, 10));

        JLabel txt = new JLabel(label);
        txt.setForeground(TEXT_DIM);
        txt.setFont(new Font("Monospaced", Font.PLAIN, 10));

        p.add(dot);
        p.add(txt);
        return p;
    }

    private void setStatus(String msg) {
        lblStatusText.setText(msg);
    }

    private void applyWindowSettings() {
        setTitle("Urban Traffic Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
        setVisible(true);
    }
}