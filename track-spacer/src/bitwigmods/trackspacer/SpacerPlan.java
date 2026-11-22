package bitwigmods.trackspacer;

import java.util.ArrayList;
import java.util.List;

/**
 * Works out which tracks get or lose a spacer tag, in one track list (the tracks of one group, top to bottom).
 * Plain Java, no Bitwig classes, so it can be tested alone.
 *
 * Inserting:
 *  - Selected tracks that sit next to each other form a block. Each block gets ONE gap:
 *    "before" tags its first track, "after" tags its last track.
 *  - If there already is a gap at that spot (from either neighbour's tag), nothing is added.
 * Removing:
 *  - Every gap touching a selected track goes: its own tags, the "after" tag of the track above
 *    and the "before" tag of the track below.
 */
public final class SpacerPlan {
    public enum Side { BEFORE, AFTER }

    /** What to remove from one track's comment. */
    public record Clear(int index, boolean before, boolean after) {
    }

    private SpacerPlan() {
    }

    /**
     * The tracks to tag (indexes, top to bottom).
     *
     * @param selected  selected[i]: track i is selected (and can have a spacer)
     * @param gapBefore gapBefore[i]: track i already has "#spacer-before"
     * @param gapAfter  gapAfter[i]: track i already has "#spacer-after"
     */
    public static List<Integer> toTag(boolean[] selected, boolean[] gapBefore, boolean[] gapAfter, Side side) {
        int n = checkLengths(selected, gapBefore, gapAfter);
        List<Integer> result = new ArrayList<>();
        int i = 0;
        while (i < n) {
            if (!selected[i]) {
                i++;
                continue;
            }
            int start = i;
            while (i < n && selected[i]) {
                i++;
            }
            int end = i - 1; // last track of the block
            if (side == Side.BEFORE) {
                boolean gapThere = gapBefore[start] || (start > 0 && gapAfter[start - 1]);
                if (!gapThere) {
                    result.add(start);
                }
            } else {
                boolean gapThere = gapAfter[end] || (end + 1 < n && gapBefore[end + 1]);
                if (!gapThere) {
                    result.add(end);
                }
            }
        }
        return result;
    }

    /** The tags to remove so no gap touches a selected track (only tracks that have something to remove). */
    public static List<Clear> toClear(boolean[] selected, boolean[] gapBefore, boolean[] gapAfter) {
        int n = checkLengths(selected, gapBefore, gapAfter);
        boolean[] clearBefore = new boolean[n];
        boolean[] clearAfter = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (!selected[i]) {
                continue;
            }
            clearBefore[i] = true;
            clearAfter[i] = true;
            if (i > 0) {
                clearAfter[i - 1] = true;   // gap above i, from the track above
            }
            if (i + 1 < n) {
                clearBefore[i + 1] = true;  // gap below i, from the track below
            }
        }
        List<Clear> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            boolean b = clearBefore[i] && gapBefore[i];
            boolean a = clearAfter[i] && gapAfter[i];
            if (b || a) {
                result.add(new Clear(i, b, a));
            }
        }
        return result;
    }

    private static int checkLengths(boolean[] selected, boolean[] gapBefore, boolean[] gapAfter) {
        int n = selected.length;
        if (gapBefore.length != n || gapAfter.length != n) {
            throw new IllegalArgumentException("all arrays must have the same length");
        }
        return n;
    }
}
