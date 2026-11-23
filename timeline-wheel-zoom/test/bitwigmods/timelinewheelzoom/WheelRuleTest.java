package bitwigmods.timelinewheelzoom;

/**
 * Small tests for WheelRule without any test library.
 * Run: sh tools/test-java.sh timeline-wheel-zoom   (prints WHEELRULE_TESTS_OK or fails)
 */
public final class WheelRuleTest {
    private static int failures = 0;

    public static void main(String[] args) {
        check("plain wheel over the arranger ruler zooms", WheelRule.shouldZoom(true, false, true));
        check("horizontal swipe keeps Bitwig's scrolling", !WheelRule.shouldZoom(false, false, true));
        check("wheel with a modifier keeps Bitwig's behaviour", !WheelRule.shouldZoom(true, true, true));
        check("ruler outside the arranger (detail editor) is left alone", !WheelRule.shouldZoom(true, false, false));
        check("horizontal swipe with modifier is left alone", !WheelRule.shouldZoom(false, true, true));

        if (failures > 0) {
            throw new AssertionError(failures + " test(s) failed");
        }
        System.out.println("WHEELRULE_TESTS_OK");
    }

    private static void check(String name, boolean ok) {
        if (!ok) {
            failures++;
            System.out.println("FAILED: " + name);
        }
    }
}
