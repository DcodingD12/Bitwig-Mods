# Arranger: Zoom Toggle (prototype)

Needs **Framework: Actions and Menus** to be enabled too.

In Settings > Shortcuts search for **Zoom to Fit Selection (Toggle)** and give it a key (e.g. Z).

- First press: hides the clip launcher (if it is shown), zooms the time axis to the selection (Bitwig's own
  "Zoom to Fit Selection or Previous") and makes the track(s) of the selected clip(s) tall. Other tracks are not
  touched. With no clip selected (e.g. only a track, after a click into empty space) the key does nothing.
- While zoomed: select a clip on another track and the tall track follows: the track of the selected clip
  becomes tall, the previous one goes back down (several clips selected: all their tracks become tall).
  Selecting only a track (track header, empty part of a track row) or clicking into empty space changes
  nothing: the tall track stays tall. The time axis is not changed by this.
- Second press: zooms back, lowers the tall track again (the one that is tall at that moment, even if
  something else is selected now) and shows the clip launcher again (only if this mod hid it).

Why the launcher is hidden: the clip launcher and the arranger share their rows, so a taller track would
make the launcher row tall as well.

Works in the **arranger only**; with the focus elsewhere (e.g. the clip editor) the key does nothing.

Known limits:
- The height uses a fixed number of steps (40) up and the same number down. If Bitwig stops at its maximum height
  on the way up, the track can end up a little lower than before.
- If the selected clips can't be read, the first press falls back to Bitwig's own step on the selected tracks.
- If following fails, it switches itself off until the next first press, and the key behaves as before.

## Bitwig versions

Written against Bitwig 6.1.4 names (macOS), so BML's map packs translate it to other versions.
Checked offline against the map packs in BML 0.36.0-dev.13 (names only):

| Bitwig (macOS) | Available |
|---|---|
| 6.1.2, 6.1.3, 6.1.7 | yes |
| 6.1.4 | yes (the reference build) |
| 6.0, 6.1 | no: two names (`bkD.sCA()`, `buZ.beI(IZList)`) have no counterpart there |

Windows and Linux were not checked (no map packs for them in this BML install).
It is a pre-release that works in daily use on Bitwig 6.1.7 (macOS), but expect rough edges.
