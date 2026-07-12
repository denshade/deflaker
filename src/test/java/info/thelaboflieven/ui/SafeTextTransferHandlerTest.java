package info.thelaboflieven.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JTextArea;
import javax.swing.TransferHandler;
import java.awt.datatransfer.StringSelection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeTextTransferHandlerTest {

    @Test
    void acceptsStringSelection() {
        JTextArea area = new JTextArea();
        SafeTextTransferHandler h = new SafeTextTransferHandler();
        StringSelection sel = new StringSelection("gradle test");
        TransferHandler.TransferSupport support = new TransferHandler.TransferSupport(area, sel);
        assertTrue(h.canImport(support));
    }

    @Test
    void importsStringSelection() {
        JTextArea area = new JTextArea();
        area.setTransferHandler(new SafeTextTransferHandler());
        StringSelection sel = new StringSelection("hello");
        TransferHandler.TransferSupport support = new TransferHandler.TransferSupport(area, sel);
        assertTrue(area.getTransferHandler().importData(support));
        assertEquals("hello", area.getText());
    }
}
