# NOTES – Bitwig-Mods für Patchbay (BML)

## Umgebung
- BML 0.36.0-dev.13 unter `/Applications/BML-0.36.0-dev.13-macos` (Doku: `docs/sdk/README.md`, Modkit: `modkit/`).
- Bitwig Studio 6.1.7 macOS, Jars unter `/Applications/Bitwig Studio.app/Contents/Java/`.
- JDK 25: `/opt/homebrew/opt/openjdk@25` (nur in den Skripten benutzt, keine Shell-Konfiguration geändert).
- Bauen: `sh tools/build-mod.sh <mod-ordner>` -> `out/<mod-id>/` (helper.jar, mod.json, README.md).
  Das Compile-Jar `build/bitwig-compile.jar` entsteht beim ersten Bau (nie teilen/committen).
- Tests: `python3 -m unittest discover -s tools` (Manifest-Skript), `sh tools/test-java.sh track-height-lock` (Sperr-Regel).
- Offline-Check (`loader/prepare-mod.sh`) überschreibt `loader/mod-runtime` im BML-Ordner -> nur nach Rückfrage.

## Mod: track-height-lock (Spurhöhe sperren)

### Erkenntnisse (Bitwig 6.1.7, nur gelesen)
- Spurhöhe im Arranger = Property `clip_lane_height` (int, Standard 43) in den Display-Einstellungen
  einer Spur: Datenklasse `ydZ` (Basis `Sc`, Feld `Sc.y3f`), Meta-Klasse `ctK`.
- Setter-Kette: `Sc.Gef(int)` -> `Sc.Heb(int)` (begrenzt) -> `Sc.iMb(int)` (nur bei Änderung) -> `Sc.PJ3(int)` (schreibt, Undo, meldet UI).
- Wege dorthin:
  - Ziehen am Spurrand: `bvd` (Resize-Callback `dYW` des Widgets `dYU`) -> `zCn.Gef(int,bool)` = `kRM.Gef` -> `sJU.BBg` -> `qxz.beI` -> Lambda -> `ydZ.Gef(int)`.
  - Mit Modifier ziehen (alle Spuren): `bvd` -> `uVN.sCA(int)` -> `GJy.sCA(int)` -> pro Spur `ydZ.Gef(int)`.
  - Zoom-Kürzel `arranger_zoom_in/out_lane_heights_all/_selected` (Aktionen in `aCZ`): `aGq`/`buZ` -> Widget `dYU.BBg(long)` (+20 px), Bindung -> `ijY.setIntegerValue` -> `dfT.Gef` -> `ctK.Gef(ydZ,..)` -> `ydZ.Gef(int)`.
  - Neue Spur: `oKd` setzt `last_clip_lane_height` (aus `Dbp`) per `bjq.Gef(int)`.
  - Undo/Sync: `ctK.beI(ydZ,..)` -> direkt `iMb` (am Hook vorbei).
  - Laden: `ctK.sCA(ydZ,..)` schreibt das Feld direkt (am Hook vorbei).
- Getrennt davon: Automations-Lanes (`Xsy`, Property `arranger_automation_editor_height`), Detail-Editor (`ucp`/`t3h` `lane_height`).
- Spur: `ydZ.p1J()` -> `sJU`; Name `rMD.cb_()` (displayed_title), Titel `rMD.eXf()`, Kommentar `rMD.wze()`.
- Namen 6.1.4 (macOS): Sc=`Hea`, ydZ=`hPk`, sJU=`zwV`, rMD=`cXR`, Heb=`TtY`, iMb=`fJR`, p1J=`Pgx`, wze=`AgD`, cb_=`ca_`.

### Entscheidungen
- Ein Hook: `REPLACE_CALL` in `Sc.Heb(I)V` auf den Aufruf `Sc.iMb(I)V`. Gesperrte Spur -> Aufruf fällt weg.
  Deckt Ziehen, Alt-Ziehen, Zoom-Kürzel und alle Bindungen ab; Laden und Undo bleiben unberührt.
- Sperre pro Spur ohne eigene Speicherung: Name enthält `[L]` (oder Schloss-Emoji) oder Kommentar enthält `#lock`.
  Wird so automatisch mit dem Projekt gespeichert.
- `built_against` = macOS 6.1.7 (läuft nur genau auf diesem Build).

### Stand
- [x] Untersuchung, Prototyp gebaut (`out/track-height-lock`), Regel-Tests grün.
- [ ] Offline-Check (bewusst nicht ausgeführt, überschreibt BML-Dateien).
- [ ] Test in Bitwig (Patchbay > Mods > Add Mod… > `out/track-height-lock`).

### Offene Risiken
- Zoom-Kürzel: Widget wird zuerst vergrößert, Modell lehnt ab -> Zeile kann kurz falsch aussehen.
- Resize-Cursor erscheint weiter auf gesperrten Spuren.
- Automations-Lanes werden nicht gesperrt.

### Ausbaustufe (Ideen)
1. Aktion "Spurhöhe sperren/entsperren" im Spur-Kontextmenü über das Framework "action-menu-composition-framework"
   (setzt/entfernt `#lock` im Kommentar).
2. Am Widget `dYU` vertikales Resizing abschalten (`is_vert_resizing_enabled`), damit kein Cursor/kein Zieh-Griff erscheint.
3. Schloss-Symbol im Spurkopf (Hook im Spurkopf-Builder, aufwendig) – oder einfach Emoji im Namen.
4. Option "alle Spuren sperren" über BML-Settings (`MOD.settings()`), Automations-Lanes mit sperren.
5. Gegen 6.1.4-Namen bauen, damit es auf allen Versionen läuft.
