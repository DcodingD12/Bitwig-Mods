# Arranger: Track Height Lock

Keeps the height of chosen tracks in the arranger fixed.

Needs **Framework: Actions and Menus** (`action-menu-composition-framework`) to be enabled too.

## How to lock a track

- Right-click the track (its header in the arranger, or in the mixer) and choose **Lock track height**.
  This adds `#lock` to the end of the track's comment and ` (L)` to the end of its name
  (e.g. `Omnisphere` becomes `Omnisphere (L)`), so you can see the lock in the track list.
  Several selected tracks are locked at once.
- To unlock, right-click again and choose **Unlock track height**. This removes `#lock` from the comment
  and `(L)` from the name (also an older `[L]` or lock emoji); the rest of comment and name stay exactly as they were.
- Locking and unlocking are **not undoable**: Cmd+Z / Edit > Undo never locks or unlocks a track,
  and they add no entry to the undo history. Only the menu changes the lock.
  The project is marked as changed, so save it (Cmd+S) to keep the lock.

You can also lock by hand: type `#lock` into the comment, or `(L)`, `[L]` or the lock emoji into the name.
`(L)` only counts as a word of its own: `Bass (LFO)` or `Bass(L)` are not locked.

Note: a track without a name of its own shows an automatic name (e.g. the instrument's name).
Locking writes that name plus ` (L)` as the track's name. After unlocking the same name is shown again,
but it is now a fixed name and no longer follows the instrument automatically.

The lock is stored with the project, because the comment and the name are.
The ` (L)` is part of the real track name, so it also shows in the mixer and in exported file names.

## What is blocked

Every height change of a locked track: dragging its lower edge, Alt-dragging (all tracks),
the "zoom lane heights" shortcuts. Unlocked tracks behave as usual.

## Bitwig versions

Written against Bitwig 6.1.4 names (macOS), so BML's map packs translate it to other versions.
Checked offline against the map packs in BML 0.36.0-dev.13 (names only):

| Bitwig (macOS) | Available |
|---|---|
| 6.1.7, 6.1.3, 6.1.2 | yes |
| 6.1.4 | yes (the reference build) |
| 6.1.1, 6.1, 6.0 | no: one name (marking the project as changed) has no counterpart there |

Windows and Linux were not checked (no map packs for them in this BML install).
Only Bitwig 6.1.7 on macOS has been used in practice.

## Known limits (prototype)

- Undo of an older step can still bring back an older name or comment: e.g. rename a track, lock it,
  then press Cmd+Z: the rename is undone and the name loses its ` (L)`. The `#lock` in the comment
  stays, so the track stays locked; "Unlock track height" cleans up the rest.

- Automation lanes below a track are not locked.
- The resize cursor still shows on a locked track; dragging just does nothing.
- With the zoom shortcuts the row may briefly look resized until Bitwig redraws it.
- The menu entries are menu-only (no shortcut, not in the Commander).
