# Arranger: Track Height Lock

Keeps the height of chosen tracks in the arranger fixed.

Needs **Framework: Actions and Menus** (`action-menu-composition-framework`) to be enabled too.

## How to lock a track

- Right-click the track (its header in the arranger, or in the mixer) and choose **Spurhöhe sperren**.
  This adds `#lock` to the end of the track's comment. Several selected tracks are locked at once.
- To unlock, right-click again and choose **Spurhöhe entsperren**. This removes `#lock` from the comment;
  the rest of the comment and the name stay exactly as they were.
- Both are one undo step each (Edit > Undo).

You can also type `#lock` into the comment by hand, or put `[L]` (or the lock emoji) into the track's name.
A track locked by its name can only be unlocked by renaming it; the menu then shows no entry.

The lock is stored with the project, because the comment and the name are.

## What is blocked

Every height change of a locked track: dragging its lower edge, Alt-dragging (all tracks),
the "zoom lane heights" shortcuts. Unlocked tracks behave as usual.

## Known limits (prototype)

- Automation lanes below a track are not locked.
- The resize cursor still shows on a locked track; dragging just does nothing.
- With the zoom shortcuts the row may briefly look resized until Bitwig redraws it.
- The menu entries are menu-only (no shortcut, not in the Commander).
- Built for Bitwig 6.1.7 (macOS) only.
