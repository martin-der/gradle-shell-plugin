#!/usr/bin/env bash

# @params actual_return_value message expected_return_value
#    assert returned value is exactly `return_value`, print `message` otherwise
# @params actual_return_value expected_return_value
#    assert returned value is exactly `return_value`, print a default message otherwise
# @params actual_return_value
#    assert returned value not 0
__assertLastCommandFailed() {

	local last_result expected message
	last_result=$1
	shift
	message="Last command failed"
	expected=""

	if [ $# -gt 1 ] ; then
		if [ "$2" -eq "$2" ] 2>/dev/null ; then
			if [ "$2" -ne 0 ] ; then
				expected="$2"
			fi
		fi
		message="$1"
	else
		if [ $# -eq 1 ] ; then
			if [ "$1" -eq "$1" ] 2>/dev/null ; then
				if [ "$1" -ne 0 ] ; then
					expected="$1"
				fi
			fi
		fi
	fi

	if [ "x$expected" = "x" ] ; then
		assertTrue "$message expected a non 0 value as return" "[ 0 -ne $last_result ]"
	else
		assertTrue "$message expected $expected s return but was $last_result" "[ $expected -eq $last_result ]"
	fi
}
# @params actual_return_value message
#    assert returned value is 0, print `message` otherwise
# @params actual_return_value
#    assert returned value is 0, print a default message otherwise
__assertLastCommandSucceeded() {
	local last_result
	last_result=$1
	shift
	if [ $# -gt 0 ]; then
		assertTrue "$1 expected 0 as return" "[ 0 -eq $last_result ]"
	else
		assertTrue "expected 0 as return" "[ 0 -eq $last_result ]"
	fi
}