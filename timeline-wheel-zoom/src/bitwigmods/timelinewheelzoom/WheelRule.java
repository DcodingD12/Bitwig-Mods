package bitwigmods.timelinewheelzoom;

/**
 * When does a wheel event over a timeline ruler zoom instead of doing what Bitwig does?
 * No Bitwig classes here, so it can be tested on its own (test/.../WheelRuleTest.java).
 */
final class WheelRule {
    private WheelRule() {
    }

    /** Where a ruler sits. */
    enum Place {
        ARRANGER,       // the arranger's timeline header
        DETAIL_EDITOR,  // the detail editor's header (piano roll / note editor, audio, automation)
        OTHER           // anything else (unknown): left alone
    }

    /**
     * Only the plain wheel zooms: a vertical turn (a normal mouse wheel, or a vertical swipe
     * on a trackpad), no modifier key held, and the ruler sits in the arranger, or in the
     * detail editor while that part is switched on (ZoomSettings.DETAIL_EDITOR).
     * Everything else (horizontal swipes, Shift/Cmd/Alt/Ctrl + wheel) stays Bitwig's.
     */
    static boolean shouldZoom(boolean verticalWheel, boolean anyModifierHeld, Place place,
                              boolean detailEditorOn) {
        if (!verticalWheel || anyModifierHeld || place == null) {
            return false;
        }
        return switch (place) {
            case ARRANGER -> true;
            case DETAIL_EDITOR -> detailEditorOn;
            case OTHER -> false;
        };
    }
}
