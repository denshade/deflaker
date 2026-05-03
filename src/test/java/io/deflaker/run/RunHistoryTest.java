package io.deflaker.run;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RunHistoryTest {

    @Test
    void recordsAndCounts() {
        RunHistory h = new RunHistory(10_000);
        h.record(true, 10);
        h.record(false, 20);
        h.record(true, 15);
        assertEquals(3, h.totalRuns());
        assertEquals(2, h.successCount());
        assertEquals(1, h.failureCount());
        assertEquals(
                List.of(new RunOutcome(true, 10), new RunOutcome(false, 20), new RunOutcome(true, 15)),
                h.snapshot());
    }

    @Test
    void dropsOldestWhenOverMax() {
        RunHistory h = new RunHistory(3);
        h.record(true, 1);
        h.record(false, 2);
        h.record(true, 3);
        h.record(false, 4);
        assertEquals(List.of(new RunOutcome(false, 2), new RunOutcome(true, 3), new RunOutcome(false, 4)), h.snapshot());
        assertEquals(3, h.totalRuns());
    }

    @Test
    void clear() {
        RunHistory h = new RunHistory(100);
        h.record(true, 5);
        h.clear();
        assertEquals(0, h.totalRuns());
        assertEquals(List.of(), h.snapshot());
    }

    @Test
    void maxRunsMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new RunHistory(0));
    }
}
