package info.thelaboflieven.run;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Runs an external process from a token list (as from {@link info.thelaboflieven.cli.SimpleCommandLineSplitter}).
 * Merged stdout/stderr are captured (bounded) for inspection after each run.
 */
public final class CommandRunner {

    /** Merge limit so huge test logs do not exhaust memory. */
    static final int MAX_CAPTURE_BYTES = 256 * 1024;

    private CommandRunner() {}

    /**
     * @param mergedOutput merged stdout and stderr, decoded with {@link Charset#defaultCharset()} (console encoding).
     */
    public record ProcessRunResult(int exitCode, String mergedOutput) {}

    /**
     * Starts the process, optionally notifies {@code onStarted} with it (for cancellation), drains output in the
     * background (so pipes cannot fill), then waits for exit.
     */
    public static ProcessRunResult run(List<String> command, Path workingDirectory, Consumer<Process> onStarted)
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
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        Process process = pb.start();
        if (onStarted != null) {
            onStarted.accept(process);
        }
        CompletableFuture<byte[]> drain =
                CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                return readStreamLimited(process.getInputStream(), MAX_CAPTURE_BYTES);
                            } catch (IOException e) {
                                return new byte[0];
                            }
                        });
        try {
            int exitCode = process.waitFor();
            byte[] raw = drain.get();
            Charset charset = Charset.defaultCharset();
            if (exitCode == 0) {
                return new ProcessRunResult(0, "");
            }
            return new ProcessRunResult(exitCode, new String(raw, charset));
        } catch (ExecutionException e) {
            Throwable c = e.getCause();
            if (c instanceof RuntimeException re) {
                throw re;
            }
            throw new IOException(c != null ? c : e);
        }
    }

    static byte[] readStreamLimited(InputStream in, int maxBytes) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(Math.min(maxBytes, 16384));
        byte[] chunk = new byte[8192];
        int total = 0;
        while (total < maxBytes) {
            int r = in.read(chunk);
            if (r == -1) {
                break;
            }
            int n = Math.min(r, maxBytes - total);
            buf.write(chunk, 0, n);
            total += n;
        }
        return buf.toByteArray();
    }
}
