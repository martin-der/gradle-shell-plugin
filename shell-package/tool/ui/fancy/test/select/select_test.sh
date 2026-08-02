#!/usr/bin/env bash

FANCY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "${FANCY_DIR}/test/test_helper.sh"

echo "=== fancy_select — source & guard ==="

source "${FANCY_DIR}/select.sh"
src=$(type fancy_select 2>/dev/null)
assert_contains "fancy_select is a function" "$src" "function"

echo "=== fancy_select — empty varnames ==="

fancy_select "" 2>/dev/null && rc=$? || rc=$?
assert_eq "empty choices varname returns 1" 1 $rc

fancy_select "choices" "" 2>/dev/null && rc=$? || rc=$?
assert_eq "empty result varname returns 1" 1 $rc

echo "=== fancy_select — parse choice ==="

out=$(__fancy_select_parse_choice "foo|I like foo")
assert_eq "parse value|label" "foo|I like foo" "$out"

out=$(__fancy_select_parse_choice "bar")
assert_eq "parse bare value defaults label" "bar|bar" "$out"

out=$(__fancy_select_parse_choice "baz|")
assert_eq "parse empty label defaults to value" "baz|baz" "$out"

out=$(__fancy_select_parse_choice "a|b|c")
assert_eq "parse keeps extra pipes in label" "a|b|c" "$out"

echo "=== fancy_select — truncate ==="

out=$(__fancy_select_truncate "I like foo" 5)
assert_eq "truncate shortens with indicator" "I li…" "$out"

out=$(__fancy_select_truncate "short" 10)
assert_eq "truncate keeps short label" "short" "$out"

out=$(__fancy_select_truncate "exact" 5)
assert_eq "truncate keeps exact length" "exact" "$out"

out=$(__fancy_select_truncate "longer" 1)
assert_eq "truncate min width 1" "…" "$out"

echo "=== fancy_select — choices parsing & initial value logic ==="

choices="foo|I like foo
bar|I want bar"
choices_var="choices"

parsed=""
n=0
while IFS= read -r line; do
	[ -z "$line" ] && continue
	parsed="$parsed$(__fancy_select_parse_choice "$line")
"
	n=$((n+1))
done <<< "${!choices_var}"
assert_eq "multiline choices parsed" 2 $n
assert_contains "first choice kept" "$parsed" "foo|I like foo"
assert_contains "second choice kept" "$parsed" "bar|I want bar"

# indirect expansion of the result variable used for the initial selection
result_var="result"
result="foo"
initial="${!result_var:-}"
assert_eq "indirect expansion picks up initial value" "foo" "$initial"

unset result
initial="${!result_var:-}"
assert_eq "indirect expansion on unset var yields empty" "" "$initial"

echo "=== fancy_select — no choices returns 1 ==="

empty_choices=""
fancy_select "empty_choices" "result" 2>/dev/null && rc=$? || rc=$?
assert_eq "no choices returns 1" 1 $rc

echo "=== fancy_select — choices as literal content ==="

result=""
out=$(fancy_select "foo|this is foo
bar|this is bar" result 30 </dev/null 2>&1); rc=$?
assert_eq "direct content exits without error" 1 $rc
assert_contains "direct content renders first label" "$out" "this is foo"
assert_contains "direct content renders second label" "$out" "this is bar"
assert_eq "direct content leaves result untouched" "" "$result"

out=$(fancy_select "justvalue" result 30 </dev/null 2>&1); rc=$?
assert_eq "bare value not a set varname is literal" 1 $rc
assert_contains "bare value rendered as label" "$out" "justvalue"

report
