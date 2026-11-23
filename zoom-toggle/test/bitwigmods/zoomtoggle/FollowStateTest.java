package bitwigmods.zoomtoggle;

import java.util.List;

/**
 * Small tests for FollowState without any test library.
 * Run: sh tools/test-java.sh zoom-toggle   (prints FOLLOWSTATE_TESTS_OK or fails)
 */
public final class FollowStateTest {
    private static int failures = 0;

    public static void main(String[] args) {
        notZoomedDoesNothing();
        clickOtherTrackSwapsTallTrack();
        sameSelectionDoesNothing();
        emptySelectionKeepsTallTrack();
        addingToSelectionOnlyGrowsTheNewOne();
        eventsWhileBusyOrPendingAreIgnored();
        backWithSameSelectionUsesBitwigsAction();
        backAfterFollowShrinksTheTallTrackNotTheSelected();
        backAfterSelectionChangeWithoutFollowShrinksTheTallTrack();
        failureStopsFollowingAndFallsBack();
        clipTracksAreUniqueAndSkipNonClips();
        twoClipsOnOneTrackMeanOneTrack();
        firstPressOnlyWithClips();

        if (failures > 0) {
            throw new AssertionError(failures + " test(s) failed");
        }
        System.out.println("FOLLOWSTATE_TESTS_OK");
    }

    private static void notZoomedDoesNothing() {
        FollowState<String> s = new FollowState<>();
        check("not zoomed: no follow scheduled", !s.requestFollow());
        check("not zoomed: follow step empty", s.follow(List.of("B")).isEmpty());
    }

    private static void clickOtherTrackSwapsTallTrack() {
        FollowState<String> s = new FollowState<>();
        s.start(List.of("A"));
        check("zoomed: follow scheduled", s.requestFollow());
        FollowState.Step<String> step = s.follow(List.of("B"));
        check("A shrinks", step.shrink.equals(List.of("A")));
        check("B grows", step.grow.equals(List.of("B")));
        check("B is now the tall track", s.tall().equals(List.of("B")));
    }

    private static void sameSelectionDoesNothing() {
        FollowState<String> s = new FollowState<>();
        s.start(List.of("A"));
        s.requestFollow();
        check("same track (e.g. another clip on it): nothing", s.follow(List.of("A")).isEmpty());
        check("can schedule again after the step", s.requestFollow());
    }

    private static void emptySelectionKeepsTallTrack() {
        FollowState<String> s = new FollowState<>();
        s.start(List.of("A"));
        s.requestFollow();
        check("empty selection: nothing", s.follow(List.of()).isEmpty());
        check("empty selection: A stays tall", s.tall().equals(List.of("A")));
    }

    private static void addingToSelectionOnlyGrowsTheNewOne() {
        FollowState<String> s = new FollowState<>();
        s.start(List.of("A"));
        s.requestFollow();
        FollowState.Step<String> step = s.follow(List.of("A", "B"));
        check("shift-add: nothing shrinks", step.shrink.isEmpty());
        check("shift-add: only B grows", step.grow.equals(List.of("B")));
        s.requestFollow();
        step = s.follow(List.of("B"));
        check("back to B only: A shrinks", step.shrink.equals(List.of("A")) && step.grow.isEmpty());
    }

    private static void eventsWhileBusyOrPendingAreIgnored() {
        FollowState<String> s = new FollowState<>();
        s.start(List.of("A"));
        check("first event schedules", s.requestFollow());
        check("second event while pending: ignored", !s.requestFollow());
        s.follow(List.of("B"));
        s.begin();
        check("event during our own step: ignored (no loop)", !s.requestFollow());
        s.finish();
        check("after our step: events count again", s.requestFollow());
    }

    private static void backWithSameSelectionUsesBitwigsAction() {
        FollowState<String> s = new FollowState<>();
        s.start(List.of("A"));
        check("back, A still selected: Bitwig's own action (null)", s.end(List.of("A")) == null);
        check("back: not zoomed any more", !s.zoomed());
    }

    private static void backAfterFollowShrinksTheTallTrackNotTheSelected() {
        FollowState<String> s = new FollowState<>();
        s.start(List.of("A"));
        s.requestFollow();
        s.follow(List.of("B"));
        check("back, B selected and tall: Bitwig's own action", s.end(List.of("B")) == null);

        s.start(List.of("A"));
        s.requestFollow();
        s.follow(List.of("B"));
        // e.g. the user clicked into empty space after following to B
        check("back, nothing selected: shrink B explicitly", List.of("B").equals(s.end(List.of())));
    }

    private static void backAfterSelectionChangeWithoutFollowShrinksTheTallTrack() {
        FollowState<String> s = new FollowState<>();
        s.start(List.of("A"));
        // C got selected, but the step never ran (e.g. arranger hidden)
        check("back, C selected but A tall: shrink A, not C", List.of("A").equals(s.end(List.of("C"))));
    }

    private static void failureStopsFollowingAndFallsBack() {
        FollowState<String> s = new FollowState<>();
        s.start(List.of("A"));
        s.requestFollow();
        s.follow(List.of("B"));
        s.begin();
        s.fail();
        check("after an error: no more following", !s.requestFollow());
        check("after an error: back uses the old behaviour (null)", s.end(List.of("C")) == null);
        s.start(List.of("A"));
        check("next first press: following works again", s.requestFollow());
    }

    private static void clipTracksAreUniqueAndSkipNonClips() {
        // items "clip@Track"; anything else (e.g. a marker) has no track
        List<String> items = List.of("c1@B", "marker", "c2@B", "c3@C");
        List<String> tracks = FollowState.tracksOf(items, i -> i.contains("@") ? i.substring(i.indexOf('@') + 1) : null);
        check("tracks of clips: B, C once each, marker skipped", tracks.equals(List.of("B", "C")));
        check("no clips: no tracks", FollowState.tracksOf(List.of("marker"), i -> (String) null).isEmpty());
    }

    private static void twoClipsOnOneTrackMeanOneTrack() {
        FollowState<String> s = new FollowState<>();
        s.start(List.of("A"));
        s.requestFollow();
        FollowState.Step<String> step = s.follow(List.of("B", "B"));
        check("duplicate track: grows once", step.grow.equals(List.of("B")));
        check("duplicate track: tall once", s.tall().equals(List.of("B")));
        s.requestFollow();
        check("only a track clicked (no clips): nothing changes", s.follow(List.of()).isEmpty());
        check("B stays tall", s.tall().equals(List.of("B")));
    }

    private static void firstPressOnlyWithClips() {
        check("first press, no clip selected (only a track): do nothing",
              FollowState.firstPress(List.of()) == FollowState.FirstPress.NOTHING);
        check("first press, clips selected: grow their tracks",
              FollowState.firstPress(List.of("A")) == FollowState.FirstPress.CLIP_TRACKS);
        check("first press, clips unknown (error/lane mode): Bitwig's own action as before",
              FollowState.firstPress(null) == FollowState.FirstPress.BITWIG_ACTION);

        // the clip tracks become the tall ones; going back with track D selected (click into empty space)
        FollowState<String> s = new FollowState<>();
        s.start(List.of("A"));
        check("back with D selected: shrink A, never D", List.of("A").equals(s.end(List.of("D"))));
    }

    private static void check(String name, boolean ok) {
        if (!ok) {
            failures++;
            System.out.println("FAILED: " + name);
        }
    }
}
