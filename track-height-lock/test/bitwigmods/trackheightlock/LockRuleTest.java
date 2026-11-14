package bitwigmods.trackheightlock;

/**
 * Small tests for LockRule without any test library.
 * Run: sh tools/test-java.sh track-height-lock   (prints LOCKRULE_TESTS_OK or fails)
 */
public final class LockRuleTest {
    private static int failures = 0;

    public static void main(String[] args) {
        check("plain name", !LockRule.isLocked("Bass", null));
        check("[L] in name", LockRule.isLocked("[L] Bass", null));
        check("[l] lower case", LockRule.isLocked("Bass [l]", ""));
        check("lock emoji", LockRule.isLocked("🔒 Drums", null));
        check("#lock in comment", LockRule.isLocked("Bass", "keep this #lock please"));
        check("#LOCK upper case", LockRule.isLocked("Bass", "#LOCK"));
        check("other comment", !LockRule.isLocked("Bass", "locked? no tag here"));
        check("L without brackets", !LockRule.isLocked("Lead", "lock"));
        check("both null", !LockRule.isLocked(null, null));

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
}
