package com.trafficsim.gui.panels;

import com.trafficsim.gui.util.GuiConstants;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Panel de registro de eventos con scroll automático.
 * addEntry() es seguro para llamar desde cualquier hilo.
 */
public class LogPanel extends JPanel {

    private static final int MAX_ENTRIES = 120;
    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JPanel      container;
    private final JScrollPane scrollPane;
    private int entryCount = 0;

    public LogPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, GuiConstants.INTER_BORDER));
        setPreferredSize(new Dimension(GuiConstants.SIDE_W, 180));
        setMaximumSize(new Dimension(GuiConstants.SIDE_W, 180));

        JLabel title = new JLabel("REGISTRO");
        title.setFont(GuiConstants.FONT_MONO_BOLD);
        title.setForeground(GuiConstants.ACCENT);
        title.setBorder(new EmptyBorder(14, 20, 8, 20));
        add(title, BorderLayout.NORTH);

        container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(GuiConstants.PANEL_BG);

        scrollPane = new JScrollPane(container);
        scrollPane.setBackground(GuiConstants.PANEL_BG);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void addEntry(int vehicleId, String message) {
        SwingUtilities.invokeLater(() -> {
            if (entryCount >= MAX_ENTRIES) {
                container.remove(0);
            }

            String time = LocalTime.now().format(TIME_FMT);
            String html = vehicleId >= 0
                ? String.format(
                    "<html><span style='color:#8899aa'>%s</span> "
                    + "<span style='color:#ff6b35'>V-%02d</span> "
                    + "<span style='color:#ccd6e0'>%s</span></html>",
                    time, vehicleId, message)
                : String.format(
                    "<html><span style='color:#8899aa'>%s</span> "
                    + "<span style='color:#00d4ff'>%s</span></html>",
                    time, message);

            JLabel entry = new JLabel(html);
            entry.setFont(GuiConstants.FONT_LOG);
            entry.setBorder(new EmptyBorder(4, 20, 4, 20));
            entry.setAlignmentX(Component.LEFT_ALIGNMENT);

            container.add(entry);
            container.revalidate();

            SwingUtilities.invokeLater(() -> {
                JScrollBar bar = scrollPane.getVerticalScrollBar();
                bar.setValue(bar.getMaximum());
            });

            entryCount++;
        });
    }

    public void clear() {
        SwingUtilities.invokeLater(() -> {
            container.removeAll();
            container.revalidate();
            container.repaint();
            entryCount = 0;
        });
    }
}