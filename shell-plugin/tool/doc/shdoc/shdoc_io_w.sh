#!/bin/sh

set -u

GAWK_COMMAND=gawk
test "x${3:-}" != x && GAWK_COMMAND="$3"

SHDOC="$(dirname "$0")/shdoc"

FILENAME="$(basename "$1")"

cat "$1" | "${GAWK_COMMAND}" -f "$SHDOC" -v script_name="$FILENAME" > "$2"