package info.thelaboflieven.run;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Aggregated view of distinct failure patterns in a run series. */
public record FlakeAnalysis(List<FlakeSummary> flakes, Map<Integer, String> runIndexToFingerprint) {

    public static final FlakeAnalysis EMPTY = new FlakeAnalysis(List.of(), Map.of());

    public static FlakeAnalysis fromOutcomes(List<RunOutcome> outcomes) {
        if (outcomes == null || outcomes.isEmpty()) {
            return EMPTY;
        }
        int totalRuns = outcomes.size();
        Map<String, MutableGroup> groups = new LinkedHashMap<>();
        Map<Integer, String> indexToFp = new LinkedHashMap<>();
        int failureCount = 0;

        for (int i = 0; i < outcomes.size(); i++) {
            RunOutcome o = outcomes.get(i);
            if (o.success()) {
                continue;
            }
            failureCount++;
            String fp = FlakeFingerprinter.fingerprint(o.exitCode(), o.capturedOutput());
            indexToFp.put(i, fp);
            MutableGroup g = groups.computeIfAbsent(fp, k -> new MutableGroup(k, o));
            g.count++;
            g.runNumbers.add(i + 1);
        }

        if (groups.isEmpty()) {
            return EMPTY;
        }

        List<FlakeSummary> summaries = new ArrayList<>();
        for (MutableGroup g : groups.values()) {
            summaries.add(
                    new FlakeSummary(
                            g.fingerprint,
                            FlakeFingerprinter.label(g.sample.exitCode(), g.sample.capturedOutput()),
                            g.count,
                            failureCount,
                            totalRuns,
                            List.copyOf(g.runNumbers)));
        }
        summaries.sort(Comparator.comparingInt(FlakeSummary::count).reversed().thenComparing(FlakeSummary::label));
        return new FlakeAnalysis(List.copyOf(summaries), Map.copyOf(indexToFp));
    }

    public int colorIndex(String fingerprint) {
        for (int i = 0; i < flakes.size(); i++) {
            if (flakes.get(i).fingerprint().equals(fingerprint)) {
                return i;
            }
        }
        return -1;
    }

    private static final class MutableGroup {
        final String fingerprint;
        final RunOutcome sample;
        int count;
        final List<Integer> runNumbers = new ArrayList<>();

        MutableGroup(String fingerprint, RunOutcome sample) {
            this.fingerprint = fingerprint;
            this.sample = sample;
        }
    }
}
