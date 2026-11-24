package bitwigmods.pinnedtracks;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decides whether a track is pinned to the top of the arranger, and adds or removes the tag
 * "#pin" in a track's comment. Plain Java, no Bitwig classes, so it can be tested alone.
 *
 * A track is pinned when its comment contains the word "#pin" (any case; "#pinned" or "#pin2" don't count).
 */
public final class PinRule {
    public static final String TAG = "#pin";

    // "#pin" not followed by a letter, digit or "_" (so "#pinned" is not the tag).
    private static final Pattern PATTERN = Pattern.compile("(?i)#pin(?![\\p{L}\\p{N}_])");

    private PinRule() {
    }

    public static boolean isPinned(String comment) {
        return comment != null && PATTERN.matcher(comment).find();
    }

    /** The comment with " #pin" added at the end, or unchanged if it's already there. */
    public static String withTag(String comment) {
        if (comment == null || comment.isEmpty()) {
            return TAG;
        }
        return isPinned(comment) ? comment : comment + " " + TAG;
    }

    /** The comment with every "#pin" removed (and one space next to each), the rest unchanged. */
    public static String withoutTag(String comment) {
        if (comment == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        Matcher m = PATTERN.matcher(comment);
        int last = 0;
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            if (start > last && comment.charAt(start - 1) == ' ') {
                start--;                     // "text #pin" -> "text"
            } else if (end < comment.length() && comment.charAt(end) == ' ') {
                end++;                       // "#pin text" -> "text"
            }
            out.append(comment, last, start);
            last = end;
        }
        out.append(comment, last, comment.length());
        return out.toString();
    }
}
