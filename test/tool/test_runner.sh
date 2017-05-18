#!/usr/bin/env bash

pushd "$(dirname "${BASH_SOURCE[0]}")" > /dev/null
root_dir="$(pwd -P)/../.."
popd > /dev/null
src_root_dir="${root_dir}/src"
test_root_dir="${root_dir}/test"


runTests() {
	source "${test_root_dir}/shunit2-2.0.3/src/shell/shunit2" || exit 4
	[ $__shunit_testsFailed -gt 0 ] && exit 5 || exit 0
}


source "${ZZZ_tool_test}/extra_assertions.sh"
