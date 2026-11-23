# Arranger: Track Spacer

Adds an empty gap of a few pixels above or below tracks in the arranger, like REAPER's track spacers
("Insert spacer before/after tracks"). **No extra track** is created: nothing changes in the mixer,
track numbers stay the same.

Needs **Framework: Actions and Menus** (`action-menu-composition-framework`) to be enabled too.

## How to use it

Right-click a track (its header in the arranger):

- **Insert Spacer Before Tracks** adds a gap above the selected tracks.
- **Insert Spacer After Tracks** adds a gap below the selected tracks.
- **Remove Spacer** removes every gap that touches the selected tracks (above or below them).

Group tracks work too: "before" puts the gap above the group's header, "after" below the end of the
group (below its tracks when the group is open, right below the header when it is closed).

With several selected tracks, each block of tracks that sit next to each other gets one gap
(above its first or below its last track). If there already is a gap at that spot, no second one is added.
Spacers are **not part of Undo**: inserting or removing one adds no entry to Edit > Undo, and Cmd+Z / Redo of
other changes never adds, removes or changes a spacer. A spacer only changes through the menu (or by editing the
tag in the comment yourself). The project is marked as changed, so save it (Cmd+S) to keep the spacer.

The gap shows in the track headers, the timeline and the clip launcher column of the arranger, so they stay lined up.
It is painted in the arranger's background colour (no track colour, no selection colour).

A copy of a track (Duplicate, Copy & Paste) does not get the gap: its spacer tags are removed, the rest of
its comment stays. Moving or grouping a track keeps its gap.

## Where the gap is stored

In the track's comment, as a tag:

- `#spacer-before`: a gap of 16 pixels above the track
- `#spacer-after`: a gap of 16 pixels below the track
- `#spacer-before=30`: the same with a size of your own (1 to 400 pixels)

You can type or edit these tags yourself (in the inspector); the gap follows right away.
They are saved with the project.

## Bitwig versions

Written against Bitwig 6.1.4 names (macOS), so BML's map packs translate it to other versions.
Checked offline against the map packs in BML 0.36.0-dev.13 (names only):

| Bitwig (macOS) | Available |
|---|---|
| 6.1.2, 6.1.3, 6.1.7 | yes |
| 6.1.4 | yes (the reference build) |
| 6.0, 6.1, 6.1.1 | no: one name (marking the project as changed) has no counterpart there |

Windows and Linux were not checked (no map packs for them in this BML install).
It is a pre-release that works in daily use on Bitwig 6.1.7 (macOS), but expect rough edges.

## Known limits (prototype)

- Effect tracks and the master track get no gaps.
- The tags are part of the comment, so they show wherever Bitwig shows the comment.
- The menu entries are menu-only (no shortcut, not in the Commander).
