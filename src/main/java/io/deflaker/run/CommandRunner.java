package io.deflaker.run;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Runs an external process from a token list (as from {@link io.deflaker.cli.SimpleCommandLineSplitter}).
 */
public final class CommandRunner {

    private CommandRunner() {}

    /**
     * Starts the process, optionally notifies {@code onStarted} with it (for cancellation), then waits for exit.
     *
     * @return process exit code
     */
    public static int run(List<String> command, Path workingDirectory, Consumer<Process> onStarted)
            throws IOException, InterruptedException {
        Objects.requireNonNull(command);
        if (command.isEmpty()) {
            throw new IllegalArgumentException("command must not be empty");
        }
        ProcessBuilder pb = new ProcessBuilder(command);
        if (workingDirectory != null) {
            pb.directory(workingDirectory.toFile());
        }
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        Process process = pb.start();
        if (onStarted != null) {
            onStarted.accept(process);
        }
        return process.waitFor();
    }
}
