package bitwigmods.timelinewheelzoom;

import bitwigmods.timelinewheelzoom.WheelRule.Place;

/**
 * Small tests for WheelRule without any test library.
 * Run: sh tools/test-java.sh timeline-wheel-zoom   (prints WHEELRULE_TESTS_OK or fails)
 */
public final class WheelRuleTest {
    private static int failures = 0;

    public static void main(String[] args) {
        // Arranger (unchanged behaviour, also with the detail editor part switched off).
        check("plain wheel over the arranger ruler zooms", WheelRule.shouldZoom(true, false, Place.ARRANGER, true));
        check("arranger zooms even with the detail editor part off", WheelRule.shouldZoom(true, false, Place.ARRANGER, false));
        check("horizontal swipe keeps Bitwig's scrolling", !WheelRule.shouldZoom(false, false, Place.ARRANGER, true));
        check("wheel with a modifier keeps Bitwig's behaviour", !WheelRule.shouldZoom(true, true, Place.ARRANGER, true));
        check("horizontal swipe with modifier is left alone", !WheelRule.shouldZoom(false, true, Place.ARRANGER, true));

        // Detail editor (piano roll).
        check("plain wheel over the detail editor ruler zooms", WheelRule.shouldZoom(true, false, Place.DETAIL_EDITOR, true));
        check("detail editor part switched off: left alone", !WheelRule.shouldZoom(true, false, Place.DETAIL_EDITOR, false));
        check("detail editor: modifier keeps Bitwig's behaviour", !WheelRule.shouldZoom(true, true, Place.DETAIL_EDITOR, true));
        check("detail editor: horizontal swipe keeps Bitwig's scrolling", !WheelRule.shouldZoom(false, false, Place.DETAIL_EDITOR, true));

        // Unknown places.
        check("ruler somewhere else is left alone", !WheelRule.shouldZoom(true, false, Place.OTHER, true));
        check("no place known is left alone", !WheelRule.shouldZoom(true, false, null, true));

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
