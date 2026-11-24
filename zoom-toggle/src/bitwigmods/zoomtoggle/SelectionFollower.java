package bitwigmods.zoomtoggle;

import bitwig.obf.BVS; // an arranger clip (note or audio)
import bitwig.obf.aGq; // the arranger's track list widget (owns the selection model)
import bitwig.obf.aZJ; // the arranger view
import bitwig.obf.bkD; // the arranger selection model (selected tracks)
import bitwig.obf.buZ; // the widget with all track rows (Bitwig's "zoom lane heights" step lives here)
import bitwig.obf.cxg; // an observable list (here: the selected clips)
import bitwig.obf.oxD; // what a clip sits in (knows its track)
import bitwig.obf.doe; // a callback that runs once per frame, before layout and painting
import bitwig.obf.dre; // one frame's time
import bitwig.obf.dsB; // Bitwig's frame clock
import bitwig.obf.dwF; // a widget
import bitwig.obf.epr; // widget tree helpers
import bitwig.obf.loD; // a listener for an observable list
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import com.bitwig.flt.document.core.iface.zCn; // a track

/**
 * While zoomed: when the selected CLIPS change, the tall track follows: the tracks of the selected clips
 * become tall, the previous tall ones small. Selecting only a track, or clicking into empty space
 * (no clip selected), changes nothing.
 *
 * How (Bitwig 6.1.7, read from the bytecode):
 * - Bitwig's "zoom in/out lane heights (selected)" ends in buZ.beI(int direction, true, List tracks):
 *   it steps the height of every track row whose track is in the list. Bitwig passes the selected
 *   tracks (bkD.sCA()); we pass our own list. Same code, same step size, no selection change.
 * - The arranger's selected objects (clips) are an observable list: aZJ.eNb() (arranger timeline) .yX3()
 *   (object selection, the one Bitwig's bjs/bkb use) .jvQ(). A clip (BVS) -> ZRB() (its lane) -> GwM() (its track),
 *   the same way Bitwig's bkb.scr() gets the lanes of the selected clips.
 * - The listener only notes "changed"; the work runs once on the next frame (Bitwig's frame clock),
 *   so a selection change is finished first and several events become one step.
 *
 * Any error: following stops (FollowState.fail), the second press falls back to the old behaviour.
 * Everything runs on Bitwig's UI thread (selection changes and frames).
 */
final class SelectionFollower implements loD, doe {
    private static final SelectionFollower INSTANCE = new SelectionFollower();
    private static final FollowState<Object> STATE = new FollowState<>();

    private static aZJ view;       // the arranger view we zoomed in
    private static cxg selection;  // the observable list of selected clips we listen to
    private static dsB clock;      // registered only while one step is waiting for the next frame
    private static boolean loggedFailure = false;

    private SelectionFollower() {
    }

    /** The selected tracks, as Bitwig's "lane heights (selected)" step sees them; null if unknown. */
    static List<Object> selectedTracks(aZJ arrangerView) {
        bkD model = selectionModel(arrangerView);
        // bkD.unz(): lane selection mode, then Bitwig steps lanes instead of tracks -> don't follow.
        if (model == null || model.unz()) {
            return null;
        }
        List<?> tracks = model.sCA();
        return tracks == null ? null : new ArrayList<>(tracks);
    }

    /**
     * The tracks of the selected arranger clips (each once; empty = no clip selected),
     * or null if unknown (lane mode, no arranger view, any error) -> then the old behaviour is used.
     */
    static List<Object> selectedClipTracks(aZJ arrangerView) {
        try {
            if (selectedTracks(arrangerView) == null) { // also covers lane mode (bkD.unz())
                return null;
            }
            cxg clips = clipSelection(arrangerView);
            return clips == null ? null : clipTracks(clips);
        } catch (Throwable failure) {
            failed("could not read the selected clips", failure);
            return null;
        }
    }

    /**
     * First press is done: these tracks were made tall. Starts listening to the clip selection.
     * Errors only switch following off.
     */
    static void start(aZJ arrangerView, List<Object> tallTracks) {
        try {
            stopListening();
            cxg clips = clipSelection(arrangerView);
            if (clips == null || tallTracks == null) {
                return;
            }
            STATE.start(tallTracks);
            view = arrangerView;
            selection = clips;
            selection.beI(INSTANCE);       // cxg.beI(loD) = add listener
        } catch (Throwable failure) {
            STATE.fail();
            stopListening();
            failed("could not start following the selection", failure);
        }
    }

    /**
     * Second press: stops listening and returns the tracks that must become small,
     * or null if Bitwig's own "zoom out lane heights (selected)" hits exactly the right tracks
     * (or after an error = the old behaviour).
     */
    static List<Object> stop(aZJ arrangerView) {
        try {
            stopListening();
            List<Object> selected = arrangerView == null ? null : selectedTracks(arrangerView);
            return STATE.end(selected == null ? List.of() : selected);
        } catch (Throwable failure) {
            STATE.end(List.of());
            failed("could not stop following the selection", failure);
            return null;
        }
    }

    /**
     * Steps the height of exactly these tracks (steps > 0 taller, < 0 smaller) with Bitwig's own
     * lane height step. False if the track rows weren't found (nothing changed then).
     */
    static boolean resize(aZJ arrangerView, List<Object> tracks, int steps) {
        if (tracks.isEmpty()) {
            return true;
        }
        buZ rows = trackRows(arrangerView);
        if (rows == null) {
            return false;
        }
        int direction = steps > 0 ? 1 : -1;
        for (int i = 0; i < Math.abs(steps); i++) {
            rows.beI(direction, true, tracks); // the same call Bitwig's action makes, with our list
        }
        return true;
    }

    // ---- selection listener (loD): only notes the change, the work waits for the next frame ----

    @Override
    public void insertedItems(cxg list, int from, int count) {
        selectionChanged();
    }

    @Override
    public void aboutToRemoveItems(cxg list, int from, int count) {
        selectionChanged(); // the removal itself happens after this call; the step runs later anyway
    }

    @Override
    public void changedItems(cxg list, int from, int count) {
        selectionChanged();
    }

    @Override
    public void aboutToChangeItems(cxg list, int from, int count) {
    }

    @Override
    public void addedAsListenerTo(cxg list) {
    }

    @Override
    public void aboutToRemoveAsListenerFrom(cxg list) {
    }

    private static void selectionChanged() {
        try {
            if (STATE.requestFollow() && clock == null) {
                clock = dsB.Gef(INSTANCE); // run aboutToPaint once, on the next frame
            }
        } catch (Throwable failure) {
            STATE.fail();
            failed("could not schedule following the selection", failure);
        }
    }

    // ---- the step itself, once on the next frame (doe) ----

    @Override
    public void aboutToPaint(dre frame) {
        unregisterClock(); // one shot
        try {
            // vTn = shown. Lane mode (bkD.unz()) -> no following, like for the first press.
            boolean usable = view != null && view.vTn() && selectedTracks(view) != null;
            List<Object> wanted = usable && selection != null ? clipTracks(selection) : List.of();
            FollowState.Step<Object> step = STATE.follow(wanted);
            if (step.isEmpty()) {
                return;
            }
            STATE.begin();
            // shrink first, grow last: Bitwig's step also scrolls to the tracks it changed -> ends on the new one
            boolean ok = resize(view, step.shrink, -ZoomToggleActions.HEIGHT_STEPS)
                      && resize(view, step.grow, ZoomToggleActions.HEIGHT_STEPS);
            STATE.finish();
            if (!ok) {
                STATE.fail();
                failed("track rows not found, following switched off", null);
            }
        } catch (Throwable failure) {
            STATE.fail();
            failed("following the selection failed", failure);
        }
    }

    // ---- helpers ----

    /** The arranger's observable list of selected objects (clips), or null. */
    private static cxg clipSelection(aZJ arrangerView) {
        if (arrangerView == null || arrangerView.eNb() == null || arrangerView.eNb().yX3() == null) {
            return null;
        }
        return arrangerView.eNb().yX3().jvQ(); // BPz (arranger timeline) -> WSI (object selection) -> list
    }

    /** The tracks of the selected clips (each once); other selected objects are skipped. */
    private static List<Object> clipTracks(cxg clips) {
        List<Object> items = new ArrayList<>();
        for (int i = 0; i < clips.Gef(); i++) { // cxg.Gef() = size, Gef(int) = item
            items.add(clips.Gef(i));
        }
        return FollowState.tracksOf(items, SelectionFollower::trackOfClip);
    }

    private static Object trackOfClip(Object item) {
        if (!(item instanceof BVS clip)) {
            return null;
        }
        oxD lane = clip.ZRB();
        zCn track = lane == null ? null : lane.GwM();
        return track;
    }

    private static bkD selectionModel(aZJ arrangerView) {
        if (arrangerView == null) {
            return null;
        }
        aGq trackList = arrangerView.ocs();
        return trackList == null ? null : trackList.UIe();
    }

    /**
     * The outermost buZ under the arranger's track list = the one Bitwig's action uses (aGq field gNZ).
     * epr.beI visits parents before children, so the first buZ found is not one nested in a group.
     * Searched inside the scrolling part only (aGj.rcd(), the scroll view around Bitwig's list): the mod
     * "Pinned Tracks" adds a second list above it, which would be found first otherwise.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static buZ trackRows(aZJ arrangerView) {
        aGq trackList = arrangerView == null ? null : arrangerView.ocs();
        if (trackList == null) {
            return null;
        }
        buZ[] found = new buZ[1];
        Consumer first = widget -> {
            if (found[0] == null) {
                found[0] = (buZ) widget;
            }
        };
        dwF scrolling = trackList.rcd(); // the scroll view around Bitwig's own track list
        epr.beI(scrolling != null ? scrolling : trackList, buZ.class, first);
        // sanity check: it must belong to this arranger view (buZ.XzC() = its arranger view)
        return found[0] != null && found[0].XzC() == arrangerView ? found[0] : null;
    }

    private static void stopListening() {
        unregisterClock();
        if (selection != null) {
            try {
                selection.Gef(INSTANCE); // cxg.Gef(loD) = remove listener
            } catch (Throwable ignored) {
                // the list may be gone with its project; nothing left to undo
            }
        }
        selection = null;
        view = null;
    }

    private static void unregisterClock() {
        if (clock != null) {
            try {
                clock.beI(); // unregister from the frame clock
            } catch (Throwable ignored) {
                // already gone
            }
            clock = null;
        }
    }

    private static void failed(String what, Throwable failure) {
        if (!loggedFailure) {
            loggedFailure = true;
            ZoomToggleActions.log(what, failure);
        }
    }
}
