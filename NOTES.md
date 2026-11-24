# NOTES – Bitwig mods for Patchbay (BML)

## Environment
- BML 0.36.0-dev.13 at `/Applications/BML-0.36.0-dev.13-macos` (docs: `docs/sdk/README.md`, mod kit: `modkit/`).
- Bitwig Studio 6.1.7 macOS, jars at `/Applications/Bitwig Studio.app/Contents/Java/`.
- JDK 25: `/opt/homebrew/opt/openjdk@25` (only used inside the scripts, no shell configuration changed).
- Build: `sh tools/build-mod.sh <mod-folder>` -> `out/<mod-id>/` (helper.jar, mod.json, README.md).
  The compile jar `build/bitwig-compile.jar` is created by the first build (never share/commit it).
- Tests: `python3 -m unittest discover -s tools` (manifest script, translate_hooks), `sh tools/test-java.sh track-height-lock` (lock rule),
  `sh tools/test-java.sh track-spacer` (spacer tags + plan + copy rule), `sh tools/test-java.sh timeline-wheel-zoom`,
  `sh tools/test-java.sh zoom-toggle` (follow-the-selection state),
  `sh tools/probe-layout.sh` (layout + painting with Bitwig's real classes).
- `build-mod.sh` only accepts mod folder names of `a-z 0-9 -` that contain a `mod.json` (checked before its `rm -rf`).
  `invert_remap.py` reads `$BML`, `make_uses.py` reads `$JDK` (defaults as before).
  Known: `build-mod.sh` still fails if the repo path contains a space (javac `@sources.txt`); `test-java.sh` handles it.
- Offline check (`loader/prepare-mod.sh`) overwrites `loader/mod-runtime` in the BML folder -> only after asking.

## Mod: track-height-lock (lock the track height)

### Findings (Bitwig 6.1.7, read only)
- Track height in the arranger = property `clip_lane_height` (int, default 43) in a track's display settings:
  data class `ydZ` (base `Sc`, field `Sc.y3f`), meta class `ctK`.
- Setter chain: `Sc.Gef(int)` -> `Sc.Heb(int)` (clamps) -> `Sc.iMb(int)` (only on change) -> `Sc.PJ3(int)` (writes, undo, informs the UI).
- Ways to get there:
  - Dragging the track edge: `bvd` (resize callback `dYW` of widget `dYU`) -> `zCn.Gef(int,bool)` = `kRM.Gef` -> `sJU.BBg` -> `qxz.beI` -> lambda -> `ydZ.Gef(int)`.
  - Dragging with modifier (all tracks): `bvd` -> `uVN.sCA(int)` -> `GJy.sCA(int)` -> per track `ydZ.Gef(int)`.
  - Zoom shortcuts `arranger_zoom_in/out_lane_heights_all/_selected` (actions in `aCZ`): `aGq`/`buZ` -> widget `dYU.BBg(long)` (+20 px), binding -> `ijY.setIntegerValue` -> `dfT.Gef` -> `ctK.Gef(ydZ,..)` -> `ydZ.Gef(int)`.
  - New track: `oKd` sets `last_clip_lane_height` (from `Dbp`) via `bjq.Gef(int)`.
  - Undo/sync: `ctK.beI(ydZ,..)` -> directly `iMb` (bypasses the hook).
  - Loading: `ctK.sCA(ydZ,..)` writes the field directly (bypasses the hook).
- Separate from this: automation lanes (`Xsy`, property `arranger_automation_editor_height`), detail editor (`ucp`/`t3h` `lane_height`).
- Track: `ydZ.p1J()` -> `sJU`; name `rMD.cb_()` (displayed_title), title `rMD.eXf()`, comment `rMD.wze()`.
- Names in 6.1.4 (macOS): Sc=`Hea`, ydZ=`hPk`, sJU=`zwV`, rMD=`cXR`, Heb=`TtY`, iMb=`fJR`, p1J=`Pgx`, wze=`AgD`, cb_=`ca_`.

### Context menu (version 0.2.0-dev.1)
- Framework "Actions and Menus" (`action-menu-composition-framework` 0.8.0-dev.1, installed in the BML folder,
  already prepared successfully for macOS 6.1.7 on this machine). Menu: `Menu.selection("track")`.
- Registration in `mod.json` (`registrations`) + `package/actions.properties` -> `TrackHeightLockActions`.
- Tracks from the menu: `context.selection()` is `bju` -> `bju.unz()` = `GRp` -> `GRp.MgQ()` = list (tracks = `sJU`).
  Pattern taken from BML's mod `hibernate-tracks` (6.1.4 names there: `big.UNj()`, `ICh.FzB()`).
- Setting the comment: `rMD.Heb(String)` (public, like Bitwig's own operation
  `channel_properties.operations.change_comment` in `FMm`). Getter `rMD.wze()`.
- Undo: `CmH.beI("<step name>", false)` opens a step, `CmH.beI(false)` closes it,
  on failure `CmH.cov()` (undo). Project via `sJU.cc_()`. (6.1.4 names: `g2L.cfM(String,Z)`, `cfM(Z)`, `Mb2()`.)
- `LockRule`: `#lock` only counts as a whole word (`#locked` doesn't). `withTag` appends " #lock",
  `withoutTag` removes every `#lock` plus exactly one space next to it, everything else stays the same.
- Bitwig shows track comments in the mixer (comment section) and in the inspector.
- `tools/build-mod.sh` now puts `classpath_mods` from build.json (the framework's helper.jar from BML/mods) on the class path.
- 0.2.1-dev.1: English menu texts ("Lock track height" / "Unlock track height"), author "Silent Orbit" (kept).

### Name mark " (L)" (version 0.2.2-dev.1)
- Title setter: `rMD.d_(String)` (Bitwig's own rename `channel_properties.operations.change_title` in `FMm`
  calls `gEH.d_`). Title getter `rMD.eXf()` (field `scr`, may be empty). Displayed name `rMD.cb_()`
  (`displayed_title`, computed separately by Bitwig, not right away in the setter).
- Lock: comment + " #lock" and title + " (L)" in the same undo step. Without a title of its own, the
  displayed name + " (L)" is set as the title (after that the name is fixed and no longer follows the device).
- Unlock: removes `#lock`, and `(L)`, `[L]` and the lock emoji from the title (each plus one space).
  The menu shows "Unlock" as soon as the comment OR the title/displayed name has a mark.
- `(L)` only counts as a word of its own (regex: no letter/digit right before/after): `(LFO)`, `Bass(L)` don't count.
- The lock check lives in `TrackLock` (no framework dependency), so the hook locks even without the framework.
- ZIP: `out/track-height-lock-0.2.2-dev.1.zip` (folder `track-height-lock/` at the top).

### Without undo (version 0.2.3-dev.1)
- Undo recording: every attribute change reports through `ytz.beI(dgs, ytz, String)` to the observer
  `dgT` (belongs to the undo writer `dgR`). `dgT.aboutToChangeAttributeValue(dgs, ytz, String)` only records
  if `dgs.dvk()` is true (flag bit 16 in the constructor value) and no undo/redo is running (`dgR.PJ3`).
- Flags: title 410 and comment 410 (undoable), displayed_title 385, is_modified 385, clip_lane_height 394 (not undoable).
- Without an open undo bracket, `diF.beI(dfz, ytz, Object)` still creates an entry: either a step of its own
  or (for some properties) appended to the PREVIOUS step -> leaving out the bracket is not enough.
- No public API to remove an undo step (`diF`; the history is also kept on disk).
- Solution: hook `lock-marks-without-undo` = REPLACE_CALL in `dgT.aboutToChangeAttributeValue(Ldgs;Lytz;Ljava/lang/String;)V`
  on `dgs.dvk()Z` (1st call). Returns false only while `TrackHeightLockHooks.runWithoutUndo(...)` runs, only on the
  calling thread, only for the attribute objects `rMD.RiZ().jTs()` (title) and `.mEL()` (comment).
- Afterwards `TzU.nhI(true)` = `is_modified` (not undoable itself), so Bitwig asks to save when closing.
- Undo/redo of other steps simply sets the stored old/new value (`dhd` -> `JDx.Gef(Object)`) without any check
  -> no crash/abort, but older steps can set name/comment back to an old state (mark gone or back).

### Reference 6.1.4 (version 0.3.0-dev.1)
- Only the 6.1.7 macOS jar is available; no 6.1.4 jar, no BML workspace (SourcePorter, port_manifest.py, mod_uses.py are missing).
  Map packs in the install only for macOS targets (6.0–6.1.7), no Windows/Linux packs.
- Approach without a 6.1.4 jar: the source stays in 6.1.7 names, build + modkit finish against the real 6.1.7 jar; then
  `tools/translate-mod.sh` (called by build-mod.sh, controlled by build.json `translate_to`):
  1. `tools/invert_remap.py`: inverse of `maps/macos-6.1.7-map1/remap/macos-6.1.4__macos-6.1.7.tsv.gz`
     (uniquely invertible, 639 537 mappings), H rows from the 6.1.7 jar -> `build/macos-6.1.7__macos-6.1.4.tsv.gz`.
  2. BML's `loader/tools/bitwig-helper-remapper.jar` translates helper.jar to 6.1.4 (0 warnings).
  3. `tools/translate_hooks.py` translates the hook targets, `finish_manifest.py` builds mod.json, `tools/make_uses.py` writes uses.tsv
     (declaring class via the 6.1.7 hierarchy, strictly checked against the remap).
  4. Checks: code round trip (back with the official remap = same code as the 6.1.7 build), hook round trip,
     `tools/check_uses.py` against all installed packs (read only).
- Result: available on macOS 6.1.2, 6.1.3, 6.1.4 (reference), 6.1.7. Not available on 6.0, 6.1, 6.1.1: the only missing name is
  `AJH.Z1C(Z)V` (6.1.4) = `TzU.nhI(Z)V` (6.1.7) = setter of `is_modified`.
  Possible way out (not built in, behaviour should stay the same): `TzU.iUU().setBoolValue(true)`
  (6.1.4 `AJH.Guv()` + `lBQ.setBoolValue`), both names exist in 6.0/6.1/6.1.1; test on 6.1.7 first.
- Names 6.1.7 -> 6.1.4 (excerpt): Sc=Hea, ydZ=hPk, sJU=zwV, rMD=cXR, gEH=TL, kRM=jJs, kd=Zb1, CmH=g2L, TzU=AJH,
  bju=big, GRp=ICh, dgs=dfg, dgm=deZ, dgT=dfL, ytz=oDc; Heb(I)=TtY, iMb=fJR, p1J=Pgx, wze=AgD, eXf=Dq, cb_=ca_,
  cc_=cb_ (careful: shifted!), d_=d_, rMD.Heb(String)=TtY, RiZ=FXc, jTs=ucQ, mEL=tp2, nhI=Z1C, dvk=Grk, unz=UNj, MgQ=FzB.
- Offline check (`loader/prepare-mod.sh`) NOT run (overwrites loader/mod-runtime) -> user step.

### Decisions
- One hook: `REPLACE_CALL` in `Sc.Heb(I)V` on the call `Sc.iMb(I)V`. Locked track -> the call is dropped.
  Covers dragging, Alt-dragging, zoom shortcuts and all bindings; loading and undo stay untouched.
- Lock per track without storage of its own: the name contains `(L)`/`[L]` (or the lock emoji) or the comment contains `#lock`.
  That way it is saved with the project automatically.
- Up to 0.2.3: `built_against` = macOS 6.1.7. From 0.3.0: `built_against` = macOS 6.1.4 (see "Reference 6.1.4").

### Status
- [x] Investigation, prototype built (`out/track-height-lock`), rule tests green.
- [x] Context menu (0.2.0/0.2.1) built and compiled.
- [x] 0.2.2-dev.1: " (L)" in the name, built, ZIP created (old 0.2.1 ZIP deleted).
- [x] 0.2.3-dev.1: lock/unlock without undo, built.
- [x] 0.3.0-dev.1: reference 6.1.4 (translated), ZIP `out/track-height-lock-0.3.0-dev.1.zip` (0.2.3 ZIP deleted).
- [ ] Offline check (deliberately not run, overwrites BML files).
- [ ] Test in Bitwig (Patchbay > Mods > Add Mod… > `out/track-height-lock`, enable Framework "Actions and Menus" too).

### Open risks
- Zoom shortcuts: the widget is enlarged first, the model refuses -> the row may briefly look wrong.
- The resize cursor still appears on locked tracks.
- Automation lanes are not locked.
- " (L)" is part of the real name: visible in the mixer, in export/stem file names, for controller scripts.
- Tracks without a title of their own: after unlocking the same name is shown, but as a fixed title (no automatic name any more).
- If the user renames a locked track and deletes "(L)", it stays locked through `#lock`.
- Without undo (0.2.3): not tested in Bitwig. Cmd+Z on an OLDER step that touched name/comment
  (e.g. a rename before locking) sets the old value -> " (L)" or "#lock" can disappear/come back.
  The lock stays as long as one mark is left. Whether the engine/controllers see the new name right away is unchecked.
- The dgT hook runs on every text change in Bitwig (fast path: just one thread comparison).

### Next steps (ideas)
1. (done: context menu)
2. Switch off vertical resizing on widget `dYU` (`is_vert_resizing_enabled`), so no cursor/drag handle appears.
3. Lock symbol: not wanted by the user.
4. Option "lock all tracks" via BML settings (`MOD.settings()`), lock automation lanes too.
5. Build against 6.1.4 names so it runs on all versions.

## Mod: track-spacer (counterpart of REAPER's "Insert spacer before/after tracks")

### Decision (0.2.0-dev.1): a pure gap, NO extra track
- 0.1.0-dev.1 (an empty audio track as spacer) was rejected by the user -> replaced completely.
- Now: an empty gap of N px above/below a track in the arranger (track header, timeline, launcher column in the arranger).
- Storage: tags in the track comment `#spacer-before` / `#spacer-after`, optionally `=N` (default 16 px, 1..400).
  Saved with the project; the menu actions are one undo step each.
- Menu (menuOnly, `Menu.selection("track")`): "Insert Spacer Before Tracks" (the first track of each block gets
  `#spacer-before`), "Insert Spacer After Tracks" (the last track gets `#spacer-after`), "Remove Spacer"
  (removes every gap touching a selected track: its own tags, `after` of the track above, `before` of the track below).
  A gap already at that spot (tag of either neighbour) -> nothing doubled. Logic in `SpacerPlan`/`SpacerRule` (tested).

### Layout findings (Bitwig 6.1.7, read only)
- Every arranger row of a (non-group) track is a `bvc` (extends `bvg` -> `bDw` ... -> `dvl`).
  Built in `bvc.Gef(YCD, bvi, aZJ)`; `bvi` is the variant: track header (`bub`/`bui`/`bum` via `buT`),
  timeline (`timeline.nSh` -> `uLT`/`IQP`, bvi `NlD`), launcher slots in the arranger (`bcW`, "arranger_matrix_track_slots_widget"),
  launcher controls (`bgI`, "track_launcher_controls_widget").
- Structure: vertical stack `dXL.sSO()` [ `dYU` (height = `clip_lane_height`, binding `YCD.rcd()->ZhV.LYs()`), automation container `dZi` ];
  last step `this.beI(dAc)` (= `dvl.beI(dAc)`, sets the row's content).
- The only reader of `ZhV.LYs()` (track height for layout) is `bvc` -> rows stack by widget sizes only;
  track header/timeline/launcher stay in sync as long as all `bvc` are treated the same.
- Spacers: `dXL.sCA(long)` appends a fixed `dZl` (unit: logical pixels, Bitwig uses e.g. 2/4),
  `dXL.Gef(min,max)` inserts at the top. `dZl.sCA(J)` = min, `dZl.MgQ(J)` = max, change -> `unz(J)` -> relayout.
  `dXL.needsGapBetweenWidgets` returns false if a neighbour is `oSd()` (a spacer) -> 0 px spacers change nothing.
- Groups: `GJy` is not a `YCD`; group header = `buH` (class of its own, height via `BPz.LYs()`). The group's master track is `wDt.smz()`.
- Comment observable: `rMD.GKF()` -> `Vyh` (new per call, attribute `kd.mEL()`), `addListener(NVD)`;
  `NVD.eventOccurred(Object)` (stable name). Setter chain `Heb` -> `iMb` -> `PJ3` -> `gNZ` fires `wLf.fireValueChanged(GKF(),..)`.
- Activate `rMD.ZRB(Z)` (6.1.4 `qoU`), delete tracks `qxz.beI(List,CmH)` (private in 6.1.7), `CmH.Gef(Collection)` (public) –
  known from 0.1.0, no longer used.

### Implementation
- Hook `track-row-spacers`: REPLACE_CALL in `bvc.Gef(LYCD;Lbvi;LaZJ;)V` on `bvc.beI(LdAc;)V` (1st call).
  Bridge `TrackSpacerHooks.setRowContent(bvc, dAc)`: puts the content into `dXL.sSO()` [dZl on top, content, dZl below], both 0 px,
  size from the comment; registry `WeakHashMap<sJU, List<WeakReference<dZl>[]>>`; one comment listener per track -> `refresh`.
  On failure -> Bitwig's original content, logged once.
- Only tracks in `group.xDy()` (main_tracks), no group master track (`group.smz() == track`) -> otherwise header/timeline could drift apart.
  Group tracks themselves were skipped in the menu (until 0.2.4).
- All `bvc` rows of such tracks get the stack, even without a tag (so tags take effect live later).
- 6.1.4 names: bvc=btR, Gef(YCD,bvi,aZJ)=MRY, beI(dAc)=cfM, dAc=dyC, YCD=iKU.

### Version 0.2.1-dev.1 (after the user's test in Bitwig)
- Problem 1: duplicating copied `#spacer-before/-after` (the comment is copied along).
  - Paths: Duplicate -> `CmH.sCA(List,Z)` -> `GJy.beI(List,int,Z,PPR,Eoh)` -> clipboard op (`gHg` subclasses
    `IdK`/`WCT`/`qHV`/`Cux`...) -> per track `GJy.beI(sJU,int)`. Copy & paste the same. Moving (`CyJ`/`M1M`) via `unz(PiT,int)`,
    but grouping/re-parenting (`PJ3(List)`, private `beI(GJy,sJU,int)`) ALSO calls `GJy.beI(sJU,int)` – with the same objects,
    sometimes briefly detached -> "has no parent" is no good test for a copy.
  - Solution: hook `copies-without-spacers` = ENTRY on `GJy.beI(Lcom/bitwig/flt/document/core/master/sJU;I)V` (6.1.4 `ruS.cfM`).
    Memory `SEEN` (WeakHashMap): on the first row build or first insert per project, mark all tracks of the project
    (`sJU.TUb()` = root group, recursively `xDy()`) as seen. An inserted track (for groups also all tracks inside)
    that was never seen = a copy -> `SpacerRule.withoutAll` via `Heb(String)`, then mark it as seen.
    Runs BEFORE inserting (the copy is not in the project yet) -> no undo step of its own.
  - Assumption: undo/redo and loading insert lists at attribute level (not via `GJy.beI(sJU,int)`); unchecked in Bitwig.
  - Tracks from other projects/presets (new to the project) also lose the tags – intended.
- Problem 2: the gap showed the blue stripe on the left (and possibly the selection colour), because the row (`bvc`/`bvV`, property
  `background_color` = `dxu.jZa()`, bound to "selected") paints over its whole height and `dZl` is transparent.
  - Solution: each spacer sits in a `dXL` box of its own with background `row.mum().SQL()` (theme "Grey 1", like the
    arranger panel `aZD` and the default row background `bvi.beI(egM)`), set via `dXM.beI(ekS)` (public).
    Children paint over the parent's background.
  - Outer box `byf(0)`: `dXL` has a default spacing of 2 px between non-spacer children (`dXM.LYs`), otherwise every track would get +4 px.
  - Unchecked: whether the stripe comes from the parent's background (then covered) or is painted as an overlay after the children
    (then it would stay visible); whether "Grey 1" matches the visible arranger black exactly; whether an empty `dXL` without padding is 0 px high.

### Version 0.2.2-dev.1 (fixes the 0.2.1 bug: no gap at all)
- User test of 0.2.1 (Bitwig 6.1.7, BML 0.36.0-dev.17 at `~/Library/Application Support/PatchbayLauncher/installs/BML-0.36.0-dev.17-macos`,
  NOT the install under /Applications): mod loaded and prepared (`PREPARE_OK ... verify`, replacement classes `bvc` and `GJy`
  loaded, `~/Library/Logs/Bitwig/BitwigStudio.log`), no exception, no `[track-spacer]` line in the log -> silent failure.
- Cause (proven): `dXU.kwH(child)` only lays out children that are spacers (`dZl`) OR whose max size has width>0 AND height>0.
  `dZl` always reports 0 across the stacking direction (`dZl.mL_()`), so a box holding only a spacer is 0 wide -> dropped.
  0.2.1 put each spacer into a holder box of its own (for the colour) -> gap always 0 px.
  Reproduced with the real classes: `track-spacer/probe/LayoutProbe.java` (`sh tools/probe-layout.sh`, read only):
  holder 16 px, row box 0 px.
- Fix: spacers as direct children again (as in 0.2.0). Colour via `dXM.beI(ekS)` on the row box itself: `dXL.beI(ijY, SZB)` paints the
  background ONLY over spacers and gaps between children, never under the content -> gap opaque in `mum().SQL()` ("Grey 1").
  `byf(0)` removed (not needed: next to spacers there is no spacing).
- Copy hook more defensive: only acts once the project is shown and recorded (`SEEN_PROJECTS`), never while loading;
  decision in `CopyRule` (tested). The hook is not part of the menu chain (menu -> comment -> listener -> gap).
- Checks: `sh tools/test-java.sh track-spacer` (incl. flow menu tag -> gap, copy -> without tag), `sh tools/probe-layout.sh`
  (`LAYOUT_PROBE_OK`), build with 2 hooks, round trip OK, name check 6.0–6.1.7 OK. Offline check (prepare-mod.sh) not run;
  but 0.2.1 with the same hook targets was prepared and verified in the real BML dev.17 install.
- Untested in Bitwig: painting of the gap colour (the probe only checks layout), whether that covers the blue stripe, copy hook in practice.

### Version 0.2.3-dev.1 (the gap in the track header was still blue)
- User test of 0.2.2: gap there, duplicating ok, but in the track header a blue stripe on the left through the gap.
- Cause (proven, `buT.sCA(YCD,GRp,aZJ)`): the track header is a SIDE-BY-SIDE box `dXL.fpX()` (spacing 1, background `egM.zzi()`)
  with [colour/selection widget `beI(zCn,Pv selected,Pv,Pv,buQ)`, bvc row]. The stripe is a SIBLING of the bvc row and as high
  as it -> a gap inside the bvc leaves it standing next to it. Can't be covered from inside the bvc.
  Other views: launcher slots (`bcS.sCA`) and launcher controls (`bgE.beI`) return the bvc directly, timeline (`nSh.sCA`)
  sets the bvc as content and colours itself (`nEh.cCV()`) -> there the bvc's box (a child) covers the parent's background.
  `buT` also inherits from `bvV` (background_color bound to "selected") -> also covered in the gaps by our box (a child).
- Fix: no spacers in the bvc inside the track header (ThreadLocal `IN_HEADER` around building the bvc); instead a box [spacer, whole header box, spacer]
  around the header's content. Hooks: REPLACE_CALL in `buT.sCA(LYCD;LGRp;LaZJ;)V` on `bvc.beI(LYCD;Lbvi;LaZJ;)Lbvc;`
  (`header-row-without-spacers`) and on `buT.beI(LdAc;)V` (`header-spacers`). 6.1.4: buT=btI, sCA(YCD,GRp,aZJ)=Hnt.
- Colour: `mum().zzi()` = theme "Grey 1" (default grey level 26 = #1A1A1A, defined in `egM` via `beI("Grey 1", 26)`) – the same colour
  Bitwig paints between tracks (track header box `buT`, timeline track list `timeline.mFm`). Passed as an egG object -> theme mods apply.
- Verified with Bitwig's real classes (`sh tools/probe-layout.sh`): `PaintProbe` builds the box like the hook, lays it out and paints it into a
  recording graphics object (proxy on `com.bitwig.graphics.ijY`): exactly the gap rectangles are painted across the full width in
  RGB 0.102/0.102/0.102, nothing under the content. `LayoutProbe`: heights correct live.
- Unverified: paint order in the real window (parents before children assumed, the standard), possible overlays after the children
  (e.g. "Muted by Audition" in `bDp`), the real picture in Bitwig.

### Version 0.2.4-dev.1 (group tracks, menu order)
- Groups: every arranger row of a group is a `buH` (header `buC`/`buL`, timeline `timeline.vak` with `pOC`, launcher `bcN`),
  built in `buH.Gef(uVN,buK,aZJ)`. Content = the whole group block: header box (colour stripe `bDm.beI(...)` INSIDE if `buK.Gef()`)
  above the fold-out container `dZi` with the child tracks (`aZJ.Iba()`). Set via `this.beI(dAc)` in two branches.
  -> hooks `group-spacers` / `group-spacers-plain`: REPLACE_CALL on `buH.beI(LdAc;)V` calls 1 and 2 (the manifest counts after the mod's own
  replacements -> both `occurrence 1`, correct according to the mod kit README). Group via `bDp.lN_()`. "Before" above the group header,
  "After" after the end of the group (open: below the children, closed: right below the header). Header/timeline/launcher all `buH` -> lined up.
  `buC` puts `buH` into a `doV` (padding 0, background Grey 0/`aZJ.DYQ()`, rounded left corners `bNN.Anf`) -> no sibling stripe,
  the background lies under `buH` and is painted over in the gaps by our box (a child).
  6.1.4: buH=btu, Gef(uVN,buK,aZJ)=MRY.
- Menu/plan: group exclusion removed (actions `lanes`). A group's master track, effect tracks and the project master stay excluded
  (not in `xDy()`). Duplicating a group: `collect` covers group + children -> tags of all copies removed (unchanged).
- Menu order: the framework sorts by action LABEL (`Registry.ORDER`, alphabetically) but shows `menuLabel`. For menuOnly the
  label appears nowhere -> labels "Insert Spacer 1 Before Tracks" / "Insert Spacer 2 After Tracks", menuLabel = real texts. IDs unchanged.
  Applies to framework 0.8.0-dev.1 (build) and 0.9.0-dev.4 (BML dev.17).
- Check: PaintProbe extended for groups (open: after-gap after header+children; closed: right after the header; only gaps
  painted in Grey 1). Build 6 hooks, round trip OK, name check 6.0–6.1.7 OK.
- Unverified in Bitwig: group look (rounded corners/frame of `buC` around the gap), animation when opening/closing,
  nested groups, menu order in the real menu.

### Version 0.2.5-dev.1 (spacers outside of undo)
- Request: insert/remove without an undo entry, undo/redo of other steps must never change a spacer.
- Decision: tags stay in the comment (saved with the project, survive Save As/autosave, duplicate logic stays,
  no migration). An external file per project was rejected (projects without a path, Save As, moving, packages -> fragile).
- Undo path (Bitwig 6.1.7, read): undo `dgR.LYs()` -> `diF.scr()`, redo `dgR.mEL()` -> `diF.Bqk()` (both with the private flag `dgR.PJ3`).
  Entry `dhd` -> `JDx.Gef(Object)` -> `dgs.sCA(ytz,String)` -> meta interface `dge.sCA(IqI,int,String)` -> `kd.sCA(gEH,int,String)`
  -> by attribute ID `gEH.PJ3(String)` (comment) -> `gNZ` (the only field writer). Apart from `kd`, nobody calls `gEH.PJ3` from outside.
  `dgO` (loading the history from disk) uses `dgR.SQL()/af()` -> deliberately NOT covered.
- Implementation (4 new hooks):
  - `spacer-tags-without-undo`: REPLACE_CALL in `dgT.aboutToChangeAttributeValue(dgs,ytz,String)` on `dgR.beI(ytz)Z` (Bitwig's own
    "don't record") -> true for the tracks in `runWithoutUndo` (this thread only). NOT `dgs.dvk()` (track-height-lock replaces that;
    two mods on the same invokevirtual would stop preparation; chaining only works for INVOKESTATIC).
  - `undo-marker`/`redo-marker`: REPLACE_CALL `diF.scr()` in `dgR.LYs()` and `diF.Bqk()` in `dgR.mEL()` -> thread counter (try/finally).
  - `undo-keeps-spacer-tags`: REPLACE_CALL `gEH.PJ3(String)` in `kd.sCA(gEH,int,String)`: during undo/redo
    `SpacerRule.keepTags(new, current)` (the spacer tags of the value written back are replaced by the current ones).
  - Menu: `runWithoutUndo` instead of an undo bracket, then `CmH.nhI(true)` (is_modified). The copy hook also removes tags without undo.
- Collision check against all mods in BML dev.17: only track-height-lock in the same dgT method, a different call -> ok.
- Availability now 6.1.2–6.1.7 (like track-height-lock): `TzU.nhI(Z)` (6.1.4 `AJH.Z1C`) is missing in 6.0/6.1/6.1.1.
- Tests: `keepTags` + simulation (set spacer silently, comment edit, undo/undo/redo/redo -> spacer stays; remove + undo doesn't bring it back).
- Unverified in Bitwig: that undo really goes through `kd.sCA` (derived from the code), undo history list/jumping in the history,
  behaviour after a restart (history from disk). Editing the tags by hand in the inspector is still undoable, but undo leaves the tags as they are.

### Build / checks (0.2.0-dev.1)
- `sh tools/build-mod.sh track-spacer`: modkit finish hooks=1 (target checked against the real 6.1.7 jar), remap 0 warnings,
  code + hook round trip OK, name check: available on macOS 6.0, 6.1, 6.1.1, 6.1.2, 6.1.3, 6.1.4, 6.1.7.
- Bytecode checked: the calls are the intended overloads.
- `tools/make_uses.py` doesn't know F rows/constructors (no longer relevant for 0.2.0, just a note).
- Offline check (`loader/prepare-mod.sh`) NOT run -> user step (checks among other things the bytecode verification of the hook).

### Status
- [x] Feasibility checked, gap variant built, logic tests green.
- [x] 0.2.1-dev.1: copies without spacers, gap opaque in the background colour; built (2 hooks), tests green.
- [x] 0.2.2-dev.1: gap visible again (holder box removed), colour via the row box, flow test with Bitwig's real classes green.
- [x] 0.2.3-dev.1: track header gap without colour stripe (spacers around the whole header), paint test with the real classes green.
- [x] 0.2.4-dev.1: group tracks (above the header / after the end of the group), menu Before above After; probes + tests green.
- [x] 0.2.5-dev.1: spacers without undo entry, undo/redo leaves spacers unchanged; built (10 hooks), tests green.
- [ ] Offline check (needs asking).
- [ ] Test in Bitwig (Patchbay > Mods > Add Mod… > `out/track-spacer`, enable Framework "Actions and Menus" too).

### Open risks (untested in Bitwig)
- Whether all arranger lists really go only through `bvc`/`buH` (e.g. effect/master area) – check header/timeline alignment in particular.
- Click/drop into the gap: belongs to the track's row (behaviour unchecked).
- Extra box level in every track row (even without a gap): possible side effects on background/hover/drag.
- Comment listener thread: assumed to be the UI thread (like all document changes from menus).
- Gaps live in the comment (visible in the inspector/mixer comment).

## Mod: timeline-wheel-zoom (mouse wheel over the arranger / detail editor ruler zooms the time axis)

### Findings (Bitwig 6.1.7, read only)
- Wheel path: macOS `CocoaTopLevelWindowHandle.handleMouseWheelEvent` -> `eqH` (window wheel event; units always
  `eqI.Gef` = pixels on macOS, a mouse notch = 15) -> `dqz.beI(eqH)`: reuses the last handler for 1 s while the mouse is
  inside the handler's widget (latching, fields `qJX`/`CtA`), else asks the widget tree -> handler `dty`, then `dty.beI(dLl)`.
- Search: `dwF.sCA(x,y,mLC,eqw)` -> `epr.beI` with post-order visitor `dwN` -> calls `dwF.beI(DDLmLC;Leqw;)Ldty;` deepest
  widget first, then parents; first non-null wins. Base `dwF` returns null. (Second path `dwF.Gef(...)`/`dwO` when
  `eqH.LYs()`, momentum-like events; `dVg.Gef` only scrolls there.)
- `dVg` (zoomable scroll view): `beI(...)` returns `sCA(axis)` = `dVm` (zoom handler) with Cmd (`eqw.sCA()`) or pref
  `drf.gMN()` (middle mouse navigation mode), else `Gef(axis)` = `dVk` (scroll). `dVm.beI(dLl)`: amount (negated if
  `drf.Sye()` = reverse wheel zoom pref), steps -> `dUD.beI(I,D)`, pixels -> `dUD.Gef(I,D)`, position = mouse along axis
  in the view -> zoom around the mouse. `dUD` = scrollbar model (controller API `ScrollbarModel`, `zoomAtPosition` = `dUD.sCA(DD)`).
- Static `dVg.MgQ(dwF, mLC)` walks up to the nearest dVg whose model on that axis is zoomable.
- Rulers: `vMG` (bars, extends `YxJ`) and `m2M` (time, extends `GmT`), both under `SiG`; only built in `PYe`.
  No class from `vMG`/`m2M` up to `dwF` overrides `beI(DDLmLC;Leqw;)Ldty;` (checked with javap).
- Arranger header: `aGm.beI(aZJ)` builds `haN` (subclass of `PES` -> `PYe`) and wraps it in `MhB` (extends `F2l` -> `dVg`);
  `MhB.qVz()` = horizontal `dUD`, also bound to the arranger's horizontal scrollbar `pQW`. Track lanes have their own `MhB`.
- `mLC`: `beI` = HORIZONTAL, `Gef` = VERTICAL (`mLC.Gef()` = is vertical). `eqw.Bqk()` = any modifier held.

### Implementation (0.1.0-dev.1)
- One hook `ruler-wheel-zooms`: OVERRIDE_INSTEAD on `SiG` for `beI(DDLcom/bitwig/base/geom/mLC;Leqw;)Ldty;`
  (manifest: ADD_METHOD, args slots `arg5`/`arg6`). Vertical wheel + no modifier + a `haN` among the parents ->
  `dVg.MgQ(ruler, HORIZONTAL).sCA(HORIZONTAL)` (Bitwig's own Cmd+wheel zoom handler); otherwise null (= old behaviour).
- Decision in `WheelRule` (tested: `sh tools/test-java.sh timeline-wheel-zoom`).
- Build: 1 hook, round trip OK, names available on macOS 6.0–6.1.7. `make_uses.py` reports the field `mLC.beI`
  (6.1.4 `LWw.cfM`) as UNRESOLVED (no F rows); checked by hand: the field exists in all 6 packs.
- ZIP: `out/timeline-wheel-zoom-0.1.0-dev.1.zip`. User test in Bitwig: works, direction fine, but choppy.

### Smooth zoom (0.2.0-dev.1)
- Why choppy: Bitwig's `dVm` applies each event at once: `dUD.Gef(int px, double mouse)` = zoom * 2^(px / `dpT.gNZ()`)
  (`dxM.Gef(IJ)`; notches: `dxM.beI(IJ)` = zoom * (1+`jTs()`)^n), then a deferred runnable (`drf.sCA(Runnable)`, only valid
  during event dispatch) sets offset = round(`dxM.beI(time, 0)` - mouse). A mouse notch on macOS = 15 px -> one visible jump.
- Names (6.1.7): `dUD.af()`/`dxM.beI()` = zoom (time per pixel), `dxM.beI(D,J,Z)` = set zoom, `dUD.PJ3()` = model (cast `dxM`),
  `dUD.VNw()` = offset, `dUD.Gak()` = visible size, `dUD.LRq()` = content size, `dUD.sCA(D)` = pixel -> time,
  `duW.beI(D,D)` = time -> pixel, `dUD.Gef(J)` = set offset (clamped by `duW.a_`).
  Frame clock like look-and-feel's SmoothScroll (6.1.4 `drd`/`dmO`/`dpM`): 6.1.7 `dsB.Gef(doe)` registers,
  `dsB.beI()` unregisters, `doe.aboutToPaint(dre)` per frame before layout, `dre.MgQ()` = seconds since last frame (0 first).
  look-and-feel's code itself is not reusable (its classes, its scroll models `dSX` = offset only), only the pattern.
- Implementation: hook returns `SmoothZoomHandler` (extends `dty`, widget = header `MhB`, so latching is unchanged).
  It turns each event into log2 zoom with Bitwig's own formula (x `ZOOM_STRENGTH`) -> `ZoomAnimator` (implements `doe`):
  `ZoomGlide` (pure Java, tested) hands out an exponential share per frame (`SMOOTHING_SECONDS`), each frame sets the zoom and
  the offset immediately so the time under the mouse stays (anchor time fixed while the mouse stays put).
  Stops if offset/zoom were changed by someone else, the view is hidden, or the zoom limit is reached.
  Any exception -> fallback to Bitwig's `dVm` for the rest of the session (logged once).
- Constants in `ZoomSettings`: `ZOOM_STRENGTH` 1.0, `SMOOTHING_SECONDS` 0.07, `REVERSE_DIRECTION` false,
  `MAX_PENDING_DOUBLINGS` 4, `FINISH_BELOW` 0.0005.
- `drf.Sye()` (reverse wheel zoom pref) has NO 6.1.4 counterpart in the official remap (pref not in 6.1.4) -> not read
  (would break 6.0–6.1.4 or hit a foreign method). Replaced by `REVERSE_DIRECTION`.
- Build: 1 hook, round trip OK, 37 names OK on 6.0–6.1.7. make_uses UNRESOLVED: fields `LWw.cfM`, `epi.cfM` (checked by hand,
  in all 6 packs) and the constructor `dsc.<init>(dve)` (constructors aren't remapped; class exists in all packs).
  Translated jar: `SmoothZoomHandler.cfM(dJN)` = 6.1.7 `dty.beI(dLl)` (correct override).
- ZIP: `out/timeline-wheel-zoom-0.2.0-dev.1.zip` (the 0.1.0 ZIP is still there; delete only after asking). Not committed.

### Open risks (untested in Bitwig)
- Offset set immediately per frame (Bitwig defers it to after the event): if the offset clamp uses a content size that is
  only updated at layout, the anchor may lag a frame; the next frame corrects it (anchor time is fixed).
- Offset is whole pixels: at very deep zoom the anchor can wobble by up to half a pixel.
- If Bitwig's follow-playhead moves the view during a glide, the glide stops (by design).
- Latching: Bitwig keeps the handler for 1 s while the mouse is inside the header `MhB`.
- The old plain-wheel behaviour over the ruler is replaced; with a modifier it is unchanged.
- Offline check (`prepare-mod.sh`) not run (overwrites BML's loader/mod-runtime) -> ask first.

### Detail editor / piano roll (0.3.0-dev.1, NOT tested in Bitwig)
- Wish: the same wheel zoom over the ruler of the detail editor (piano roll / note editor).
- Decision: COMBINED in this mod (no separate `piano-roll-wheel-zoom`). Reasons (6.1.7, read only, own ClassFile-API scanner on bitwig.jar):
  - Same ruler classes: `vMG`/`m2M` (base `SiG`) are only built in `PYe.beI(olw,bkw,OnD)`/`PYe.Gef(...)`; the existing hook on `SiG`
    already runs for the detail editor's rulers, it just answered null there (`haN` check). No ruler class is a `PYe`.
  - Same scroll view: the detail editor wraps its header in the same `MhB` (`aNi.beI(dwF, ju1, Z)` -> `MhB.beI(dwF,ju1)`);
    the first `MhB.qVz()` becomes the editor's horizontal model `aNi.ccv` (`dUD`), later ones share it (`MhB.beI(dVI)`).
    So `dVg.MgQ(ruler, HORIZONTAL)`, `SmoothZoomHandler` and `ZoomAnimator` work unchanged.
  - No second hook, no new hook target, the arranger path is unchanged (only the place check got a second answer).
- Detail editor structure: panel `baN` (string `detail_editor.no_clip_selected`) -> `baN.beI(bke,HSH)` picks one of four content widgets
  (all extend `aNi`, a widget): `aHd` (model `bka`, header `timeline.mLC`), `aKK` (`bky`, header `aeK`), `aHp` (`bkc`, header `bMt`),
  `aKU` (`bkz`, header `tic`). Header built in `aNi.Gef(HSH,bke,baN)` -> `beI(HSH,bke,SYP,baN)`. Which variant is which mode
  (notes / audio / automation, clip / track mode) was not worked out; all four are detail editor and get the zoom.
- PYe subclasses in 6.1.7: `haN` (arranger, built only in `aGm.beI(aZJ)`) and `timeline.mLC`, `aeK`, `bMt`, `tic` (built only by the
  four `aNi` subclasses above). Rule: first `PYe` above the ruler: `haN` -> ARRANGER, any other `PYe` -> DETAIL_EDITOR, none -> OTHER.
- Note: `timeline.mLC` (detail editor header) is NOT `com.bitwig.base.geom.mLC` (the axis).
- Implementation: `WheelRule.Place` (ARRANGER / DETAIL_EDITOR / OTHER), `WheelRule.shouldZoom(vertical, modifier, place, detailEditorOn)`
  (tested), `TimelineWheelZoomHooks.placeOf(ruler)`, switch `ZoomSettings.DETAIL_EDITOR` (true; false = arranger only as before).
- Collision check: no mod in BML dev.17 hooks `SiG`/`Mwz` (6.1.4). Note: BML has a "Framework: Piano Roll" (`piano-roll-framework`), it does not hook this method.
- 6.1.4 names: SiG=Mwz, PYe=dBl, haN=BrJ, MhB=Nfw, aNi=aMh, baN=aZv.
- Build: 1 hook, remap 0 warnings, round trip OK, 38 names (new: `PYe`) OK on macOS 6.0–6.1.7; same 3 UNRESOLVED as 0.2.0 (known).
  Enum switch compiles to a plain `ordinal()` tableswitch (no extra class).
- ZIP: `out/timeline-wheel-zoom-0.3.0-dev.1.zip` (0.2.1 ZIP still there). Not committed.
- Open (only checkable in Bitwig): that the wheel over the detail editor ruler really zooms smoothly around the mouse; that
  Cmd+wheel there did zoom before (same `MhB`, so expected); that the note area follows the ruler (shared model `ccv`); behaviour in the
  audio/automation modes and in the detail editor in a separate window / Edit layout; the wheel over the notes stays unchanged.

## Mod: zoom-toggle (one shortcut: zoom to the selection + tall track, again: back)

### Attempt "restore exact height" REVERTED (after review, version number unchanged)
- Idea: remember lane heights (`btR`/`dYU.gPa()`), set them back with `dYU.BBg(old)` instead of 40 steps down
  (Bitwig stops at its maximum on the way up, so 40 down can end below the start height).
- In Bitwig 6.1.7 it broke: the second press did not shrink the track, further presses made it taller and taller.
  Reverted to the old 40 steps up / 40 steps down. The below-start-height case remains a known limit.
- Not understood why (probably `gPa()`/`BBg()` do not behave like the zoom step does). Needs a Bitwig-side probe before a retry.

### Tall track follows the selection (still 0.2.0-dev.1, version number unchanged, NOT tested in Bitwig)
- Wish: while zoomed, clicking another track/clip makes that track tall and the previous one small again.
- Findings (6.1.7, read only; javap + a small ClassFile-API scanner on bitwig.jar. Careful: unzipping the jar on macOS
  loses classes, names differ only in case, e.g. `aZJ`/`azj` -> always read from the jar):
  - Action chain: `aCZ.Bqr`/`Tsf` -> `aCZ.VkJ`/`gHt` -> `aZJ.beI(±1, true)` -> `aGq.beI(IZ)` -> if `bkD.unz()` (lane mode) lanes via
    `buZ.beI(I,List,List)`, else `buZ.beI(I, true, bkD.sCA())`. `buZ.beI(IZList)` visits ALL `bDm` rows below it (recursive, incl.
    groups) and steps each row whose track (`bDm.lIt()`) is in the list (`buT.beI(I)`/`buC.beI(I)`), then scrolls to the listed tracks.
    -> the selection is only a filter list; we pass our own list. No selection change, same step code, height is not undoable (flag 394).
  - No Bitwig action for "a specific track": only `arranger_zoom_in/out_lane_heights_all/_selected`.
  - `aGq` sets `bkD.beI(false)` (no lane mode) when the arranger is built. `aZJ.ocs()` = `aGq`, `aGq.UIe()` = `bkD`,
    `bkD.sCA()` = selected tracks (List of `zCn`), `bkD.Anf()` = `bkE`, `bkE.TaR()` = observable list `cxg`;
    `cxg.beI(loD)`/`Gef(loD)` add/remove a list listener (`loD` methods have real names: insertedItems, aboutToRemoveItems, ...).
    Bitwig's own `aGq.Gef(uVN)` listens to that same list.
  - Root rows widget: `aGq.gNZ` (private) = the first `buZ` found by `epr.beI(aGq, buZ.class, Consumer)` (parents before children);
    nested groups have their own `buZ` (`buC.beI(uVN,aZJ)`) inside it. Sanity check `buZ.XzC() == view`.
- Implementation: `FollowState` (pure Java, tested: `sh tools/test-java.sh zoom-toggle`), `SelectionFollower` (listener + one-shot frame
  callback `dsB.Gef(doe)` like timeline-wheel-zoom; the listener only schedules, the step runs on the next frame, several events = one step).
  - First press unchanged (Bitwig's action); before it the selected tracks are remembered as "tall".
  - Selection changed: shrink tall-not-selected by 40, grow selected-not-tall by 40 (shrink first, so the scroll ends on the new track).
    Empty selection = no change. Same tracks = no change. Lane mode (`bkD.unz()`) = no following.
  - Second press: if tall == selected -> Bitwig's `aCZ.Tsf` as before; otherwise shrink exactly the tall tracks via `buZ`.
    Fixes "the wrong track shrinks" (also when the selection changed without a follow step).
  - Loop guard: events ignored while our step runs (busy) and while one is pending; our step never touches the selection.
  - Any error: following off until the next first press; second press then uses `aCZ.Tsf` (old behaviour). Logged once.
- Build: 0 hooks, remap 0 warnings, round trip OK. Names: OK on 6.1.1–6.1.7; on 6.0 and 6.1 two names are missing
  (`bkD.sCA()` = 6.1.4 `bjp.Hnt`, `buZ.beI(IZList)` = `btO.cfM`) -> the mod is no longer available there (README requires 6.1.2+ anyway).
  The 3 UNRESOLVED `aBW` fields are the old action fields (as before). Link probe (classes loaded against the real jar) OK.
- First user test (track-selection version): following works, BUT it also followed a pure track selection (click on the
  track header / empty track row) and made tracks without clips tall. -> changed to follow the CLIP selection (below).

### Follow the selected clips instead of the selected tracks (still 0.2.0-dev.1, NOT tested in Bitwig)
- Wish: the tall track only changes when clips are selected; it becomes the track(s) of the selected clip(s).
  Only a track selected / click into empty space (no clips selected) = nothing changes.
- Findings (6.1.7, read only):
  - Arranger timeline selection model `bkb` (field `aZJ.Hqm`, also `aCG.fmw()`) extends `bkw` -> `bjw`: `bjw.MgQ()` = `bjs`
    (object selection = clips), `bjw.ckt()` = `bjv` (time selection). `bjs` is built on `rBG.yX3()` = `WSI` (impl `YLc`, a `GRp`).
  - Public path: `aZJ.eNb()` = `BPz` (arranger timeline, an `rBG`) -> `yX3()` (declared in `pXr`) = `WSI` -> `jvQ()` = `AAN` (a `cxg`):
    the observable list of selected objects. Same list `bjs.TaR()` returns.
  - Arranger clips: `BVS` (impl `TKq` -> `Iui`/`EdV`, concrete `eOj` and `Pb` = note clip). Clip -> track: `BVS.ZRB()` (`sct`) = `oxD`
    -> `oxD.GwM()` = ancestor of class `YCD` cast to `zCn`. Bitwig's own `bkb.scr()` maps selected clips the same way (`BVS.ZRB()`).
  - `YCD` is implemented by `PiT`, which extends `sJU` (checked with `isAssignableFrom`) -> the same track objects as in the track
    selection and the rows (`bDm.lIt()`). Group tracks have no clips.
- Change: `SelectionFollower` listens to the clip list (not the track list); a follow step takes the tracks of the selected `BVS`
  items (`FollowState.tracksOf`: each once, non-clips skipped). No clips = no change (`FollowState.follow` with an empty list).
  First press and second press unchanged (tall = selected tracks at the first press; back: `aCZ.Tsf` if tall == selected tracks,
  else shrink exactly the tall tracks). Lane mode (`bkD.unz()`) still = no following.
- Build: 0 hooks, remap 0 warnings, round trip OK, 48 names OK on 6.1.1–6.1.7; 6.0/6.1 still missing the same two names. Link probe OK.
- ZIP: `out/zoom-toggle-0.2.0-dev.1.zip` (replaced). Not committed.
- Open risks (untested in Bitwig): whether the clip list fires on every clip click (also on re-selecting); clips selected in the
  launcher or in the detail editor are a different selection (not followed, intended); selecting only automation points / a time range
  is no clip -> no change; whether the frame callback comes promptly; Z back after following: Bitwig's "zoom to fit selection or previous"
  decides the time axis; shrinking only (no grow) scrolls to the shrunk track; deleted tracks in the tall list are simply skipped.

### First press: only tracks with a selected clip (still 0.2.0-dev.1, NOT tested in Bitwig)
- User test of the clip-follow version: following on clip clicks works. Left over: a click into empty space selects a track
  (without a clip); Z then made THAT track tall, because the first press still ran `aCZ.Bqr` on the track selection.
- Change (`ZoomToggleActions`): the first press reads the tracks of the selected clips (`SelectionFollower.selectedClipTracks`,
  same path as the follow step) and decides with `FollowState.firstPress` (tested):
  - no clip selected -> NOTHING: return at once (launcher, time axis, `zoomed` untouched, nothing half done);
  - clips selected -> CLIP_TRACKS: launcher hidden as before, `buZ.beI(+1, true, clipTracks)` x 40 (same step size as `Bqr`),
    `aCZ.Or` as before, following starts with tall = the clip tracks;
  - clip tracks unknown (lane mode, no view, error) -> BITWIG_ACTION: `aCZ.Bqr` x 40 on the selected tracks as before, tall = selected tracks.
  - If the `buZ` step fails (rows not found / exception before a step) -> fallback `aCZ.Bqr`, tall = selected tracks.
- Way back unchanged: `aCZ.Tsf` only if tall == selected tracks, else shrink exactly the tall tracks (never another one).
- Build: 0 hooks, remap 0 warnings, round trip OK, 48 names OK on 6.1.1–6.1.7 (6.0/6.1: same two missing names).
- ZIP: `out/zoom-toggle-0.2.0-dev.1.zip` (replaced). Not committed.
- Open: the first press no longer checks via Bitwig's action whether the arranger handles it (the arranger panel around the focus
  and a shown arranger view are still required); "Zoom to Fit Selection or Previous" still decides the time axis by itself.
