#!/usr/bin/env bash

source "$(dirname "${BASH_SOURCE[0]}")/extra_assertions.sh"

runTests() {
	source "${MDU_SHELLTEST_TESTUNIT_SHUNIT2_EXEC}" || exit $MDU_SHELLTEST_TEST_EXECUTION_ERROR_EXIT_CODE
	[ $__shunit_testsFailed -gt 0 ] && exit $MDU_SHELLTEST_TEST_ASSERTION_FAILURE_EXIT_CODE || exit 0
}


