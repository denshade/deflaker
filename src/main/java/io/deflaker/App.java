package io.deflaker;

import io.deflaker.ui.CommandLinePasteFrame;

import javax.swing.SwingUtilities;

public final class App {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CommandLinePasteFrame::new);
    }
}