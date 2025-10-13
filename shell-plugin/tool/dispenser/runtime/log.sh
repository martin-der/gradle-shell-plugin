#!/usr/bin/env bash
set -euo pipefail

NO_COLOR=0
HUMAN_MODE=1
MDU_LOG_STDOUT=

MDU_LOG_LEVEL_DEBUG=4
MDU_LOG_LEVEL_INFO=3
MDU_LOG_LEVEL_WARN=2
MDU_LOG_LEVEL_ERROR=1
MDU_LOG_LEVEL_NONE=0


MDU_DEFAULT_LOG_LEVEL=${MDU_LOG_LEVEL_WARN}

[ ${NO_COLOR} -ne 0 ] && {
	GREEN=""
	GREEN_BOLD=""
	YELLOW=""
	YELLOW_BOLD=""
	RED=""
	RED_BOLD=""
	BLUE=""
	BLUE_BOLD=""
	#WHITE=""
	GRAY_LIGHT=""
	GRAY_DARK=""
	COLOR_RESET=""
	FONT_STYLE_BOLD=""
	FONT_STYLE_ITALIC=""
	FONT_STYLE_UNDERLINE=""
	FONT_STYLE_STRIKE=""
} || {
	GREEN="\033[0;32m"
	GREEN_BOLD="\033[1;32m"
	YELLOW="\033[0;33m"
	YELLOW_BOLD="\033[1;33m"
	RED="\033[0;31m"
	RED_BOLD="\033[1;31m"
	BLUE="\033[0;34m"
	BLUE_BOLD="\033[1;34m"
	#WHITE="\033[1;37m\]"
	GRAY_LIGHT="\033[0;37m"
	GRAY_DARK='\033[1;30m'
	COLOR_RESET="\033[0;0m"
	FONT_STYLE_BOLD="\033[1m"
	FONT_STYLE_ITALIC="\033[3m"
	FONT_STYLE_UNDERLINE="\033[4m"
	FONT_STYLE_STRIKE="\033[9m"
}



MDU_ICON_WARN="${YELLOW_BOLD}/!\\\\${COLOR_RESET}"
MDU_ICON_ERROR="${RED_BOLD}/!\\\\${COLOR_RESET}"
MDU_ICON_INFO="${BLUE_BOLD}(*)${COLOR_RESET}"
MDU_ICON_DEBUG="${GREEN_BOLD}[#]${COLOR_RESET}"

function _getLogLevel() {
	local level=${MDU_LOG_LEVEL:-}
	[ "x$level" = xDEBUG -o "x$level" = xdebug ] && {
	    echo ${MDU_LOG_LEVEL_DEBUG}
	    return
	  }
	[ "x$level" = xINFO -o "x$level" = xinfo ] && {
	    echo ${MDU_LOG_LEVEL_INFO}
	    return
	  }
	[ "x$level" = xWARN -o "x$level" = xwarn ] && {
	    echo ${MDU_LOG_LEVEL_WARN}
	    return
	  }
	[ "x$level" = xERROR -o "x$level" = xerror ] && {
	    echo ${MDU_LOG_LEVEL_ERROR}
	    return
	  }
	[ "x$level" = xNONE -o "x$level" = xnone ] && {
	    echo ${MDU_LOG_LEVEL_NONE}
	    return
	  }
	echo ${MDU_DEFAULT_LOG_LEVEL}
}

log_convertVerbosityToLevel() {
  local verbose=$1
  if [ ${verbose} -eq 1 ]; then
    LOG_LEVEL=info
  elif [ ${verbose} -ge 2 ]; then
    LOG_LEVEL=debug
  fi
}


# print script prefix for 'non human' output
# Param 1 : String severity
function echo_script_prefix() {
	echo -n "[$1] "
	local tag="${LOG_TAG:-}"
	[ "x$tag" != "x" ] && echo -n "$tag "
}

function log_debug() {
	local level=$(_getLogLevel)
	test ${MDU_LOG_LEVEL_DEBUG} -gt ${level} && return 0
	local human_mode=${HUMAN_MODE}
	local output
	[ "x${MDU_LOG_STDOUT:-}" != x ] && {
		output="$MDU_LOG_STDOUT"
		if [ ${human_mode} -eq 0 ] ; then
			echo_script_prefix DEBUG
		else
			echo -e -n "$MDU_ICON_DEBUG${LOG_USER_PREFIX:-} "
		fi >> "$output"
		echo -e "$@" >> "$output"
	} || {
		if [ ${human_mode} -eq 0 ] ; then
			echo_script_prefix DEBUG
		else
			echo -e -n "$MDU_ICON_DEBUG${LOG_USER_PREFIX:-} "
		fi
		echo -e "$@"
	}
}

function log_info() {
	local level=$(_getLogLevel)
	test ${MDU_LOG_LEVEL_INFO} -gt ${level} && return 0
	local human_mode=${HUMAN_MODE}
	local output
	[ "x${MDU_LOG_STDOUT:-}" != x ] && {
		output="$MDU_LOG_STDOUT"
		if [ ${human_mode} -eq 0 ] ; then
			echo_script_prefix INFO
		else
			echo -e -n "$MDU_ICON_INFO${LOG_USER_PREFIX:-} "
		fi >> "$output"
		echo -e "$@" >> "$output"
	} || {
		if [ ${human_mode} -eq 0 ] ; then
			echo_script_prefix INFO
		else
			echo -e -n "$MDU_ICON_INFO${LOG_USER_PREFIX:-} "
		fi
		echo -e "$@"
	}
}

function log_warn() {
  local level=$(_getLogLevel)
	test ${MDU_LOG_LEVEL_WARN} -gt ${level} && return 0
	local human_mode=${HUMAN_MODE}
	local output
	[ "x${LOG_STDERR:-}" != x ] && {
		output="$LOG_STDERR"
		if [ ${human_mode} -eq 0 ] ; then
			echo_script_prefix WARN
		else
			echo -e -n "$MDU_ICON_WARN${LOG_USER_PREFIX:-} "
		fi >> "$output"
		echo -e "$@" >> "$output"
	} || {
		if [ ${human_mode} -eq 0 ] ; then
			echo_script_prefix WARN
		else
			echo -e -n "$MDU_ICON_WARN${LOG_USER_PREFIX:-} "
		fi >&2
		echo -e "$@" >&2
	}
}

function log_error()  {
	local level=$(_getLogLevel)
	test ${MDU_LOG_LEVEL_ERROR} -gt ${level} && return 0
	local human_mode=${HUMAN_MODE}
	local output
	[ "x${LOG_STDERR:-}" != x ] && {
		output="$LOG_STDERR"
		if [ ${human_mode} -eq 0 ] ; then
			echo_script_prefix ERROR
		else
			echo -e -n "$MDU_ICON_ERROR${LOG_USER_PREFIX:-} "
		fi >> "$output"
		echo -e "$@" >> "$output"
	} || {
		if [ ${human_mode} -eq 0 ] ; then
			echo_script_prefix ERROR
		else
			echo -e -n "$MDU_ICON_ERROR${LOG_USER_PREFIX:-} "
		fi >&2
		echo -e "$@" >&2
	}
	#output="$LOG_STDERR" || output="/proc/$$/fd/2"
}


log_input_with_prefix() {
  local wt=$(/usr/bin/tput cols)
  local prefix="${1}"
  #local raw_prefix="$(echo -e "${prefix}" | sed 's/\x1B\[[0-9;]\{1,\}[A-Za-z]//g')"
  local raw_prefix="$(echo -e "${prefix}" |  sed 's/\x1B[@A-Z\\\]^_]\|\x1B\[[0-9:;<=>?]*[-!"#$%&'"'"'()*+,.\/]*[][\\@A-Z^_`a-z{|}~]//g')"
  #local raw_prefix="$(echo -e "${prefix}" | ansi2txt)"
  #raw_prefix="  | "
  local w=$(( $wt - 1 - ${#raw_prefix}))
  fold -w${w} | while IFS="\n" read l; do
      local s="${l}"
      echo -e -n "${prefix}"
      echo "${s}"
  done
}
