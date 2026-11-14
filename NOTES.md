# NOTES – Bitwig-Mods für Patchbay (BML)

## Ziel
Display-Skalierung in Bitwig (Settings > Display > Scaling) per Maus-Ziehen ändern, möglichst feiner als 25 %.

## Was ist Patchbay?
- "Patchbay Mod Loader" = BML (Bitwig Mod Loader), von "shaboogen", Tester-Version 0.36.0-dev.13.
- Installiert unter `/Applications/BML-0.36.0-dev.13-macos` (Start: `BML.command`).
- Patcht Bitwigs Java-Code beim Start (Bitwigs Dateien auf der Platte bleiben unverändert).
- Mod = Ordner mit `mod.json` + `helper.jar` (Java-Code mit `@Hook`-Methoden). Doku: `docs/sdk/README.md` im BML-Ordner.
- Einstellungen der Mods: `~/Library/Application Support/BML`.
- Mods-Releases: https://github.com/shaboogen-dev/Mods/releases

## Erkenntnisse (Bitwig 6.1.7 macOS, nur gelesen, nichts geändert)
- Gespeichert wird die Skalierung in `~/Library/Application Support/Bitwig/Bitwig Studio/prefs/6.1.7.prefs`
  (Bitwigs Binärformat, Werte als double, z. B. 1.0 / 1.75 / 2.0 / 2.25).
- Datenmodell `dSp` (Felder `custom_scaling_factor`, `should_use_automatic_scaling_factor`, ...).
  Jeder Setter rundet über `com.bitwig.base.geom.YEs`: `round(scale * 4)`, begrenzt auf 1..20 Viertel.
- Das Fenster (`dwe`) gibt die Skalierung als **ganze Zahl von Vierteln** an alle Widgets weiter
  (`dwF.ZeX()I`, Standard 4 = 100 %). Pixel-Umrechnung `YEs.beI(JI)J` / `YEs.Gef(JI)J` rechnet fest mit 4.
  => 25-%-Schritte sind Bitwigs internes Koordinatensystem, nicht nur eine UI-Grenze.
- Die Zeile "- 100% +" baut `dSt.Gef(LdSp;)V`. Der Prozentwert ist ein Zahlenfeld `dPn` (Min 0.25, Max 4.0).
  Bitwig schaltet dort `can_adjust_value_with_mouse` per `tWW(false)` absichtlich aus (und `should_show_bar` per `af(false)`).
- Namen 6.1.7 -> 6.1.4: `dSt`=`dQL`, `dPn`=`dNN`, `dPp`=`dNP`, `tWW(Z)V`=`LaE(Z)V`, `dSp`=`dQI`
  (aus `maps/macos-6.1.7-map1/remap/macos-6.1.4__macos-6.1.7.tsv.gz`).
- Controller-Script-API: kein Zugriff auf die Skalierung. Es gibt nur die Aktionen `increase_gui_scaling` / `decrease_gui_scaling` (je 25 %).

## Entscheidungen
- Prototyp = Variante A: ein `REPLACE_CALL`-Hook, der `tWW(false)` durch `tWW(true)` ersetzt -> Ziehen geht, aber in 25-%-Schritten.
- `built_against` = macOS 6.1.7 (Same-Build, laut BML-Doku erlaubt). Läuft nur auf genau diesem Bitwig-Build.
  Für alle Versionen müsste man gegen 6.1.4 bauen (dafür fehlt die 6.1.4-`bitwig.jar`).

## Stand
- [x] Mod-Quelle `settings-scaling-drag/` (mod.json, build.json, README, Hook-Klasse)
- [x] `tools/build-mod.sh` (Bauen auf macOS), `tools/finish_manifest.py` + Tests
- [ ] Bauen: braucht ein JDK 21+ (javac). Bitwigs Java reicht nur fürs Modkit, nicht zum Kompilieren.
- [ ] Offline-Check (`prepare-mod.sh`) und Test in Bitwig über Patchbay

## Nächste Schritte
1. JDK installieren (Nutzer fragen!), dann `sh tools/build-mod.sh settings-scaling-drag`.
2. Offline-Check, dann `out/settings-scaling-drag` in Patchbay hinzufügen und testen.
3. Danach entscheiden, ob Variante B (feinere Schritte durch Umbau von `YEs`) einen Versuch wert ist.

## Tests
`python3 -m unittest discover -s tools`
