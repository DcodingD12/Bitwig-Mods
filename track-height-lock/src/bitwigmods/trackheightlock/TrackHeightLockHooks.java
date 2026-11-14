package bitwigmods.trackheightlock;

import bitwig.obf.Sc;  // generated base of a track's arranger display settings; holds "clip_lane_height"
import bitwig.obf.dgT; // the undo recorder's observer: records every change of an undoable attribute
import bitwig.obf.dgs; // a text attribute of a document object (e.g. a track's "title" or "comment")
import bitwig.obf.ydZ; // a track's arranger display settings ("track_or_track_group_display_settings")
import com.bitwig.flt.document.core.master.sJU; // a track (or group track) in the document
import bml.api.At;
import bml.api.Bml;
import bml.api.Hook;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Arranger: Track Height Lock.
 *
 * Every change of a track's arranger height ("clip_lane_height") goes through the setter
 * Sc.Gef(int) -> Sc.Heb(int) (clamps the value) -> Sc.iMb(int) (writes it, records undo, informs the UI).
 * That covers dragging the track's lower edge, Alt-dragging (all tracks), the
 * "zoom lane heights" shortcuts and every UI binding. Loading a project writes the field
 * directly and is not affected, so saved heights load normally.
 *
 * This mod replaces the one call to iMb(int) inside Heb(int): for a locked track it is skipped,
 * so the height stays as it is. Undo/redo use another path and still work.
 *
 * Second hook: the menu actions change a track's "title" and "comment" without an undo entry.
 * Bitwig's undo recorder (dgT) records a text change only if the attribute says it is undoable
 * (dgs.dvk(), flag bit 16; "title" and "comment" have it). While runWithoutUndo(...) runs,
 * exactly those attributes answer "not undoable", the way Bitwig treats e.g. the track height.
 *
 * Names are Bitwig 6.1.7 (macOS).
 */
public final class TrackHeightLockHooks {
    private static final Bml.Mod MOD = Bml.mod("track-height-lock");

    // Log a problem only once, the setter can run many times per second while dragging.
    private static volatile boolean loggedProblem = false;

    // Set only while runWithoutUndo(...) runs, on Bitwig's UI thread.
    private static volatile Thread silentThread = null;
    private static final Set<Object> SILENT_ATTRIBUTES = Collections.newSetFromMap(new IdentityHashMap<>());

    private TrackHeightLockHooks() {
    }

    /**
     * Runs the changes with the given attributes left out of undo. Only these attributes,
     * only on this thread, only while the changes run.
     */
    static void runWithoutUndo(Set<Object> attributes, Runnable changes) {
        synchronized (SILENT_ATTRIBUTES) {
            SILENT_ATTRIBUTES.clear();
            SILENT_ATTRIBUTES.addAll(attributes);
        }
        silentThread = Thread.currentThread();
        try {
            changes.run();
        } finally {
            silentThread = null;
            synchronized (SILENT_ATTRIBUTES) {
                SILENT_ATTRIBUTES.clear();
            }
        }
    }

    // dgT.aboutToChangeAttributeValue(dgs, ytz, String): Bitwig's undo recorder, about to record a text change.
    // Its first call, dgs.dvk(), asks whether the attribute is undoable at all.
    @Hook(id = "lock-marks-without-undo", target = dgT.class,
            method = "aboutToChangeAttributeValue(Ldgs;Lcom/bitwig/ramona/core/ytz;Ljava/lang/String;)V",
            at = At.REPLACE_CALL, callOwner = dgs.class, call = "dvk()Z", occurrence = 1)
    public static boolean isUndoable(dgs attribute) {
        // Fast path: runs for every text change in Bitwig, so normally just Bitwig's answer.
        if (silentThread == Thread.currentThread()) {
            synchronized (SILENT_ATTRIBUTES) {
                if (SILENT_ATTRIBUTES.contains(attribute)) {
                    return false; // our lock marks: no undo entry
                }
            }
        }
        return attribute.dvk();
    }

    // Sc.Heb(I)V: clamps the new height, then calls iMb(I)V (occurrence 1) to store it.
    @Hook(id = "keep-locked-track-height", target = Sc.class, method = "Heb(I)V", at = At.REPLACE_CALL,
            callOwner = Sc.class, call = "iMb(I)V", occurrence = 1)
    public static void storeHeightUnlessLocked(Sc settings, int newHeight) {
        if (isLocked(settings)) {
            return; // locked: keep the old height
        }
        settings.iMb(newHeight); // what Bitwig does normally
    }

    private static boolean isLocked(Sc settings) {
        try {
            if (!(settings instanceof ydZ displaySettings)) {
                return false;
            }
            sJU track = displaySettings.p1J(); // the track these settings belong to
            if (track == null) {
                return false;
            }
            return TrackLock.isLocked(track); // comment "#lock", or "(L)"/"[L]" in title or shown name
        } catch (Throwable t) {
            // Never break Bitwig: if anything is odd, behave as if the mod weren't there.
            if (!loggedProblem) {
                loggedProblem = true;
                MOD.log("could not check the track lock, height changes are allowed", t);
            }
            return false;
        }
    }
}
