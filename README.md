# Bitwig Mods

Small arranger mods for [Bitwig Studio](https://www.bitwig.com/) that run on [**Patch Bay**](https://github.com/shaboogen-dev/Patchbay) (BML, the Bitwig Mod Loader). By Silent Orbit.

| Mod | What it does |
|---|---|
| [**Track Height Lock**](track-height-lock/README.md) | Lock the arranger height of chosen tracks so it can't be changed by accident. |
| [**Track Spacer**](track-spacer/README.md) | Add an empty gap above or below tracks, like REAPER's "Insert spacer before/after tracks". No extra track is created. |
| [**Zoom Toggle**](zoom-toggle/README.md) | One key hides the clip launcher, zooms to the selected clip and makes its track tall; press again to go back. |
| [**Timeline Wheel Zoom**](timeline-wheel-zoom/README.md) | Plain mouse wheel over the timeline ruler (arranger and detail editor / piano roll) zooms the time axis smoothly around the mouse. |
| [**Pinned Tracks**](pinned-tracks/README.md) | Like Cubase's "Divide Track List": pinned tracks stay fixed at the top of the arranger, the rest scrolls below a thin divider. |

All mods are **pre-releases** (`0.x-dev`), used daily on Bitwig 6.1.7 (macOS). Windows and Linux are untested.

## Requirements

- Bitwig Studio **6.1.2 – 6.1.7** on **macOS**.
- [Patch Bay](https://github.com/shaboogen-dev/Patchbay) with BML. The mods don't run without it.
- **Framework: Actions and Menus** (`action-menu-composition-framework`), enabled, for Track Height Lock, Track Spacer, Zoom Toggle and Pinned Tracks. Timeline Wheel Zoom doesn't need it.
- Pinned Tracks runs on Bitwig 6.1.4 and 6.1.7 only (a method it needs is missing in older versions).

## Installation

1. Install Patch Bay and follow its setup steps.
2. Download the mod's ZIP from the [Releases page](../../releases). Don't unzip it.
3. In Patch Bay: **Mods > Add Mod…**, choose the ZIP, enable the mod (and the framework if needed).
4. Start Bitwig through Patch Bay. Remove an older version of the mod first.

## Usage in short

- **Track Height Lock:** right-click a track > *Lock / Unlock track height*. The track gets `#lock` in its comment and ` (L)` in its name and ignores all height changes. Not undoable; save to keep it.
- **Track Spacer:** right-click a track > *Insert Spacer Before / After Tracks* or *Remove Spacer*. Stored as `#spacer-before` / `#spacer-after` (optionally `=30` for the size in px) in the comment. Not part of Undo; not copied when duplicating a track.
- **Zoom Toggle:** assign a key to **Zoom to Fit Selection (Toggle)** in *Settings > Shortcuts*. It zooms to the selected clip, makes its track tall (following the selection) and goes back on the second press.
- **Timeline Wheel Zoom:** turn the wheel over the ruler of the arranger or the detail editor (piano roll), no modifier needed. With Shift/Cmd/Alt/Ctrl held, the wheel behaves as before.
- **Pinned Tracks:** right-click a top-level track > *Pin to Top* / *Unpin*. Stored as `#pin` in the comment; undoable. Pinned tracks sit between the ruler and the scrolling tracks in every arranger column (track headers, timeline, clip launcher), followed by a thin divider in the bar line colour and a 20 px gap. They can be reordered by drag and drop within the pinned area.

Each mod's README has the details and limits.

## Building from source

Needs JDK 25, Python 3, Bitwig Studio and BML (paths and internals in [NOTES.md](NOTES.md)).

```sh
sh tools/build-mod.sh track-spacer          # build one mod -> out/<mod-id>/
sh tools/test-java.sh track-spacer          # Java tests
sh tools/probe-pinned.sh                    # pinned-tracks building blocks with Bitwig's real classes
python3 -m unittest discover -s tools       # tool tests
```

The mods use Bitwig-internal classes and are built against the Bitwig jar on your machine (the compile jar in `build/` is never committed). Zip `out/<mod-id>/` to get an installable mod.

## Known limits

- Prototype quality; only used on Bitwig 6.1.7 (macOS).
- Track Height Lock and Track Spacer are menu-only (no shortcut) and store their tags in the track comment.
- Bitwig updates can change internal names; BML's map packs may need to catch up first.

## Disclaimer

Not affiliated with or endorsed by Bitwig GmbH. Use at your own risk and keep backups of your projects.
