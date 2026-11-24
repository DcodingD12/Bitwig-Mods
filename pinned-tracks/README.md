# Arranger: Pinned Tracks

Like Cubase's **Divide Track List**: pinned tracks stay in a fixed area at the top of the arranger,
right below the ruler. The other tracks scroll below it as usual. A thin divider separates the two areas.

Needs **Framework: Actions and Menus** (`action-menu-composition-framework`) to be enabled too.

## How to pin a track

- Right-click a track header in the arranger and choose **Pin to Top**. Several selected tracks are pinned at once.
  This adds `#pin` to the end of the track's comment.
- To unpin, right-click it (in the pinned area) and choose **Unpin**. This removes `#pin`; the rest of the comment stays as it was.
- Both are one undo step each (Cmd+Z unpins/pins again).
- You can also pin by hand: type `#pin` into the track's comment (inspector or mixer). `#pinned` doesn't count.

The pin is stored with the project, because the comment is. Without the mod the project opens as usual
(the tracks are just in their normal place, `#pin` stays in the comment).

## What it looks like

- Order from the top: ruler, Global row (tempo/markers, as before), **pinned tracks**, divider, scrolling tracks.
- Pinned tracks keep their order from the project. They don't move when you scroll up or down, in every
  column of the arranger: track headers, the timeline with the clips, and the clip launcher (slots and
  controls) when it is shown in the arranger. Scrolling and zooming in time works for both areas together.
- The pinned tracks are Bitwig's normal track rows: edit clips, mute/solo/arm, select, resize, automation lanes.
- The divider has the colour of the timeline's bar lines on the track lanes (taken from Bitwig's theme and
  grid contrast setting when the arranger is shown). It is only there while at least one track is pinned.
- A pinned track is not shown a second time in the scrolling area.

## Which tracks

Only tracks and group tracks at the **top level** of the project. Pinning a group pins the whole group
with its tracks. Tracks inside a group, effect tracks and the master track can't be pinned
(the menu entry isn't shown for them).

## Bitwig versions

Written against Bitwig 6.1.4 names (macOS), like the other mods in this repository, so BML's map packs
translate it to other versions. Checked offline against the map packs in BML 0.36.0-dev.13 (names only):

| Bitwig (macOS) | Available |
|---|---|
| 6.1.7 | yes (the target) |
| 6.1.4 | yes (the reference build) |
| 6.1.3, 6.1.2, 6.1.1, 6.1, 6.0 | no: one method the mod needs (the switch for the effect track section of a track list) doesn't exist there yet |

With **Zoom Toggle**, use version 0.3.1-dev.1 or newer (older versions could pick the pinned list instead of the scrolling one).

## Known limits (prototype, not tested in Bitwig yet)

- The pinned area has no maximum height and the divider can't be dragged yet: pin many tall tracks and the
  scrolling area gets small.
- Duplicating a pinned track copies the comment, so the copy is pinned too.
- Bitwig builds every track row twice (once per area, one of them hidden). Very large projects may feel slower.
- Changing the theme or the grid contrast setting changes the divider colour only after the arranger is
  rebuilt (e.g. switch the project or the display profile).
- Shortcuts that scroll to a track (e.g. after creating one) scroll the lower area; a pinned track is always visible anyway.
- Clip launcher in the arranger: the pinned tracks' clip slots are a second slot grid that scrolls through the
  scenes together with Bitwig's, but isn't the one Bitwig's scene handling knows about. Launching clips should work;
  scene-wide actions (e.g. keyboard navigation through slots, scene highlighting) may not reach pinned tracks.
- The clip launcher in the Mix view is not changed (only the arranger).
- Menu-only (no shortcut, not in the Commander).
