package info.thelaboflieven.ui;

import info.thelaboflieven.run.FlakeAnalysis;
import info.thelaboflieven.run.FlakeSummary;
import info.thelaboflieven.run.RunOutcome;

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
import java.util.Map;

/**
 * Bar timeline: each stripe is one run — color = pass/fail, height = duration (scaled to max in view).
 */
public final class OutcomeChartPanel extends JPanel {

    private static final Color PASS = new Color(40, 167, 69);
    private static final Color FAIL = new Color(220, 53, 69);
    private static final Color AXIS = new Color(108, 117, 125);

    private volatile List<RunOutcome> outcomes = List.of();
    private volatile FlakeAnalysis flakeAnalysis = FlakeAnalysis.EMPTY;
    private volatile String highlightedFingerprint;

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
        setOutcomes(data, FlakeAnalysis.fromOutcomes(data));
    }

    public void setOutcomes(List<RunOutcome> data, FlakeAnalysis analysis) {
        this.outcomes = data != null ? List.copyOf(data) : List.of();
        this.flakeAnalysis = analysis != null ? analysis : FlakeAnalysis.fromOutcomes(this.outcomes);
        repaint();
    }

    public void setHighlightedFingerprint(String fingerprint) {
        this.highlightedFingerprint = fingerprint;
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
        if (o.success()) {
            return "Run " + (i + 1) + ": pass, " + formatDuration(o.durationMs());
        }
        String flakeLabel = flakeLabelForRun(i);
        StringBuilder sb = new StringBuilder("<html>");
        sb.append("Run ")
                .append(i + 1)
                .append(": fail, exit=")
                .append(o.exitCode())
                .append(", ")
                .append(formatDuration(o.durationMs()));
        if (flakeLabel != null) {
            sb.append("<br>Flake: ").append(escapeHtml(flakeLabel));
        }
        if (o.capturedOutput() != null && !o.capturedOutput().isEmpty()) {
            String snip = escapeHtml(o.capturedOutput().trim());
            if (snip.length() > 500) {
                snip = snip.substring(0, 500) + "…";
            }
            snip = snip.replace("\n", "<br>");
            sb.append("<br><span style='font-size:10px;font-family:monospace'>").append(snip).append("</span>");
        }
        sb.append("</html>");
        return sb.toString();
    }

    private String flakeLabelForRun(int runIndex) {
        Map<Integer, String> map = flakeAnalysis.runIndexToFingerprint();
        String fp = map.get(runIndex);
        if (fp == null) {
            return null;
        }
        for (FlakeSummary s : flakeAnalysis.flakes()) {
            if (s.fingerprint().equals(fp)) {
                return s.label();
            }
        }
        return fp;
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
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
        g2.drawString("Bar height = duration; failure color = unique flake pattern (select row below)", padLeft, 16);

        List<RunOutcome> data = outcomes;
        FlakeAnalysis flakes = flakeAnalysis;
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
            Color barColor = colorForRun(i, ok, flakes);
            if (!ok && highlightedFingerprint != null) {
                String fp = flakes.runIndexToFingerprint().get(i);
                if (fp == null || !fp.equals(highlightedFingerprint)) {
                    barColor = new Color(barColor.getRed(), barColor.getGreen(), barColor.getBlue(), 72);
                }
            }
            g2.setColor(barColor);
            g2.fillRect(x1, y, bw, barH);
            if (!ok && highlightedFingerprint != null) {
                String fp = flakes.runIndexToFingerprint().get(i);
                if (fp != null && fp.equals(highlightedFingerprint)) {
                    g2.setColor(Color.BLACK);
                    g2.drawRect(x1, y, bw - 1, barH);
                }
            }
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
                        + "   Unique flakes: "
                        + flakes.flakes().size()
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

    private static Color colorForRun(int runIndex, boolean ok, FlakeAnalysis flakes) {
        if (ok) {
            return PASS;
        }
        String fp = flakes.runIndexToFingerprint().get(runIndex);
        if (fp == null) {
            return FAIL;
        }
        int idx = flakes.colorIndex(fp);
        return FlakeColors.forIndex(idx);
    }
}
