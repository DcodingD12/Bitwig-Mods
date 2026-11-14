package bitwigmods.trackheightlock;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decides whether a track's height is locked, and adds or removes the lock tag in a comment.
 * Plain Java, no Bitwig classes, so it can be tested alone.
 *
 * A track is locked when
 *  - its name contains "[L]" (any case) or the lock emoji, or
 *  - its comment contains the word "#lock" (any case; "#locked" does not count).
 */
public final class LockRule {
    public static final String NAME_MARKER = "[l]";        // compared in lower case, so "[L]" works too
    public static final String NAME_EMOJI = "🔒"; // the lock emoji
    public static final String COMMENT_TAG = "#lock";

    // "#lock" not followed by a letter, digit or "_" (so "#locked" or "#lock2" are not the tag).
    private static final Pattern TAG = Pattern.compile("(?i)#lock(?![\\p{L}\\p{N}_])");

    private LockRule() {
    }

    public static boolean isLocked(String trackName, String trackComment) {
        return nameIsLocked(trackName) || commentIsLocked(trackComment);
    }

    static boolean nameIsLocked(String name) {
        if (name == null) {
            return false;
        }
        return name.toLowerCase(Locale.ROOT).contains(NAME_MARKER) || name.contains(NAME_EMOJI);
    }

    public static boolean commentIsLocked(String comment) {
        return comment != null && TAG.matcher(comment).find();
    }

    /** The comment with "#lock" added at the end (one space before it), or unchanged if it's already there. */
    public static String withTag(String comment) {
        if (comment == null || comment.isEmpty()) {
            return COMMENT_TAG;
        }
        if (commentIsLocked(comment)) {
            return comment;
        }
        return comment + " " + COMMENT_TAG;
    }

    /**
     * The comment with every "#lock" tag removed, and one space next to each tag
     * (the one withTag added), so the rest of the comment stays exactly as it was.
     */
    public static String withoutTag(String comment) {
        if (comment == null || comment.isEmpty()) {
            return comment == null ? "" : comment;
        }
        StringBuilder out = new StringBuilder();
        Matcher m = TAG.matcher(comment);
        int last = 0;
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            if (start > last && comment.charAt(start - 1) == ' ') {
                start--;                     // "text #lock" -> "text"
            } else if (end < comment.length() && comment.charAt(end) == ' ') {
                end++;                       // "#lock text" -> "text"
            }
            out.append(comment, last, start);
            last = end;
        }
        out.append(comment, last, comment.length());
        return out.toString();
    }
}
