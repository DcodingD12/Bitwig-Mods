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
 * "Lock track height" adds "#lock" to the comment and " (L)" to the name of the selected tracks,
 * "Unlock track height" removes both again (and an older "[L]" or lock emoji in the name).
 * Each runs as one undo step. Only these marks are touched; the rest of name and comment stay as they are.
 *
 * Names are Bitwig 6.1.7 (macOS).
 */
public final class TrackHeightLockActions implements ActionProvider {
    private static final Bml.Mod MOD = Bml.mod("track-height-lock");

    @Override
    public void declare(Actions actions) {
        actions.action("track_height_lock_lock", "Lock track height")
               .in(Menu.selection("track"))
               .menuOnly()
               .visibleWhen(context -> anyUnlocked(tracks(context)))
               .onRun(context -> setLock(tracks(context), true));
        actions.action("track_height_lock_unlock", "Unlock track height")
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
            if (!TrackLock.isLocked(track)) {
                return true;
            }
        }
        return false;
    }

    /** Show "unlock" when at least one selected track has a lock mark the menu can remove (comment or name). */
    private static boolean anyTagged(List<sJU> tracks) {
        for (sJU track : tracks) {
            if (TrackLock.isLocked(track)) {
                return true;
            }
        }
        return false;
    }

    /** Adds or removes the marks on every selected track, as one undo step. */
    private static void setLock(List<sJU> tracks, boolean lock) {
        if (tracks.isEmpty()) {
            return;
        }
        CmH project = tracks.get(0).cc_();
        boolean open = false;
        try {
            project.beI(lock ? "Lock track height" : "Unlock track height", false); // open one undo step
            open = true;
            for (sJU track : tracks) {
                String comment = track.wze() == null ? "" : track.wze();
                String newComment = lock ? LockRule.withTag(comment) : LockRule.withoutTag(comment);
                if (!newComment.equals(comment)) {
                    track.Heb(newComment); // Bitwig's comment setter (the one its own "change comment" uses)
                }

                String title = track.eXf() == null ? "" : track.eXf();
                String newTitle = lock ? LockRule.withNameTag(TrackLock.currentName(track)) : LockRule.withoutNameTag(title);
                if (!newTitle.equals(title)) {
                    track.d_(newTitle); // Bitwig's title setter (the one its own "rename" uses)
                }
            }
            project.beI(false); // close the undo step
            open = false;
        } catch (Throwable failure) {
            MOD.log("could not change the lock marks", failure);
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
