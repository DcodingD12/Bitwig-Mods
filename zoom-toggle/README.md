# Arranger: Zoom Toggle (prototype)

Needs **Framework: Actions and Menus** to be enabled too.

In Settings > Shortcuts search for **Zoom to Fit Selection (Toggle)** and give it a key (e.g. Z).

- First press: hides the clip launcher (if it is shown), zooms the time axis to the selection (Bitwig's own
  "Zoom to Fit Selection or Previous") and makes the selected track tall.
- Second press: zooms back, lowers the track again and shows the clip launcher again (only if this mod hid it).

Why the launcher is hidden: the clip launcher and the arranger share their rows, so a taller track would
make the launcher row tall as well.

Works in the **arranger only**; with the focus elsewhere (e.g. the clip editor) the key does nothing.
