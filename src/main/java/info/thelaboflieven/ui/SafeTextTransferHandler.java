package info.thelaboflieven.ui;

import javax.swing.TransferHandler;
import javax.swing.text.JTextComponent;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.Reader;

/**
 * Paste/drop handler that only accepts plain text flavors. Copy/paste from IntelliJ can put
 * JVM-local flavors ({@code application/x-java-jvm-local-objectref}) on the clipboard; resolving those loads IDE
 * classes like {@code FoldingData} and causes {@link ClassNotFoundException} in an external app. This handler avoids
 * touching those flavors.
 */
public final class SafeTextTransferHandler extends TransferHandler {

    private static final DataFlavor UNICODE_TEXT = DataFlavor.getTextPlainUnicodeFlavor();

    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDataFlavorSupported(DataFlavor.stringFlavor)
                || support.isDataFlavorSupported(UNICODE_TEXT);
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) {
            return false;
        }
        JTextComponent tc = (JTextComponent) support.getComponent();
        Transferable t = support.getTransferable();
        try {
            if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String data = (String) t.getTransferData(DataFlavor.stringFlavor);
                tc.replaceSelection(data);
                return true;
            }
            if (support.isDataFlavorSupported(UNICODE_TEXT)) {
                try (Reader r = UNICODE_TEXT.getReaderForText(t)) {
                    StringBuilder sb = new StringBuilder();
                    char[] buf = new char[4096];
                    int n;
                    while ((n = r.read(buf)) != -1) {
                        sb.append(buf, 0, n);
                    }
                    tc.replaceSelection(sb.toString());
                    return true;
                }
            }
        } catch (UnsupportedFlavorException | IOException e) {
            return false;
        }
        return false;
    }
}
