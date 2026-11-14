# Arranger: Track Height Lock

Keeps the height of chosen tracks in the arranger fixed.

## How to lock a track

Either
- put `[L]` (or the lock emoji) into the track's name, e.g. `[L] Drums`, or
- write `#lock` into the track's comment (Inspector).

Remove the marker again to unlock. The lock is stored with the project, because the name and the comment are.

## What is blocked

Every height change of a locked track: dragging its lower edge, Alt-dragging (all tracks),
the "zoom lane heights" shortcuts. Unlocked tracks behave as usual.

## Known limits (prototype)

- Automation lanes below a track are not locked.
- The resize cursor still shows on a locked track; dragging just does nothing.
- With the zoom shortcuts the row may briefly look resized until Bitwig redraws it.
- Built for Bitwig 6.1.7 (macOS) only.
