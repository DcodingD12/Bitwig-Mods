package bitwigmods.trackheightlock;

/**
 * Small tests for LockRule without any test library.
 * Run: sh tools/test-java.sh track-height-lock   (prints LOCKRULE_TESTS_OK or fails)
 */
public final class LockRuleTest {
    private static int failures = 0;

    public static void main(String[] args) {
        // Is it locked?
        check("plain name", !LockRule.isLocked("Bass", null));
        check("[L] in name", LockRule.isLocked("[L] Bass", null));
        check("[l] lower case", LockRule.isLocked("Bass [l]", ""));
        check("lock emoji", LockRule.isLocked("🔒 Drums", null));
        check("#lock in comment", LockRule.isLocked("Bass", "keep this #lock please"));
        check("#LOCK upper case", LockRule.isLocked("Bass", "#LOCK"));
        check("#lock at the end", LockRule.isLocked("Bass", "note #lock"));
        check("#lock. with dot", LockRule.isLocked("Bass", "#lock."));
        check("#locked is not the tag", !LockRule.isLocked("Bass", "#locked"));
        check("#lock2 is not the tag", !LockRule.isLocked("Bass", "#lock2"));
        check("other comment", !LockRule.isLocked("Bass", "locked? no tag here"));
        check("L without brackets", !LockRule.isLocked("Lead", "lock"));
        check("both null", !LockRule.isLocked(null, null));

        // Adding the tag
        same("add to empty", LockRule.withTag(""), "#lock");
        same("add to null", LockRule.withTag(null), "#lock");
        same("add to text", LockRule.withTag("Vocals take 3"), "Vocals take 3 #lock");
        same("add twice", LockRule.withTag("x #lock"), "x #lock");
        same("add keeps #locked", LockRule.withTag("#locked"), "#locked #lock");

        // Removing the tag: the rest stays exactly as it was
        same("remove alone", LockRule.withoutTag("#lock"), "");
        same("remove at end", LockRule.withoutTag("Vocals take 3 #lock"), "Vocals take 3");
        same("remove at start", LockRule.withoutTag("#lock Vocals"), "Vocals");
        same("remove in middle", LockRule.withoutTag("a #lock b"), "a b");
        same("remove upper case", LockRule.withoutTag("a #LOCK"), "a");
        same("remove keeps #locked", LockRule.withoutTag("#locked #lock"), "#locked");
        same("remove all tags", LockRule.withoutTag("#lock a #lock"), "a");
        same("remove keeps line breaks", LockRule.withoutTag("line1\nline2 #lock"), "line1\nline2");
        same("nothing to remove", LockRule.withoutTag("  spaced  "), "  spaced  ");
        same("round trip", LockRule.withoutTag(LockRule.withTag("Bass  line")), "Bass  line");

        if (failures > 0) {
            System.out.println("LOCKRULE_TESTS_FAILED " + failures);
            System.exit(1);
        }
        System.out.println("LOCKRULE_TESTS_OK");
    }

    private static void check(String what, boolean ok) {
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + what);
        }
    }

    private static void same(String what, String actual, String expected) {
        if (!expected.equals(actual)) {
            failures++;
            System.out.println("FAIL: " + what + ": got [" + actual + "], expected [" + expected + "]");
        }
    }
}
