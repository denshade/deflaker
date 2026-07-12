package info.thelaboflieven.run;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandRunnerTest {

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void windowsExitCodePropagates() throws Exception {
        assertEquals(0, CommandRunner.run(List.of("cmd", "/c", "exit", "0"), null, null).exitCode());
        assertEquals(42, CommandRunner.run(List.of("cmd", "/c", "exit", "42"), null, null).exitCode());
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void unixExitCodePropagates() throws Exception {
        assertEquals(0, CommandRunner.run(List.of("sh", "-c", "exit 0"), null, null).exitCode());
        assertEquals(17, CommandRunner.run(List.of("sh", "-c", "exit 17"), null, null).exitCode());
    }

    @Test
    void javaVersionExitsZero() throws Exception {
        Path javaExe =
                Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java");
        CommandRunner.ProcessRunResult r = CommandRunner.run(List.of(javaExe.toString(), "-version"), null, null);
        assertEquals(0, r.exitCode());
        assertEquals("", r.mergedOutput());
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void failureIsNonZero() throws Exception {
        int code = CommandRunner.run(List.of("cmd", "/c", "exit", "1"), null, null).exitCode();
        assertNotEquals(0, code);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void capturesStdoutStderrOnFailure() throws Exception {
        CommandRunner.ProcessRunResult r =
                CommandRunner.run(List.of("cmd", "/c", "echo flaky-marker& exit 9"), null, null);
        assertEquals(9, r.exitCode());
        assertTrue(r.mergedOutput().contains("flaky-marker"));
    }

    @Test
    void readStreamLimitedTruncates() throws Exception {
        byte[] huge = new byte[CommandRunner.MAX_CAPTURE_BYTES + 1000];
        Arrays.fill(huge, (byte) 'x');
        byte[] got = CommandRunner.readStreamLimited(new ByteArrayInputStream(huge), CommandRunner.MAX_CAPTURE_BYTES);
        assertEquals(CommandRunner.MAX_CAPTURE_BYTES, got.length);
    }
}
