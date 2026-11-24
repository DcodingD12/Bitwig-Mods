package bitwigmods.timelinewheelzoom;

import bitwig.obf.dLl; // a wheel event for a widget (amount, units, position)
import bitwig.obf.dUD; // a scroll model of one axis (offset, size, zoom)
import bitwig.obf.dVg; // a scroll view that can also zoom
import bitwig.obf.dty; // a wheel handler: gets the wheel events of one gesture
import bitwig.obf.dxM; // the zoom part of a scroll model
import bitwig.obf.eqI; // the units of a wheel event: notches or pixels
import com.bitwig.base.geom.mLC; // an axis: HORIZONTAL or VERTICAL

/**
 * Wheel handler for a timeline ruler (arranger or detail editor): instead of zooming at once (Bitwig's own handler),
 * it turns the wheel input into an amount of zoom and hands it to the ZoomAnimator, which
 * zooms there smoothly, frame by frame, around the mouse position.
 *
 * The amount is computed with Bitwig's own formula (the one Cmd+wheel uses), so the total zoom
 * per notch is the same as before, times ZoomSettings.ZOOM_STRENGTH. Fine trackpad input gives
 * small amounts, a mouse notch a bigger one; both are smoothed the same way.
 *
 * Runs on Bitwig's event thread only. Names are Bitwig 6.1.7 (macOS).
 */
final class SmoothZoomHandler extends dty {
    private final dVg view;

    SmoothZoomHandler(dVg view) {
        super(view); // the gesture belongs to the whole header view (MhB), as with Bitwig's own handler
        this.view = view;
    }

    // dty.beI(dLl): one wheel event of the gesture.
    @Override
    public void beI(dLl event) {
        try {
            dUD model = view.MgQ(mLC.beI);            // the horizontal scroll model (mLC.beI = HORIZONTAL)
            dxM zoom = (dxM) model.PJ3();             // its zoom part
            int amount = event.beI();                 // signed amount, in notches or pixels
            // Bitwig 6.1.7's "reverse wheel zoom direction" setting is NOT read on purpose: Bitwig 6.1.4
            // and older don't have it, reading it would break the mod there. Our own switch instead:
            if (ZoomSettings.REVERSE_DIRECTION) {
                amount = -amount;
            }
            double now = zoom.beI();                  // current zoom (time per pixel)
            double target = event.Gef() == eqI.beI    // eqI.beI = counted in notches (eqI.Gef = pixels, macOS)
                    ? zoom.beI(amount, model.Gak())   // Bitwig's zoom after n notches (Gak = visible size)
                    : zoom.Gef(amount, model.Gak());  // Bitwig's zoom after n pixels
            if (!(now > 0.0) || !(target > 0.0)) {
                return;
            }
            double doublings = Math.log(target / now) / Math.log(2.0) * ZoomSettings.ZOOM_STRENGTH;
            ZoomAnimator.add(view, model, event.beI(mLC.beI), doublings); // mouse position along the time axis
        } catch (Throwable problem) {
            TimelineWheelZoomHooks.failed("smooth zoom input failed", problem);
        }
    }
}
