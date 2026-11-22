# NOTES – Bitwig-Mods für Patchbay (BML)

## Umgebung
- BML 0.36.0-dev.13 unter `/Applications/BML-0.36.0-dev.13-macos` (Doku: `docs/sdk/README.md`, Modkit: `modkit/`).
- Bitwig Studio 6.1.7 macOS, Jars unter `/Applications/Bitwig Studio.app/Contents/Java/`.
- JDK 25: `/opt/homebrew/opt/openjdk@25` (nur in den Skripten benutzt, keine Shell-Konfiguration geändert).
- Bauen: `sh tools/build-mod.sh <mod-ordner>` -> `out/<mod-id>/` (helper.jar, mod.json, README.md).
  Das Compile-Jar `build/bitwig-compile.jar` entsteht beim ersten Bau (nie teilen/committen).
- Tests: `python3 -m unittest discover -s tools` (Manifest-Skript, translate_hooks), `sh tools/test-java.sh track-height-lock` (Sperr-Regel),
  `sh tools/test-java.sh track-spacer` (Spacer-Tags + Plan).
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

### Ohne Undo (Version 0.2.3-dev.1)
- Undo-Aufzeichnung: jede Attribut-Änderung meldet sich über `ytz.beI(dgs, ytz, String)` beim Beobachter
  `dgT` (gehört zum Undo-Schreiber `dgR`). `dgT.aboutToChangeAttributeValue(dgs, ytz, String)` zeichnet nur auf,
  wenn `dgs.dvk()` true ist (Flag-Bit 16 im Konstruktor-Wert) und nicht gerade Undo/Redo läuft (`dgR.PJ3`).
- Flags: title 410 und comment 410 (undo-fähig), displayed_title 385, is_modified 385, clip_lane_height 394 (nicht undo-fähig).
- Ohne offene Undo-Klammer legt `diF.beI(dfz, ytz, Object)` trotzdem einen Eintrag an: entweder automatisch einen eigenen
  Schritt oder (bei manchen Properties) angehängt an den VORHERIGEN Schritt -> Klammer weglassen reicht nicht.
- Keine öffentliche API zum Entfernen eines Undo-Schritts (`diF`, Verlauf liegt auch auf der Platte).
- Lösung: Hook `lock-marks-without-undo` = REPLACE_CALL in `dgT.aboutToChangeAttributeValue(Ldgs;Lytz;Ljava/lang/String;)V`
  auf `dgs.dvk()Z` (1. Aufruf). Liefert false nur während `TrackHeightLockHooks.runWithoutUndo(...)`, nur auf dem
  aufrufenden Thread, nur für die Attribut-Objekte `rMD.RiZ().jTs()` (title) und `.mEL()` (comment).
- Danach `TzU.nhI(true)` = `is_modified` (selbst nicht undo-fähig), damit Bitwig beim Schließen nach Speichern fragt.
- Undo/Redo anderer Schritte setzt einfach den gespeicherten alten/neuen Wert (`dhd` -> `JDx.Gef(Object)`), ohne Prüfung
  -> kein Absturz/Abbruch, aber ältere Schritte können Name/Kommentar auf alten Stand setzen (Markierung weg oder zurück).

### Referenz 6.1.4 (Version 0.3.0-dev.1)
- Nur die 6.1.7-macOS-Jar ist da; keine 6.1.4-Jar, kein BML-Workspace (SourcePorter, port_manifest.py, mod_uses.py fehlen).
  Map-Packs im Install nur für macOS-Ziele (6.0–6.1.7), keine Windows-/Linux-Packs.
- Weg ohne 6.1.4-Jar: Quelltext bleibt in 6.1.7-Namen, Bau + modkit finish gegen die echte 6.1.7-Jar; danach
  `tools/translate-mod.sh` (von build-mod.sh aufgerufen, gesteuert über build.json `translate_to`):
  1. `tools/invert_remap.py`: Umkehrung von `maps/macos-6.1.7-map1/remap/macos-6.1.4__macos-6.1.7.tsv.gz`
     (eindeutig umkehrbar, 639 537 Zuordnungen), H-Zeilen aus der 6.1.7-Jar -> `build/macos-6.1.7__macos-6.1.4.tsv.gz`.
  2. BML's `loader/tools/bitwig-helper-remapper.jar` übersetzt helper.jar nach 6.1.4 (0 Warnungen).
  3. `tools/translate_hooks.py` übersetzt die Hook-Ziele, `finish_manifest.py` baut mod.json, `tools/make_uses.py` schreibt uses.tsv
     (deklarierende Klasse über die 6.1.7-Hierarchie, streng gegen die Remap geprüft).
  4. Prüfungen: Rundreise Code (zurück mit offizieller Remap = gleicher Code wie 6.1.7-Build), Rundreise Hooks,
     `tools/check_uses.py` gegen alle installierten Packs (nur lesend).
- Ergebnis: verfügbar macOS 6.1.2, 6.1.3, 6.1.4 (Referenz), 6.1.7. Nicht verfügbar 6.0, 6.1, 6.1.1: es fehlt nur
  `AJH.Z1C(Z)V` (6.1.4) = `TzU.nhI(Z)V` (6.1.7) = Setter von `is_modified`.
  Möglicher Ausweg (nicht eingebaut, Verhalten soll gleich bleiben): `TzU.iUU().setBoolValue(true)`
  (6.1.4 `AJH.Guv()` + `lBQ.setBoolValue`), beide Namen sind in 6.0/6.1/6.1.1 vorhanden; vorher auf 6.1.7 testen.
- Namen 6.1.7 -> 6.1.4 (Auszug): Sc=Hea, ydZ=hPk, sJU=zwV, rMD=cXR, gEH=TL, kRM=jJs, kd=Zb1, CmH=g2L, TzU=AJH,
  bju=big, GRp=ICh, dgs=dfg, dgm=deZ, dgT=dfL, ytz=oDc; Heb(I)=TtY, iMb=fJR, p1J=Pgx, wze=AgD, eXf=Dq, cb_=ca_,
  cc_=cb_ (Achtung: verschoben!), d_=d_, rMD.Heb(String)=TtY, RiZ=FXc, jTs=ucQ, mEL=tp2, nhI=Z1C, dvk=Grk, unz=UNj, MgQ=FzB.
- Offline-Check (`loader/prepare-mod.sh`) NICHT ausgeführt (überschreibt loader/mod-runtime) -> Nutzer-Schritt.

### Entscheidungen
- Ein Hook: `REPLACE_CALL` in `Sc.Heb(I)V` auf den Aufruf `Sc.iMb(I)V`. Gesperrte Spur -> Aufruf fällt weg.
  Deckt Ziehen, Alt-Ziehen, Zoom-Kürzel und alle Bindungen ab; Laden und Undo bleiben unberührt.
- Sperre pro Spur ohne eigene Speicherung: Name enthält `(L)`/`[L]` (oder Schloss-Emoji) oder Kommentar enthält `#lock`.
  Wird so automatisch mit dem Projekt gespeichert.
- Bis 0.2.3: `built_against` = macOS 6.1.7. Ab 0.3.0: `built_against` = macOS 6.1.4 (siehe "Referenz 6.1.4").

### Stand
- [x] Untersuchung, Prototyp gebaut (`out/track-height-lock`), Regel-Tests grün.
- [x] Kontextmenü (0.2.0/0.2.1) gebaut und kompiliert.
- [x] 0.2.2-dev.1: " (L)" im Namen, gebaut, ZIP erstellt (altes 0.2.1-ZIP gelöscht).
- [x] 0.2.3-dev.1: Sperren/Entsperren ohne Undo, gebaut.
- [x] 0.3.0-dev.1: Referenz 6.1.4 (übersetzt), ZIP `out/track-height-lock-0.3.0-dev.1.zip` (0.2.3-ZIP gelöscht).
- [ ] Offline-Check (bewusst nicht ausgeführt, überschreibt BML-Dateien).
- [ ] Test in Bitwig (Patchbay > Mods > Add Mod… > `out/track-height-lock`, Framework "Actions and Menus" mit aktivieren).

### Offene Risiken
- Zoom-Kürzel: Widget wird zuerst vergrößert, Modell lehnt ab -> Zeile kann kurz falsch aussehen.
- Resize-Cursor erscheint weiter auf gesperrten Spuren.
- Automations-Lanes werden nicht gesperrt.
- " (L)" ist Teil des echten Namens: sichtbar im Mixer, in Export-/Stem-Dateinamen, für Controller-Skripte.
- Spuren ohne eigenen Titel: nach Entsperren gleicher Name sichtbar, aber fester Titel (kein Auto-Name mehr).
- Benennt der Nutzer eine gesperrte Spur um und löscht dabei "(L)", bleibt sie über `#lock` gesperrt.
- Ohne Undo (0.2.3): nicht in Bitwig getestet. Ein Cmd+Z auf einen ÄLTEREN Schritt, der Name/Kommentar betraf
  (z. B. Umbenennen vor dem Sperren), setzt den alten Wert -> " (L)" bzw. "#lock" kann verschwinden/zurückkommen.
  Sperre bleibt, solange eine Markierung übrig ist. Ob die Engine/Controller den neuen Namen sofort sehen, ist ungeprüft.
- Der dgT-Hook läuft bei jeder Textänderung in Bitwig (schneller Pfad: nur ein Thread-Vergleich).

### Ausbaustufe (Ideen)
1. (erledigt: Kontextmenü)
2. Am Widget `dYU` vertikales Resizing abschalten (`is_vert_resizing_enabled`), damit kein Cursor/kein Zieh-Griff erscheint.
3. Schloss-Symbol: vom Nutzer nicht gewünscht.
4. Option "alle Spuren sperren" über BML-Settings (`MOD.settings()`), Automations-Lanes mit sperren.
5. Gegen 6.1.4-Namen bauen, damit es auf allen Versionen läuft.

## Mod: track-spacer (Gegenstück zu REAPER "Insert spacer before/after tracks")

### Entscheidung (0.2.0-dev.1): reiner Abstand, KEINE Zusatzspur
- 0.1.0-dev.1 (leere Audiospur als Spacer) vom Nutzer abgelehnt -> komplett ersetzt.
- Jetzt: leere Lücke von N px über/unter einer Spur im Arranger (Spurkopf, Timeline, Launcher-Spalte im Arranger).
- Speicher: Tags im Spurkommentar `#spacer-before` / `#spacer-after`, optional `=N` (Standard 16 px, 1..400).
  Wird mit dem Projekt gespeichert; Menüaktionen sind je ein Undo-Schritt.
- Menü (menuOnly, `Menu.selection("track")`): "Insert Spacer Before Tracks" (erste Spur jedes Blocks bekommt
  `#spacer-before`), "Insert Spacer After Tracks" (letzte Spur bekommt `#spacer-after`), "Remove Spacer"
  (entfernt jede Lücke, die eine gewählte Spur berührt: eigene Tags, `after` der Spur darüber, `before` der Spur darunter).
  Schon vorhandene Lücke an der Stelle (Tag einer der beiden Nachbarn) -> nichts doppelt. Logik in `SpacerPlan`/`SpacerRule` (getestet).

### Erkenntnisse Layout (Bitwig 6.1.7, nur gelesen)
- Jede Arranger-Zeile einer (Nicht-Gruppen-)Spur ist ein `bvc` (extends `bvg` -> `bDw` ... -> `dvl`).
  Gebaut in `bvc.Gef(YCD, bvi, aZJ)`; `bvi` ist die Variante: Spurkopf (`bub`/`bui`/`bum` via `buT`),
  Timeline (`timeline.nSh` -> `uLT`/`IQP`, bvi `NlD`), Launcher-Slots im Arranger (`bcW`, "arranger_matrix_track_slots_widget"),
  Launcher-Steuerung (`bgI`, "track_launcher_controls_widget").
- Aufbau: senkrechter Stapel `dXL.sSO()` [ `dYU` (Höhe = `clip_lane_height`, Bindung `YCD.rcd()->ZhV.LYs()`), Automations-Container `dZi` ];
  letzter Schritt `this.beI(dAc)` (= `dvl.beI(dAc)`, setzt den Zeileninhalt).
- Einziger Leser von `ZhV.LYs()` (Spurhöhe fürs Layout) ist `bvc` -> Zeilen stapeln sich nur über Widget-Größen;
  Spurkopf/Timeline/Launcher bleiben synchron, solange alle `bvc` gleich behandelt werden.
- Abstandshalter: `dXL.sCA(long)` hängt einen festen `dZl` an (Einheit: logische Pixel, Bitwig nutzt z. B. 2/4),
  `dXL.Gef(min,max)` fügt oben ein. `dZl.sCA(J)` = min, `dZl.MgQ(J)` = max, Änderung -> `unz(J)` -> Neu-Layout.
  `dXL.needsGapBetweenWidgets` gibt false, wenn ein Nachbar `oSd()` (Spacer) ist -> 0-px-Spacer ändern nichts.
- Gruppen: `GJy` ist kein `YCD`; Gruppenkopf = `buH` (eigene Klasse, Höhe über `BPz.LYs()`). Master-Spur der Gruppe `wDt.smz()`.
- Kommentar beobachtbar: `rMD.GKF()` -> `Vyh` (neu je Aufruf, Attribut `kd.mEL()`), `addListener(NVD)`;
  `NVD.eventOccurred(Object)` (Name stabil). Setter-Kette `Heb` -> `iMb` -> `PJ3` -> `gNZ` feuert `wLf.fireValueChanged(GKF(),..)`.
- Aktivieren `rMD.ZRB(Z)` (6.1.4 `qoU`), Spur-Löschen `qxz.beI(List,CmH)` (in 6.1.7 privat), `CmH.Gef(Collection)` (öffentlich) –
  aus 0.1.0 bekannt, jetzt nicht mehr benutzt.

### Umsetzung
- Hook `track-row-spacers`: REPLACE_CALL in `bvc.Gef(LYCD;Lbvi;LaZJ;)V` auf `bvc.beI(LdAc;)V` (1. Aufruf).
  Bridge `TrackSpacerHooks.setRowContent(bvc, dAc)`: packt den Inhalt in `dXL.sSO()` [dZl oben, Inhalt, dZl unten], beide 0 px,
  Größe aus dem Kommentar; Register `WeakHashMap<sJU, List<WeakReference<dZl>[]>>`; je Spur ein Kommentar-Listener -> `refresh`.
  Fehler -> Bitwigs Original-Inhalt, einmal geloggt.
- Nur Spuren in `group.xDy()` (main_tracks), keine Gruppen-Master-Spur (`group.smz() == track`) -> sonst könnte Kopf/Timeline auseinanderlaufen.
  Gruppen-Spuren selbst werden im Menü übergangen.
- Alle `bvc`-Zeilen solcher Spuren bekommen den Stapel, auch ohne Tag (damit Tags später live wirken).
- 6.1.4-Namen: bvc=btR, Gef(YCD,bvi,aZJ)=MRY, beI(dAc)=cfM, dAc=dyC, YCD=iKU.

### Bau / Prüfung (0.2.0-dev.1)
- `sh tools/build-mod.sh track-spacer`: modkit finish hooks=1 (Ziel gegen echtes 6.1.7-Jar geprüft), Remap 0 Warnungen,
  Rundreise Code + Hooks OK, Namensprüfung verfügbar macOS 6.0, 6.1, 6.1.1, 6.1.2, 6.1.3, 6.1.4, 6.1.7.
- Bytecode geprüft: Aufrufe sind die gewollten Überladungen.
- `tools/make_uses.py` kennt keine F-Zeilen/Konstruktoren (für 0.2.0 nicht mehr relevant, nur Hinweis).
- Offline-Check (`loader/prepare-mod.sh`) NICHT ausgeführt -> Nutzer-Schritt (prüft u. a. Bytecode-Verifikation des Hooks).

### Stand
- [x] Machbarkeit geprüft, Lücken-Variante gebaut, Logik-Tests grün.
- [ ] Offline-Check (Rückfrage nötig).
- [ ] Test in Bitwig (Patchbay > Mods > Add Mod… > `out/track-spacer`, Framework "Actions and Menus" mit aktivieren).

### Offene Risiken (ungetestet in Bitwig)
- Ob alle Arranger-Listen wirklich nur über `bvc` laufen (z. B. Gruppen-Zeilen, Effekt-/Master-Bereich) – v. a. Ausrichtung Kopf/Timeline prüfen.
- Gruppen: keine Lücke über/unter dem Gruppenkopf (nur an Spuren darin). Ausbau: gleiches Muster an `buH` + Gruppen-Timeline-Zeile.
- Klick/Drop in die Lücke: gehört zur `bvc`-Zeile der Spur (Verhalten ungeprüft).
- Zusätzliche Box-Ebene in jeder Spurzeile (auch ohne Lücke): mögliche Nebenwirkungen auf Hintergrund/Hover/Drag.
- Comment-Listener-Thread: angenommen UI-Thread (wie alle Dokument-Änderungen aus Menüs).
- Lücken liegen im Kommentar (sichtbar im Inspector/Mixer-Kommentar).
