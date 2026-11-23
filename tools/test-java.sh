#!/bin/sh
# Compiles and runs a mod's plain-Java tests (no Bitwig classes, no test library).
#
#   sh tools/test-java.sh track-height-lock
#
# Tests live in <mod>/test/ and are classes with a main() whose name ends in "Test".
set -eu

MOD_ID="${1:?usage: sh tools/test-java.sh <mod-folder>}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MOD="$ROOT/$MOD_ID"
OUT="$ROOT/build/$MOD_ID-tests"
JDK="${JDK:-/opt/homebrew/opt/openjdk@25}"

rm -rf "$OUT"
mkdir -p "$OUT"
# Only the sources that don't need Bitwig: the tests and the classes they use (found via -sourcepath).
find "$MOD/test" -name '*Test.java' > "$OUT/sources.txt"
# javac's @file splits at spaces, so every path goes in quotes there.
sed 's/.*/"&"/' "$OUT/sources.txt" > "$OUT/javac-args.txt"
"$JDK/bin/javac" --release 21 -encoding UTF-8 -sourcepath "$MOD/src:$MOD/test" -d "$OUT" @"$OUT/javac-args.txt"

# One path per line (works with spaces in paths); java gets no stdin so it can't eat the list.
while IFS= read -r f; do
  cls="$(echo "${f#"$MOD/test/"}" | sed 's/\.java$//; s#/#.#g')"
  "$JDK/bin/java" -cp "$OUT" "$cls" </dev/null
done < "$OUT/sources.txt"
