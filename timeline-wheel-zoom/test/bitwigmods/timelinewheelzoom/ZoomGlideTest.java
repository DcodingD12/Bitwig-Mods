package bitwigmods.timelinewheelzoom;

/**
 * Small tests for ZoomGlide without any test library.
 * Run: sh tools/test-java.sh timeline-wheel-zoom   (prints ZOOMGLIDE_TESTS_OK or fails)
 */
public final class ZoomGlideTest {
    private static final double FRAME = 1.0 / 60.0;
    private static final double TAU = 0.07;
    private static final double FINISH = 0.0005;
    private static int failures = 0;

    public static void main(String[] args) {
        // One wheel notch: the steps add up to the input, no overshoot, the glide ends.
        ZoomGlide g = new ZoomGlide();
        g.add(0.25, 4.0);
        double sum = 0.0;
        double first = g.advance(FRAME, TAU, FINISH);
        sum += first;
        int frames = 1;
        boolean overshoot = false;
        while (g.running() && frames < 1000) {
            sum += g.advance(FRAME, TAU, FINISH);
            frames++;
            if (sum > 0.25 + 1e-12) {
                overshoot = true;
            }
        }
        near("steps add up to the input", sum, 0.25);
        check("no overshoot", !overshoot);
        check("first frame moves only a part (no jump)", first > 0.0 && first < 0.25 * 0.5);
        check("glide ends within half a second", frames <= 30);

        // Spinning the wheel: input adds up while the glide runs.
        g = new ZoomGlide();
        g.add(0.25, 4.0);
        double done = g.advance(FRAME, TAU, FINISH);
        g.add(0.25, 4.0);
        while (g.running()) {
            done += g.advance(FRAME, TAU, FINISH);
        }
        near("two notches give twice the zoom", done, 0.5);

        // Turning back cancels out.
        g = new ZoomGlide();
        g.add(0.3, 4.0);
        g.add(-0.3, 4.0);
        check("in and out again: nothing to do", !g.running());

        // Fine trackpad input is proportional: half the input, half the zoom.
        ZoomGlide a = new ZoomGlide();
        ZoomGlide b = new ZoomGlide();
        a.add(0.02, 4.0);
        b.add(0.01, 4.0);
        near("proportional first step", a.advance(FRAME, TAU, 0.0), 2 * b.advance(FRAME, TAU, 0.0));

        // The pending amount is capped.
        g = new ZoomGlide();
        g.add(100.0, 4.0);
        double total = 0.0;
        while (g.running()) {
            total += g.advance(FRAME, TAU, FINISH);
        }
        near("pending amount is capped", total, 4.0);

        // A frame time of 0 (first frame) moves nothing; no smoothing moves everything at once.
        g = new ZoomGlide();
        g.add(0.5, 4.0);
        near("zero frame time: no step", g.advance(0.0, TAU, FINISH), 0.0);
        near("time constant 0: all at once", g.advance(FRAME, 0.0, FINISH), 0.5);

        if (failures > 0) {
            throw new AssertionError(failures + " test(s) failed");
        }
        System.out.println("ZOOMGLIDE_TESTS_OK");
    }

    private static void near(String name, double actual, double expected) {
        check(name + " (got " + actual + ", expected " + expected + ")", Math.abs(actual - expected) < 1e-9);
    }

    private static void check(String name, boolean ok) {
        if (!ok) {
            failures++;
            System.out.println("FAILED: " + name);
        }
    }
}
