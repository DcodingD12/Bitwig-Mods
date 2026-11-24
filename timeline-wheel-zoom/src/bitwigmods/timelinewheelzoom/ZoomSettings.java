package bitwigmods.timelinewheelzoom;

/**
 * The knobs. Change a value, rebuild (sh tools/build-mod.sh timeline-wheel-zoom), re-import.
 */
final class ZoomSettings {
    private ZoomSettings() {
    }

    /**
     * How strong one wheel notch (or one trackpad move) zooms, compared with Bitwig's Cmd+wheel.
     * 1.0 = the same total zoom as Cmd+wheel, 0.5 = half, 2.0 = twice.
     */
    static final double ZOOM_STRENGTH = 1.0;

    /**
     * true = the wheel also zooms over the detail editor's ruler (piano roll / note editor, and the
     * audio and automation modes, which use the same ruler). false = arranger ruler only (as up to 0.2.x).
     */
    static final boolean DETAIL_EDITOR = true;

    /** true = wheel up and down swapped (Bitwig's own reverse-zoom setting is not followed). */
    static final boolean REVERSE_DIRECTION = false;

    /**
     * Smoothing in seconds: after this time about 63% of a notch is done, after 3x this time 95%.
     * Smaller = snappier, larger = softer (and a little more lag). 0 = no smoothing at all.
     */
    static final double SMOOTHING_SECONDS = 0.07;

    /** At most this many doublings of zoom can be waiting (stops a fast spin from running on and on). */
    static final double MAX_PENDING_DOUBLINGS = 4.0;

    /** A rest smaller than this (in doublings, 0.0005 = 0.03%) is done in one go, so a glide ends. */
    static final double FINISH_BELOW = 0.0005;
}
