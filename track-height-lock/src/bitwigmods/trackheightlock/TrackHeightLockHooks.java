package bitwigmods.trackheightlock;

import bitwig.obf.Sc;  // generated base of a track's arranger display settings; holds "clip_lane_height"
import bitwig.obf.ydZ; // a track's arranger display settings ("track_or_track_group_display_settings")
import com.bitwig.flt.document.core.master.sJU; // a track (or group track) in the document
import bml.api.At;
import bml.api.Bml;
import bml.api.Hook;

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
 * Names are Bitwig 6.1.7 (macOS).
 */
public final class TrackHeightLockHooks {
    private static final Bml.Mod MOD = Bml.mod("track-height-lock");

    // Log a problem only once, the setter can run many times per second while dragging.
    private static volatile boolean loggedProblem = false;

    private TrackHeightLockHooks() {
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
            // cb_() = displayed title (the name you see), wze() = the track's comment
            return LockRule.isLocked(track.cb_(), track.wze());
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
