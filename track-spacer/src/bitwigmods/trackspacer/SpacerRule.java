package bitwigmods.trackspacer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads and writes the spacer tags in a track's comment. Plain Java, no Bitwig classes, so it can be tested alone.
 *
 *  - "#spacer-before"      a gap of DEFAULT_GAP pixels above the track
 *  - "#spacer-after"       a gap below the track
 *  - "#spacer-before=24"   the same with a size of its own (1..MAX_GAP pixels)
 * Tags are any case; "#spacer-beforehand" or "#spacer-after2" don't count.
 */
public final class SpacerRule {
    public static final String TAG_BEFORE = "#spacer-before";
    public static final String TAG_AFTER = "#spacer-after";
    public static final int DEFAULT_GAP = 16;
    public static final int MAX_GAP = 400;

    // The tag, optionally "=<number>", and nothing word-like glued to it.
    private static final Pattern BEFORE = Pattern.compile("(?i)#spacer-before(?:=(\\d{1,5}))?(?![\\p{L}\\p{N}_=-])");
    private static final Pattern AFTER = Pattern.compile("(?i)#spacer-after(?:=(\\d{1,5}))?(?![\\p{L}\\p{N}_=-])");

    private SpacerRule() {
    }

    /** Pixels of gap above the track (0: none). */
    public static int gapBefore(String comment) {
        return gap(comment, BEFORE);
    }

    /** Pixels of gap below the track (0: none). */
    public static int gapAfter(String comment) {
        return gap(comment, AFTER);
    }

    public static boolean hasAny(String comment) {
        return gapBefore(comment) > 0 || gapAfter(comment) > 0;
    }

    /** The comment with " #spacer-before" added at the end, or unchanged if it already has one. */
    public static String withBefore(String comment) {
        return gapBefore(comment) > 0 ? orEmpty(comment) : append(comment, TAG_BEFORE);
    }

    /** The comment with " #spacer-after" added at the end, or unchanged if it already has one. */
    public static String withAfter(String comment) {
        return gapAfter(comment) > 0 ? orEmpty(comment) : append(comment, TAG_AFTER);
    }

    /** The comment without any "#spacer-before" tag (and one space next to each), the rest unchanged. */
    public static String withoutBefore(String comment) {
        return without(comment, BEFORE);
    }

    /** The comment without any "#spacer-after" tag (and one space next to each), the rest unchanged. */
    public static String withoutAfter(String comment) {
        return without(comment, AFTER);
    }

    private static int gap(String comment, Pattern pattern) {
        if (comment == null) {
            return 0;
        }
        Matcher m = pattern.matcher(comment);
        if (!m.find()) {
            return 0;
        }
        if (m.group(1) == null) {
            return DEFAULT_GAP;
        }
        int value = Integer.parseInt(m.group(1));
        return Math.max(1, Math.min(MAX_GAP, value)); // "=0" still means "a gap": the smallest one
    }

    private static String orEmpty(String text) {
        return text == null ? "" : text;
    }

    private static String append(String text, String tag) {
        if (text == null || text.isEmpty()) {
            return tag;
        }
        return text + " " + tag;
    }

    /** Removes every match plus one space next to it (the one append added), so the rest stays as it was. */
    private static String without(String text, Pattern pattern) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        Matcher m = pattern.matcher(text);
        int last = 0;
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            if (start > last && text.charAt(start - 1) == ' ') {
                start--;                     // "text #tag" -> "text"
            } else if (end < text.length() && text.charAt(end) == ' ') {
                end++;                       // "#tag text" -> "text"
            }
            out.append(text, last, start);
            last = end;
        }
        out.append(text, last, text.length());
        return out.toString();
    }
}
