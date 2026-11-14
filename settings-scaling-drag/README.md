# Settings: Drag Display Scaling

Lets you change **Settings > Display > Scaling** by dragging the percent value up or down with the mouse, instead of clicking **-** / **+**.

## What it does

- Bitwig builds the scaling value as a normal number field, but switches mouse dragging off for it. This mod switches it back on.
- Nothing else changes: **-**, **+** and **Auto** work as before.

## Limits

- **Still 25% steps.** Bitwig rounds the scale to quarter steps (25%) deep in its drawing code, not only in this field. Dragging moves 100% -> 125% -> 150% and so on.
- While you drag, the whole window is redrawn at the new scale, so the value can move under the mouse pointer.

## Version

Built against Bitwig Studio 6.1.7 (macOS). It runs only on that exact Bitwig build.
