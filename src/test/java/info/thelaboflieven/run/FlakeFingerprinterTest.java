package info.thelaboflieven.run;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FlakeFingerprinterTest {

    @Test
    void sameFailureDifferentDurationGroupsTogether() {
        String a = "FAILED MyTest\nAssertionError: expected 1 but was 2\nBUILD FAILED in 3s";
        String b = "FAILED MyTest\nAssertionError: expected 1 but was 2\nBUILD FAILED in 9s";
        assertEquals(FlakeFingerprinter.fingerprint(1, a), FlakeFingerprinter.fingerprint(1, b));
    }

    @Test
    void differentAssertionIsDifferentFlake() {
        String a = "FAILED MyTest\nAssertionError: expected 1 but was 2";
        String b = "FAILED MyTest\nAssertionError: expected 1 but was 3";
        assertNotEquals(FlakeFingerprinter.fingerprint(1, a), FlakeFingerprinter.fingerprint(1, b));
    }

    @Test
    void emptyOutputUsesExitCode() {
        assertEquals("exit:7", FlakeFingerprinter.fingerprint(7, null));
        assertEquals("exit:7", FlakeFingerprinter.fingerprint(7, "   "));
    }

    @Test
    void labelUsesFirstMeaningfulLine() {
        String out = "> Task :test\nFAILED com.example.MyTest\njava.lang.AssertionError";
        assertEquals("FAILED com.example.MyTest", FlakeFingerprinter.label(1, out));
    }
}
