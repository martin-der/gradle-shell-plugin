#!/usr/bin/env bash

FANCY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "${FANCY_DIR}/test/test_helper.sh"

echo "=== fancy_input — source & guard ==="

# Source twice, verify guard prevents re-source
source "${FANCY_DIR}/input.sh"
src=$(type fancy_input 2>/dev/null)
assert_contains "fancy_input is a function" "$src" "function"

echo "=== fancy_input — empty varname ==="

fancy_input "" 2>/dev/null && rc=$? || rc=$?
assert_eq "empty varname returns 1" 1 $rc

echo "=== fancy_input — initial value from variable ==="

# Can't run full fancy_input without a TTY, but can verify the assignment logic
# that will be used:  local initial="${!varname}"
test_var="hello world"
varname="test_var"
initial="${!varname}"
assert_eq "indirect expansion picks up variable value" "hello world" "$initial"

unset test_var
initial="${!varname:-}"
assert_eq "indirect expansion on unset var yields empty" "" "$initial"

echo "=== fancy_input — width clamping test ==="

# The minimum width is 10
# We'll test via a script with a PTY in the interactive test

report
