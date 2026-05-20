package com.trafficsim.gui.renderer;

import com.trafficsim.gui.util.GuiConstants;
import com.trafficsim.model.Position;
import com.trafficsim.threads.VehicleThread;
import com.trafficsim.threads.VehicleThread.VehicleState;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Dibuja vehículos con posición interpolada entre celdas.
 *
 * Usa currentPosition + nextPosition + moveProgress del VehicleThread
 * para calcular exactamente dónde está el vehículo en píxeles en cada frame,
 * logrando un movimiento suave en lugar de saltos celda a celda.
 */
public class VehicleRenderer {

    public void draw(Graphics2D g2, List<VehicleThread> vehicles) {
        drawRoutes(g2, vehicles);
        drawBodies(g2, vehicles);
    }

    // ── Rutas punteadas ──────────────────────────────────────────────────────

    private void drawRoutes(Graphics2D g2, List<VehicleThread> vehicles) {
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                                     0, new float[]{3, 5}, 0));

        for (VehicleThread v : vehicles) {
            if (v.getVeichleState() == VehicleState.ARRIVED) continue;

            List<Position> route = v.getRoute();
            if (route.size() < 2) continue;

            Color base = vehicleColor(v);
            g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 40));

            // Dibujamos la ruta restante desde la siguiente posición
            Position currentPos = v.getCurrentPosition();
            int startIdx = route.indexOf(currentPos);
            if (startIdx < 0) startIdx = 0;

            for (int i = startIdx + 1; i < route.size(); i++) {
                Point2D.Double a = CityRenderer.cellCenter(route.get(i - 1).row, route.get(i - 1).col);
                Point2D.Double b = CityRenderer.cellCenter(route.get(i).row,     route.get(i).col);
                g2.drawLine((int) a.x, (int) a.y, (int) b.x, (int) b.y);
            }
        }

        g2.setStroke(new BasicStroke(1));
    }

    // ── Cuerpos con interpolación ────────────────────────────────────────────

    private void drawBodies(Graphics2D g2, List<VehicleThread> vehicles) {
        for (VehicleThread v : vehicles) {
            if (v.getVeichleState() == VehicleState.ARRIVED) continue;
            drawSingleVehicle(g2, v);
        }
    }

    private void drawSingleVehicle(Graphics2D g2, VehicleThread v) {
        // Calculamos la posición exacta en píxeles interpolando entre
        // currentPosition y nextPosition según el progreso actual (0.0 → 1.0)
        Point2D.Double pos = interpolatedPosition(v);

        boolean isWaiting = v.getVeichleState() == VehicleState.WAITING_LIGHT
                         || v.getVeichleState() == VehicleState.WAITING_LOCK;

        Color color = isWaiting ? GuiConstants.VEH_WAIT : vehicleColor(v);

        int cx = (int) pos.x;
        int cy = (int) pos.y;

        // Glow exterior
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
        g2.fillRoundRect(cx - 9, cy - 6, 18, 12, 4, 4);

        // Cuerpo principal (un poco más grande que antes)
        g2.setColor(color);
        g2.fillRoundRect(cx - 7, cy - 5, 14, 10, 3, 3);

        // Brillo superior para dar sensación de volumen
        g2.setColor(new Color(255, 255, 255, 60));
        g2.fillRoundRect(cx - 6, cy - 4, 12, 4, 2, 2);

        // ID del vehículo
        g2.setColor(new Color(0, 0, 0, 200));
        g2.setFont(new Font("Monospaced", Font.BOLD, 7));
        FontMetrics fm = g2.getFontMetrics();
        String idStr = String.valueOf(v.getVehicleId());
        int idW = fm.stringWidth(idStr);
        g2.drawString(idStr, cx - idW / 2, cy + 3);
    }

    // ── Interpolación ────────────────────────────────────────────────────────

    /**
     * Calcula la posición en píxeles del vehículo usando interpolación lineal
     * entre el centro de currentPosition y el centro de nextPosition.
     *
     * Cuando progress=0.0 → está en currentPosition
     * Cuando progress=1.0 → está en nextPosition
     * Cuando progress=0.5 → está exactamente a la mitad entre ambas celdas
     */
    private Point2D.Double interpolatedPosition(VehicleThread v) {
        Position current = v.getCurrentPosition();
        Position next    = v.getNextPosition();
        double progress  = v.getMoveProgress();

        Point2D.Double a = CityRenderer.cellCenter(current.row, current.col);
        Point2D.Double b = CityRenderer.cellCenter(next.row,    next.col);

        // Lerp: a + (b - a) * progress
        double x = a.x + (b.x - a.x) * progress;
        double y = a.y + (b.y - a.y) * progress;

        return new Point2D.Double(x, y);
    }

    // ── Utilidad ─────────────────────────────────────────────────────────────

    private Color vehicleColor(VehicleThread v) {
        return GuiConstants.VEH_COLORS[v.getVehicleId() % GuiConstants.VEH_COLORS.length];
    }
}