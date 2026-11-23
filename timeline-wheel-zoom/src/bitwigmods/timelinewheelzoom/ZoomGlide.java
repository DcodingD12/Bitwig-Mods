package bitwigmods.timelinewheelzoom;

/**
 * The smoothing, without Bitwig classes (tested in test/.../ZoomGlideTest.java).
 *
 * Zoom is counted in "doublings" (log2 of the zoom factor): +1 = twice as much time on screen,
 * -1 = half. Wheel input adds to what is still to do; each frame hands out a part of it
 * (exponential smoothing: the same share per unit of time), so the zoom runs fast at first and
 * settles softly. The steps of one glide add up exactly to the input, nothing overshoots.
 */
final class ZoomGlide {
    private double remaining = 0.0;

    /** Adds wheel input (in doublings). Keeps the pending amount within +-maxPending. */
    void add(double doublings, double maxPending) {
        remaining = Math.max(-maxPending, Math.min(maxPending, remaining + doublings));
    }

    /**
     * The step for this frame (in doublings). seconds = time since the last frame,
     * timeConstant = after this long about 63% of the input is done.
     */
    double advance(double seconds, double timeConstant, double finishBelow) {
        if (remaining == 0.0) {
            return 0.0;
        }
        double share = timeConstant <= 0.0 ? 1.0 : 1.0 - Math.exp(-Math.max(0.0, seconds) / timeConstant);
        double step = remaining * share;
        if (Math.abs(remaining - step) < finishBelow) {
            step = remaining; // the last tiny rest in one go, so the glide ends
        }
        remaining -= step;
        return step;
    }

    boolean running() {
        return remaining != 0.0;
    }

    void stop() {
        remaining = 0.0;
    }
}
