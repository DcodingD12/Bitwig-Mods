# Arranger: Timeline Wheel Zoom (prototype)

Move the mouse over the arranger's timeline ruler (the strip at the top with the bar numbers
1, 2, 3 … and, if shown, the time 0:00, 0:01 …) and turn the mouse wheel:

- the time axis zooms in or out, smoothly, around the mouse position,
- by the same amount per notch and in the same direction as Cmd+wheel, but spread over a few
  frames instead of one jump. Spinning the wheel adds up; fine trackpad moves zoom a little,
  a coarse mouse notch zooms more, both glide.

Everywhere else nothing changes. With any modifier key held (Shift, Cmd, Alt, Ctrl) the wheel
over the ruler works as before. If something else moves the view during a glide (scrollbar,
follow playhead, a shortcut), the glide stops there.

No framework needed. Only the arranger's ruler is affected, not the ruler of the detail editor.

## Tuning

The values are constants in `src/bitwigmods/timelinewheelzoom/ZoomSettings.java`
(change, rebuild with `sh tools/build-mod.sh timeline-wheel-zoom`, re-import):

- `ZOOM_STRENGTH` (1.0): zoom per notch compared with Cmd+wheel.
- `SMOOTHING_SECONDS` (0.07): how soft the glide is; smaller = snappier, 0 = no smoothing.
- `REVERSE_DIRECTION` (false): swap wheel up and down. Bitwig's own "reverse zoom direction"
  setting is not followed (Bitwig 6.1.4 and older don't have it).
- `MAX_PENDING_DOUBLINGS` (4.0): how much zoom a fast spin can queue up.
