#!/usr/bin/env bash
# Interactive tests using Python PTY
# Delegates to input_interactive_test.py

FANCY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PYTHON=${PYTHON:-python3}

if ! command -v $PYTHON >/dev/null 2>&1; then
    echo "SKIP: Python not available"
    exit 0
fi

exec $PYTHON "${SCRIPT_DIR}/input_interactive_test.py" "$FANCY_DIR" "$@"
