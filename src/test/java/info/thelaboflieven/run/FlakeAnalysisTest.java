package info.thelaboflieven.run;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlakeAnalysisTest {

    @Test
    void groupsFailuresAndComputesRates() {
        String flakeA = "FAILED TestA\nAssertionError: boom";
        String flakeB = "FAILED TestB\nAssertionError: other";
        List<RunOutcome> outcomes =
                List.of(
                        new RunOutcome(true, 10, 0, null),
                        new RunOutcome(false, 20, 1, flakeA),
                        new RunOutcome(false, 30, 1, flakeA),
                        new RunOutcome(false, 40, 1, flakeB),
                        new RunOutcome(true, 50, 0, null));

        FlakeAnalysis analysis = FlakeAnalysis.fromOutcomes(outcomes);
        assertEquals(2, analysis.flakes().size());

        FlakeSummary top = analysis.flakes().get(0);
        assertEquals(2, top.count());
        assertEquals("FAILED TestA", top.label());
        assertEquals(66.7, top.percentOfFailures(), 0.1);
        assertEquals(40.0, top.percentOfRuns(), 0.1);
        assertEquals(List.of(2, 3), top.runNumbers());

        assertTrue(analysis.runIndexToFingerprint().containsKey(1));
        assertEquals(
                analysis.runIndexToFingerprint().get(1),
                analysis.runIndexToFingerprint().get(2));
    }
}
