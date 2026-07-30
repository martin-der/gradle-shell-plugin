#!/usr/bin/env bash

test "x${MDU_UI:-}" = "xMDU-UI" && return 0
MDU_UI=MDU-UI

# @description Check whether the current terminal can handle a "fancy" UI
#              (ANSI escapes, raw key input, ...). When this returns non-zero,
#              callers should fall back to plain commands such as `read`.
#
# @exitcode 0 The terminal can handle fancy UI
# @exitcode 1 The terminal is "dumb" or not a terminal at all
mdu_ui_can_fancy() {
	[ -t 0 ] || return 1
	[ -t 1 ] || return 1
	[ -n "$TERM" ] || return 1
	[ "$TERM" = "dumb" ] && return 1
	return 0
}
