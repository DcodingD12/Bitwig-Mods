package bitwigmods.timelinewheelzoom;

/**
 * When does a wheel event over the arranger ruler zoom instead of doing what Bitwig does?
 * No Bitwig classes here, so it can be tested on its own (test/.../WheelRuleTest.java).
 */
final class WheelRule {
    private WheelRule() {
    }

    /**
     * Only the plain wheel zooms: a vertical turn (a normal mouse wheel, or a vertical swipe
     * on a trackpad), no modifier key held, and the ruler really belongs to the arranger.
     * Everything else (horizontal swipes, Shift/Cmd/Alt/Ctrl + wheel) stays Bitwig's.
     */
    static boolean shouldZoom(boolean verticalWheel, boolean anyModifierHeld, boolean inArranger) {
        return verticalWheel && !anyModifierHeld && inArranger;
    }
}
