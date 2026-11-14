package bitwigmods.trackheightlock;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decides whether a track's height is locked, and adds or removes the lock marks
 * in a track's comment ("#lock") and name ("(L)").
 * Plain Java, no Bitwig classes, so it can be tested alone.
 *
 * A track is locked when
 *  - its name contains "(L)" as a word of its own, "[L]" (any case) or the lock emoji, or
 *  - its comment contains the word "#lock" (any case; "#locked" does not count).
 */
public final class LockRule {
    public static final String COMMENT_TAG = "#lock";
    public static final String NAME_TAG = "(L)";

    // "#lock" not followed by a letter, digit or "_" (so "#locked" or "#lock2" are not the tag).
    private static final Pattern COMMENT_PATTERN = Pattern.compile("(?i)#lock(?![\\p{L}\\p{N}_])");

    // Name marks, any case:
    //  - "(L)" with no letter/digit right before or after it ("(LFO)" or "Bass(L)" don't count),
    //  - "[L]" anywhere (the older mark),
    //  - the lock emoji.
    private static final Pattern NAME_PATTERN = Pattern.compile(
            "(?i)(?<![\\p{L}\\p{N}_])\\(l\\)(?![\\p{L}\\p{N}_])|\\[l\\]|\\x{1F512}");

    private LockRule() {
    }

    public static boolean isLocked(String trackName, String trackComment) {
        return nameIsLocked(trackName) || commentIsLocked(trackComment);
    }

    public static boolean nameIsLocked(String name) {
        return name != null && NAME_PATTERN.matcher(name).find();
    }

    public static boolean commentIsLocked(String comment) {
        return comment != null && COMMENT_PATTERN.matcher(comment).find();
    }

    /** The comment with "#lock" added at the end (one space before it), or unchanged if it's already there. */
    public static String withTag(String comment) {
        return withMark(comment, COMMENT_TAG, commentIsLocked(comment));
    }

    /** The comment with every "#lock" removed (and one space next to each), the rest unchanged. */
    public static String withoutTag(String comment) {
        return withoutMarks(comment, COMMENT_PATTERN);
    }

    /**
     * The name with " (L)" added at the end, or unchanged if it already has a lock mark
     * ("(L)", "[L]" or the emoji). An empty name becomes "(L)".
     */
    public static String withNameTag(String name) {
        return withMark(name, NAME_TAG, nameIsLocked(name));
    }

    /** The name with every lock mark ("(L)", "[L]", emoji) removed (and one space next to each), the rest unchanged. */
    public static String withoutNameTag(String name) {
        return withoutMarks(name, NAME_PATTERN);
    }

    private static String withMark(String text, String mark, boolean alreadyThere) {
        if (text == null || text.isEmpty()) {
            return mark;
        }
        if (alreadyThere) {
            return text;
        }
        return text + " " + mark;
    }

    /**
     * Removes every match, plus one space next to it (the one withMark added),
     * so the rest of the text stays exactly as it was.
     */
    private static String withoutMarks(String text, Pattern pattern) {
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
                start--;                     // "text #lock" -> "text"
            } else if (end < text.length() && text.charAt(end) == ' ') {
                end++;                       // "#lock text" -> "text"
            }
            out.append(text, last, start);
            last = end;
        }
        out.append(text, last, text.length());
        return out.toString();
    }
}
