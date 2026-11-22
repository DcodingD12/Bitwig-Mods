#!/bin/sh
# Moves a mod built by tools/build-mod.sh from the installed Bitwig's names (macOS 6.1.7)
# to the names of the reference build in build.json "translate_to" (e.g. macOS 6.1.4),
# so BML's map packs can translate it to the other Bitwig versions.
#
#   sh tools/translate-mod.sh <mod-folder>        (build-mod.sh calls it when build.json asks for it)
#
# Why: only the 6.1.7 jar is installed. The mod is compiled and checked (modkit finish) against the
# real 6.1.7 jar; then its names are translated with BML's own helper remapper and the INVERSE of
# BML's official remap <ref>__<installed>. Checks afterwards (all read-only for the BML folder):
#   1. round trip: translating back with the official remap gives the same code as the 6.1.7 build,
#   2. the same for the hook targets,
#   3. every name in uses.tsv + hooks has a counterpart in each installed map pack (like mod_uses.py check).
# Nothing is written into the BML or Bitwig folders.
set -eu

MOD_ID="${1:?usage: sh tools/translate-mod.sh <mod-folder>}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MOD="$ROOT/$MOD_ID"
WORK="$ROOT/build/$MOD_ID"
OUT="$ROOT/out/$MOD_ID"
BML="${BML:-/Applications/BML-0.36.0-dev.13-macos}"
BITWIG_JAR="/Applications/Bitwig Studio.app/Contents/Java/bitwig.jar"
JDK="${JDK:-/opt/homebrew/opt/openjdk@25}"
REMAPPER="$BML/loader/tools/bitwig-helper-remapper.jar"

# build.json: "translate_to": {"build": "macos-6.1.4", "installed": "macos-6.1.7"}
REF="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["translate_to"]["build"])' "$MOD/build.json")"
INST="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["translate_to"]["installed"])' "$MOD/build.json")"
OFFICIAL="$BML/maps/$INST-map1/remap/${REF}__${INST}.tsv.gz"
INVERSE="$ROOT/build/${INST}__${REF}.tsv.gz"
[ -f "$OFFICIAL" ] || { echo "TRANSLATE_ERROR: no remap $OFFICIAL" >&2; exit 1; }

# The inverse remap is made once per remap (it only depends on BML's map and the Bitwig jar).
if [ ! -f "$INVERSE" ]; then
  python3 -I "$ROOT/tools/invert_remap.py" "$OFFICIAL" "$BITWIG_JAR" "$INVERSE"
fi

T="$WORK/translate"
rm -rf "$T"; mkdir -p "$T"
cp "$OUT/helper.jar" "$T/helper-installed.jar"

# 1. helper.jar -> reference names (BML's own remapper; writes only into $T)
"$JDK/bin/java" -jar "$REMAPPER" "$T/helper-installed.jar" "$T/helper-ref.jar" "$INVERSE" "$T/none.tsv" "$T/report-to-ref.txt" "$MOD_ID" \
  | grep -E "classes_rewritten|warnings=" || true
grep -q "^warnings=0$" "$T/report-to-ref.txt" || { echo "TRANSLATE_ERROR: remapper warnings, see $T/report-to-ref.txt" >&2; exit 1; }

# 2. hook targets -> reference names, then the finished mod.json from the source mod.json
python3 -I "$ROOT/tools/translate_hooks.py" "$WORK/hooks.json" "$INVERSE" "$T/hooks-ref.json"
python3 "$ROOT/tools/finish_manifest.py" "$MOD/mod.json" "$T/hooks-ref.json" "$T/mod.json"

# 3. uses.tsv (BML reads it to decide on which versions the mod is available)
python3 -I "$ROOT/tools/make_uses.py" "$T/helper-ref.jar" "$OFFICIAL" "$BITWIG_JAR" "$T/uses.tsv"

# Check 1: round trip of the code
"$JDK/bin/java" -jar "$REMAPPER" "$T/helper-ref.jar" "$T/helper-back.jar" "$OFFICIAL" "$T/none.tsv" "$T/report-back.txt" "$MOD_ID" >/dev/null
mkdir -p "$T/a" "$T/b"
(cd "$T/a" && "$JDK/bin/jar" xf ../helper-installed.jar) && (cd "$T/b" && "$JDK/bin/jar" xf ../helper-back.jar)
for c in $(cd "$T/a" && find . -name '*.class'); do
  "$JDK/bin/javap" -c -p "$T/a/$c" | grep -v '^Classfile\|Last modified\|SHA-256\|MD5\|Compiled from' > "$T/code-a.txt"
  "$JDK/bin/javap" -c -p "$T/b/$c" | grep -v '^Classfile\|Last modified\|SHA-256\|MD5\|Compiled from' > "$T/code-b.txt"
  if ! cmp -s "$T/code-a.txt" "$T/code-b.txt"; then
    echo "TRANSLATE_ERROR: round trip differs for $c" >&2; exit 1
  fi
done
echo "ROUNDTRIP_CODE_OK"

# Check 2: round trip of the hook targets
python3 -I "$ROOT/tools/translate_hooks.py" "$T/hooks-ref.json" "$OFFICIAL" "$T/hooks-back.json" >/dev/null
python3 -c 'import json,sys; sys.exit(0 if json.load(open(sys.argv[1]))==json.load(open(sys.argv[2])) else 1)' "$WORK/hooks.json" "$T/hooks-back.json" \
  || { echo "TRANSLATE_ERROR: hook targets don't round-trip" >&2; exit 1; }
echo "ROUNDTRIP_HOOKS_OK"

# Check 3: availability per installed map pack (read only; a missing name means "not available there")
python3 -I "$ROOT/tools/check_uses.py" "$T/uses.tsv" "$T/hooks-ref.json" "$BML"/maps/*/remap/"${REF}"__*.tsv.gz | sed "s|$BML/maps/||" || true

# The release folder gets the reference-named files.
cp "$T/helper-ref.jar" "$OUT/helper.jar"
cp "$T/mod.json" "$OUT/mod.json"
cp "$T/uses.tsv" "$OUT/uses.tsv"
echo "TRANSLATE_OK $OUT (names: $REF)"
