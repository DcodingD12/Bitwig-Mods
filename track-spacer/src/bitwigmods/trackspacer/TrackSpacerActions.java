package bitwigmods.trackspacer;

import bitwig.obf.GRp; // the selection of document objects behind a view's selection
import bitwig.obf.bju; // the view's selection object a selection menu opens on
import bml.actions.ActionContext;
import bml.actions.ActionProvider;
import bml.actions.Actions;
import bml.actions.Menu;
import bml.api.Bml;
import com.bitwig.flt.document.core.master.CmH; // a project
import com.bitwig.flt.document.core.master.GJy; // a group track (also the project's top level)
import com.bitwig.flt.document.core.master.sJU; // a track or group track (channel)

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Three rows in the track's right-click menu (Framework: Actions and Menus), like REAPER's
 * "Insert spacer before/after tracks" and "Remove spacer". They only write tags into track comments
 * ("#spacer-before" / "#spacer-after"); TrackSpacerHooks draws the gaps. Each run is one undo step.
 *
 * Names are Bitwig 6.1.7 (macOS).
 */
public final class TrackSpacerActions implements ActionProvider {
    private static final Bml.Mod MOD = Bml.mod("track-spacer");

    @Override
    public void declare(Actions actions) {
        // The framework sorts a mod's menu rows by their action LABEL (alphabetically) but shows the
        // MENU label. For menu-only actions the action label is shown nowhere else, so it is used here
        // only to put "Before" above "After" ("... 1 ..." < "... 2 ..."); the menu shows the real text.
        actions.action("track_spacer_insert_before", "Insert Spacer 1 Before Tracks")
               .menuLabel("Insert Spacer Before Tracks")
               .in(Menu.selection("track"))
               .menuOnly()
               .visibleWhen(context -> !plans(tracks(context), SpacerPlan.Side.BEFORE).isEmpty())
               .onRun(context -> insert(tracks(context), SpacerPlan.Side.BEFORE));
        actions.action("track_spacer_insert_after", "Insert Spacer 2 After Tracks")
               .menuLabel("Insert Spacer After Tracks")
               .in(Menu.selection("track"))
               .menuOnly()
               .visibleWhen(context -> !plans(tracks(context), SpacerPlan.Side.AFTER).isEmpty())
               .onRun(context -> insert(tracks(context), SpacerPlan.Side.AFTER));
        actions.action("track_spacer_remove", "Remove Spacer")
               .in(Menu.selection("track"))
               .menuOnly()
               .visibleWhen(context -> !clears(tracks(context)).isEmpty())
               .onRun(context -> remove(tracks(context)));
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

    /** One group's track list (top to bottom) with the selection and existing tags. */
    private record Lane(List<sJU> order, boolean[] selected, boolean[] before, boolean[] after) {
    }

    /**
     * The track lists holding the selected tracks and group tracks (a group's gap goes above its header
     * or below the end of the group). Effect tracks and master tracks are not in these lists.
     */
    private static List<Lane> lanes(List<sJU> selectedTracks) {
        Set<sJU> selected = Collections.newSetFromMap(new IdentityHashMap<>());
        selected.addAll(selectedTracks);
        Map<GJy, Lane> lanes = new LinkedHashMap<>();
        for (sJU track : selectedTracks) {
            GJy group = track.QJW(); // the group (or the project's top level) holding the track
            if (group == null || lanes.containsKey(group)) {
                continue;
            }
            List<sJU> order = new ArrayList<>();
            for (Object item : group.xDy()) { // the group's tracks, top to bottom
                if (item instanceof sJU t) {
                    order.add(t);
                }
            }
            if (!order.contains(track)) {
                continue; // an effect track or the master track: not in this list
            }
            int n = order.size();
            boolean[] isSelected = new boolean[n];
            boolean[] before = new boolean[n];
            boolean[] after = new boolean[n];
            for (int i = 0; i < n; i++) {
                sJU t = order.get(i);
                isSelected[i] = selected.contains(t);
                before[i] = SpacerRule.gapBefore(t.wze()) > 0; // wze: comment
                after[i] = SpacerRule.gapAfter(t.wze()) > 0;
            }
            lanes.put(group, new Lane(order, isSelected, before, after));
        }
        return new ArrayList<>(lanes.values());
    }

    /** New comments for the tracks to tag (empty: nothing to do). */
    private static Map<sJU, String> plans(List<sJU> selectedTracks, SpacerPlan.Side side) {
        Map<sJU, String> changes = new LinkedHashMap<>();
        for (Lane lane : lanes(selectedTracks)) {
            for (int i : SpacerPlan.toTag(lane.selected(), lane.before(), lane.after(), side)) {
                sJU track = lane.order().get(i);
                String comment = track.wze();
                changes.put(track, side == SpacerPlan.Side.BEFORE ? SpacerRule.withBefore(comment) : SpacerRule.withAfter(comment));
            }
        }
        return changes;
    }

    /** New comments without the tags of every gap touching a selected track (empty: nothing to do). */
    private static Map<sJU, String> clears(List<sJU> selectedTracks) {
        Map<sJU, String> changes = new LinkedHashMap<>();
        for (Lane lane : lanes(selectedTracks)) {
            for (SpacerPlan.Clear clear : SpacerPlan.toClear(lane.selected(), lane.before(), lane.after())) {
                sJU track = lane.order().get(clear.index());
                String comment = track.wze();
                if (clear.before()) {
                    comment = SpacerRule.withoutBefore(comment);
                }
                if (clear.after()) {
                    comment = SpacerRule.withoutAfter(comment);
                }
                changes.put(track, comment);
            }
        }
        return changes;
    }

    private static void insert(List<sJU> selectedTracks, SpacerPlan.Side side) {
        apply(plans(selectedTracks, side),
                side == SpacerPlan.Side.BEFORE ? "Insert Spacer Before Tracks" : "Insert Spacer After Tracks");
    }

    private static void remove(List<sJU> selectedTracks) {
        apply(clears(selectedTracks), "Remove Spacer");
    }

    /**
     * Writes the new comments WITHOUT an undo entry (Cmd+Z never adds or removes a spacer; only this menu
     * does). The project is marked as changed, so Bitwig asks to save it. The comment listener in
     * TrackSpacerHooks redraws the gaps.
     */
    private static void apply(Map<sJU, String> changes, String what) {
        if (changes.isEmpty()) {
            return;
        }
        try {
            TrackSpacerHooks.runWithoutUndo(changes.keySet(), () -> {
                for (Map.Entry<sJU, String> change : changes.entrySet()) {
                    change.getKey().Heb(change.getValue()); // Bitwig's comment setter
                }
            });
            CmH project = changes.keySet().iterator().next().cc_();
            if (project != null) {
                project.nhI(true); // "is_modified" (not undoable itself): Bitwig asks to save
            }
        } catch (Throwable failure) {
            MOD.log("could not change the spacer tags (" + what + ")", failure);
        }
        for (sJU track : changes.keySet()) {
            TrackSpacerHooks.refresh(track); // in case Bitwig's comment event didn't reach us
        }
    }
}
