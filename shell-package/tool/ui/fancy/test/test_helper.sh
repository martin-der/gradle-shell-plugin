#!/usr/bin/env bash

__fail_count=0
__pass_count=0
__test_count=0

assert_eq() {
	local label="$1" expected="$2" actual="$3"
	__test_count=$((__test_count + 1))
	if [ "$expected" = "$actual" ]; then
		__pass_count=$((__pass_count + 1))
	else
		echo "FAIL: $label — expected [${#expected}]'$expected', got [${#actual}]'$actual'"
		__fail_count=$((__fail_count + 1))
	fi
}

assert_contains() {
	local label="$1" string="$2" substring="$3"
	__test_count=$((__test_count + 1))
	case "$string" in
		*"$substring"*)
			__pass_count=$((__pass_count + 1))
			;;
		*)
			echo "FAIL: $label — expected to contain '$substring'"
			__fail_count=$((__fail_count + 1))
			;;
	esac
}

assert_success() {
	local label="$1"
	shift
	__test_count=$((__test_count + 1))
	if "$@"; then
		__pass_count=$((__pass_count + 1))
	else
		echo "FAIL: $label — expected success, got exit code $?"
		__fail_count=$((__fail_count + 1))
	fi
}

assert_failure() {
	local label="$1"
	shift
	__test_count=$((__test_count + 1))
	if "$@"; then
		echo "FAIL: $label — expected failure, got success"
		__fail_count=$((__fail_count + 1))
	else
		__pass_count=$((__pass_count + 1))
	fi
}

report() {
	echo "---"
	echo "Passed: $__pass_count / $__test_count"
	if [ $__fail_count -gt 0 ]; then
		echo "FAILED: $__fail_count"
		exit 1
	fi
}
