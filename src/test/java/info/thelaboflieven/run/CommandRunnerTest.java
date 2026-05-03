package info.thelaboflieven.run;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CommandRunnerTest {

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void windowsExitCodePropagates() throws Exception {
        assertEquals(0, CommandRunner.run(List.of("cmd", "/c", "exit", "0"), null, null));
        assertEquals(42, CommandRunner.run(List.of("cmd", "/c", "exit", "42"), null, null));
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void unixExitCodePropagates() throws Exception {
        assertEquals(0, CommandRunner.run(List.of("sh", "-c", "exit 0"), null, null));
        assertEquals(17, CommandRunner.run(List.of("sh", "-c", "exit 17"), null, null));
    }

    @Test
    void javaVersionExitsZero() throws Exception {
        Path javaExe =
                Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java");
        int code = CommandRunner.run(List.of(javaExe.toString(), "-version"), null, null);
        assertEquals(0, code);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void failureIsNonZero() throws Exception {
        int code = CommandRunner.run(List.of("cmd", "/c", "exit", "1"), null, null);
        assertNotEquals(0, code);
    }
}
