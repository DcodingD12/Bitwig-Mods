package bitwigmods.pinnedtracks;

import bitwig.obf.KNG;  // a yes/no value computed from another value (here: "comment contains #pin")
import bitwig.obf.SZB;  // a colour (r, g, b, alpha)
import bitwig.obf.YCD;  // a (non-group) track, as the UI sees it
import bitwig.obf.Dix;  // a fixed-size layout hint (used for the launcher's scene scroller)
import bitwig.obf.NKg;  // a layout hint Bitwig gives the launcher lists
import bitwig.obf.aGa;  // arranger column: clip launcher slots
import bitwig.obf.aGf;  // arranger column: launcher controls
import bitwig.obf.aGj;  // base of every arranger column: builds [ruler, Global row, track list, footer]
import bitwig.obf.aGm;  // arranger column: timeline (clips)
import bitwig.obf.aGq;  // arranger column: track headers
import bitwig.obf.aZD;  // base of the arranger panel (theme colours of its columns)
import bitwig.obf.aZJ;  // the arranger panel
import bitwig.obf.bBY;  // the empty "add a track" area below the tracks of a list
import bitwig.obf.bCf;  // base of a track list widget (one per group level)
import bitwig.obf.bCh;  // keeps a track list's rows in sync with its tracks
import bitwig.obf.bCs;  // track list of the launcher columns
import bitwig.obf.bDO;  // track list of the launcher's clip slot grid
import bitwig.obf.bdo;  // which part of the launcher column a bCs list shows
import bitwig.obf.ccS;  // yes/no: ALL of its values are yes
import bitwig.obf.dVg;  // a scroll view
import bitwig.obf.dXL;  // a box that stacks widgets
import bitwig.obf.doI;  // widget base; doI.beI(property, value) binds a property to a value
import bitwig.obf.dwF;  // a widget
import bitwig.obf.egG;  // a theme colour
import bitwig.obf.ekS;  // what a box takes as its background
import bitwig.obf.epr;  // widget tree search
import bitwig.obf.m32;  // yes/no: ANY of its values is yes
import bitwig.obf.sFY;  // yes/no: NOT its value
import bitwig.obf.twa;  // a test for the widget tree search
import bitwig.obf.uB1;  // a value (observable)
import bml.api.Arg;
import bml.api.At;
import bml.api.Bml;
import bml.api.Hook;
import bml.api.This;
import com.bitwig.flt.document.core.iface.uVN; // a group track, as the UI sees it (also the project's top level)
import com.bitwig.flt.document.core.iface.zCn; // a track or group track, as the UI sees it
import com.bitwig.flt.document.core.master.sJU; // a track (channel) in the document
import com.bitwig.flt.widget.core.timeline.ju1; // base of Bitwig's timeline widgets (grid line colour)
import com.bitwig.ramona.core.EO; // a property of a document object (here: a row's "visible")

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * Arranger: Pinned Tracks. Like Cubase's "Divide Track List": tracks with "#pin" in their comment stay
 * in a fixed area at the top of the arranger, the other tracks scroll below it.
 *
 * How (Bitwig 6.1.7 names):
 * - The arranger is made of side-by-side COLUMNS, each an aGj: track headers (aGq), timeline with the clips
 *   (aGm), clip launcher slots (aGa) and launcher controls (aGf). Each column stacks (aGj.MgQ) [ruler,
 *   Global row, its own scrolling track list, footer]; all scrolling lists share one vertical scroll model.
 *   The Global row stays fixed above the scrolling list. In EVERY column a second track list (built with the
 *   column's own builder kwH(), so the same widgets Bitwig uses) is put between the Global row and the
 *   scrolling list: the PINNED list. Below it sits a thin divider in the colour of the timeline's bar lines.
 *   (0.1.0-dev.1 did this only in the header column: the timeline of pinned tracks still scrolled.)
 * - Every track row's visibility is bound by Bitwig to "track active OR show deactivated tracks"
 *   (bCh.beI(zCn)). For the top level of both lists this rule gets one more condition:
 *   pinned list: AND "#pin in the comment"; scrolling list: AND NOT "#pin in the comment".
 *   The condition is a live value on the comment, so pinning/unpinning (also Undo) shows at once.
 *   Hidden rows take no space. Tracks inside groups follow their group (only top-level tracks/groups pin).
 * - The pinned list has no effect tracks, no master track and no "add track" area.
 * - The pinned area (list + divider) is only shown while at least one of its rows is shown.
 * - "Find the row of this track" (aGq.beI) now prefers a shown row; with two lists the first row found
 *   could be the hidden copy.
 *
 * Every hook falls back to Bitwig's own behaviour if anything fails (logged once).
 */
public final class PinnedTracksHooks {
    private static final Bml.Mod MOD = Bml.mod("pinned-tracks");
    private static volatile boolean loggedProblem = false;

    /** Height of the divider below the pinned tracks, in Bitwig's logical pixels. */
    static final long DIVIDER_PX = 2L;

    private PinnedTracksHooks() {
    }

    // ---- which list is which -------------------------------------------------------------------

    enum Role { MAIN, PINNED }

    /** What we know about one of the two top-level track lists of an arranger view. */
    static final class ListInfo {
        final Role role;
        /** PINNED only: "any pinned row is shown" (OR of the rows' visibility values). */
        final m32 anyShown;
        /** PINNED only: each row's visibility value, to take it out of anyShown when the row goes. */
        final Map<dwF, uB1> rows = new WeakHashMap<>();
        /** MAIN only: switched on (a "yes" value added) if the pinned area could not be built: show every track here. */
        final m32 showAll;

        ListInfo(Role role) {
            this.role = role;
            this.anyShown = role == Role.PINNED ? new m32() : null;
            this.showAll = role == Role.MAIN ? new m32() : null;
        }
    }

    /** The arranger view being built right now (only during aGj.MgQ, on the UI thread). */
    private static final class Build {
        final uVN root;     // the project's top level the view shows
        final aZJ panel;
        final aGj view;
        final ListInfo main = new ListInfo(Role.MAIN);     // this column's scrolling list(s)
        final ListInfo pinned = new ListInfo(Role.PINNED); // this column's pinned list(s)
        ListInfo building;  // set while one of the two is being built (kwH)
        dwF pinnedContent;  // the pinned list(s) as built by the column, until the area is added
        final Build outer;  // a column build this one runs inside of (normally null), restored at the end

        /** view == null: an aGj this mod leaves alone (still pushed, so an outer build isn't lost). */
        Build(uVN root, aZJ panel, aGj view, Build outer) {
            this.root = root;
            this.panel = panel;
            this.view = view;
            this.outer = outer;
        }
    }

    private static final ThreadLocal<Build> BUILD = new ThreadLocal<>();
    /** The two top-level lists of every arranger view (weak: closed views can go). */
    private static final Map<bCf, ListInfo> LISTS = Collections.synchronizedMap(new WeakHashMap<>());
    /** "Add track" areas of pinned lists: always hidden. */
    private static final Set<bBY> HIDDEN_ADD_AREAS = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    /**
     * The role of a track list, or null for every other list (group levels, mixer, other views).
     * A list is recognised while a column builds it (kwH, see buildLists): every list showing the column's
     * root (the project's top level) built then gets that role. Lists of groups inside are left alone.
     */
    static ListInfo infoOf(bCf list) {
        ListInfo info = LISTS.get(list);
        if (info != null) {
            return info;
        }
        Build build = BUILD.get();
        if (build == null || build.view == null || build.building == null || list.WYl() != build.root) { // WYl: the group the list shows
            return null;
        }
        info = build.building;
        LISTS.put(list, info);
        return info;
    }

    /** The arranger columns this mod knows how to build twice. */
    static boolean isColumn(aGj view) {
        return view instanceof aGq || view instanceof aGm || view instanceof aGf || view instanceof aGa;
    }

    // ---- 1. the arranger view: remember what is being built, add the pinned area -----------------

    @Hook(id = "view-build-start", target = aGj.class,
            method = "MgQ(Lcom/bitwig/flt/document/core/iface/uVN;LaZJ;)V", at = At.ENTRY)
    public static void viewBuildStart(@This aGj view, @Arg(0) uVN root, @Arg(1) aZJ panel) {
        boolean column = isColumn(view) && root != null && panel != null; // every column of the arranger
        BUILD.set(column ? new Build(root, panel, view, BUILD.get()) : new Build(null, null, null, BUILD.get()));
    }

    @Hook(id = "view-build-end", target = aGj.class,
            method = "MgQ(Lcom/bitwig/flt/document/core/iface/uVN;LaZJ;)V", at = At.BEFORE_RETURN)
    public static void viewBuildEnd(@This aGj view) {
        Build build = BUILD.get();
        if (build != null && build.outer != null) {
            BUILD.set(build.outer);
        } else {
            BUILD.remove();
        }
    }

    // aGj.MgQ calls the column's kwH() once to build its track list (then wraps it in the scroll view).
    // Here the column builds the PINNED list first, then Bitwig's own one (last, so a column that keeps its
    // list in a field, like aGq, keeps Bitwig's). The clip slot column (aGa) can't run kwH() twice (it puts
    // one existing widget into its result), so its pinned copy is built the same way by hand.
    @Hook(id = "column-lists", target = aGj.class,
            method = "MgQ(Lcom/bitwig/flt/document/core/iface/uVN;LaZJ;)V",
            at = At.REPLACE_CALL, callOwner = aGj.class, call = "kwH()LdwF;", occurrence = 1)
    public static dwF buildLists(aGj view) {
        Build build = BUILD.get();
        if (build == null || build.view != view) {
            return view.kwH(); // what Bitwig does
        }
        build.building = build.pinned;
        try {
            build.pinnedContent = view instanceof aGa slots ? launcherSlotsCopy(slots, build) : view.kwH();
        } catch (Throwable t) {
            build.pinnedContent = null;
            problem("could not build the pinned track list, the arranger is shown as usual", t);
        } finally {
            build.building = null;
        }
        build.building = build.main;
        try {
            return view.kwH(); // Bitwig's own list
        } finally {
            build.building = null;
        }
    }

    /**
     * The clip slot column's list like aGa.kwH() builds it: side by side [launcher list (part bdo.beI), 1 px,
     * horizontal scroll view around a clip slot grid, 1 px, launcher list (part bdo.sCA)]. The grid is new
     * (Bitwig's own one stays where it is) and is NOT registered with the column's scene coordinator (bct holds
     * just one grid). Its scene scrolling is bound to the same document value as Bitwig's, so both move together.
     */
    private static dwF launcherSlotsCopy(aGa view, Build build) {
        dXL box = dXL.fpX();                               // side by side
        box.byf(0);
        box.beI((ekS) aZD.Gef(view));
        bCs left = bCs.beI(build.root, build.panel, bdo.beI);
        left.a_(NKg.beI());
        left.unz((ekS) view.mum().rw3());
        box.IvP(left);
        box.sCA(1L);
        bDO grid = bDO.beI(build.root, build.panel);
        dVg scenes = dVg.MgQ(grid);
        scenes.ymo(false);
        scenes.wGt(true);
        scenes.PJ3(false);
        scenes.a_(new Dix(170L));
        box.IvP(scenes);
        box.sCA(1L);
        bCs right = bCs.beI(build.root, build.panel, bdo.sCA);
        right.a_(NKg.beI());
        right.unz((ekS) view.mum().rw3());
        box.IvP(right);
        doI.beI(scenes.qVz().cCV(), view.DIi().BBg());     // scene scroll position, shared with Bitwig's grid
        return box;
    }

    // aGj.MgQ: box5 (top to bottom) gets [Global row, scrolling track list]; the 3rd IvP call adds the
    // scrolling list. The pinned area goes in right before it.
    @Hook(id = "pinned-area", target = aGj.class,
            method = "MgQ(Lcom/bitwig/flt/document/core/iface/uVN;LaZJ;)V",
            at = At.REPLACE_CALL, callOwner = dXL.class, call = "IvP(LdwF;)V", occurrence = 3)
    public static void addTrackList(dXL box, dwF scrollingList) {
        Build build = BUILD.get();
        if (build != null && build.view != null) {
            boolean added = false;
            try {
                if (build.pinnedContent != null) {
                    box.IvP(pinnedArea(build));
                    added = true;
                }
            } catch (Throwable t) {
                problem("could not build the pinned area, the arranger is shown as usual", t);
            }
            if (!added) {
                build.main.showAll.beI((uB1) new ccS()); // AND of nothing = yes: pinned tracks show in the scrolling list again
            }
        }
        box.IvP(scrollingList); // what Bitwig does
    }

    /** [pinned list(s) of this column, divider], shown only while a pinned row is shown. */
    private static dwF pinnedArea(Build build) {
        ListInfo info = build.pinned;
        dXL area = dXL.sSO();                              // top to bottom
        area.IvP(build.pinnedContent);                     // framed by the column itself (kwH), like its scrolling list
        area.sCA(DIVIDER_PX);                              // a fixed spacer = the divider
        // A box paints its background only over spacers and gaps, never under its children
        // (checked for track-spacer), so the background colour is exactly the divider's colour.
        egG colour;
        try {
            colour = dividerColour(build.view);
        } catch (Throwable t) {
            colour = build.view.mum().zzi(); // "Grey 1", the colour between tracks (e.g. a theme without "Grey 2")
            problem("could not take the grid line colour, the divider uses the track gap colour", t);
        }
        area.beI((ekS) colour);
        doI.beI(area.Afd(), info.anyShown);                // visible only while a pinned row is shown
        return area;
    }

    /**
     * The colour of the timeline's bar lines as they look on the track lanes, opaque.
     * Bitwig paints the grid with ju1.beI(boolean, boolean) (bar lines at full strength, bqT), a black (or,
     * with the "light grid" setting, white) line whose alpha comes from the grid contrast setting, over the
     * lanes' background, theme colour "Grey 2" (timeline theme nEh, jTT). Mixed here the same way.
     * Taken when the arranger view is built: changing the grid setting or the theme shows after a rebuild.
     */
    static egG dividerColour(dwF owner) {
        SZB lane = owner.mum().beI("Grey 2").PJ3();        // the lanes' background (a theme colour's value)
        SZB line = ju1.beI(false, true);                   // bar line colour incl. its alpha
        float[] rgba = DividerColour.over(
                new float[] {lane.MgQ(), lane.BBg(), lane.unz(), lane.Anf()},   // r, g, b, alpha
                new float[] {line.MgQ(), line.BBg(), line.unz(), line.Anf()});
        SZB mixed = new SZB(rgba[0], rgba[1], rgba[2], rgba[3]);
        return egG.beI("pinned-tracks divider", mixed);    // a fixed colour, like Bitwig's own theme colours
    }

    // ---- 2. the pinned list: no effect tracks, no master, no "add track" area ------------------

    // bCf.Gef(uVN, aZD) builds a list; HOV() decides about the effect track section and the master row.
    @Hook(id = "pinned-list-without-effects", target = bCf.class,
            method = "Gef(Lcom/bitwig/flt/document/core/iface/uVN;LaZD;)V",
            at = At.REPLACE_CALL, callOwner = bCf.class, call = "HOV()Z", occurrence = 1)
    public static boolean effectsAndMaster(bCf list) {
        try {
            ListInfo info = infoOf(list);
            if (info != null && info.role == Role.PINNED) {
                return false;
            }
        } catch (Throwable t) {
            problem("could not check a track list", t);
        }
        return list.HOV(); // what Bitwig does
    }

    @Hook(id = "pinned-list-add-area", target = bCf.class,
            method = "Gef(Lcom/bitwig/flt/document/core/iface/uVN;LaZD;)V",
            at = At.REPLACE_CALL, callOwner = bCf.class, call = "nf_()LbBY;", occurrence = 1)
    public static bBY addTrackArea(bCf list) {
        bBY area = list.nf_(); // what Bitwig does
        try {
            ListInfo info = infoOf(list);
            if (info != null && info.role == Role.PINNED && area != null) {
                HIDDEN_ADD_AREAS.add(area);
                area.t_(false);
            }
        } catch (Throwable t) {
            problem("could not check a track list", t);
        }
        return area;
    }

    // bCf.fBD() shows/hides the "add track" area whenever the list changes.
    @Hook(id = "pinned-list-add-area-hidden", target = bCf.class, method = "fBD()V",
            at = At.REPLACE_CALL, callOwner = bBY.class, call = "t_(Z)V", occurrence = 1)
    public static void showAddTrackArea(bBY area, boolean show) {
        area.t_(show && !HIDDEN_ADD_AREAS.contains(area));
    }

    // ---- 3. the rows: pinned list shows only pinned tracks, the scrolling list all others ------

    /** The row just built by bCh.beI(zCn), waiting for its visibility binding a few instructions later. */
    private record Pending(ListInfo info, zCn track, dwF row) {
    }

    private static final ThreadLocal<Pending> PENDING = new ThreadLocal<>();

    @Hook(id = "row-of-track", target = bCh.class,
            method = "beI(Lcom/bitwig/flt/document/core/iface/zCn;)LdwF;",
            at = At.REPLACE_CALL, callOwner = bCf.class, call = "unz(LYCD;)LdwF;", occurrence = 1)
    public static dwF trackRow(bCf list, YCD track) {
        dwF row = list.unz(track); // what Bitwig does (may build nested lists; PENDING is set afterwards)
        notePending(list, track, row);
        return row;
    }

    @Hook(id = "row-of-group", target = bCh.class,
            method = "beI(Lcom/bitwig/flt/document/core/iface/zCn;)LdwF;",
            at = At.REPLACE_CALL, callOwner = bCf.class, call = "Gef(Lcom/bitwig/flt/document/core/iface/uVN;)LdwF;", occurrence = 1)
    public static dwF groupRow(bCf list, uVN group) {
        dwF row = list.Gef(group); // what Bitwig does
        notePending(list, group, row);
        return row;
    }

    private static void notePending(bCf list, zCn track, dwF row) {
        PENDING.remove();
        try {
            ListInfo info = infoOf(list);
            if (info != null && row != null) {
                PENDING.set(new Pending(info, track, row));
            }
        } catch (Throwable t) {
            problem("could not check a track list", t);
        }
    }

    // bCh.beI(zCn): doI.beI(row.Afd(), new m32(track.Ml2(), showDeactivated)) binds the row's visibility.
    @Hook(id = "row-visibility", target = bCh.class,
            method = "beI(Lcom/bitwig/flt/document/core/iface/zCn;)LdwF;",
            at = At.REPLACE_CALL, callOwner = doI.class, call = "beI(Lcom/bitwig/ramona/core/EO;LuB1;)V", occurrence = 1)
    public static void bindRowVisibility(EO visible, uB1 bitwigRule) {
        Pending pending = PENDING.get();
        PENDING.remove();
        uB1 rule = bitwigRule;
        if (pending != null) {
            try {
                rule = withPinCondition(pending, bitwigRule);
            } catch (Throwable t) {
                rule = bitwigRule;
                problem("could not filter a track row, it is shown as usual", t);
            }
        }
        doI.beI(visible, rule); // what Bitwig does, with our rule
    }

    private static uB1 withPinCondition(Pending pending, uB1 bitwigRule) {
        if (!(pending.track() instanceof sJU track)) {
            return bitwigRule;
        }
        // Live "comment contains #pin": KNG maps the comment value and passes on its change events.
        Function<Object, Object> pinned = comment -> PinRule.isPinned(comment instanceof String s ? s : null);
        KNG isPinned = new KNG(track.GKF(), pinned); // GKF: the track's comment as a value
        if (pending.info().role == Role.MAIN) {
            uB1 notPinned = new m32(new sFY(isPinned), pending.info().showAll); // NOT pinned (or pinned area missing)
            ROW_CONDITIONS.put(pending.row(), notPinned);
            return new ccS(bitwigRule, notPinned);          // shown AND NOT pinned
        }
        ROW_CONDITIONS.put(pending.row(), isPinned);
        uB1 rule = new ccS(bitwigRule, isPinned);           // shown AND pinned
        synchronized (pending.info()) {
            pending.info().rows.put(pending.row(), rule);
            // The pinned area is shown while any row is. Careful: the argument must be typed uB1. For an lIU
            // argument Java picks lIU.beI(lIU) (builds a NEW combined value, adds nothing), found by the probe.
            pending.info().anyShown.beI(rule);
        }
        return rule;
    }

    /** Our extra condition per filtered row ("pinned" or "not pinned"), for Bitwig's direct visibility calls. */
    private static final Map<dwF, uB1> ROW_CONDITIONS = Collections.synchronizedMap(new WeakHashMap<>());

    // Besides the binding, bCf.fBD() also sets every row's visibility directly (show deactivated tracks,
    // focus mode) whenever the list or those settings change. Without this hook it would show the hidden
    // copies again (each track twice). A row it shows only stays shown if our condition says so too.
    @Hook(id = "row-visibility-refresh", target = bCf.class, method = "fBD()V",
            at = At.REPLACE_CALL, callOwner = dwF.class, call = "t_(Z)V", occurrence = 1)
    public static void setRowVisible(dwF row, boolean visible) {
        boolean shown = visible;
        if (shown) {
            try {
                uB1 condition = ROW_CONDITIONS.get(row);
                if (condition != null) {
                    shown = Boolean.TRUE.equals(condition.getValue());
                }
            } catch (Throwable t) {
                shown = visible;
                problem("could not check a track row's pin", t);
            }
        }
        row.t_(shown); // what Bitwig does, with our condition
    }

    // bCh.Gef(dwF, zCn, int) removes a row (track deleted or moved into a group): take it out of anyShown.
    @Hook(id = "row-removed", target = bCh.class,
            method = "Gef(LdwF;Lcom/bitwig/flt/document/core/iface/zCn;I)V",
            at = At.REPLACE_CALL, callOwner = bCf.class, call = "MgQ(LdwF;Lcom/bitwig/flt/document/core/iface/zCn;)V", occurrence = 1)
    public static void rowRemoved(bCf list, dwF row, zCn track) {
        try {
            ListInfo info = LISTS.get(list);
            if (info != null && info.role == Role.PINNED) {
                synchronized (info) {
                    uB1 rule = info.rows.remove(row);
                    if (rule != null) {
                        info.anyShown.Gef(rule);
                    }
                }
            }
        } catch (Throwable t) {
            problem("could not forget a removed row", t);
        }
        list.MgQ(row, track); // what Bitwig does
    }

    // ---- 4. "find the row of this track": prefer a row that is shown ---------------------------

    // aGq.beI(zCn) returns the first bDm in the widget tree whose track matches, shown or not.
    // The pinned list comes first in the tree, so for an unpinned track that would be its hidden copy.
    @Hook(id = "row-lookup-shown", target = aGq.class,
            method = "beI(Lcom/bitwig/flt/document/core/iface/zCn;)LbDm;",
            at = At.REPLACE_CALL, callOwner = epr.class, call = "beI(LdwF;Ltwa;Z)LdwF;", occurrence = 1)
    public static dwF findTrackRow(dwF root, twa test, boolean flag) {
        try {
            dwF shown = epr.beI(root, shownOnly(root, test), flag);
            if (shown != null) {
                return shown;
            }
        } catch (Throwable t) {
            problem("could not look for a shown track row", t);
        }
        return epr.beI(root, test, flag); // Bitwig's search (e.g. rows inside a closed group)
    }

    // aGq.beI(mIM) does the same for automation lanes.
    @Hook(id = "lane-lookup-shown", target = aGq.class, method = "beI(LmIM;)LbBC;",
            at = At.REPLACE_CALL, callOwner = epr.class, call = "beI(LdwF;Ltwa;)LdwF;", occurrence = 1)
    public static dwF findLaneRow(dwF root, twa test) {
        try {
            dwF shown = epr.beI(root, shownOnly(root, test));
            if (shown != null) {
                return shown;
            }
        } catch (Throwable t) {
            problem("could not look for a shown automation lane", t);
        }
        return epr.beI(root, test);
    }

    private static twa shownOnly(dwF root, twa test) {
        return widget -> test.accepts(widget) && isShown(widget, root);
    }

    /** True if the widget and all its parents up to root are visible. */
    static boolean isShown(Object widget, dwF root) {
        for (dwF w = widget instanceof dwF d ? d : null; w != null && w != root; w = w.oRH()) { // oRH: parent
            if (!w.KrW()) { // KrW: visible
                return false;
            }
        }
        return true;
    }

    private static void problem(String what, Throwable t) {
        if (!loggedProblem) {
            loggedProblem = true;
            MOD.log(what, t);
        }
    }
}
