package com.trafficsim.gui.renderer;

import com.trafficsim.gui.util.GuiConstants;
import com.trafficsim.model.Intersection;
import com.trafficsim.threads.TrafficLightThread;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Dibuja los semáforos activos sobre el canvas.
 */
public class SemaphoreRenderer {

    public void draw(Graphics2D g2, List<TrafficLightThread> lightThreads) {
        for (TrafficLightThread lightThread : lightThreads) {
            drawLight(g2, lightThread.getIntersection());
        }
    }

    private void drawLight(Graphics2D g2, Intersection inter) {
        int row = inter.getPosition().row;
        int col = inter.getPosition().col;

        Point2D.Double center = CityRenderer.cellCenter(row, col);
        Color color = resolveColor(inter.getLightState());

        int cx = (int) center.x;
        int cy = (int) center.y;

        // Halo exterior
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 51));
        g2.fillOval(cx - 10, cy - 10, 20, 20);

        // Núcleo sólido
        g2.setColor(color);
        g2.fillOval(cx - 5, cy - 5, 10, 10);

        // Anillo semitransparente
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 136));
        g2.drawOval(cx - 8, cy - 8, 16, 16);
    }

    private Color resolveColor(Intersection.LightState state) {
        switch (state) {
            case GREEN:  return GuiConstants.SEM_GREEN;
            case YELLOW: return GuiConstants.SEM_YELLOW;
            default:     return GuiConstants.SEM_RED;
        }
    }
}