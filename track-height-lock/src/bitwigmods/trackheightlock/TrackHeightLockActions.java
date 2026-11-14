package bitwigmods.trackheightlock;

import bitwig.obf.GRp; // the selection of document objects behind a view's selection
import bitwig.obf.bju; // the view's selection object a selection menu opens on
import bml.actions.ActionContext;
import bml.actions.ActionProvider;
import bml.actions.Actions;
import bml.actions.Menu;
import bml.api.Bml;
import com.bitwig.flt.document.core.master.CmH; // a project ("is_modified" = has unsaved changes)
import com.bitwig.flt.document.core.master.sJU; // a track or group track (channel)

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Two rows in the track's right-click menu (Framework: Actions and Menus):
 * "Lock track height" adds "#lock" to the comment and " (L)" to the name of the selected tracks,
 * "Unlock track height" removes both again (and an older "[L]" or lock emoji in the name).
 * Not undoable on purpose (Cmd+Z doesn't lock or unlock). Only these marks are touched;
 * the rest of name and comment stay as they are.
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

    /**
     * Adds or removes the marks on every selected track. Deliberately NOT undoable: Cmd+Z never
     * locks or unlocks a track; only this menu does. The project is marked as changed, so Bitwig
     * asks to save it.
     */
    private static void setLock(List<sJU> tracks, boolean lock) {
        if (tracks.isEmpty()) {
            return;
        }
        try {
            // Work out every new value first, so nothing is left half-done if a track is odd.
            List<sJU> targets = new ArrayList<>();
            List<String> comments = new ArrayList<>();
            List<String> titles = new ArrayList<>();
            Set<Object> attributes = Collections.newSetFromMap(new IdentityHashMap<>());
            for (sJU track : tracks) {
                String comment = track.wze() == null ? "" : track.wze(); // wze: comment
                String title = track.eXf() == null ? "" : track.eXf();   // eXf: the track's own title
                String newComment = lock ? LockRule.withTag(comment) : LockRule.withoutTag(comment);
                String newTitle = lock ? LockRule.withNameTag(TrackLock.currentName(track)) : LockRule.withoutNameTag(title);
                targets.add(track);
                comments.add(newComment.equals(comment) ? null : newComment);
                titles.add(newTitle.equals(title) ? null : newTitle);
                attributes.add(track.RiZ().jTs()); // the "title" attribute, as Bitwig's setter passes it
                attributes.add(track.RiZ().mEL()); // the "comment" attribute
            }

            boolean[] changed = {false};
            TrackHeightLockHooks.runWithoutUndo(attributes, () -> {
                for (int i = 0; i < targets.size(); i++) {
                    if (comments.get(i) != null) {
                        targets.get(i).Heb(comments.get(i)); // Bitwig's comment setter
                        changed[0] = true;
                    }
                    if (titles.get(i) != null) {
                        targets.get(i).d_(titles.get(i)); // Bitwig's title setter (the one "rename" uses)
                        changed[0] = true;
                    }
                }
            });
            if (changed[0]) {
                CmH project = targets.get(0).cc_();
                project.nhI(true); // "is_modified" (not undoable itself): Bitwig asks to save
            }
        } catch (Throwable failure) {
            MOD.log("could not change the lock marks", failure);
        }
    }
}
