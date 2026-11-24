#!/bin/sh
# Flow test of pinned-tracks' building blocks with Bitwig's real classes (read only, no UI, nothing is changed):
# visibility rules on a comment stand-in, the pinned area's layout, the divider colour,
# and that every arranger column (aGj subclass) is one the mod handles.
# Needs JDK 25 (ClassFile API) and runs on Bitwig's own Java 25. Build the mod first (sh tools/build-mod.sh pinned-tracks).
#
#   sh tools/probe-pinned.sh
set -eu
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/pinned-tracks-probe"
JDK="${JDK:-/opt/homebrew/opt/openjdk@25}"
BW="/Applications/Bitwig Studio.app/Contents"
case "$(uname -m)" in
  arm64) JAVA="$BW/PlugIns/JavaVM-arm64.bundle/Contents/Home/bin/java" ;;
  *)     JAVA="$BW/PlugIns/JavaVM-x64.bundle/Contents/Home/bin/java" ;;
esac
rm -rf "$OUT"; mkdir -p "$OUT"
"$JDK/bin/javac" --release 25 -cp "$BW/Java/bitwig.jar:$BW/Java/libs.jar" -d "$OUT" "$ROOT/pinned-tracks/probe/PinProbe.java"
"$JAVA" -cp "$OUT:$BW/Java/bitwig.jar:$BW/Java/libs.jar" PinProbe "$BW/Java/bitwig.jar" "$ROOT/build/pinned-tracks/classes/bitwigmods/pinnedtracks/PinnedTracksHooks.class"
