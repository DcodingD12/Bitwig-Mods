# Arranger: Timeline Wheel Zoom (prototype)

Move the mouse over a timeline ruler (the strip at the top with the bar numbers 1, 2, 3 … and,
if shown, the time 0:00, 0:01 …) and turn the mouse wheel. This works on the ruler of the
**arranger** and, since 0.3.0, on the ruler of the **detail editor** (piano roll / note editor;
its audio and automation modes use the same ruler, so they zoom too):

- the time axis zooms in or out, smoothly, around the mouse position,
- by the same amount per notch and in the same direction as Cmd+wheel, but spread over a few
  frames instead of one jump. Spinning the wheel adds up; fine trackpad moves zoom a little,
  a coarse mouse notch zooms more, both glide.

Everywhere else nothing changes. With any modifier key held (Shift, Cmd, Alt, Ctrl) the wheel
over the ruler works as before. If something else moves the view during a glide (scrollbar,
follow playhead, a shortcut), the glide stops there.

No framework needed. Only the rulers are affected: the wheel over the notes, clips or tracks
stays as it is.

## Tuning

The values are constants in `src/bitwigmods/timelinewheelzoom/ZoomSettings.java`
(change, rebuild with `sh tools/build-mod.sh timeline-wheel-zoom`, re-import):

- `DETAIL_EDITOR` (true): also zoom over the detail editor's ruler; false = arranger ruler only.
- `ZOOM_STRENGTH` (1.0): zoom per notch compared with Cmd+wheel.
- `SMOOTHING_SECONDS` (0.07): how soft the glide is; smaller = snappier, 0 = no smoothing.
- `REVERSE_DIRECTION` (false): swap wheel up and down. Bitwig's own "reverse zoom direction"
  setting is not followed (Bitwig 6.1.4 and older don't have it).
- `MAX_PENDING_DOUBLINGS` (4.0): how much zoom a fast spin can queue up.

## Bitwig versions

Written against Bitwig 6.1.4 names (macOS), so BML's map packs translate it to other versions.
Checked offline against the map packs in BML 0.36.0-dev.13 (names only):

| Bitwig (macOS) | Available |
|---|---|
| 6.0, 6.1, 6.1.1, 6.1.2, 6.1.3, 6.1.7 | yes |
| 6.1.4 | yes (the reference build) |

Windows and Linux were not checked (no map packs for them in this BML install).
It is a pre-release that works in daily use on Bitwig 6.1.7 (macOS), but expect rough edges.
