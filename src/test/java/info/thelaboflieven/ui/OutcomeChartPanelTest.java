package info.thelaboflieven.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutcomeChartPanelTest {

    @Test
    void formatDurationMillisecondsAndSeconds() {
        assertEquals("0 ms", OutcomeChartPanel.formatDuration(0));
        assertEquals("42 ms", OutcomeChartPanel.formatDuration(42));
        assertEquals("999 ms", OutcomeChartPanel.formatDuration(999));
        assertEquals("1.00 s", OutcomeChartPanel.formatDuration(1000));
        assertEquals("1.50 s", OutcomeChartPanel.formatDuration(1500));
    }
}
