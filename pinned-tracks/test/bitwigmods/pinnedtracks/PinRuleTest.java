package bitwigmods.pinnedtracks;

/**
 * Small tests for PinRule without any test library.
 * Run: sh tools/test-java.sh pinned-tracks   (prints PINRULE_TESTS_OK or fails)
 */
public final class PinRuleTest {
    private static int failures = 0;

    public static void main(String[] args) {
        // Is it pinned?
        check("null", !PinRule.isPinned(null));
        check("empty", !PinRule.isPinned(""));
        check("#pin alone", PinRule.isPinned("#pin"));
        check("#pin in text", PinRule.isPinned("lead vocal #pin take 3"));
        check("#PIN upper case", PinRule.isPinned("#PIN"));
        check("#pin. with dot", PinRule.isPinned("#pin."));
        check("#pin next to other tags", PinRule.isPinned("#lock #pin #spacer-before"));
        check("#pinned is not the tag", !PinRule.isPinned("#pinned"));
        check("#pin2 is not the tag", !PinRule.isPinned("#pin2"));
        check("#pin_x is not the tag", !PinRule.isPinned("#pin_x"));
        check("pin without # is not the tag", !PinRule.isPinned("pin"));

        // Adding
        same("add to null", PinRule.withTag(null), "#pin");
        same("add to empty", PinRule.withTag(""), "#pin");
        same("add to text", PinRule.withTag("Vocals"), "Vocals #pin");
        same("add twice", PinRule.withTag("x #pin"), "x #pin");
        same("add keeps #pinned", PinRule.withTag("#pinned"), "#pinned #pin");

        // Removing: the rest stays exactly as it was
        same("remove from null", PinRule.withoutTag(null), "");
        same("remove alone", PinRule.withoutTag("#pin"), "");
        same("remove at end", PinRule.withoutTag("Vocals #pin"), "Vocals");
        same("remove at start", PinRule.withoutTag("#pin Vocals"), "Vocals");
        same("remove in middle", PinRule.withoutTag("a #pin b"), "a b");
        same("remove upper case", PinRule.withoutTag("a #PIN"), "a");
        same("remove keeps #pinned", PinRule.withoutTag("#pinned #pin"), "#pinned");
        same("remove all", PinRule.withoutTag("#pin a #pin"), "a");
        same("remove keeps other tags", PinRule.withoutTag("#lock #pin #spacer-after=20"), "#lock #spacer-after=20");
        same("nothing to remove", PinRule.withoutTag("  spaced  "), "  spaced  ");
        same("round trip", PinRule.withoutTag(PinRule.withTag("Bass  line")), "Bass  line");

        if (failures > 0) {
            System.out.println("PINRULE_TESTS_FAILED " + failures);
            System.exit(1);
        }
        System.out.println("PINRULE_TESTS_OK");
    }

    private static void check(String name, boolean ok) {
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + name);
        }
    }

    private static void same(String name, String actual, String expected) {
        if (!expected.equals(actual)) {
            failures++;
            System.out.println("FAIL: " + name + ": expected [" + expected + "] got [" + actual + "]");
        }
    }
}
