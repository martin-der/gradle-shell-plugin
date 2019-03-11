#!/bin/sh

set -u

SHDOC="$(dirname "$0")/shdoc"

FILENAME="$(basename "$1")"

cat "$1" | "$SHDOC" -v script_name="$FILENAME" > "$2"