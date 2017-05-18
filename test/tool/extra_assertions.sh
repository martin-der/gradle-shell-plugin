#!/usr/bin/env bash


assertLastCommandFailed() {
	local last_result
	last_result=$?
	[ $# -gt 1 ] && {
		assertNotSame "$1" "$2" $last_result
		return $?
	} || {
		assertNotSame "Assert Failed" 0 $last_result
		return $?
	}
}
assertLastCommandSucceeded() {
	local last_result
	last_result=$?
	[ $# -gt 0 ] && {
		assertEquals "$1" 0 $last_result
		return $?
	} || {
		assertEquals 0 $last_result
		return $?
	}
}