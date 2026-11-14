package bitwigmods.trackheightlock;

import java.util.Locale;

/**
 * Decides whether a track's height is locked. Plain Java, no Bitwig classes, so it can be tested alone.
 *
 * A track is locked when
 *  - its name contains "[L]" (any case) or the lock emoji, or
 *  - its comment contains "#lock" (any case).
 */
public final class LockRule {
    public static final String NAME_MARKER = "[l]";        // compared in lower case, so "[L]" works too
    public static final String NAME_EMOJI = "🔒"; // the lock emoji
    public static final String COMMENT_MARKER = "#lock";

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

    static boolean commentIsLocked(String comment) {
        return comment != null && comment.toLowerCase(Locale.ROOT).contains(COMMENT_MARKER);
    }
}
