package io.deflaker.ui;

import io.deflaker.cli.SimpleCommandLineSplitter;
import io.deflaker.run.CommandRunner;
import io.deflaker.run.RunHistory;
import io.deflaker.run.RunOutcome;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public final class CommandLinePasteFrame extends JFrame {

    private static final int MAX_CHART_SAMPLES = 50_000;

    private final JTextArea commandArea = new JTextArea(5, 72);
    private final JTextField previewField = new JTextField();
    private final OutcomeChartPanel chartPanel = new OutcomeChartPanel();
    private final RunHistory history = new RunHistory(MAX_CHART_SAMPLES);
    private final JSpinner runCountSpinner =
            new JSpinner(new SpinnerNumberModel(100, 1, 1_000_000, 1));
    private final JCheckBox untilStopped = new JCheckBox("Run until Stop");
    private final JButton startButton = new JButton("Start");
    private final JButton stopButton = new JButton("Stop");
    private final JLabel statusLabel = new JLabel(" ");

    private volatile SwingWorker<Void, RunOutcome> worker;
    private final AtomicReference<Process> currentProcess = new AtomicReference<>();

    public CommandLinePasteFrame() {
        super("Flaky run checker");

        commandArea.setLineWrap(true);
        commandArea.setWrapStyleWord(true);
        Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 13);
        commandArea.setFont(mono);

        previewField.setEditable(false);
        previewField.setFont(mono);

        stopButton.setEnabled(false);

        JLabel hint = new JLabel("Paste a shell command (e.g. gradle test). Exit code 0 = pass, non-zero = fail.");
        hint.setBorder(new EmptyBorder(0, 0, 8, 0));

        JPanel cmdPanel = new JPanel(new BorderLayout());
        cmdPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
        cmdPanel.add(hint, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(commandArea);
        scroll.setPreferredSize(new Dimension(720, 110));
        cmdPanel.add(scroll, BorderLayout.CENTER);

        JPanel cmdButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton clear = new JButton("Clear");
        clear.addActionListener(ev -> {
            commandArea.setText("");
            updatePreview();
        });
        cmdButtons.add(clear);
        cmdPanel.add(cmdButtons, BorderLayout.SOUTH);

        JPanel runRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        runRow.setBorder(BorderFactory.createEmptyBorder(0, 12, 4, 12));
        runRow.add(startButton);
        runRow.add(stopButton);
        runRow.add(new JLabel("Target runs:"));
        runRow.add(runCountSpinner);
        runRow.add(untilStopped);

        JPanel statusRow = new JPanel(new BorderLayout());
        statusRow.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
        statusRow.add(statusLabel, BorderLayout.WEST);

        JPanel north = new JPanel(new BorderLayout());
        north.add(cmdPanel, BorderLayout.NORTH);
        Box runBox = Box.createVerticalBox();
        runBox.add(runRow);
        runBox.add(statusRow);
        north.add(runBox, BorderLayout.SOUTH);

        JPanel chartWrap = new JPanel(new BorderLayout());
        chartWrap.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
        chartWrap.add(chartPanel, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        south.add(new JLabel("Parsed tokens:"), BorderLayout.NORTH);
        south.add(previewField, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(north, BorderLayout.NORTH);
        add(chartWrap, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        commandArea
                .getDocument()
                .addDocumentListener(
                        new javax.swing.event.DocumentListener() {
                            @Override
                            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                                updatePreview();
                            }

                            @Override
                            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                                updatePreview();
                            }

                            @Override
                            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                                updatePreview();
                            }
                        });

        startButton.addActionListener(ev -> startRuns());
        stopButton.addActionListener(ev -> stopRuns());

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(760, 520));
        setVisible(true);

        updatePreview();
        setStatus("Idle.");
    }

    private void startRuns() {
        List<String> tokens = SimpleCommandLineSplitter.split(commandArea.getText());
        if (tokens.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a non-empty command.", "Nothing to run", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (worker != null && !worker.isDone()) {
            return;
        }

        Path cwd = Path.of("").toAbsolutePath().normalize();
        boolean infinite = untilStopped.isSelected();
        int target = infinite ? Integer.MAX_VALUE : ((Number) runCountSpinner.getValue()).intValue();

        history.clear();
        chartPanel.setOutcomes(history.snapshot());
        currentProcess.set(null);

        startButton.setEnabled(false);
        stopButton.setEnabled(true);

        worker =
                new SwingWorker<>() {
                    private int completed;

                    @Override
                    protected Void doInBackground() {
                        try {
                            while (!isCancelled() && completed < target) {
                                int exit;
                                long durationMs;
                                long startNs = System.nanoTime();
                                try {
                                    exit =
                                            CommandRunner.run(
                                                    tokens,
                                                    cwd,
                                                    proc -> {
                                                        if (isCancelled()) {
                                                            proc.destroyForcibly();
                                                        } else {
                                                            currentProcess.set(proc);
                                                        }
                                                    });
                                } finally {
                                    currentProcess.set(null);
                                    durationMs = Math.max(0L, (System.nanoTime() - startNs) / 1_000_000L);
                                }
                                if (isCancelled()) {
                                    break;
                                }
                                boolean pass = exit == 0;
                                publish(new RunOutcome(pass, durationMs));
                                completed++;
                            }
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        } catch (Exception ex) {
                            SwingUtilities.invokeLater(
                                    () ->
                                            JOptionPane.showMessageDialog(
                                                    CommandLinePasteFrame.this,
                                                    ex.getMessage() != null ? ex.getMessage() : ex.toString(),
                                                    "Run failed",
                                                    JOptionPane.ERROR_MESSAGE));
                        }
                        return null;
                    }

                    @Override
                    protected void process(List<RunOutcome> chunks) {
                        for (RunOutcome row : chunks) {
                            history.record(row.success(), row.durationMs());
                        }
                        chartPanel.setOutcomes(history.snapshot());
                        setStatus(
                                "Running… completed "
                                        + history.totalRuns()
                                        + (infinite ? "" : (" / " + target))
                                        + ".");
                    }

                    @Override
                    protected void done() {
                        currentProcess.set(null);
                        startButton.setEnabled(true);
                        stopButton.setEnabled(false);
                        chartPanel.setOutcomes(history.snapshot());
                        setStatus(
                                "Finished. Total runs: "
                                        + history.totalRuns()
                                        + ", pass: "
                                        + history.successCount()
                                        + ", fail: "
                                        + history.failureCount()
                                        + ".");
                    }
                };
        worker.execute();
        setStatus(infinite ? "Running until Stop…" : ("Running " + target + " times…"));
    }

    private void stopRuns() {
        SwingWorker<?, ?> w = worker;
        if (w != null) {
            w.cancel(true);
        }
        Process p = currentProcess.getAndSet(null);
        if (p != null) {
            p.destroyForcibly();
        }
        setStatus("Stopping…");
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    private void updatePreview() {
        List<String> tokens = SimpleCommandLineSplitter.split(commandArea.getText());
        previewField.setText(
                tokens.stream().map(CommandLinePasteFrame::quoteForDisplay).collect(Collectors.joining(" ")));
    }

    private static String quoteForDisplay(String token) {
        if (token.indexOf(' ') >= 0 || token.indexOf('"') >= 0) {
            return '"' + token.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
        }
        return token;
    }
}
