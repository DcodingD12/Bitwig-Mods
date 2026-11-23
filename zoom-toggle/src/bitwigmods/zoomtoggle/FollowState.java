package bitwigmods.zoomtoggle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The plain logic of "the tall track follows the selected clips" (no Bitwig classes, so it can be tested).
 *
 * T is whatever identifies a track (in Bitwig: the track object, in the tests: strings).
 *
 * Flow:
 *   start(selected)      first press: these (selected) tracks were made tall
 *   requestFollow()      the clip selection changed: should a follow step be scheduled?
 *   follow(clipTracks)   the scheduled step: the tracks of the selected clips should be tall;
 *                        which tracks to make small, which tall (no clips selected = no change)
 *   end(selected)        second press: which tracks to make small again
 *
 * Protection against loops: while a step runs (busy) selection events are ignored, at most one
 * step is pending at a time, and a step with the same tracks as before does nothing.
 */
final class FollowState<T> {

    /** What one follow step has to do. Both lists empty = nothing to do. */
    static final class Step<T> {
        final List<T> shrink;
        final List<T> grow;

        Step(List<T> shrink, List<T> grow) {
            this.shrink = shrink;
            this.grow = grow;
        }

        boolean isEmpty() {
            return shrink.isEmpty() && grow.isEmpty();
        }
    }

    private boolean zoomed = false;
    private boolean pending = false;
    private boolean busy = false;
    private boolean broken = false;
    private List<T> tall = new ArrayList<>();

    /** First press: the tracks that were just made tall (Bitwig's selected tracks at that moment). */
    void start(List<T> selected) {
        zoomed = true;
        pending = false;
        busy = false;
        broken = false;
        tall = new ArrayList<>(selected);
    }

    /** The tracks that are tall right now (a copy). */
    List<T> tall() {
        return new ArrayList<>(tall);
    }

    boolean zoomed() {
        return zoomed;
    }

    /**
     * The selection changed. True = schedule one follow step (for the next frame).
     * False while not zoomed, while our own step runs, if a step is already pending, or after an error.
     */
    boolean requestFollow() {
        if (!zoomed || busy || pending || broken) {
            return false;
        }
        pending = true;
        return true;
    }

    /**
     * The scheduled step: compares the tracks of the selected clips with the tall tracks.
     * No clips selected (only a track, or a click into empty space) = keep the tall track as it is.
     * Remembers the new tall tracks right away; the caller then runs the step between begin() and finish().
     */
    Step<T> follow(List<T> clipTracks) {
        pending = false;
        List<T> none = new ArrayList<>();
        List<T> wanted = without(clipTracks, none); // each track once
        if (!zoomed || broken || wanted.isEmpty()) {
            return new Step<>(none, none);
        }
        List<T> shrink = without(tall, wanted);
        List<T> grow = without(wanted, tall);
        if (!shrink.isEmpty() || !grow.isEmpty()) {
            tall = wanted;
        }
        return new Step<>(shrink, grow);
    }

    /** What the first press does with the track heights. */
    enum FirstPress {
        /** No clip selected: do nothing at all (no launcher, no time zoom, stay not zoomed). */
        NOTHING,
        /** Make exactly the tracks of the selected clips tall. */
        CLIP_TRACKS,
        /** The clip tracks are unknown (error, lane mode): Bitwig's own action on the selected tracks, as before. */
        BITWIG_ACTION
    }

    /** First press: clipTracks = the tracks of the selected clips, or null if they couldn't be found out. */
    static <T> FirstPress firstPress(List<T> clipTracks) {
        if (clipTracks == null) {
            return FirstPress.BITWIG_ACTION;
        }
        return clipTracks.isEmpty() ? FirstPress.NOTHING : FirstPress.CLIP_TRACKS;
    }

    /**
     * The tracks of some selected items, each once, in order; items without a track
     * (trackOf returns null, e.g. not a clip) are skipped.
     */
    static <I, T> List<T> tracksOf(List<I> items, Function<I, T> trackOf) {
        List<T> result = new ArrayList<>();
        for (I item : items) {
            T track = trackOf.apply(item);
            if (track != null && !result.contains(track)) {
                result.add(track);
            }
        }
        return result;
    }

    /** Our own height changes start: selection events from now on are ignored. */
    void begin() {
        busy = true;
    }

    /** Our own height changes are done. */
    void finish() {
        busy = false;
    }

    /** Something went wrong: no more following until the next first press. */
    void fail() {
        broken = true;
        busy = false;
        pending = false;
    }

    boolean broken() {
        return broken;
    }

    /**
     * Second press: which tracks must become small again.
     * Returns null if they are exactly the selected tracks: then Bitwig's own
     * "zoom out lane heights (selected)" hits the right tracks and is used as before.
     * Otherwise the list of tall tracks (never the selected ones, so no wrong track shrinks).
     * After an error we don't know for sure which tracks are tall -> null (the old behaviour).
     * Resets everything.
     */
    List<T> end(List<T> selected) {
        List<T> result = broken || sameTracks(tall, selected) ? null : new ArrayList<>(tall);
        zoomed = false;
        pending = false;
        busy = false;
        broken = false;
        tall = new ArrayList<>();
        return result;
    }

    static <T> boolean sameTracks(List<T> a, List<T> b) {
        return without(a, b).isEmpty() && without(b, a).isEmpty();
    }

    /** The items of `from` that are not in `remove` (order kept, each item once). */
    private static <T> List<T> without(List<T> from, List<T> remove) {
        List<T> result = new ArrayList<>();
        for (T item : from) {
            if (!remove.contains(item) && !result.contains(item)) {
                result.add(item);
            }
        }
        return result;
    }
}
