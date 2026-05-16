package com.trafficsim.gui.util;

import java.awt.Color;
import java.awt.Font;

/**
 * Constantes globales de la interfaz gráfica.
 * Todos los archivos de la GUI importan esta clase directamente —
 * sin static import — para que el IDE resuelva los símbolos sin problemas.
 */
public final class GuiConstants {

    private GuiConstants() {}

    // ── Grid / ciudad ────────────────────────────────────────────────────────
    public static final int GRID        = 12;
    public static final int CELL        = 48;
    public static final int GAP         = 8;
    public static final int ROAD_W      = CELL + GAP;
    public static final int CANVAS_SIZE = GRID * ROAD_W + GAP;

    // ── Fondo y estructura ───────────────────────────────────────────────────
    public static final Color BG           = new Color(10,  14,  20);
    public static final Color PANEL_BG     = new Color(13,  17,  23);
    public static final Color ROAD         = new Color(26,  31,  46);
    public static final Color ROAD_LINE    = new Color(37,  45,  64);
    public static final Color INTER        = new Color(15,  21,  32);
    public static final Color INTER_BORDER = new Color(30,  38,  55);

    // ── Semáforos ────────────────────────────────────────────────────────────
    public static final Color SEM_GREEN  = new Color(57,  255, 20);
    public static final Color SEM_YELLOW = new Color(255, 215, 0);
    public static final Color SEM_RED    = new Color(255, 36,  66);

    // ── Vehículos y UI ───────────────────────────────────────────────────────
    public static final Color VEH_WAIT    = new Color(255, 107, 53);
    public static final Color ACCENT      = new Color(0,   212, 255);
    public static final Color TEXT        = new Color(136, 153, 170);
    public static final Color TEXT_BRIGHT = new Color(204, 214, 224);

    public static final Color[] VEH_COLORS = {
        new Color(0,   212, 255), new Color(255, 107, 53),  new Color(167, 139, 250),
        new Color(52,  211, 153), new Color(251, 191, 36),  new Color(244, 114, 182),
        new Color(96,  165, 250), new Color(251, 113, 133), new Color(74,  222, 128),
        new Color(232, 121, 249)
    };

    // ── Dimensiones panel lateral ────────────────────────────────────────────
    public static final int SIDE_W = 350;

    // ── Fuentes ──────────────────────────────────────────────────────────────
    public static final Font FONT_MONO_SM   = new Font("Monospaced", Font.PLAIN,  9);
    public static final Font FONT_MONO_MD   = new Font("Monospaced", Font.PLAIN,  10);
    public static final Font FONT_MONO_BOLD = new Font("Monospaced", Font.BOLD,   10);
    public static final Font FONT_MONO_11   = new Font("Monospaced", Font.PLAIN,  11);
    public static final Font FONT_STAT_VAL  = new Font("Arial",      Font.BOLD,   18);
    public static final Font FONT_TITLE     = new Font("Arial",      Font.BOLD,   20);
    public static final Font FONT_LOG       = new Font("Monospaced", Font.PLAIN,  10);

    // ── Timer de refresco ────────────────────────────────────────────────────
    public static final int REPAINT_INTERVAL_MS = 16; // ~60 fps
}