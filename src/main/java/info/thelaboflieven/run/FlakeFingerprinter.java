package info.thelaboflieven.run;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * Builds stable identities for failure output so the same underlying flake groups together even when
 * timestamps or durations differ.
 */
public final class FlakeFingerprinter {

    private static final Pattern ISO_TIMESTAMP =
            Pattern.compile("\\d{4}-\\d{2}-\\d{2}[T\\s]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:?\\d{2})?");
    private static final Pattern DURATION =
            Pattern.compile("\\b\\d+(?:\\.\\d+)?\\s*(?:ms|milliseconds|s|sec|seconds|m|min|minutes)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern BUILD_TIMING =
            Pattern.compile("BUILD (?:FAILED|SUCCESSFUL) in \\d+(?:\\.\\d+)?s", Pattern.CASE_INSENSITIVE);
    private static final Pattern GRADLE_TASK_LINE = Pattern.compile("^> Task :.*$");
    private static final Pattern STACK_LINE_NUMBER = Pattern.compile("(\\(\\w+\\.java:)\\d+(\\))");

    private FlakeFingerprinter() {}

    /** Fingerprint for a failed run; uses exit code when there is no captured text. */
    public static String fingerprint(int exitCode, String capturedOutput) {
        if (capturedOutput == null || capturedOutput.isBlank()) {
            return "exit:" + exitCode;
        }
        return hash(normalize(capturedOutput));
    }

    /** Short human label for tables and tooltips. */
    public static String label(int exitCode, String capturedOutput) {
        if (capturedOutput == null || capturedOutput.isBlank()) {
            return "(no output, exit " + exitCode + ")";
        }
        String normalized = normalize(capturedOutput);
        for (String line : normalized.split("\n")) {
            String t = line.trim();
            if (!t.isEmpty() && !GRADLE_TASK_LINE.matcher(t).matches()) {
                if (t.length() > 96) {
                    return t.substring(0, 96) + "…";
                }
                return t;
            }
        }
        String oneLine = normalized.replace('\n', ' ').trim();
        if (oneLine.isEmpty()) {
            return "(empty after normalize, exit " + exitCode + ")";
        }
        return oneLine.length() > 96 ? oneLine.substring(0, 96) + "…" : oneLine;
    }

    /** Normalized text used for grouping (visible in tests). */
    public static String normalize(String capturedOutput) {
        if (capturedOutput == null || capturedOutput.isBlank()) {
            return "";
        }
        String unified = capturedOutput.replace("\r\n", "\n").replace('\r', '\n').trim();
        StringBuilder sb = new StringBuilder();
        for (String rawLine : unified.split("\n")) {
            String line = rawLine.stripTrailing();
            if (line.isBlank()) {
                continue;
            }
            line = ISO_TIMESTAMP.matcher(line).replaceAll("<timestamp>");
            line = DURATION.matcher(line).replaceAll("<duration>");
            line = BUILD_TIMING.matcher(line).replaceAll("BUILD <timing>");
            line = STACK_LINE_NUMBER.matcher(line).replaceAll("$1#$2");
            sb.append(line).append('\n');
        }
        return sb.toString().trim();
    }

    private static String hash(String normalized) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
