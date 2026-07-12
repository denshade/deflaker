package info.thelaboflieven.run;

import java.util.List;

/** One distinct failure pattern and how often it appeared. */
public record FlakeSummary(
        String fingerprint,
        String label,
        int count,
        int totalFailures,
        int totalRuns,
        List<Integer> runNumbers) {

    public double percentOfFailures() {
        return totalFailures == 0 ? 0.0 : 100.0 * count / totalFailures;
    }

    public double percentOfRuns() {
        return totalRuns == 0 ? 0.0 : 100.0 * count / totalRuns;
    }
}
