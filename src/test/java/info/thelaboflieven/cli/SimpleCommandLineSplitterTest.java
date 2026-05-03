package info.thelaboflieven.cli;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleCommandLineSplitterTest {

    @Test
    void emptyAndBlankYieldNoTokens() {
        assertTrue(SimpleCommandLineSplitter.split(null).isEmpty());
        assertTrue(SimpleCommandLineSplitter.split("").isEmpty());
        assertTrue(SimpleCommandLineSplitter.split("   \t  ").isEmpty());
    }

    @Test
    void splitsOnWhitespace() {
        assertEquals(List.of("git", "status"), SimpleCommandLineSplitter.split("  git   status  "));
    }

    @Test
    void doubleQuotedSegmentIsSingleToken() {
        assertEquals(
                List.of("echo", "hello world"),
                SimpleCommandLineSplitter.split("echo \"hello world\""));
    }

    @Test
    void quotesAreStrippedFromTokens() {
        assertEquals(List.of("a", "b"), SimpleCommandLineSplitter.split("\"a\" b"));
    }
}
