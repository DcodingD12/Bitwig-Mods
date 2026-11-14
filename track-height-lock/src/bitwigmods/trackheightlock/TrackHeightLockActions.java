package bitwigmods.trackheightlock;

import bitwig.obf.GRp; // the selection of document objects behind a view's selection
import bitwig.obf.bju; // the view's selection object a selection menu opens on
import bml.actions.ActionContext;
import bml.actions.ActionProvider;
import bml.actions.Actions;
import bml.actions.Menu;
import bml.api.Bml;
import com.bitwig.flt.document.core.master.CmH; // a project (opens and closes undo steps)
import com.bitwig.flt.document.core.master.sJU; // a track or group track (channel)

import java.util.ArrayList;
import java.util.List;

/**
 * Two rows in the track's right-click menu (Framework: Actions and Menus):
 * "Spurhöhe sperren" adds "#lock" to the comment of the selected tracks,
 * "Spurhöhe entsperren" removes it again. Each runs as one undo step.
 * Only the "#lock" tag is touched; the name and the rest of the comment stay as they are.
 *
 * Names are Bitwig 6.1.7 (macOS).
 */
public final class TrackHeightLockActions implements ActionProvider {
    private static final Bml.Mod MOD = Bml.mod("track-height-lock");

    @Override
    public void declare(Actions actions) {
        actions.action("track_height_lock_lock", "Spurhöhe sperren")
               .in(Menu.selection("track"))
               .menuOnly()
               .visibleWhen(context -> anyUnlocked(tracks(context)))
               .onRun(context -> setLock(tracks(context), true));
        actions.action("track_height_lock_unlock", "Spurhöhe entsperren")
               .in(Menu.selection("track"))
               .menuOnly()
               .visibleWhen(context -> anyTagged(tracks(context)))
               .onRun(context -> setLock(tracks(context), false));
    }

    /** The tracks the menu was opened on (right-clicking a track selects it). */
    static List<sJU> tracks(ActionContext context) {
        List<sJU> tracks = new ArrayList<>();
        if (context.selection() instanceof bju view) {
            GRp selection = view.unz(); // the selection behind the view
            if (selection != null) {
                for (Object item : selection.MgQ()) { // the selected objects
                    if (item instanceof sJU track) {
                        tracks.add(track);
                    }
                }
            }
        }
        return tracks;
    }

    /** Show "lock" when at least one selected track isn't locked yet (by name or comment). */
    private static boolean anyUnlocked(List<sJU> tracks) {
        for (sJU track : tracks) {
            if (!LockRule.isLocked(track.cb_(), track.wze())) { // cb_: shown name, wze: comment
                return true;
            }
        }
        return false;
    }

    /** Show "unlock" when at least one selected track has "#lock" in its comment. */
    private static boolean anyTagged(List<sJU> tracks) {
        for (sJU track : tracks) {
            if (LockRule.commentIsLocked(track.wze())) {
                return true;
            }
        }
        return false;
    }

    /** Adds or removes the tag on every selected track, as one undo step. */
    private static void setLock(List<sJU> tracks, boolean lock) {
        if (tracks.isEmpty()) {
            return;
        }
        CmH project = tracks.get(0).cc_();
        boolean open = false;
        try {
            project.beI(lock ? "Spurhöhe sperren" : "Spurhöhe entsperren", false); // open one undo step
            open = true;
            for (sJU track : tracks) {
                String comment = track.wze() == null ? "" : track.wze();
                String changed = lock ? LockRule.withTag(comment) : LockRule.withoutTag(comment);
                if (!changed.equals(comment)) {
                    track.Heb(changed); // Bitwig's comment setter (the one its own "change comment" uses)
                }
            }
            project.beI(false); // close the undo step
            open = false;
        } catch (Throwable failure) {
            MOD.log("could not change the lock tag", failure);
            if (open) {
                try {
                    project.beI(false);
                    project.cov(); // undo the half-done step
                } catch (Throwable ignored) {
                    // nothing more we can do; the log above has the cause
                }
            }
        }
    }
}
