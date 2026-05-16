package com.trafficsim.gui.renderer;

import com.trafficsim.gui.util.GuiConstants;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

/**
 * Dibuja la cuadrícula de la ciudad: intersecciones y calles.
 */
public class CityRenderer {

    public void draw(Graphics2D g2) {
        // Fondo del canvas
        g2.setColor(new Color(8, 11, 16));
        g2.fillRect(0, 0, GuiConstants.CANVAS_SIZE, GuiConstants.CANVAS_SIZE);

        for (int r = 0; r < GuiConstants.GRID; r++) {
            for (int c = 0; c < GuiConstants.GRID; c++) {
                int x = GuiConstants.GAP + c * GuiConstants.ROAD_W;
                int y = GuiConstants.GAP + r * GuiConstants.ROAD_W;

                drawIntersection(g2, x, y);
                drawHorizontalStreet(g2, x, y, r, c);
                drawVerticalStreet(g2, x, y, r, c);
            }
        }
    }

    private void drawIntersection(Graphics2D g2, int x, int y) {
        g2.setColor(GuiConstants.INTER);
        g2.fillRect(x, y, GuiConstants.CELL, GuiConstants.CELL);
        g2.setColor(GuiConstants.INTER_BORDER);
        g2.drawRect(x, y, GuiConstants.CELL, GuiConstants.CELL);
    }

    private void drawHorizontalStreet(Graphics2D g2, int x, int y, int row, int col) {
        if (col + 1 >= GuiConstants.GRID) return;

        g2.setColor(GuiConstants.ROAD);
        g2.fillRect(x + GuiConstants.CELL, y + 4,
                    GuiConstants.GAP + GuiConstants.CELL - 4,
                    GuiConstants.CELL - 8);

        if (row % 2 == 0) {
            g2.setColor(GuiConstants.ROAD_LINE);
            g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                                         0, new float[]{4, 5}, 0));
            g2.drawLine(x + GuiConstants.CELL,
                        y + GuiConstants.CELL / 2,
                        x + GuiConstants.CELL + GuiConstants.GAP + GuiConstants.CELL - 4,
                        y + GuiConstants.CELL / 2);
            g2.setStroke(new BasicStroke(1));
        }
    }

    private void drawVerticalStreet(Graphics2D g2, int x, int y, int row, int col) {
        if (row + 1 >= GuiConstants.GRID) return;

        g2.setColor(GuiConstants.ROAD);
        g2.fillRect(x + 4, y + GuiConstants.CELL,
                    GuiConstants.CELL - 8,
                    GuiConstants.GAP + GuiConstants.CELL - 4);

        if (col % 2 == 0) {
            g2.setColor(GuiConstants.ROAD_LINE);
            g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                                         0, new float[]{4, 5}, 0));
            g2.drawLine(x + GuiConstants.CELL / 2,
                        y + GuiConstants.CELL,
                        x + GuiConstants.CELL / 2,
                        y + GuiConstants.CELL + GuiConstants.GAP + GuiConstants.CELL - 4);
            g2.setStroke(new BasicStroke(1));
        }
    }

    /**
     * Convierte coordenadas de grilla al centro en píxeles.
     * Usado también por SemaphoreRenderer y VehicleRenderer.
     */
    public static Point2D.Double cellCenter(int row, int col) {
        return new Point2D.Double(
            GuiConstants.GAP + col * GuiConstants.ROAD_W + GuiConstants.CELL / 2.0,
            GuiConstants.GAP + row * GuiConstants.ROAD_W + GuiConstants.CELL / 2.0
        );
    }
}