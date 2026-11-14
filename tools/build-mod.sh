#!/bin/sh
# Builds one BML mod on macOS (steps 1-8 of BML's docs/sdk/building.md).
#
#   sh tools/build-mod.sh settings-scaling-drag
#
# Needs a JDK 21+ for javac (Bitwig's own Java has no compiler). Set JAVAC if it isn't on PATH:
#   JAVAC=/path/to/jdk/bin/javac sh tools/build-mod.sh settings-scaling-drag
# The installable folder ends up in out/<mod-id>/.
set -eu

MOD_ID="${1:?usage: sh tools/build-mod.sh <mod-folder>}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MOD="$ROOT/$MOD_ID"
WORK="$ROOT/build/$MOD_ID"
OUT="$ROOT/out/$MOD_ID"

BML="${BML:-/Applications/BML-0.36.0-dev.13-macos}"
KIT="$BML/modkit"
BITWIG_JAVA_DIR="/Applications/Bitwig Studio.app/Contents/Java"
BITWIG_JAR="$BITWIG_JAVA_DIR/bitwig.jar"
LIBS_JAR="$BITWIG_JAVA_DIR/libs.jar"
# Bitwig's bundled Java runs the mod kit (it needs Java 25, which Bitwig ships).
case "$(uname -m)" in
  arm64) JAVA="/Applications/Bitwig Studio.app/Contents/PlugIns/JavaVM-arm64.bundle/Contents/Home/bin/java" ;;
  *)     JAVA="/Applications/Bitwig Studio.app/Contents/PlugIns/JavaVM-x64.bundle/Contents/Home/bin/java" ;;
esac
JAVAC="${JAVAC:-javac}"
JAR_TOOL="${JAR_TOOL:-$(dirname "$(command -v "$JAVAC" 2>/dev/null || echo /missing/javac)")/jar}"

# javac on macOS without a JDK is a stub that only prints a hint, so test it for real.
if ! "$JAVAC" -version >/dev/null 2>&1; then
  echo "BUILD_ERROR: no working javac found. Install a JDK 21 or newer, or set JAVAC=/path/to/javac." >&2
  exit 1
fi

# The compile jar (Bitwig's classes, renamed to bitwig.obf.*, no method bodies) is made once.
COMPILE="$ROOT/build/bitwig-compile.jar"
mkdir -p "$ROOT/build"
if [ ! -f "$COMPILE" ]; then
  "$JAVA" -jar "$KIT/bml-modkit.jar" compile-jar "$BITWIG_JAR" "$COMPILE"
fi

# 1. Start clean.
rm -rf "$WORK" "$OUT"
mkdir -p "$WORK/classes" "$OUT"

# 4. Compile (this mod uses no framework, so no extra jars on the classpath).
find "$MOD/src" -name '*.java' > "$WORK/sources.txt"
"$JAVAC" --release 21 -proc:none -encoding UTF-8 -implicit:none \
  -sourcepath "$MOD/src" -cp "$COMPILE:$LIBS_JAR:$KIT/bml-api.jar" \
  -d "$WORK/classes" @"$WORK/sources.txt"

# 5. Finish: checks the @Hook methods against Bitwig's jar and writes hooks.json.
"$JAVA" -jar "$KIT/bml-modkit.jar" finish "$WORK/classes" "$WORK/hooks.json" "$BITWIG_JAR" "$LIBS_JAR"

# 6. helper.jar
"$JAR_TOOL" --create --file "$OUT/helper.jar" -C "$WORK/classes" .

# 7. Finished mod.json
python3 "$ROOT/tools/finish_manifest.py" "$MOD/mod.json" "$WORK/hooks.json" "$OUT/mod.json"

# 8. README and package files
cp "$MOD/README.md" "$OUT/"
if [ -d "$MOD/package" ]; then cp -R "$MOD/package/." "$OUT/"; fi

echo "BUILD_OK $OUT"
echo "Offline check (writes into BML's loader/mod-runtime, Bitwig itself is not changed):"
echo "  mkdir -p ~/check-mods && cp -R \"$OUT\" ~/check-mods/ && sh \"$BML/loader/prepare-mod.sh\" \"$BITWIG_JAVA_DIR\" ~/check-mods"
