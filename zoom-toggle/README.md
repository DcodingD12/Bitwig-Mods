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
