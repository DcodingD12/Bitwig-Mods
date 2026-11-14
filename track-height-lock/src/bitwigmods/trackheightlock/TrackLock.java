package bitwigmods.trackheightlock;

import com.bitwig.flt.document.core.master.sJU; // a track or group track (channel)

/**
 * The lock state of one Bitwig track. Used by the hook and by the menu actions.
 * Deliberately free of the menu framework, so the lock keeps working without it.
 *
 * Names are Bitwig 6.1.7 (macOS).
 */
final class TrackLock {
    private TrackLock() {
    }

    /** Locked by comment, by its own title or by the shown name. */
    static boolean isLocked(sJU track) {
        // wze: comment, eXf: the title the user typed (may be empty), cb_: the name Bitwig shows
        return LockRule.commentIsLocked(track.wze())
                || LockRule.nameIsLocked(track.eXf())
                || LockRule.nameIsLocked(track.cb_());
    }

    /** The name to add " (L)" to: the user's own title, or the name Bitwig shows when there is none. */
    static String currentName(sJU track) {
        String title = track.eXf();
        if (title != null && !title.isEmpty()) {
            return title;
        }
        String shown = track.cb_();
        return shown == null ? "" : shown;
    }
}
