#!/usr/bin/env bash
# Snapshot rendering tests using Python PTY (screen-grid emulator)
# Delegates to snapshot_test.py

FANCY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PYTHON=${PYTHON:-python3}

if ! command -v $PYTHON >/dev/null 2>&1; then
    echo "SKIP: Python not available"
    exit 0
fi

exec $PYTHON "${SCRIPT_DIR}/snapshot_test.py" "$FANCY_DIR" "$@"
