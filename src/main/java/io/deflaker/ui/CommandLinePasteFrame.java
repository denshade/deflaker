package io.deflaker.ui;

import io.deflaker.cli.SimpleCommandLineSplitter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.stream.Collectors;

public final class CommandLinePasteFrame extends JFrame {

    private final JTextArea commandArea = new JTextArea(6, 72);
    private final JTextField previewField = new JTextField();

    public CommandLinePasteFrame() {
        super("Command line");

        commandArea.setLineWrap(true);
        commandArea.setWrapStyleWord(true);
        Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 13);
        commandArea.setFont(mono);

        previewField.setEditable(false);
        previewField.setFont(mono);

        JLabel hint = new JLabel("Paste a command below (tokens update as you type):");
        hint.setBorder(new EmptyBorder(0, 0, 8, 0));

        JPanel north = new JPanel(new BorderLayout());
        north.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
        north.add(hint, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(commandArea);
        scroll.setPreferredSize(new Dimension(720, 140));
        north.add(scroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton clear = new JButton("Clear");
        clear.addActionListener(ev -> {
            commandArea.setText("");
            updatePreview();
        });
        buttons.add(clear);
        north.add(buttons, BorderLayout.SOUTH);

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        south.add(new JLabel("Parsed tokens:"), BorderLayout.NORTH);
        south.add(previewField, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(north, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        javax.swing.event.DocumentListener listener =
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
                };
        commandArea.getDocument().addDocumentListener(listener);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        updatePreview();
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
