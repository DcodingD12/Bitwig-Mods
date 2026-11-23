package bitwigmods.timelinewheelzoom;

import bitwig.obf.dUD; // a scroll model of one axis (offset, size, zoom)
import bitwig.obf.dVg; // a scroll view that can also zoom
import bitwig.obf.doe; // a callback that runs once per frame, before layout and painting
import bitwig.obf.dre; // one frame's time
import bitwig.obf.dsB; // Bitwig's frame clock (the one its own animations use)
import bitwig.obf.dxM; // the zoom part of a scroll model

/**
 * Runs the smooth zoom, one step per frame, on Bitwig's frame clock (registered only while a
 * zoom is running). Each step:
 *   1. sets the zoom a bit closer to the target (ZoomGlide decides how much),
 *   2. moves the scroll offset so the time under the mouse stays under the mouse.
 * That is what Bitwig's own Cmd+wheel zoom does in one go (dUD.Gef(int, double) and its
 * follow-up), here in many small steps.
 *
 * If something else changes the view meanwhile (scrollbar, follow playhead, a shortcut, the
 * view is closed), the zoom stops there. Everything runs on Bitwig's event thread.
 */
final class ZoomAnimator implements doe {
    private static final ZoomAnimator FRAMES = new ZoomAnimator();
    private static final ZoomGlide GLIDE = new ZoomGlide();

    private static dVg view;
    private static dUD model;
    private static double anchorPixel;    // mouse position in the view (pixels from its left edge)
    private static double anchorTime;     // the time under the mouse, kept in place while zooming
    private static long writtenOffset;    // what we last wrote, to notice other changes
    private static double writtenZoom;
    private static dsB clock;

    private ZoomAnimator() {
    }

    /** New wheel input: doublings of zoom (+ = out, - = in) around the mouse position. */
    static void add(dVg newView, dUD newModel, double mousePixel, double doublings) {
        boolean running = GLIDE.running() && model == newModel && !movedElsewhere();
        if (!running) {
            GLIDE.stop();
            view = newView;
            model = newModel;
        }
        // The anchor: the time under the mouse. Kept while the mouse stays put, so rounding
        // of the offset doesn't let it wander during a glide.
        if (!running || Math.abs(mousePixel - anchorPixel) > 0.5) {
            anchorPixel = mousePixel;
            anchorTime = model.sCA(model.VNw() + mousePixel); // dUD.sCA: pixel -> time, VNw = offset
        }
        GLIDE.add(doublings, ZoomSettings.MAX_PENDING_DOUBLINGS);
        remember();
        if (GLIDE.running() && clock == null) {
            clock = dsB.Gef(FRAMES); // per-frame callback until stop()
        }
        if (!GLIDE.running() && clock == null) {
            view = null;  // total input 0: nothing runs, so don't hold on to the view
            model = null;
        }
    }

    // doe.aboutToPaint(dre): once per frame while a zoom runs.
    @Override
    public void aboutToPaint(dre frame) {
        try {
            if (model == null || !view.vTn() || movedElsewhere()) { // vTn = still shown
                stop();
                return;
            }
            double seconds = frame.MgQ(); // time since the last frame, 0 on the first one
            if (seconds <= 0.0) {
                seconds = 1.0 / 60.0;
            }
            seconds = Math.min(seconds, 0.1); // after a hiccup: no big jump
            double step = GLIDE.advance(seconds, ZoomSettings.SMOOTHING_SECONDS, ZoomSettings.FINISH_BELOW);
            if (step != 0.0) {
                apply(step);
            }
            if (!GLIDE.running()) {
                stop();
            }
        } catch (Throwable problem) {
            stop();
            TimelineWheelZoomHooks.failed("smooth zoom step failed", problem);
        }
    }

    private static void apply(double doublings) {
        dxM zoom = (dxM) model.PJ3();
        double before = zoom.beI();
        zoom.beI(before * Math.pow(2.0, doublings), model.Gak(), true); // set zoom (Gak = visible size), as Bitwig does
        if (zoom.beI() == before) {
            GLIDE.stop(); // at the zoom limit: nothing more to do
        }
        // Keep the anchor time under the mouse: its new pixel position minus the mouse position.
        double timePixel = zoom.beI(anchorTime, 0.0); // duW.beI: time -> pixel, as in Bitwig's own follow-up
        model.Gef(Math.round(timePixel - anchorPixel)); // dUD.Gef(long): set the offset (Bitwig clamps it)
        remember();
    }

    private static void remember() {
        writtenOffset = model.VNw();
        writtenZoom = ((dxM) model.PJ3()).beI();
    }

    /** True if the offset or the zoom is not what we left (someone else moved the view). */
    private static boolean movedElsewhere() {
        return model.VNw() != writtenOffset || ((dxM) model.PJ3()).beI() != writtenZoom;
    }

    private static void stop() {
        GLIDE.stop();
        if (clock != null) {
            clock.beI(); // unregister from the frame clock
            clock = null;
        }
        view = null;
        model = null;
    }
}
