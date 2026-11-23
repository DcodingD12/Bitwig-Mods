package bitwigmods.timelinewheelzoom;

import bitwig.obf.dVg; // a scroll view that can also zoom (it owns the scroll models of both axes)
import bitwig.obf.dty; // a wheel handler: the object that gets the wheel events of one gesture
import bitwig.obf.dwF; // a widget
import bitwig.obf.eqw; // the modifier keys held during an event
import com.bitwig.base.geom.mLC; // an axis: HORIZONTAL or VERTICAL
import com.bitwig.flt.widget.core.timeline.SiG; // base of the timeline rulers (bars ruler and time ruler)
import com.bitwig.flt.widget.core.timeline.haN; // the arranger's timeline header (holds its rulers)
import bml.api.Arg;
import bml.api.At;
import bml.api.Bml;
import bml.api.Hook;
import bml.api.This;

/**
 * Arranger: Timeline Wheel Zoom.
 *
 * How Bitwig handles the wheel: for every wheel gesture the window asks the widgets under the
 * mouse for a wheel handler, the deepest widget first, then its parents
 * (dwF.beI(x, y, axis, modifiers); a widget without one answers null). The first answer wins.
 * The rulers (SiG and its two subclasses) have no handler, so today their parent answers:
 * the arranger header's scroll view (dVg). That view zooms only with Cmd held (dVg.sCA(axis)
 * = Bitwig's zoom handler, which zooms around the mouse position).
 *
 * This mod gives the rulers their own answer: for a plain vertical wheel turn over the
 * ARRANGER's ruler, its own handler (SmoothZoomHandler) on that scroll view. It zooms by the
 * same amount and in the same direction as Cmd+wheel, but smoothly over a few frames
 * (ZoomAnimator), around the mouse position. (0.1.0 returned Bitwig's own handler, view.sCA(axis),
 * which jumps once per notch; that is still the fallback after a failure.)
 * In every other case the hook answers null, which is what the rulers answered before
 * (none of their classes up to dwF has an own answer), so the parent decides as usual.
 *
 * Names are Bitwig 6.1.7 (macOS).
 */
public final class TimelineWheelZoomHooks {
    private static final Bml.Mod MOD = Bml.mod("timeline-wheel-zoom");

    // Log a problem only once, wheel events come many per second.
    private static volatile boolean loggedProblem = false;

    // Set after a failure in the smooth zoom (SmoothZoomHandler / ZoomAnimator).
    private static volatile boolean smoothFailed = false;

    private TimelineWheelZoomHooks() {
    }

    // dwF.beI(double x, double y, mLC axis, eqw modifiers) -> dty: "your wheel handler at this point?"
    // A new override on the rulers' base class; Bitwig's inherited version returns null.
    @Hook(id = "ruler-wheel-zooms", target = SiG.class,
            method = "beI(DDLcom/bitwig/base/geom/mLC;Leqw;)Ldty;", at = At.OVERRIDE_INSTEAD)
    public static dty rulerWheelHandler(@This SiG ruler, @Arg(2) mLC axis, @Arg(3) eqw modifiers) {
        try {
            boolean vertical = axis != null && axis.Gef();          // mLC.Gef() = is VERTICAL
            boolean anyModifier = modifiers == null || modifiers.Bqk(); // eqw.Bqk() = any key held
            if (!WheelRule.shouldZoom(vertical, anyModifier, isInArranger(ruler))) {
                return null; // as before: the parent widgets decide
            }
            // The nearest parent scroll view that can zoom horizontally (the arranger header's).
            dVg view = dVg.MgQ(ruler, mLC.beI);                    // mLC.beI = HORIZONTAL
            if (view == null) {
                return null;
            }
            if (smoothFailed) {
                return view.sCA(mLC.beI); // fallback: Bitwig's own (stepwise) zoom handler, horizontal axis
            }
            return new SmoothZoomHandler(view);
        } catch (Throwable problem) {
            if (!loggedProblem) {
                loggedProblem = true;
                MOD.log("wheel zoom on the ruler failed, the wheel works as before", problem);
            }
            return null;
        }
    }

    /** Called once something in the smooth zoom fails: from then on Bitwig's own zoom (until restart). */
    static void failed(String what, Throwable problem) {
        if (!smoothFailed) {
            smoothFailed = true;
            MOD.log(what + ", using Bitwig's own wheel zoom on the ruler until Bitwig restarts", problem);
        }
    }

    /** True if the ruler sits in the arranger's timeline header (not the detail editor's). */
    private static boolean isInArranger(dwF widget) {
        for (dwF w = widget; w != null; w = w.oRH()) { // dwF.oRH() = parent widget
            if (w instanceof haN) {
                return true;
            }
        }
        return false;
    }
}
