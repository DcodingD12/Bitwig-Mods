# Bitwig Mods

Small arranger mods for [Bitwig Studio](https://www.bitwig.com/) that run on [**Patch Bay**](https://github.com/shaboogen-dev/Patchbay) (BML, the Bitwig Mod Loader).
By Silent Orbit.

| Mod | What it does |
|---|---|
| [**Track Height Lock**](track-height-lock/README.md) | Lock the arranger height of chosen tracks so it can't be changed by accident. |
| [**Track Spacer**](track-spacer/README.md) | Add an empty gap above or below tracks in the arranger, like REAPER's "Insert spacer before/after tracks". No extra track is created. |
| [**Zoom Toggle**](zoom-toggle/README.md) | One key zooms to the selected clip in time and makes its track tall; press again to go back. |

All mods are **pre-releases** (`0.x-dev`): they work in daily use on Bitwig 6.1.7 (macOS), but expect rough edges.
Windows and Linux have not been checked.

## Requirements

- Bitwig Studio **6.1.2 – 6.1.7** on **macOS** (built against 6.1.4, translated to other versions by BML's map packs).
- [Patch Bay](https://github.com/shaboogen-dev/Patchbay) with BML installed. The mods do not run without it.
- The mod **Framework: Actions and Menus** (`action-menu-composition-framework`) must be installed and enabled.
  The mods add their menu entries and shortcut actions through it.

## Installation

1. **Install Patch Bay first.** It is the program that loads mods into Bitwig; without it none of these mods work.
   Get it from [github.com/shaboogen-dev/Patchbay](https://github.com/shaboogen-dev/Patchbay) and follow the
   installation steps there.
2. Download the ZIP of the mod you want from the [Releases page](../../releases)
   (`track-height-lock-….zip`, `track-spacer-….zip` and/or `zoom-toggle-….zip`). Don't unzip it.
3. Open **Patch Bay**, go to **Mods > Add Mod…** and choose the ZIP.
4. Make sure **Framework: Actions and Menus** is enabled, and enable the mod.
5. Start Bitwig through Patch Bay. If an older version of the mod is installed, remove it first.

## Track Height Lock

Right-click a track (arranger header or mixer):

- **Lock track height** locks the selected tracks. `#lock` is added to the track comment and ` (L)` to the name
  (`Omnisphere` becomes `Omnisphere (L)`).
- **Unlock track height** removes both again; everything else in comment and name stays untouched.

A locked track ignores every height change: dragging its lower edge, Alt-drag (all tracks) and the
"zoom lane heights" shortcuts. Unlocked tracks behave as usual.

- Locking and unlocking are **not undoable** and never get an undo entry. Save the project (Cmd+S) to keep the lock.
- You can lock by hand: type `#lock` into the comment or `(L)` into the name.
- Not covered: automation lanes below a track.

Details: [track-height-lock/README.md](track-height-lock/README.md)

## Track Spacer

Right-click a track in the arranger:

| Menu entry | Effect |
|---|---|
| **Insert Spacer Before Tracks** | Gap above the selected tracks (above the header for a group). |
| **Insert Spacer After Tracks** | Gap below the selected tracks (below the end of a group). |
| **Remove Spacer** | Removes every gap touching the selected tracks. |

- With several tracks selected, each block of neighbouring tracks gets one gap. An existing gap is never doubled.
- The gap appears in the track header, the timeline and the clip launcher column, and stays lined up. It is painted
  in the arranger's background colour, so there is no coloured stripe.
- It is stored as a tag in the track comment: `#spacer-before` / `#spacer-after`, 16 px by default.
  Use `#spacer-before=30` for your own size (1–400 px). You can edit the tags by hand.
- Spacers are **not part of Undo**: inserting or removing one adds no undo entry, and undo/redo of other changes
  never adds, removes or changes a spacer. Save the project (Cmd+S) to keep it.
- Duplicating or pasting a track does **not** copy its spacer; moving or grouping keeps it.
- Effect tracks and the master track get no gaps.

Details: [track-spacer/README.md](track-spacer/README.md)

## Zoom Toggle

Adds the action **Zoom to Fit Selection (Toggle)**. It has no default key: in **Settings > Shortcuts** search for
it and assign one (for example **Z**).

- First press: zooms the time axis to the selection (Bitwig's own "Zoom to Fit Selection or Previous") and makes
  the selected track tall.
- Second press: zooms back and lowers the track again.

- The track height uses a fixed number of steps (40), so the result is a prototype's guess, not an exact fit
  to the window height.
- Only tested on Bitwig 6.1.7 (macOS); it may not work on other versions yet.
- Whether it also works in the clip editor has not been checked.

Details: [zoom-toggle/README.md](zoom-toggle/README.md)

## Building from source

Requirements: JDK 25, Python 3, Bitwig Studio and BML (see [NOTES.md](NOTES.md) for paths and background).

```sh
sh tools/build-mod.sh track-spacer          # build one mod -> out/<mod-id>/
sh tools/test-java.sh track-spacer          # Java tests
python3 -m unittest discover -s tools       # tool tests
```

The mods call Bitwig-internal classes, so they are built against the Bitwig jar on your machine
(the compile jar in `build/` is generated from it and is never committed). Zip the resulting
`out/<mod-id>/` folder to get an installable mod. [NOTES.md](NOTES.md) documents the internals
(which Bitwig classes are used, how the hooks work, what was verified).

## Known limits

- Prototype quality; only used on Bitwig 6.1.7 (macOS).
- The menu entries of Track Height Lock and Track Spacer are menu-only (no keyboard shortcut, not in the Commander). Zoom Toggle is a shortcut action.
- Track Height Lock and Track Spacer use the track comment for their tags, so the tags show wherever Bitwig shows the comment.
- Bitwig updates can change internal names; BML's map packs may need to catch up first.

## Disclaimer

Not affiliated with or endorsed by Bitwig GmbH. Use at your own risk and keep backups of your projects.
