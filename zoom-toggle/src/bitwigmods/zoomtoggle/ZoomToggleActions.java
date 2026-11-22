package bitwigmods.zoomtoggle;

import bml.actions.ActionProvider;
import bml.actions.Actions;
import bml.api.Bml;
import bitwig.obf.aCZ; // Bitwig's arranger actions
import bitwig.obf.dom; // an action
import bitwig.obf.dvJ; // Bitwig's generic zoom actions (arranger and detail editor)
import com.bitwig.flt.app.Hvv; // the application

/**
 * One action: zoom to the selection and make the selected track tall; again: back.
 * Bitwig's own actions are run on the focused widget, the way the controller API does it
 * (ApplicationProxy.zoomToSelectionOrPrevious). Names are Bitwig 6.1.7 (macOS).
 */
public final class ZoomToggleActions implements ActionProvider {
    private static final Bml.Mod MOD = Bml.mod("zoom-toggle");

    /** Steps of "zoom in lane heights (selected)", each one makes the selected tracks taller. */
    private static final int HEIGHT_STEPS = 40;

    private static boolean zoomed = false;

    @Override
    public void declare(Actions actions) {
        actions.action("zoom_toggle_fit", "Zoom to Fit Selection (Toggle)")
               .category("arranger")
               .onRun(context -> toggle());
    }

    private static void toggle() {
        try {
            if (!zoomed) {
                run(dvJ.Bqk);                    // zoom to selection or previous (time axis)
                for (int i = 0; i < HEIGHT_STEPS; i++) {
                    run(aCZ.Bqr);                // taller selected tracks
                }
                zoomed = true;
            } else {
                run(dvJ.Bqk);                    // back to the previous time view
                for (int i = 0; i < HEIGHT_STEPS; i++) {
                    run(aCZ.Tsf);                // lower again
                }
                zoomed = false;
            }
        } catch (Throwable failure) {
            MOD.log("zoom toggle failed", failure);
        }
    }

    private static void run(dom action) {
        var window = Hvv.mEL().LRq();
        if (window == null) {
            return;
        }
        var focused = window.Sc3();
        if (focused != null) {
            focused.BBg(action);
        }
    }
}
