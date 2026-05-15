package com.trafficsim.gui;

import com.trafficsim.controller.SimulationController;
import com.trafficsim.model.City;
import com.trafficsim.model.Intersection;
import com.trafficsim.model.Position;
import com.trafficsim.threads.VehicleThread;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

public class GridPanel extends JPanel {

    // ---- Dimensiones ----
    private static final int CELL   = 50;   // px por celda
    private static final int PAD    = 24;   // margen exterior
    private static final int NODE   = 10;   // radio del nodo de intersección
    private static final int STREET = 7;    // ancho visual de la calle

    // ---- Paleta de colores ----
    private static final Color C_BG          = new Color(8, 11, 18);
    private static final Color C_STREET      = new Color(28, 36, 54);
    private static final Color C_NODE        = new Color(36, 46, 70);
    private static final Color C_NODE_SEM    = new Color(20, 28, 50);
    private static final Color C_GREEN       = new Color(0, 210, 120);
    private static final Color C_YELLOW      = new Color(255, 195, 0);
    private static final Color C_RED         = new Color(240, 60, 60);
    private static final Color C_VEHICLE     = new Color(50, 180, 255);
    private static final Color C_VEH_WAIT    = new Color(255, 140, 40);
    private static final Color C_VEH_DONE    = new Color(80, 220, 100);
    private static final Color C_ROUTE       = new Color(50, 180, 255, 35);
    private static final Color C_ROUTE_DOT   = new Color(50, 180, 255, 80);

    private final SimulationController controller;
    private final Timer repaintTimer;

    // Tick para animación de pulso en semáforos
    private int tick = 0;

    public GridPanel(SimulationController controller) {
        this.controller = controller;

        int size = City.SIZE * CELL + PAD * 2;
        setPreferredSize(new Dimension(size, size));
        setBackground(C_BG);

        // 20 fps — suficiente para movimiento fluido
        repaintTimer = new Timer(50, e -> { tick++; repaint(); });
        repaintTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);

        drawStreets(g2);
        drawNodes(g2);
        drawRoutes(g2);
        drawVehicles(g2);
    }

    // -----------------------------------------------------------------------
    // Dibuja los segmentos de calle entre intersecciones
    // -----------------------------------------------------------------------
    private void drawStreets(Graphics2D g) {
        g.setColor(C_STREET);
        int size = City.SIZE;

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int cx = PAD + col * CELL;
                int cy = PAD + row * CELL;

                // Horizontal →
                if (col < size - 1) {
                    g.fillRect(cx, cy - STREET / 2, CELL, STREET);
                }
                // Vertical ↓
                if (row < size - 1) {
                    g.fillRect(cx - STREET / 2, cy, STREET, CELL);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Dibuja los nodos de intersección y semáforos
    // -----------------------------------------------------------------------
    private void drawNodes(Graphics2D g) {
        City city = controller.getCity();

        for (int row = 0; row < City.SIZE; row++) {
            for (int col = 0; col < City.SIZE; col++) {
                Intersection inter = city.getIntersection(new Position(row, col));
                int cx = PAD + col * CELL;
                int cy = PAD + row * CELL;

                if (inter.isHasSemaphore()) {
                    // Color del semáforo actual
                    Color lightColor = switch (inter.getLightState()) {
                        case GREEN  -> C_GREEN;
                        case YELLOW -> C_YELLOW;
                        case RED    -> C_RED;
                    };

                    // Halo pulsante — radio varía con el tick
                    float pulse = 1.0f + 0.15f * (float) Math.sin(tick * 0.25);
                    int haloR = (int)(NODE * 2.2f * pulse);
                    g.setColor(new Color(
                            lightColor.getRed(),
                            lightColor.getGreen(),
                            lightColor.getBlue(), 40));
                    g.fillOval(cx - haloR / 2, cy - haloR / 2, haloR, haloR);

                    // Nodo con color del semáforo
                    g.setColor(lightColor.darker());
                    g.fillOval(cx - NODE, cy - NODE, NODE * 2, NODE * 2);

                    g.setColor(lightColor);
                    g.fillOval(cx - NODE + 2, cy - NODE + 2,
                            NODE * 2 - 4, NODE * 2 - 4);

                } else {
                    // Nodo normal (sin semáforo)
                    g.setColor(C_NODE);
                    g.fillOval(cx - NODE / 2, cy - NODE / 2, NODE, NODE);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Dibuja la ruta de cada vehículo como línea punteada
    // -----------------------------------------------------------------------
    private void drawRoutes(Graphics2D g) {
        List<VehicleThread> vehicles = controller.getVehicles();
        if (vehicles == null || vehicles.isEmpty()) return;

        // Línea sólida muy tenue
        g.setColor(C_ROUTE);
        g.setStroke(new BasicStroke(1.5f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                0, new float[]{3f, 5f}, tick % 8));  // dash animado

        for (VehicleThread v : vehicles) {
            if (v.getVehicleState() == VehicleThread.VehicleState.ARRIVED) continue;
            List<Position> route = v.getRoute();
            if (route == null || route.size() < 2) continue;

            for (int i = 0; i < route.size() - 1; i++) {
                Position a = route.get(i);
                Position b = route.get(i + 1);
                g.drawLine(
                        PAD + a.col * CELL, PAD + a.row * CELL,
                        PAD + b.col * CELL, PAD + b.row * CELL
                );
            }
        }

        // Puntos de destino
        g.setColor(C_ROUTE_DOT);
        g.setStroke(new BasicStroke(1f));
        for (VehicleThread v : vehicles) {
            if (v.getVehicleState() == VehicleThread.VehicleState.ARRIVED) continue;
            Position dest = v.getDestination();
            if (dest == null) continue;
            int dx = PAD + dest.col * CELL;
            int dy = PAD + dest.row * CELL;
            g.drawOval(dx - 7, dy - 7, 14, 14);
            g.drawOval(dx - 4, dy - 4, 8, 8);
        }

        g.setStroke(new BasicStroke(1f));
    }

    // -----------------------------------------------------------------------
    // Dibuja cada vehículo como un cuadrado redondeado con ID
    // -----------------------------------------------------------------------
    private void drawVehicles(Graphics2D g) {
        List<VehicleThread> vehicles = controller.getVehicles();
        if (vehicles == null || vehicles.isEmpty()) return;

        int vSize = 12; // tamaño del cuadrado del vehículo

        for (VehicleThread v : vehicles) {
            Position pos = v.getCurrentPosition();
            if (pos == null) continue;

            int cx = PAD + pos.col * CELL;
            int cy = PAD + pos.row * CELL;

            // Color según estado
            Color col = switch (v.getVehicleState()) {
                case WAITING_LIGHT, WAITING_LOCK -> C_VEH_WAIT;
                case ARRIVED                     -> C_VEH_DONE;
                default                          -> C_VEHICLE;
            };

            // Sombra difusa
            g.setColor(new Color(0, 0, 0, 60));
            g.fill(new RoundRectangle2D.Float(
                    cx - vSize / 2f + 2, cy - vSize / 2f + 2,
                    vSize, vSize, 4, 4));

            // Halo del vehículo
            g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 50));
            g.fill(new RoundRectangle2D.Float(
                    cx - vSize / 2f - 3, cy - vSize / 2f - 3,
                    vSize + 6, vSize + 6, 6, 6));

            // Cuerpo
            g.setColor(col.darker());
            g.fill(new RoundRectangle2D.Float(
                    cx - vSize / 2f, cy - vSize / 2f,
                    vSize, vSize, 4, 4));
            g.setColor(col);
            g.fill(new RoundRectangle2D.Float(
                    cx - vSize / 2f + 1, cy - vSize / 2f + 1,
                    vSize - 2, vSize - 2, 3, 3));

            // ID del vehículo (solo si hay 50 o menos)
            if (vehicles.size() <= 50) {
                g.setColor(C_BG);
                g.setFont(new Font("Monospaced", Font.BOLD, 7));
                String label = String.valueOf(v.getVehicleId());
                FontMetrics fm = g.getFontMetrics();
                g.drawString(label,
                        cx - fm.stringWidth(label) / 2,
                        cy + fm.getAscent() / 2 - 1);
            }
        }
    }

    public void stopTimer() { repaintTimer.stop(); }
    public void startTimer() { repaintTimer.start(); }
}