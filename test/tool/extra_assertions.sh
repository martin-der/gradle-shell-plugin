#!/usr/bin/env bash

# @params message return_value
#    assert returned value is exacly `return_value`, print `message` otherwise
# @params return_value
#    assert returned value is exacly `return_value`, print a default message otherwise
# @params
#    assert returned value not 0
assertLastCommandFailed() {

	local last_result expected message
	last_result=$?
	message="Assert last command failed"
	expected=""

	if [ $# -gt 1 ] ; then
		expected="$2"
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
		assertNotSame "$message" 0 $last_result
		return $?
	else
		assertEquals "$message" "$expected" $last_result
		return $?
	fi
}

assertLastCommandSucceeded() {
	local last_result
	last_result=$?
	if [ $# -gt 0 ]; then
		assertEquals "$1" 0 $last_result
		return $?
	else
		assertEquals "Assert last command succeeded" 0 $last_result
		return $?
	fi
}