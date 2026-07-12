package info.thelaboflieven.run;

/**
 * One completed run: pass/fail, duration, exit code, and captured output for failures (merged stdout/stderr).
 */
public record RunOutcome(boolean success, long durationMs, int exitCode, String capturedOutput) {}
