package io.deflaker.run;

/** One completed run: pass/fail and wall-clock duration of the process. */
public record RunOutcome(boolean success, long durationMs) {}
