package bitwigmods.zoomtoggle;

import bml.actions.ActionProvider;
import bml.actions.Actions;
import bml.api.Bml;
import bitwig.obf.aCZ; // the arranger panel (also holds Bitwig's arranger actions)
import bitwig.obf.aZJ; // the arranger view (knows whether the clip launcher is shown)
import bitwig.obf.aZQ; // the holder of the arranger view
import bitwig.obf.dom; // an action
import bitwig.obf.dwF; // a widget
import com.bitwig.flt.app.Hvv; // the application

/**
 * One action: hide the clip launcher (if shown), zoom to the selection and make the selected track tall;
 * again: back, and show the launcher again if this mod hid it.
 *
 * The clip launcher shares its rows with the arranger, so a taller track would also make the launcher row tall.
 * That is why the launcher is hidden while zoomed. Arranger only: if the arranger isn't focused, nothing happens.
 * Bitwig's own actions are run on the focused widget, the way the controller API does it.
 * Names are Bitwig 6.1.7 (macOS).
 */
public final class ZoomToggleActions implements ActionProvider {
    private static final Bml.Mod MOD = Bml.mod("zoom-toggle");

    /** Steps of "zoom in lane heights (selected)", each one makes the selected tracks taller. */
    private static final int HEIGHT_STEPS = 40;

    private static boolean zoomed = false;
    private static boolean launcherHiddenByUs = false;

    @Override
    public void declare(Actions actions) {
        actions.action("zoom_toggle_fit", "Zoom to Fit Selection (Toggle)")
               .category("arranger")
               .onRun(context -> toggle());
    }

    private static void toggle() {
        try {
            var window = Hvv.mEL().LRq();
            var focused = window == null ? null : window.Sc3();
            if (focused == null) {
                return;
            }
            // Arranger only: the arranger panel must be around the focused widget (not the clip editor).
            aCZ arranger = (aCZ) focused.byf(aCZ.class);
            if (arranger == null) {
                return;
            }
            aZJ view = arrangerView(arranger);

            dom heights = zoomed ? aCZ.Tsf : aCZ.Bqr;
            if (!zoomed && view != null && view.waK()) {
                view.Bqk(false);                 // hide the clip launcher while zoomed
                launcherHiddenByUs = true;
            }
            if (!run(focused, heights)) {
                // not handled (not the arranger): put the launcher back if we just hid it
                if (launcherHiddenByUs && view != null) {
                    view.Bqk(true);
                    launcherHiddenByUs = false;
                }
                return;
            }
            for (int i = 1; i < HEIGHT_STEPS; i++) {
                run(focused, heights);
            }
            run(focused, aCZ.Or);                // arranger zoom to selection or previous (time axis)
            if (zoomed && launcherHiddenByUs && view != null) {
                view.Bqk(true);                  // show the clip launcher again
                launcherHiddenByUs = false;
            }
            zoomed = !zoomed;
        } catch (Throwable failure) {
            MOD.log("zoom toggle failed", failure);
        }
    }

    private static aZJ arrangerView(aCZ arranger) {
        aZQ holder = arranger.iOJ();
        return holder != null && holder.vTn() ? holder.dNa() : null;
    }

    /** Runs the action on the focused widget; true if a widget handled it. */
    private static boolean run(dwF focused, dom action) {
        return focused.BBg(action) != null;
    }
}
