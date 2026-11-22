package bitwigmods.trackspacer;

import java.util.List;

/**
 * Small tests for SpacerRule and SpacerPlan without any test library.
 * Run: sh tools/test-java.sh track-spacer   (prints SPACER_TESTS_OK or fails)
 */
public final class SpacerTest {
    private static int failures = 0;

    public static void main(String[] args) {
        // SpacerRule: reading the gap
        same("no comment", SpacerRule.gapBefore(null), 0);
        same("plain comment", SpacerRule.gapBefore("vocals take 3"), 0);
        same("before default", SpacerRule.gapBefore("#spacer-before"), SpacerRule.DEFAULT_GAP);
        same("after default", SpacerRule.gapAfter("note #spacer-after"), SpacerRule.DEFAULT_GAP);
        same("before is not after", SpacerRule.gapAfter("#spacer-before"), 0);
        same("upper case", SpacerRule.gapBefore("#SPACER-BEFORE"), SpacerRule.DEFAULT_GAP);
        same("own size", SpacerRule.gapBefore("#spacer-before=24"), 24);
        same("own size after", SpacerRule.gapAfter("x #spacer-after=40 y"), 40);
        same("size too big", SpacerRule.gapBefore("#spacer-before=9999"), SpacerRule.MAX_GAP);
        same("size zero", SpacerRule.gapBefore("#spacer-before=0"), 1);
        same("with dot", SpacerRule.gapBefore("#spacer-before."), SpacerRule.DEFAULT_GAP);
        same("glued word", SpacerRule.gapBefore("#spacer-beforehand"), 0);
        same("glued digit", SpacerRule.gapAfter("#spacer-after2"), 0);
        same("broken size", SpacerRule.gapBefore("#spacer-before=x"), 0);
        check("hasAny", SpacerRule.hasAny("a #spacer-after"));
        check("hasAny none", !SpacerRule.hasAny("#spacer"));

        // SpacerRule: writing
        same("add to empty", SpacerRule.withBefore(""), "#spacer-before");
        same("add to null", SpacerRule.withAfter(null), "#spacer-after");
        same("add to text", SpacerRule.withBefore("Bass"), "Bass #spacer-before");
        same("add twice", SpacerRule.withBefore("Bass #spacer-before=30"), "Bass #spacer-before=30");
        same("add after next to before", SpacerRule.withAfter("#spacer-before"), "#spacer-before #spacer-after");
        same("remove before", SpacerRule.withoutBefore("Bass #spacer-before"), "Bass");
        same("remove keeps after", SpacerRule.withoutBefore("#spacer-before #spacer-after"), "#spacer-after");
        same("remove sized", SpacerRule.withoutAfter("a #spacer-after=20 b"), "a b");
        same("remove null", SpacerRule.withoutAfter(null), "");
        same("remove all", SpacerRule.withoutAll("Bass #spacer-before=20 note #spacer-after"), "Bass note");
        same("remove all keeps other tags", SpacerRule.withoutAll("#lock #spacer-after"), "#lock");
        same("remove all, nothing there", SpacerRule.withoutAll("Bass"), "Bass");
        same("remove nothing", SpacerRule.withoutAfter("  spaced  "), "  spaced  ");
        for (String c : new String[] {"", "Bass", "two  spaces", "line1\nline2", "#lock"}) {
            same("round trip before [" + c + "]", SpacerRule.withoutBefore(SpacerRule.withBefore(c)), c);
            same("round trip after [" + c + "]", SpacerRule.withoutAfter(SpacerRule.withAfter(c)), c);
        }

        // SpacerPlan.toTag: tracks 0..4
        boolean[] no = new boolean[5];
        same("single before", tag(on(5, 2), no, no, SpacerPlan.Side.BEFORE), List.of(2));
        same("single after", tag(on(5, 2), no, no, SpacerPlan.Side.AFTER), List.of(2));
        same("block before = first", tag(on(5, 1, 2, 3), no, no, SpacerPlan.Side.BEFORE), List.of(1));
        same("block after = last", tag(on(5, 1, 2, 3), no, no, SpacerPlan.Side.AFTER), List.of(3));
        same("two blocks", tag(on(5, 0, 1, 3), no, no, SpacerPlan.Side.BEFORE), List.of(0, 3));
        same("nothing selected", tag(no, no, no, SpacerPlan.Side.AFTER), List.of());
        same("already tagged", tag(on(5, 2), on(5, 2), no, SpacerPlan.Side.BEFORE), List.of());
        same("gap from track above", tag(on(5, 2), no, on(5, 1), SpacerPlan.Side.BEFORE), List.of());
        same("gap from track below", tag(on(5, 2), on(5, 3), no, SpacerPlan.Side.AFTER), List.of());
        same("other side still free", tag(on(5, 2), on(5, 2), no, SpacerPlan.Side.AFTER), List.of(2));
        same("empty list", tag(new boolean[0], new boolean[0], new boolean[0], SpacerPlan.Side.BEFORE), List.of());

        // SpacerPlan.toClear: every gap touching a selected track
        same("clear own before", clear(on(5, 2), on(5, 2), no), List.of(new SpacerPlan.Clear(2, true, false)));
        same("clear gap above (from track above)", clear(on(5, 2), no, on(5, 1)),
                List.of(new SpacerPlan.Clear(1, false, true)));
        same("clear gap below (from track below)", clear(on(5, 2), on(5, 3), no),
                List.of(new SpacerPlan.Clear(3, true, false)));
        same("leave far gaps alone", clear(on(5, 2), on(5, 0), on(5, 4)), List.of());
        same("leave other side of neighbour", clear(on(5, 2), on(5, 1), on(5, 3)), List.of());
        same("nothing to clear", clear(on(5, 2), no, no), List.of());

        // SpacerRule.keepTags: Undo/Redo keeps the spacer tags as they are now
        same("undo to older comment keeps tag", SpacerRule.keepTags("old note", "new note #spacer-before"), "old note #spacer-before");
        same("undo cannot bring back a removed tag", SpacerRule.keepTags("note #spacer-after", "note"), "note");
        same("undo keeps the size", SpacerRule.keepTags("x #spacer-before", "y #spacer-before=24"), "x #spacer-before=24");
        same("both tags kept", SpacerRule.keepTags("", "#spacer-before #spacer-after=8"), "#spacer-before #spacer-after=8");
        same("nothing to keep", SpacerRule.keepTags("a", "b"), "a");
        same("null incoming", SpacerRule.keepTags(null, "#spacer-after"), "#spacer-after");

        // Simulation: Bitwig's undo history for one track's comment, with the mod's rules.
        //   1. user types a comment (recorded: "" -> "Bass")
        //   2. menu inserts a spacer (NOT recorded): "Bass #spacer-before"
        //   3. user edits the comment (recorded: "Bass #spacer-before" -> "Bass long #spacer-before")
        //   then Undo, Undo, Redo, Redo: the spacer must stay all the time.
        java.util.List<String[]> history = new java.util.ArrayList<>(); // recorded steps: {old, new}
        String comment = "";
        history.add(new String[] {comment, "Bass"});
        comment = "Bass";
        comment = SpacerRule.withBefore(comment);                     // step 2: silent, no history entry
        history.add(new String[] {comment, "Bass long #spacer-before"});
        comment = "Bass long #spacer-before";
        comment = SpacerRule.keepTags(history.get(1)[0], comment);    // undo step 3
        same("sim undo 1", comment, "Bass #spacer-before");
        comment = SpacerRule.keepTags(history.get(0)[0], comment);    // undo step 1 (old comment had no tag)
        same("sim undo 2 keeps spacer", comment, "#spacer-before");
        comment = SpacerRule.keepTags(history.get(0)[1], comment);    // redo step 1
        same("sim redo 1", comment, "Bass #spacer-before");
        comment = SpacerRule.keepTags(history.get(1)[1], comment);    // redo step 3
        same("sim redo 2", comment, "Bass long #spacer-before");
        comment = SpacerRule.withoutAll(comment);                     // menu: Remove Spacer (silent)
        comment = SpacerRule.keepTags(history.get(1)[0], comment);    // undo step 3 again: old value had the tag
        same("sim undo after remove does not bring it back", comment, "Bass");
        check("sim: gap follows", SpacerRule.gapBefore(comment) == 0);

        // CopyRule: only fresh copies in a shown project lose their tags
        check("copy loses tags", CopyRule.removeTags(true, false, "#spacer-before"));
        check("moved/grouped track keeps tags", !CopyRule.removeTags(true, true, "#spacer-before"));
        check("while loading nothing is removed", !CopyRule.removeTags(false, false, "#spacer-after"));
        check("copy without tags: nothing to do", !CopyRule.removeTags(true, false, "Bass"));

        // Flow: menu writes the tag -> the row reads the gap from the comment (no copy step involved)
        String afterMenu = SpacerRule.withBefore("Bass");
        same("flow: menu tag gives a gap", SpacerRule.gapBefore(afterMenu), SpacerRule.DEFAULT_GAP);
        check("flow: menu-tagged existing track is never treated as a copy", !CopyRule.removeTags(true, true, afterMenu));
        // Flow: duplicate of that track -> copy loses the tag, rest stays
        same("flow: duplicate keeps the rest", CopyRule.removeTags(true, false, afterMenu) ? SpacerRule.withoutAll(afterMenu) : afterMenu, "Bass");

        boolean threw = false;
        try {
            SpacerPlan.toTag(new boolean[2], new boolean[3], new boolean[2], SpacerPlan.Side.BEFORE);
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        check("different lengths are refused", threw);

        if (failures > 0) {
            System.out.println("SPACER_TESTS_FAILED " + failures);
            System.exit(1);
        }
        System.out.println("SPACER_TESTS_OK");
    }

    private static List<Integer> tag(boolean[] sel, boolean[] before, boolean[] after, SpacerPlan.Side side) {
        return SpacerPlan.toTag(sel, before, after, side);
    }

    private static List<SpacerPlan.Clear> clear(boolean[] sel, boolean[] before, boolean[] after) {
        return SpacerPlan.toClear(sel, before, after);
    }

    /** n flags, the given ones true. */
    private static boolean[] on(int n, int... indexes) {
        boolean[] b = new boolean[n];
        for (int i : indexes) {
            b[i] = true;
        }
        return b;
    }

    private static void check(String what, boolean ok) {
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + what);
        }
    }

    private static void same(String what, Object actual, Object expected) {
        if (!expected.equals(actual)) {
            failures++;
            System.out.println("FAIL: " + what + ": got [" + actual + "], expected [" + expected + "]");
        }
    }
}
