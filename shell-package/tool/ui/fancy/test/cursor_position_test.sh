#!/usr/bin/env bash
# Regression test using Python PTY (simulated terminal cursor response)
# Delegates to cursor_position_test.py

FANCY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PYTHON=${PYTHON:-python3}

if ! command -v $PYTHON >/dev/null 2>&1; then
    echo "SKIP: Python not available"
    exit 0
fi

exec $PYTHON "${SCRIPT_DIR}/cursor_position_test.py" "$FANCY_DIR" "$@"
