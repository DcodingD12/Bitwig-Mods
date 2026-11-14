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

### Kontextmenü (Version 0.2.0-dev.1)
- Framework "Actions and Menus" (`action-menu-composition-framework` 0.8.0-dev.1, im BML-Ordner installiert,
  auf diesem Rechner schon erfolgreich für macOS 6.1.7 vorbereitet). Menü: `Menu.selection("track")`.
- Registrierung in `mod.json` (`registrations`) + `package/actions.properties` -> `TrackHeightLockActions`.
- Spuren aus dem Menü: `context.selection()` ist `bju` -> `bju.unz()` = `GRp` -> `GRp.MgQ()` = Liste (Spuren = `sJU`).
  Muster abgeschaut von BML's Mod `hibernate-tracks` (dort 6.1.4-Namen `big.UNj()`, `ICh.FzB()`).
- Kommentar setzen: `rMD.Heb(String)` (öffentlich, wie Bitwigs eigene Operation
  `channel_properties.operations.change_comment` in `FMm`). Getter `rMD.wze()`.
- Undo: `CmH.beI("Spurhöhe sperren", false)` öffnet einen Schritt, `CmH.beI(false)` schließt ihn,
  bei Fehler `CmH.cov()` (Undo). Projekt über `sJU.cc_()`. (6.1.4-Namen: `g2L.cfM(String,Z)`, `cfM(Z)`, `Mb2()`.)
- `LockRule`: `#lock` zählt nur als ganzes Wort (`#locked` nicht). `withTag` hängt " #lock" an,
  `withoutTag` entfernt jedes `#lock` plus genau ein Leerzeichen daneben, sonst bleibt alles gleich.
- Bitwig zeigt Spurkommentare im Mixer (Kommentar-Bereich) und im Inspector.
- `tools/build-mod.sh` legt jetzt `classpath_mods` aus build.json (Framework-helper.jar aus BML/mods) auf den Klassenpfad.
- 0.2.1-dev.1: Menütexte englisch ("Lock track height" / "Unlock track height"), Autor "Silent Orbit" (beibehalten).

### Name-Markierung " (L)" (Version 0.2.2-dev.1)
- Titel-Setter: `rMD.d_(String)` (Bitwigs eigenes Umbenennen `channel_properties.operations.change_title` in `FMm`
  ruft `gEH.d_` auf). Titel-Getter `rMD.eXf()` (Feld `scr`, kann leer sein). Angezeigter Name `rMD.cb_()`
  (`displayed_title`, wird von Bitwig getrennt berechnet, nicht sofort im Setter).
- Sperren: Kommentar + " #lock" und Titel + " (L)" im selben Undo-Schritt. Ohne eigenen Titel wird der
  angezeigte Name + " (L)" als Titel gesetzt (danach ist der Name fest, folgt nicht mehr dem Gerät).
- Entsperren: entfernt `#lock` und im Titel `(L)`, `[L]` und Schloss-Emoji (je plus ein Leerzeichen).
  Menü zeigt "Unlock", sobald Kommentar ODER Titel/angezeigter Name eine Markierung hat.
- `(L)` zählt nur als eigenes Wort (Regex: kein Buchstabe/Ziffer direkt davor/danach): `(LFO)`, `Bass(L)` zählen nicht.
- Sperrprüfung liegt in `TrackLock` (ohne Framework-Abhängigkeit), damit der Hook auch ohne Framework sperrt.
- ZIP: `out/track-height-lock-0.2.2-dev.1.zip` (Ordner `track-height-lock/` oben).

### Entscheidungen
- Ein Hook: `REPLACE_CALL` in `Sc.Heb(I)V` auf den Aufruf `Sc.iMb(I)V`. Gesperrte Spur -> Aufruf fällt weg.
  Deckt Ziehen, Alt-Ziehen, Zoom-Kürzel und alle Bindungen ab; Laden und Undo bleiben unberührt.
- Sperre pro Spur ohne eigene Speicherung: Name enthält `(L)`/`[L]` (oder Schloss-Emoji) oder Kommentar enthält `#lock`.
  Wird so automatisch mit dem Projekt gespeichert.
- `built_against` = macOS 6.1.7 (läuft nur genau auf diesem Build).

### Stand
- [x] Untersuchung, Prototyp gebaut (`out/track-height-lock`), Regel-Tests grün.
- [x] Kontextmenü (0.2.0/0.2.1) gebaut und kompiliert.
- [x] 0.2.2-dev.1: " (L)" im Namen, gebaut, ZIP erstellt (altes 0.2.1-ZIP gelöscht).
- [ ] Offline-Check (bewusst nicht ausgeführt, überschreibt BML-Dateien).
- [ ] Test in Bitwig (Patchbay > Mods > Add Mod… > `out/track-height-lock`, Framework "Actions and Menus" mit aktivieren).

### Offene Risiken
- Zoom-Kürzel: Widget wird zuerst vergrößert, Modell lehnt ab -> Zeile kann kurz falsch aussehen.
- Resize-Cursor erscheint weiter auf gesperrten Spuren.
- Automations-Lanes werden nicht gesperrt.
- " (L)" ist Teil des echten Namens: sichtbar im Mixer, in Export-/Stem-Dateinamen, für Controller-Skripte.
- Spuren ohne eigenen Titel: nach Entsperren gleicher Name sichtbar, aber fester Titel (kein Auto-Name mehr).
- Benennt der Nutzer eine gesperrte Spur um und löscht dabei "(L)", bleibt sie über `#lock` gesperrt.
- Undo: ein Schritt für Kommentar + Name; nicht in Bitwig getestet.

### Ausbaustufe (Ideen)
1. (erledigt: Kontextmenü)
2. Am Widget `dYU` vertikales Resizing abschalten (`is_vert_resizing_enabled`), damit kein Cursor/kein Zieh-Griff erscheint.
3. Schloss-Symbol: vom Nutzer nicht gewünscht.
4. Option "alle Spuren sperren" über BML-Settings (`MOD.settings()`), Automations-Lanes mit sperren.
5. Gegen 6.1.4-Namen bauen, damit es auf allen Versionen läuft.
