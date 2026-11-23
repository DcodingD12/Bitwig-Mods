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
import java.util.List;

/**
 * One action: hide the clip launcher (if shown), zoom to the selection and make the selected track tall;
 * again: back, and show the launcher again if this mod hid it.
 *
 * The clip launcher shares its rows with the arranger, so a taller track would also make the launcher row tall.
 * That is why the launcher is hidden while zoomed. Arranger only: if the arranger isn't focused, nothing happens.
 * Bitwig's own actions are run on the focused widget, the way the controller API does it.
 * Only tracks with a selected clip become tall; with no clip selected the key does nothing.
 * While zoomed, the tall track follows the selected clips (SelectionFollower); going back then shrinks
 * exactly the tall tracks, not whatever is selected at that moment.
 * Names are Bitwig 6.1.7 (macOS).
 */
public final class ZoomToggleActions implements ActionProvider {
    private static final Bml.Mod MOD = Bml.mod("zoom-toggle");

    /** Steps of "zoom in lane heights (selected)", each one makes the selected tracks taller. */
    static final int HEIGHT_STEPS = 40;

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

            // Going in: only the tracks of the selected CLIPS become tall (with Bitwig's own lane height
            // step, on our list instead of the track selection). No clip selected -> the key does nothing.
            // If the clips can't be read: Bitwig's own action on the selected tracks, as before.
            // Going back: the tall tracks may no longer be the selected ones (the tall track followed
            // the clips, or the selection changed) -> then shrink exactly the tall ones.
            List<Object> tallBefore = null;   // what is tall after going in (start of following)
            List<Object> growThese = null;
            List<Object> shrinkThese = null;
            if (zoomed) {
                shrinkThese = SelectionFollower.stop(view);
            } else {
                List<Object> clipTracks = SelectionFollower.selectedClipTracks(view);
                switch (FollowState.firstPress(clipTracks)) {
                    case NOTHING:
                        return;                  // no clip selected: nothing half done, stay not zoomed
                    case CLIP_TRACKS:
                        growThese = clipTracks;
                        tallBefore = clipTracks;
                        break;
                    default:                     // BITWIG_ACTION
                        tallBefore = SelectionFollower.selectedTracks(view);
                        break;
                }
            }

            dom heights = zoomed ? aCZ.Tsf : aCZ.Bqr;
            if (!zoomed && view != null && view.waK()) {
                view.Bqk(false);                 // hide the clip launcher while zoomed
                launcherHiddenByUs = true;
            }
            boolean resized = zoomed
                    ? shrinkThese != null && safeResize(view, shrinkThese, -HEIGHT_STEPS)
                    : growThese != null && safeResize(view, growThese, HEIGHT_STEPS);
            if (!resized) {
                if (!zoomed && growThese != null) {
                    // fallback: Bitwig's own action grows the selected tracks -> those are the tall ones
                    tallBefore = SelectionFollower.selectedTracks(view);
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
            }
            run(focused, aCZ.Or);                // arranger zoom to selection or previous (time axis)
            if (zoomed && launcherHiddenByUs && view != null) {
                view.Bqk(true);                  // show the clip launcher again
                launcherHiddenByUs = false;
            }
            if (!zoomed && tallBefore != null) {
                SelectionFollower.start(view, tallBefore); // from now on the tall track follows the selection
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

    /** Steps exactly these tracks; false on any problem (then Bitwig's own action is used instead). */
    private static boolean safeResize(aZJ view, List<Object> tracks, int steps) {
        try {
            return SelectionFollower.resize(view, tracks, steps);
        } catch (Throwable failure) {
            log("changing the track height failed, using Bitwig's own step", failure);
            return false;
        }
    }

    static void log(String what, Throwable failure) {
        if (failure == null) {
            MOD.log(what);
        } else {
            MOD.log(what, failure);
        }
    }

    /** Runs the action on the focused widget; true if a widget handled it. */
    private static boolean run(dwF focused, dom action) {
        return focused.BBg(action) != null;
    }
}
