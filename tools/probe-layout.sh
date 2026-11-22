#!/bin/sh
# Flow test of track-spacer's row box with Bitwig's real widget classes (read only, no UI, nothing is changed).
#
#   sh tools/probe-layout.sh
set -eu
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/track-spacer-probe"
JDK="${JDK:-/opt/homebrew/opt/openjdk@25}"
BW="/Applications/Bitwig Studio.app/Contents"
case "$(uname -m)" in
  arm64) JAVA="$BW/PlugIns/JavaVM-arm64.bundle/Contents/Home/bin/java" ;;
  *)     JAVA="$BW/PlugIns/JavaVM-x64.bundle/Contents/Home/bin/java" ;;
esac
rm -rf "$OUT"; mkdir -p "$OUT"
"$JDK/bin/javac" --release 21 -d "$OUT" "$ROOT/track-spacer/probe/LayoutProbe.java"
"$JAVA" -cp "$OUT:$BW/Java/bitwig.jar:$BW/Java/libs.jar" LayoutProbe
