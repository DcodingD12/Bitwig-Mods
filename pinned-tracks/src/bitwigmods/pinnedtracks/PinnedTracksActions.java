package bitwigmods.pinnedtracks;

import bitwig.obf.GRp; // the selection of document objects behind a view's selection
import bitwig.obf.bju; // the view's selection object a selection menu opens on
import bml.actions.ActionContext;
import bml.actions.ActionProvider;
import bml.actions.Actions;
import bml.actions.Menu;
import bml.api.Bml;
import com.bitwig.flt.document.core.master.CmH; // a project (opens and closes undo steps)
import com.bitwig.flt.document.core.master.GJy; // a group track (also the project's top level)
import com.bitwig.flt.document.core.master.sJU; // a track or group track (channel)

import java.util.ArrayList;
import java.util.List;

/**
 * Two rows in the track's right-click menu (Framework: Actions and Menus): "Pin to Top" adds "#pin" to
 * the comments of the selected top-level tracks (and group tracks), "Unpin" removes it. PinnedTracksHooks
 * shows pinned tracks in the fixed area at the top. One undo step per run (Cmd+Z unpins/pins again).
 *
 * Names are Bitwig 6.1.7 (macOS).
 */
public final class PinnedTracksActions implements ActionProvider {
    private static final Bml.Mod MOD = Bml.mod("pinned-tracks");

    @Override
    public void declare(Actions actions) {
        // The framework sorts a mod's rows by action label: "Pin" < "Unpin" already.
        actions.action("pinned_tracks_pin", "Pin to Top")
               .in(Menu.selection("track"))
               .menuOnly()
               .visibleWhen(context -> !toPin(tracks(context)).isEmpty())
               .onRun(context -> apply(toPin(tracks(context)), true));
        actions.action("pinned_tracks_unpin", "Unpin")
               .in(Menu.selection("track"))
               .menuOnly()
               .visibleWhen(context -> !toUnpin(tracks(context)).isEmpty())
               .onRun(context -> apply(toUnpin(tracks(context)), false));
    }

    /** The tracks the menu was opened on (right-clicking a track selects it). Same as in track-height-lock. */
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

    /**
     * True for tracks and group tracks directly in the project's top level (not inside a group; not effect
     * tracks or the master track, which are not in that list). Only these can be pinned.
     */
    static boolean pinnable(sJU track) {
        GJy parent = track.QJW(); // the group holding the track
        return parent != null && parent == track.TUb() && parent.xDy().contains(track); // TUb: top level, xDy: its tracks
    }

    private static List<sJU> toPin(List<sJU> selected) {
        List<sJU> out = new ArrayList<>();
        for (sJU track : selected) {
            if (pinnable(track) && !PinRule.isPinned(track.wze())) { // wze: comment
                out.add(track);
            }
        }
        return out;
    }

    /** Every selected track with the tag (also ones that can't be shown pinned, so the tag can be cleaned up). */
    private static List<sJU> toUnpin(List<sJU> selected) {
        List<sJU> out = new ArrayList<>();
        for (sJU track : selected) {
            if (PinRule.isPinned(track.wze())) {
                out.add(track);
            }
        }
        return out;
    }

    private static void apply(List<sJU> tracks, boolean pin) {
        if (tracks.isEmpty()) {
            return;
        }
        CmH project = tracks.get(0).cc_(); // the project
        if (project == null) {
            return;
        }
        boolean open = false;
        try {
            project.beI(pin ? "Pin to Top" : "Unpin", false); // open one undo step
            open = true;
            for (sJU track : tracks) {
                String comment = track.wze();
                track.Heb(pin ? PinRule.withTag(comment) : PinRule.withoutTag(comment)); // Bitwig's comment setter
            }
        } catch (Throwable failure) {
            MOD.log("could not " + (pin ? "pin" : "unpin") + " the tracks", failure);
        } finally {
            if (open) {
                try {
                    project.beI(false); // close the undo step
                } catch (Throwable failure) {
                    MOD.log("could not close the undo step", failure);
                }
            }
        }
    }
}
