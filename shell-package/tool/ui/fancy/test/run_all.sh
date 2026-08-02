#!/usr/bin/env bash
set -e

FANCY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$FANCY_DIR"

echo "========================================"
echo "  fancy.sh — core library tests"
echo "========================================"
bash test/fancy_core_test.sh
CORE_RC=$?
echo ""

echo "========================================"
echo "  input.sh — unit tests"
echo "========================================"
bash test/input/input_test.sh
INPUT_RC=$?
echo ""

echo "========================================"
echo "  input.sh — interactive tests"
echo "========================================"
bash test/input/input_interactive_test.sh
PTY_RC=$?
echo ""

echo "========================================"
echo "  checkbox.sh — unit tests"
echo "========================================"
bash test/checkbox/checkbox_test.sh
CHECKBOX_RC=$?
echo ""

echo "========================================"
echo "  checkbox.sh — interactive tests"
echo "========================================"
bash test/checkbox/checkbox_interactive_test.sh
CHECKBOX_PTY_RC=$?
echo ""

echo "========================================"
echo "  select.sh — unit tests"
echo "========================================"
bash test/select/select_test.sh
SELECT_RC=$?
echo ""

echo "========================================"
echo "  select.sh — interactive tests"
echo "========================================"
bash test/select/select_interactive_test.sh
SELECT_PTY_RC=$?
echo ""

echo "========================================"
echo "  demo.sh — chained components"
echo "========================================"
bash test/demo/demo_interactive_test.sh
DEMO_RC=$?
echo ""

echo "========================================"
echo "  Common behavior — cursor position"
echo "========================================"
bash test/cursor_position_test.sh
CURSOR_RC=$?
echo ""

echo "========================================"
echo "  Summary"
echo "========================================"
echo "  Core tests:            $([ $CORE_RC -eq 0 ] && echo PASS || echo FAIL)"
echo "  Input unit tests:      $([ $INPUT_RC -eq 0 ] && echo PASS || echo FAIL)"
echo "  Input PTY tests:       $([ $PTY_RC -eq 0 ] && echo PASS || echo FAIL)"
echo "  Checkbox unit tests:   $([ $CHECKBOX_RC -eq 0 ] && echo PASS || echo FAIL)"
echo "  Checkbox PTY tests:    $([ $CHECKBOX_PTY_RC -eq 0 ] && echo PASS || echo FAIL)"
echo "  Select unit tests:     $([ $SELECT_RC -eq 0 ] && echo PASS || echo FAIL)"
echo "  Select PTY tests:      $([ $SELECT_PTY_RC -eq 0 ] && echo PASS || echo FAIL)"
echo "  Demo PTY tests:        $([ $DEMO_RC -eq 0 ] && echo PASS || echo FAIL)"
echo "  Cursor position tests: $([ $CURSOR_RC -eq 0 ] && echo PASS || echo FAIL)"

[ $CORE_RC -eq 0 ] && [ $INPUT_RC -eq 0 ] && [ $PTY_RC -eq 0 ] \
&& [ $CHECKBOX_RC -eq 0 ] && [ $CHECKBOX_PTY_RC -eq 0 ] \
&& [ $SELECT_RC -eq 0 ] && [ $SELECT_PTY_RC -eq 0 ] \
&& [ $DEMO_RC -eq 0 ] && [ $CURSOR_RC -eq 0 ]
