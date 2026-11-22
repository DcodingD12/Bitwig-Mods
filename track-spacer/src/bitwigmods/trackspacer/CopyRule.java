package bitwigmods.trackspacer;

/**
 * Decides whether a track that is being put into a group's track list loses its spacer tags.
 * Plain Java, no Bitwig classes, so it can be tested alone.
 *
 * Only a fresh copy (Duplicate, Copy & Paste) loses them: a track that was never part of the
 * project, inserted while the project is already shown. Tracks that are moved or grouped are
 * the same objects as before ("seen"); while a project loads nothing is removed.
 */
public final class CopyRule {
    private CopyRule() {
    }

    /**
     * @param projectKnown the project is shown and all its tracks were recorded
     * @param seen         this track object was already part of the project
     * @param comment      the track's comment
     */
    public static boolean removeTags(boolean projectKnown, boolean seen, String comment) {
        return projectKnown && !seen && SpacerRule.hasAny(comment);
    }
}
