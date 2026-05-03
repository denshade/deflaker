package io.deflaker.ui;

import io.deflaker.run.RunOutcome;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Locale;

/**
 * Bar timeline: each stripe is one run — color = pass/fail, height = duration (scaled to max in view).
 */
public final class OutcomeChartPanel extends JPanel {

    private static final Color PASS = new Color(40, 167, 69);
    private static final Color FAIL = new Color(220, 53, 69);
    private static final Color AXIS = new Color(108, 117, 125);

    private volatile List<RunOutcome> outcomes = List.of();

    private final int padLeft = 56;
    private final int padRight = 12;
    private final int padTop = 28;
    private final int padBottom = 36;

    public OutcomeChartPanel() {
        setOpaque(true);
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(720, 220));
        setMinimumSize(new Dimension(400, 120));
    }

    public void setOutcomes(List<RunOutcome> data) {
        this.outcomes = data != null ? List.copyOf(data) : List.of();
        repaint();
    }

    static String formatDuration(long ms) {
        if (ms < 1000) {
            return ms + " ms";
        }
        return String.format(Locale.US, "%.2f s", ms / 1000.0);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        List<RunOutcome> data = outcomes;
        if (data.isEmpty()) {
            return null;
        }
        int w = getWidth();
        int h = getHeight();
        int chartW = Math.max(1, w - padLeft - padRight);
        int chartH = Math.max(1, h - padTop - padBottom);
        int n = data.size();
        if (e.getX() < padLeft || e.getX() >= padLeft + chartW || e.getY() < padTop || e.getY() > padTop + chartH) {
            return null;
        }
        double barW = (double) chartW / n;
        int i = (int) Math.floor((e.getX() - padLeft) / barW);
        i = Math.max(0, Math.min(n - 1, i));
        RunOutcome o = data.get(i);
        return "Run " + (i + 1) + ": " + (o.success() ? "pass" : "fail") + ", " + formatDuration(o.durationMs());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int chartW = Math.max(1, w - padLeft - padRight);
        int chartH = Math.max(1, h - padTop - padBottom);

        g2.setColor(Color.BLACK);
        g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString("Bar height = duration; color = green pass / red fail (hover for ms)", padLeft, 16);

        List<RunOutcome> data = outcomes;
        if (data.isEmpty()) {
            g2.setColor(AXIS);
            g2.drawString("No runs yet — paste a command and press Start.", padLeft, padTop + chartH / 2);
            g2.dispose();
            return;
        }

        long maxMs = 1;
        for (RunOutcome o : data) {
            maxMs = Math.max(maxMs, o.durationMs());
        }

        int n = data.size();
        double barW = (double) chartW / n;
        int baselineY = padTop + chartH;

        for (int i = 0; i < n; i++) {
            RunOutcome o = data.get(i);
            boolean ok = o.success();
            long ms = o.durationMs();
            int barH = (int) Math.round(chartH * (double) ms / maxMs);
            barH = Math.max(1, Math.min(chartH, barH));
            int x1 = padLeft + (int) Math.floor(i * barW);
            int x2 = padLeft + (int) Math.floor((i + 1) * barW);
            int bw = Math.max(1, x2 - x1);
            int y = baselineY - barH;
            g2.setColor(ok ? PASS : FAIL);
            g2.fillRect(x1, y, bw, barH);
        }

        g2.setColor(AXIS);
        g2.drawLine(padLeft, baselineY, padLeft + chartW, baselineY);
        g2.drawLine(padLeft, padTop, padLeft, baselineY);

        g2.setColor(Color.DARK_GRAY);
        g2.drawString("0", padLeft - fm.stringWidth("0") - 4, baselineY);
        String maxLabel = formatDuration(maxMs);
        g2.drawString(maxLabel, padLeft - fm.stringWidth(maxLabel) - 4, padTop + fm.getAscent());

        String startLabel = "1";
        String endLabel = String.valueOf(n);
        g2.drawString(startLabel, padLeft, baselineY + fm.getAscent() + 6);
        g2.drawString(endLabel, padLeft + chartW - fm.stringWidth(endLabel), baselineY + fm.getAscent() + 6);

        int passes = 0;
        long sumMs = 0;
        long minMs = Long.MAX_VALUE;
        long maxDur = 0;
        for (RunOutcome o : data) {
            if (o.success()) {
                passes++;
            }
            sumMs += o.durationMs();
            minMs = Math.min(minMs, o.durationMs());
            maxDur = Math.max(maxDur, o.durationMs());
        }
        int fails = n - passes;
        if (minMs == Long.MAX_VALUE) {
            minMs = 0;
        }
        long avgMs = n > 0 ? sumMs / n : 0;
        String summary =
                "Runs: "
                        + n
                        + "   Pass: "
                        + passes
                        + "   Fail: "
                        + fails
                        + "   Duration — min: "
                        + formatDuration(minMs)
                        + "   avg: "
                        + formatDuration(avgMs)
                        + "   max: "
                        + formatDuration(maxDur);
        g2.setColor(Color.DARK_GRAY);
        g2.drawString(summary, padLeft, h - 8);

        g2.dispose();
    }
}
