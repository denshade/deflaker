package info.thelaboflieven.run;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RunHistoryTest {

    @Test
    void recordsAndCounts() {
        RunHistory h = new RunHistory(10_000);
        h.record(new RunOutcome(true, 10, 0, null));
        h.record(new RunOutcome(false, 20, 1, "err"));
        h.record(new RunOutcome(true, 15, 0, null));
        assertEquals(3, h.totalRuns());
        assertEquals(2, h.successCount());
        assertEquals(1, h.failureCount());
        assertEquals(
                List.of(
                        new RunOutcome(true, 10, 0, null),
                        new RunOutcome(false, 20, 1, "err"),
                        new RunOutcome(true, 15, 0, null)),
                h.snapshot());
    }

    @Test
    void dropsOldestWhenOverMax() {
        RunHistory h = new RunHistory(3);
        h.record(new RunOutcome(true, 1, 0, null));
        h.record(new RunOutcome(false, 2, 1, null));
        h.record(new RunOutcome(true, 3, 0, null));
        h.record(new RunOutcome(false, 4, 2, "x"));
        assertEquals(
                List.of(new RunOutcome(false, 2, 1, null), new RunOutcome(true, 3, 0, null), new RunOutcome(false, 4, 2, "x")),
                h.snapshot());
        assertEquals(3, h.totalRuns());
    }

    @Test
    void clear() {
        RunHistory h = new RunHistory(100);
        h.record(new RunOutcome(true, 5, 0, null));
        h.clear();
        assertEquals(0, h.totalRuns());
        assertEquals(List.of(), h.snapshot());
    }

    @Test
    void maxRunsMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new RunHistory(0));
    }
}
