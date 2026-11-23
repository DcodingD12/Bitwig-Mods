package bitwigmods.trackspacer;

import bitwig.obf.NVD;  // Bitwig's listener: eventOccurred(Object)
import bitwig.obf.YCD;  // a (non-group) track, as the UI sees it
import bitwig.obf.aZJ;  // the arranger panel a row belongs to
import bitwig.obf.dgR;  // a project's undo history
import bitwig.obf.dgT;  // the undo recorder's observer (records attribute changes)
import bitwig.obf.diF;  // the undo steps (applies undo/redo)
import com.bitwig.flt.document.core.master.gEH; // a channel (comment setter PJ3)
import com.bitwig.flt.document.core.master.kd;  // meta class of channels (generic setters, used by undo/redo)
import com.bitwig.ramona.core.ytz;              // a document object
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import bitwig.obf.buH;  // one arranger row of a group track (header, timeline, launcher): header + its tracks
import bitwig.obf.buT;  // a track header in the arranger: [colour strip, bvc row] side by side
import bitwig.obf.bvi;  // which kind of row a bvc is (header, timeline, launcher ...)
import bitwig.obf.bvc;  // one arranger row of a track (header, timeline lane, launcher slots, launcher controls)
import bitwig.obf.dAc;  // something a widget can hold as its content
import bitwig.obf.dXL;  // a box that stacks widgets (here: top to bottom)
import bitwig.obf.dZl;  // an empty spacer widget inside such a box (min/max size)
import bitwig.obf.dwF;  // a widget
import bitwig.obf.ekS;  // what a box takes as its background
import bml.api.Arg;
import bml.api.At;
import bml.api.Bml;
import bml.api.Hook;
import bml.api.This;
import com.bitwig.flt.document.core.master.GJy; // a group track (also the project's top level)
import com.bitwig.flt.document.core.master.sJU; // a track (channel)

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Arranger: Track Spacer. A gap of N pixels above and/or below a track, without an extra track.
 *
 * Every arranger row of a track is a bvc: the track header, the timeline lane, the clip launcher
 * slots and launcher controls in the arranger. bvc.Gef(YCD, bvi, aZJ) builds the row and, as its
 * last step, sets the row's content (bvc.beI(dAc)). This mod replaces that one call: the content
 * is put into a box [spacer, content, spacer]. The spacers are 0 px unless the track's comment
 * has "#spacer-before" / "#spacer-after". Because every row of the track gets the same spacers,
 * header, timeline and launcher stay lined up. (Rows stack by their widget sizes; the only reader
 * of the track height for layout is bvc itself.)
 *
 * A listener on the track's comment resizes the spacers whenever the comment changes
 * (menu, typing in the inspector, undo/redo).
 *
 * Names are Bitwig 6.1.7 (macOS).
 */
public final class TrackSpacerHooks {
    private static final Bml.Mod MOD = Bml.mod("track-spacer");
    private static volatile boolean loggedProblem = false;

    /** Per track: its rows' spacers ([0] above, [1] below), held weakly so closed rows can go. */
    private static final Map<sJU, List<WeakReference<dZl>[]>> ROWS = new WeakHashMap<>();

    private TrackSpacerHooks() {
    }

    // bvc.Gef(YCD, bvi, aZJ): builds a track's arranger row; its last call, this.beI(dAc), sets the row's content.
    @Hook(id = "track-row-spacers", target = bvc.class, method = "Gef(LYCD;Lbvi;LaZJ;)V",
            at = At.REPLACE_CALL, callOwner = bvc.class, call = "beI(LdAc;)V", occurrence = 1)
    public static void setRowContent(bvc row, dAc content) {
        dAc result = content;
        if (!Boolean.TRUE.equals(IN_HEADER.get())) { // header rows get their spacers around the whole header
            result = withSpacers(row.lIt(), content, row); // lIt: the track this row shows
        }
        row.beI(result); // what Bitwig does normally
    }

    // The track header (buT.sCA(YCD, GRp, aZJ)) is a side-by-side box: [colour strip + selection, bvc row].
    // The strip is a sibling of the row and as high as the row, so gaps inside the row would show the
    // strip. Here the header's bvc gets NO spacers (IN_HEADER) ...
    private static final ThreadLocal<Boolean> IN_HEADER = new ThreadLocal<>();
    private static final ThreadLocal<YCD> HEADER_TRACK = new ThreadLocal<>();

    @Hook(id = "header-row-without-spacers", target = buT.class, method = "sCA(LYCD;LGRp;LaZJ;)V",
            at = At.REPLACE_CALL, callOwner = bvc.class, call = "beI(LYCD;Lbvi;LaZJ;)Lbvc;", occurrence = 1)
    public static bvc buildHeaderRow(YCD track, bvi kind, aZJ panel) {
        IN_HEADER.set(Boolean.TRUE);
        try {
            return bvc.beI(track, kind, panel); // what Bitwig does normally
        } finally {
            IN_HEADER.remove();
            HEADER_TRACK.set(track); // for setHeaderContent, a few lines later in the same method
        }
    }

    // ... and the header's whole content (strip + row) gets the spacers around it instead.
    @Hook(id = "header-spacers", target = buT.class, method = "sCA(LYCD;LGRp;LaZJ;)V",
            at = At.REPLACE_CALL, callOwner = buT.class, call = "beI(LdAc;)V", occurrence = 1)
    public static void setHeaderContent(buT header, dAc content) {
        YCD track = HEADER_TRACK.get();
        HEADER_TRACK.remove();
        header.beI(withSpacers(track, content, header)); // what Bitwig does normally, wrapped
    }

    // ---- Spacer tags outside of Undo ------------------------------------------------------------
    // 1. The menu (and the copy check) change the tags without an undo entry: while runWithoutUndo
    //    runs, Bitwig's undo recorder is told "don't record this object" for exactly these tracks.
    //    dgT.aboutToChangeAttributeValue(dgs, ytz, String) asks dgR.beI(ytz) first; true = not recorded
    //    (Bitwig's own path). Not dgs.dvk() there: track-height-lock replaces that call.
    // 2. Undo/Redo of other steps write older comments back through the meta setter kd.sCA(gEH,int,String)
    //    -> gEH.PJ3(String). While Undo/Redo runs, the spacer tags in that value are replaced by the
    //    track's current ones, so Undo/Redo never adds, removes or changes a spacer.

    private static volatile Thread silentThread = null;
    private static final Set<Object> SILENT = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final ThreadLocal<int[]> UNDO_DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    /** Runs the changes with these tracks left out of Undo (only these, only on this thread). */
    static void runWithoutUndo(Collection<? extends sJU> tracks, Runnable changes) {
        synchronized (SILENT) {
            SILENT.clear();
            SILENT.addAll(tracks);
        }
        silentThread = Thread.currentThread();
        try {
            changes.run();
        } finally {
            silentThread = null;
            synchronized (SILENT) {
                SILENT.clear();
            }
        }
    }

    @Hook(id = "spacer-tags-without-undo", target = dgT.class,
            method = "aboutToChangeAttributeValue(Ldgs;Lcom/bitwig/ramona/core/ytz;Ljava/lang/String;)V",
            at = At.REPLACE_CALL, callOwner = dgR.class, call = "beI(Lcom/bitwig/ramona/core/ytz;)Z", occurrence = 1)
    public static boolean notRecorded(dgR recorder, ytz object) {
        // Fast path: runs for every text change in Bitwig, normally just Bitwig's answer.
        if (silentThread == Thread.currentThread()) {
            synchronized (SILENT) {
                if (SILENT.contains(object)) {
                    return true; // our spacer tags: no undo entry
                }
            }
        }
        return recorder.beI(object);
    }

    // dgR.LYs() = Undo, dgR.mEL() = Redo; diF.scr() / diF.Bqk() apply the history step.
    @Hook(id = "undo-marker", target = dgR.class, method = "LYs()V",
            at = At.REPLACE_CALL, callOwner = diF.class, call = "scr()V", occurrence = 1)
    public static void undo(diF history) {
        UNDO_DEPTH.get()[0]++;
        try {
            history.scr();
        } finally {
            UNDO_DEPTH.get()[0]--;
        }
    }

    @Hook(id = "redo-marker", target = dgR.class, method = "mEL()V",
            at = At.REPLACE_CALL, callOwner = diF.class, call = "Bqk()V", occurrence = 1)
    public static void redo(diF history) {
        UNDO_DEPTH.get()[0]++;
        try {
            history.Bqk();
        } finally {
            UNDO_DEPTH.get()[0]--;
        }
    }

    @Hook(id = "undo-keeps-spacer-tags", target = kd.class,
            method = "sCA(Lcom/bitwig/flt/document/core/master/gEH;ILjava/lang/String;)V",
            at = At.REPLACE_CALL, callOwner = gEH.class, call = "PJ3(Ljava/lang/String;)V", occurrence = 1)
    public static void setComment(gEH channel, String comment) {
        String value = comment;
        try {
            if (UNDO_DEPTH.get()[0] > 0 && channel instanceof sJU track) {
                value = SpacerRule.keepTags(comment, track.wze()); // wze: the comment now
            }
        } catch (Throwable t) {
            value = comment;
            if (!loggedProblem) {
                loggedProblem = true;
                MOD.log("could not keep the spacer tags during undo/redo", t);
            }
        }
        channel.PJ3(value); // what Bitwig does normally (writes the comment)
    }

    // Group tracks: every arranger row of a group (header, timeline, launcher) is a buH, built in
    // buH.Gef(uVN, buK, aZJ). Its content is the whole group block: the group's header (with its colour
    // strip inside) and, below it, the fold-out container with the group's tracks. It is set with
    // this.beI(dAc) in one of two branches (with or without the strip). Wrapping that block puts
    // "before" above the group header and "after" below the end of the group (below its tracks
    // when the group is open, right below the header when it is closed).
    @Hook(id = "group-spacers", target = buH.class, method = "Gef(Lcom/bitwig/flt/document/core/iface/uVN;LbuK;LaZJ;)V",
            at = At.REPLACE_CALL, callOwner = buH.class, call = "beI(LdAc;)V", occurrence = 1)
    public static void setGroupContent(buH row, dAc content) {
        row.beI(withSpacers(row.lN_(), content, row)); // lN_: the group this row shows
    }

    @Hook(id = "group-spacers-plain", target = buH.class, method = "Gef(Lcom/bitwig/flt/document/core/iface/uVN;LbuK;LaZJ;)V",
            at = At.REPLACE_CALL, callOwner = buH.class, call = "beI(LdAc;)V", occurrence = 2)
    public static void setPlainGroupContent(buH row, dAc content) {
        row.beI(withSpacers(row.lN_(), content, row));
    }

    /**
     * The content in a top-to-bottom box [spacer, content, spacer], or the content unchanged if the
     * track gets no spacers (or anything is odd: never break Bitwig).
     */
    private static dAc withSpacers(Object shown, dAc content, dwF themeOwner) {
        try {
            sJU track = trackWithSpacers(shown);
            if (track == null || !(content instanceof dwF widget)) {
                return content;
            }
            seeProject(track);
            dXL box = dXL.sSO();          // a box that stacks top to bottom (as bvc's own)
            // The spacers MUST be direct children of this box: a box only lays out children
            // that are spacers or have a size > 0 in both directions (dXU.kwH), and a box
            // holding nothing but a spacer is 0 wide -> it would be dropped (bug in 0.2.1).
            dZl above = box.sCA(0L);      // a fixed spacer, 0 px for now
            box.IvP(widget);              // Bitwig's content
            dZl below = box.sCA(0L);
            // The box's background colour is painted only over its spacers and the gaps between
            // its children, never under the content (dXL.beI(ijY, SZB)). Its parent (the row or
            // header, painting the selection colour) paints first, so the gaps show this colour:
            // zzi = theme "Grey 1", the colour Bitwig paints between tracks (header box in buT,
            // timeline track list mFm).
            box.beI((ekS) themeOwner.mum().zzi()); // mum: the widget's theme colours
            register(track, above, below);
            return box;
        } catch (Throwable t) {
            if (!loggedProblem) {
                loggedProblem = true;
                MOD.log("could not add spacers to a track row, rows are shown as usual", t);
            }
            return content;
        }
    }

    // GJy.beI(sJU, int): puts a track into a group's track list. Duplicating, copy & paste (and also
    // adding new tracks, grouping and moving) go through here. A copy is a NEW object that was never
    // part of the project; tracks that are moved or grouped are the same objects as before.
    // So: tracks we have never seen in the project lose their spacer tags here, before they are
    // inserted (no undo step of its own; the copy is not in the project yet).
    @Hook(id = "copies-without-spacers", target = GJy.class,
            method = "beI(Lcom/bitwig/flt/document/core/master/sJU;I)V", at = At.ENTRY)
    public static void beforeInsertTrack(@This GJy group, @Arg(0) sJU inserted) {
        try {
            if (inserted == null) {
                return;
            }
            GJy root = group.TUb(); // TUb: the project's top-level group
            boolean projectKnown;
            synchronized (SEEN) {
                // Only once the project is shown (its rows built, all its tracks recorded): while a
                // project loads nothing is ever removed.
                projectKnown = root != null && SEEN_PROJECTS.containsKey(root);
            }
            if (!projectKnown) {
                return;
            }
            List<sJU> tracks = new ArrayList<>();
            collect(inserted, tracks); // the track, or a group with all tracks inside
            synchronized (SEEN) {
                for (sJU track : tracks) {
                    String comment = track.wze(); // wze: comment
                    if (CopyRule.removeTags(true, SEEN.containsKey(track), comment)) {
                        String clean = SpacerRule.withoutAll(comment);
                        runWithoutUndo(List.of(track), () -> track.Heb(clean)); // Bitwig's comment setter, no undo entry
                    }
                    SEEN.put(track, Boolean.TRUE); // from now on it is part of the project
                }
            }
        } catch (Throwable t) {
            if (!loggedProblem) {
                loggedProblem = true;
                MOD.log("could not check a new track for spacer tags", t);
            }
        }
    }

    /** Tracks that have been part of a project (held weakly). A track not in here is a fresh copy. */
    private static final Map<sJU, Boolean> SEEN = new WeakHashMap<>();
    /** Projects whose tracks were all marked as seen once (via their top-level group). */
    private static final Map<GJy, Boolean> SEEN_PROJECTS = new WeakHashMap<>();

    /** Marks every track of the project as seen, once per project (first row or first insert). */
    private static void seeProject(sJU anyTrack) {
        GJy root = anyTrack.TUb(); // TUb: the project's top-level group
        if (root == null) {
            return;
        }
        synchronized (SEEN) {
            if (SEEN_PROJECTS.containsKey(root)) {
                return;
            }
            SEEN_PROJECTS.put(root, Boolean.TRUE);
            List<sJU> all = new ArrayList<>();
            collect(root, all);
            for (sJU track : all) {
                SEEN.put(track, Boolean.TRUE);
            }
        }
    }

    /** The track itself and, for a group, every track inside (all levels). */
    private static void collect(sJU track, List<sJU> out) {
        out.add(track);
        if (track instanceof GJy group) {
            for (Object child : group.xDy()) { // the group's tracks, top to bottom
                if (child instanceof sJU childTrack) {
                    collect(childTrack, out);
                }
            }
        }
    }

    /**
     * The track if its rows may get spacers: a track or group track in a group's (or the top level's)
     * track list. Not a group's own master track, effect tracks or the project's master track
     * (they are not in that list).
     */
    private static sJU trackWithSpacers(Object shown) {
        if (!(shown instanceof sJU track)) {
            return null;
        }
        GJy group = track.QJW(); // the group holding it
        if (group == null || group.smz() == track) { // smz: the group's own master track
            return null;
        }
        return group.xDy().contains(track) ? track : null; // xDy: the group's tracks, top to bottom
    }

    @SuppressWarnings("unchecked")
    private static void register(sJU track, dZl above, dZl below) {
        List<WeakReference<dZl>[]> rows;
        synchronized (ROWS) {
            rows = ROWS.get(track);
            if (rows == null) {
                rows = new ArrayList<>();
                ROWS.put(track, rows);
                listenToComment(track);
            }
            // Rows Bitwig has thrown away (e.g. after rebuilding the arranger) would pile up here if the
            // comment never changes, so clear them out now as well (refresh() does the same).
            rows.removeIf(pair -> pair[0].get() == null || pair[1].get() == null);
            rows.add(new WeakReference[] {new WeakReference<>(above), new WeakReference<>(below)});
        }
        String comment = track.wze();
        resize(above, SpacerRule.gapBefore(comment));
        resize(below, SpacerRule.gapAfter(comment));
    }

    /** One listener per track on its comment (Bitwig's own change event, also on undo/redo). */
    private static void listenToComment(sJU track) {
        WeakReference<sJU> ref = new WeakReference<>(track);
        track.GKF().addListener(new NVD() { // GKF: the track's comment as a value
            @Override
            public void eventOccurred(Object event) {
                sJU t = ref.get();
                if (t != null) {
                    refresh(t);
                }
            }
        });
    }

    /** Sets every row's spacers of this track to the sizes in its comment now. */
    static void refresh(sJU track) {
        try {
            String comment = track.wze();
            int before = SpacerRule.gapBefore(comment);
            int after = SpacerRule.gapAfter(comment);
            synchronized (ROWS) {
                List<WeakReference<dZl>[]> rows = ROWS.get(track);
                if (rows == null) {
                    return;
                }
                for (Iterator<WeakReference<dZl>[]> it = rows.iterator(); it.hasNext(); ) {
                    WeakReference<dZl>[] pair = it.next();
                    dZl above = pair[0].get();
                    dZl below = pair[1].get();
                    if (above == null || below == null) {
                        it.remove(); // the row is gone
                        continue;
                    }
                    resize(above, before);
                    resize(below, after);
                }
            }
        } catch (Throwable t) {
            if (!loggedProblem) {
                loggedProblem = true;
                MOD.log("could not resize the track spacers", t);
            }
        }
    }

    private static void resize(dZl spacer, int pixels) {
        long size = pixels;
        spacer.sCA(size); // smallest size (raises the largest if needed)
        spacer.MgQ(size); // largest size: fixed now
    }
}
