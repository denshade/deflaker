package info.thelaboflieven.run;

import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe record of outcomes (pass = exit code 0) and how long each run took.
 */
public final class RunHistory {

    private final List<RunOutcome> outcomes = new ArrayList<>();
    private final int maxRuns;

    public RunHistory(int maxRuns) {
        if (maxRuns < 1) {
            throw new IllegalArgumentException("maxRuns must be positive");
        }
        this.maxRuns = maxRuns;
    }

    public synchronized void record(boolean success, long durationMs) {
        outcomes.add(new RunOutcome(success, durationMs));
        while (outcomes.size() > maxRuns) {
            outcomes.remove(0);
        }
    }

    public synchronized void clear() {
        outcomes.clear();
    }

    public synchronized List<RunOutcome> snapshot() {
        return List.copyOf(outcomes);
    }

    public synchronized int successCount() {
        return (int) outcomes.stream().filter(RunOutcome::success).count();
    }

    public synchronized int failureCount() {
        return (int) outcomes.stream().filter(o -> !o.success()).count();
    }

    public synchronized int totalRuns() {
        return outcomes.size();
    }
}
