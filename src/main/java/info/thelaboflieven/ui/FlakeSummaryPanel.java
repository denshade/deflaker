package info.thelaboflieven.ui;

import info.thelaboflieven.run.FlakeAnalysis;
import info.thelaboflieven.run.FlakeSummary;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Table of unique failure patterns sorted by how often they occur. */
public final class FlakeSummaryPanel extends JPanel {

    private static final String[] COLUMNS = {"", "Fails", "% fails", "% runs", "Pattern", "Run #s"};

    private final DefaultTableModel model =
            new DefaultTableModel(COLUMNS, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }

                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    return columnIndex == 0 ? Color.class : Object.class;
                }
            };

    private final JTable table = new JTable(model);
    private volatile FlakeAnalysis analysis = FlakeAnalysis.EMPTY;
    private Consumer<String> fingerprintListener = fp -> {};

    public FlakeSummaryPanel() {
        super(new BorderLayout());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(28);
        table.getColumnModel().getColumn(0).setMinWidth(28);
        table.getColumnModel().getColumn(1).setMaxWidth(52);
        table.getColumnModel().getColumn(2).setMaxWidth(64);
        table.getColumnModel().getColumn(3).setMaxWidth(64);
        table.getColumnModel().getColumn(0).setCellRenderer(new ColorSwatchRenderer());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            int row = table.getSelectedRow();
            if (row < 0 || row >= analysis.flakes().size()) {
                fingerprintListener.accept(null);
                return;
            }
            fingerprintListener.accept(analysis.flakes().get(row).fingerprint());
        });
        var scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(360, 120));
        add(scroll, BorderLayout.CENTER);
    }

    public void setFingerprintSelectionListener(Consumer<String> listener) {
        this.fingerprintListener = listener != null ? listener : fp -> {};
    }

    public void setAnalysis(FlakeAnalysis analysis) {
        this.analysis = analysis != null ? analysis : FlakeAnalysis.EMPTY;
        model.setRowCount(0);
        int i = 0;
        for (FlakeSummary flake : this.analysis.flakes()) {
            String runs =
                    flake.runNumbers().stream().map(String::valueOf).collect(Collectors.joining(", "));
            if (runs.length() > 48) {
                runs = runs.substring(0, 45) + "…";
            }
            model.addRow(
                    new Object[] {
                        FlakeColors.forIndex(i),
                        flake.count(),
                        String.format(Locale.US, "%.1f", flake.percentOfFailures()),
                        String.format(Locale.US, "%.1f", flake.percentOfRuns()),
                        flake.label(),
                        runs
                    });
            i++;
        }
        table.clearSelection();
    }

    private static final class ColorSwatchRenderer implements TableCellRenderer {
        private final JPanel swatch = new JPanel();

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof Color c) {
                swatch.setBackground(c);
            } else {
                swatch.setBackground(Color.LIGHT_GRAY);
            }
            swatch.setOpaque(true);
            return swatch;
        }
    }
}
