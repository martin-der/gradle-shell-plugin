#!/usr/bin/env bash


test "x${MDU_SHELL_UTIL:-}" = "xMDU-SHELL-UTIL" && return 0
MDU_SHELL_UTIL=MDU-SHELL-UTIL

LOG_LEVEL_DEBUG=4
LOG_LEVEL_INFO=3
LOG_LEVEL_WARN=2
LOG_LEVEL_ERROR=1
LOG_LEVEL_NONE=0


DEFAULT_LOG_LEVEL=${LOG_LEVEL_WARN}



MDU_HUMAN_MODE=${MDU_HUMAN_MODE:-1}
MDU_NO_COLOR=${MDU_NO_COLOR:-0}


[ ${MDU_NO_COLOR} -ne 0 ] && {
	GREEN=""
	GREEN_BOLD=""
	YELLOW=""
	YELLOW_BOLD=""
	RED=""
	RED_BOLD=""
	BLUE=""
	BLUE_BOLD=""
	#WHITE=""
	#GRAY_LIGHT=""
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
	#GRAY_LIGHT="\033[0;37m"
	COLOR_RESET="\033[0;0m"
	FONT_STYLE_BOLD="\033[1m"
	FONT_STYLE_ITALIC="\033[3m"
	FONT_STYLE_UNDERLINE="\033[4m"
	FONT_STYLE_STRIKE="\033[9m"
}




# ---------------------- #
#                        #
#        Include         #
#                        #
# ---------------------- #

_mdu_loaded_scripts=( "." )

#
# Load source
#	param 1/0 : once
#	param <name> : script name
#	return same as @see load_source
#
function _mdu_source_if_exists() {
	[ -r "$2" ] && {
		local linked="$(readlink -f "$2")"
		log_debug " Found '$2' in '$linked' (once:$1)"
		[ $1 -ne 0 ] && {
			log_debug "  Sourced ? (${_mdu_loaded_scripts[*]})"
			for loaded in "${_mdu_loaded_scripts[@]}"; do
				[ "$loaded" == "$linked" ] && return 0;
			done
		}
		#log_debug "  Sourcing '$2'"
		_mdu_loaded_scripts+=("$linked")
		#log_debug "   \\-> Sourced : (${_mdu_loaded_scripts[*]})"
		source "$2"
		local result=$?
		[ ${result} -eq 0 ] && return 0
		log_error "Error sourcing '$2' : returned $?"
		[ ${result} -eq 254 ] && return 253
		return ${result}
	}
	return 254
}


#
# Load source
#	param 1/0 : [O]nce
#	param <name>
#	param ... : [W]ith [S]uffixes
#
function _mdu_load_source_O_WS() {
	local result only_once script suffixed
	only_once=$1
	shift
	script=$1
	shift
	log_debug "Try '$script'..."
	_mdu_source_if_exists ${only_once} "$script"
	result=$?
	[ $result -ne 254 ] && return $result

	for suffix in "$@" ; do
		suffixed="${script}.${suffix}"
		log_debug "Try suffixed '$suffixed'..."
		_mdu_source_if_exists ${only_once} "$suffixed"
		result=$?
		[ $result -ne 254 ] && {
			log_debug "  '$suffixed' returned $result"
			return $result
		}
	done
	return 254
}

function _mdu_load_source() {

	local only_once around_linked around_callers
	only_once=1
	around_linked=1
	around_callers=0
	local o="${MDU_SOURCE_OPTIONS:-1lC}$1"
	for (( i=0; i<${#o}; i++ )); do
		local ov="${o:$i:1}"
		[ ${ov} == 1 ] && { only_once=1 ; continue ; }
		[ ${ov} == n ] && { only_once=0 ; continue ; }
		[ ${ov} == c ] && { around_callers=1 ; continue ; }
		[ ${ov} == C ] && { around_callers=0 ; continue ; }
		[ ${ov} == l ] && { around_linked=1 ; continue ; }
		[ ${ov} == L ] && { around_linked=0 ; continue ; }
	done
	shift

	local sourced_file request parent
	request="$1"
	shift
	[ "x${request:0:1}" != "x/" ] && {
		[ ${around_linked}  -ne 0 ] && {
			local parent="${BASH_SOURCE[2]}"
			while [ -h "$parent" ] ; do
				sourced_file="$(dirname "$(readlink -m "${parent}")")/$request"
				_mdu_load_source_O_WS ${only_once} "$sourced_file" $@
				result=$?
				[ ${result} -ne 254 ] && return ${result}
				#log_debug "'$request' not found as \"near link\" '$sourced_file' ( parent '$parent' => '$(readlink "${parent}")' )"
				parent="$sourced_file"
			done
		}

		sourced_file="$(dirname "${BASH_SOURCE[2]}")/$request"
		_mdu_load_source_O_WS ${only_once} "$sourced_file" $@
		result=$?
		[ ${result} -ne 254 ] && return ${result}
		#log_debug "'$request' not found as '$sourced_file'"

	}
	_mdu_load_source_O_WS ${only_once} "$request" $@
	result=$?
	[ ${result} -eq 0 ] && return 0
	[ ${result} -eq 254 ] &&  {
		#log_debug "'$request' not found directly as '$request'"
		log_error "Error sourcing '$request' : not found"
		return 254
	}
	return ${result}
}

# @description Source a `script`
#
# @example
#   load_source my_script sh py
#
# @param $1 string the `script` to source
# @param $@ string extra `suffixes` to try loading against
#
# @exitcode 254 If script was not found or couldn't be read
# @exitcode 0 If script was sourced without error
# @exitcode [1-253] If script returned an error ( if error code was 254, then 253 is returned instead )
function load_source() {
	_mdu_load_source "" $@
}
# @description Source a `script`, but do it only once
#
# @see load_source
function load_source_once() {
	_mdu_load_source 1 $@
}


# ---------------------- #
#                        #
#        Logging         #
#                        #
# ---------------------- #

# From MAN pages :
# This  variable  can be used with BASH_LINENO and BASH_SOURCE.
# Each element of FUNCNAME has corresponding elements in BASH_LINENO and BASH_SOURCE to describe the call stack.
# For instance, ${FUNCNAME[$i]} was called from the file ${BASH_SOURCE[$i+1]} at line number ${BASH_LINENO[$i]}.
# The caller builtin displays the current call stack using this information.

ICON_WARN="${YELLOW_BOLD}/!\\\\${COLOR_RESET}"
ICON_ERROR="${RED_BOLD}/!\\\\${COLOR_RESET}"
ICON_INFO="${BLUE_BOLD}(*)${COLOR_RESET}"
ICON_DEBUG="${GREEN_BOLD}[#]${COLOR_RESET}"

function _mdu_getLogLevel {
	local level=${MDU_LOG_LEVEL:-${LOG_LEVEL:-default}}
	[ "x$level" = xDEBUG -o "x$level" = xdebug ] && return ${LOG_LEVEL_DEBUG}
	[ "x$level" = xINFO -o "x$level" = xinfo ] && return ${LOG_LEVEL_INFO}
	[ "x$level" = xWARN -o "x$level" = xwarn ] && return ${LOG_LEVEL_WARN}
	[ "x$level" = xERROR -o "x$level" = xerror ] && return ${LOG_LEVEL_ERROR}
	[ "x$level" = xNONE -o "x$level" = xnone ] && return ${LOG_LEVEL_NONE}
	return ${DEFAULT_LOG_LEVEL}
}

# print script prefix for 'non human' output
# Param 1 : String severity
function echo_script_prefix() {
	echo -n "[$1] "
	local tag="${MDU_LOG_TAG:-}"
	[ "x$tag" != "x" ] && echo -n "$tag "
}

function log_debug() {
	_mdu_getLogLevel
	local level=$?
	test ${LOG_LEVEL_DEBUG} -gt ${level} && return 0
	local human_mode=${MDU_HUMAN_MODE}
	local output
	[ "x${MDU_LOG_STDOUT:-}" != x ] && {
		output="$MDU_LOG_STDOUT"
		if [ ${human_mode} -eq 0 ] ; then
			echo_script_prefix DEBUG
		else
			echo -e -n "$ICON_DEBUG "
		fi >> "$output"
		echo -e "$@" >> "$output"
	} || {
		if [ ${human_mode} -eq 0 ] ; then
			echo_script_prefix DEBUG
		else
			echo -e -n "$ICON_DEBUG "
		fi
		echo -e "$@"
	}
}

function log_info() {
	_mdu_getLogLevel
	local level=$?
	test ${LOG_LEVEL_INFO} -gt ${level} && return 0
	local human_mode=${MDU_HUMAN_MODE}
	local output
	[ "x${MDU_LOG_STDOUT:-}" != x ] && {
		output="$MDU_LOG_STDOUT"
		if [ ${human_mode} -eq 0 ] ; then
			echo_script_prefix INFO
		else
			echo -e -n "$ICON_INFO "
		fi >> "$output"
		echo -e "$@" >> "$output"
	} || {
		if [ ${human_mode} -eq 0 ] ; then
			echo_script_prefix INFO
		else
			echo -e -n "$ICON_INFO "
		fi
		echo -e "$@"
	}
}

function log_warn() {
	_mdu_getLogLevel
	local level=$?
	test ${LOG_LEVEL_WARN} -gt ${level} && return 0
	local human_mode=${MDU_HUMAN_MODE}
	local output
	[ "x${MDU_LOG_STDERR:-}" != x ] && {
		output="$MDU_LOG_STDERR"
		if [ ${human_mode} -eq 0 ] ; then
			echo_script_prefix WARN
		else
			echo -e -n "$ICON_WARN "
		fi >> "$output"
		echo -e "$@" >> "$output"
	} || {
		if [ ${human_mode} -eq 0 ] ; then
			echo_script_prefix WARN
		else
			echo -e -n "$ICON_WARN "
		fi >&2
		echo -e "$@" >&2
	}
}

function log_error()  {
	_mdu_getLogLevel
	local level=$?
	test ${LOG_LEVEL_ERROR} -gt ${level} && return 0
	local human_mode=${MDU_HUMAN_MODE}
	local output
	[ "x${MDU_LOG_STDERR:-}" != x ] && {
		output="$MDU_LOG_STDERR"
		if [ ${human_mode} -eq 0 ] ; then
			echo_script_prefix ERROR
		else
			echo -e -n "$ICON_ERROR "
		fi >> "$output"
		echo -e "$@" >> "$output"
	} || {
		if [ ${human_mode} -eq 0 ] ; then
			echo_script_prefix ERROR
		else
			echo -e -n "$ICON_ERROR "
		fi >&2
		echo -e "$@" >&2
	}
	#output="$MDU_LOG_STDERR" || output="/proc/$$/fd/2"
}

# ---------------------- #
#                        #
#   Script Attributes    #
#                        #
# ---------------------- #

# @description Extract attributes line from a shell `script`
#
# @example
#   hasScriptAttribute my_script.sh
#
# @arg $1 string path to the `script`
#
# @exitcode 1 If script could not be read
# @exitcode 2 'attributes' line was invalid ( ex. does start with # )
# @exitcode 0 if successful
extract_script_attributes_line() {
	sed -e '2q' -e '2d' -e '/^#!\/.\+/d' "$1"
}

# @description Extract attributes line from a shell script
#
# @example
#   hasScriptAttribute somewhere/my_script.sh foobar
#
# @arg $1 string path to the `script`
# @arg $2 string `attribute` of which the presence must be checked
#
# @exitcode 1 If `script` could not be read
# @exitcode 2 _attributes line_ was invalid ( ex. does start with # )
# @exitcode 3 `script` does not have the requested `attribute`
# @exitcode 0 the `attribute` exists
#
# @see extract_script_attributes_line
has_script_attribute() {
	local script="$1" attribute="$2"
	local attributesLine
	attribute="$(escaped_for_regex "$attribute")"
	attributesLine="$(extract_script_attributes_line "$script")" || return $?
	[[ "${attributesLine}" =~ ^#([\ \t]*|.*[\ \t]+)@${attribute}([\ \t]*|[\ \t]+.*)$ ]] || return 3
	return 0
}



# ---------------------- #
#                        #
#      String util       #
#                        #
# ---------------------- #

# @description Get the value of a decoration key
#
# @arg $1 string key
#
# @stdout value of the decoration
#
# @exitcode 0 If a decoration with this name existed
# @exitcode 1 If no decoration with this name existed
mdu_getTextDecoration() {
	local prefix="_mdu_text_decoration__"
	local i="${prefix}$1"
	[ -z ${!i+x} ] && return 1 || {
		echo -n -e "${!i-}"
		return 0
	}
	return 1
}
# @description Set the value of a decoration
#
# @arg $1 string key
# @arg $2 string value
#
# @exitcode 0
mdu_setTextDecoration() {
	local prefix="_mdu_text_decoration__"
	local i="${prefix}$1"
	#read -d"\0" "$i" <<<"$2"
	read -r "$i" <<<"$2"
}
# @description Check if a decoration exists
#
# @arg $1 string key
#
# @exitcode 0 if a decoration for this key  exists
# @exitcode !0 if no decoration for this key  exists
mdu_isSetTextDecoration() {
	local prefix="_mdu_text_decoration__"
	local i="${prefix}$1"
	[ -z ${!i+x} ] && return 1 || return 0
}
# @description Remove a decoration
#
# @arg $1 string key
#
# @exitcode 0
mdu_unsetTextDecoration() {
	local prefix="_mdu_text_decoration__"
	local i="${prefix}$1"
	unset "$i"
}


function _decorationBiggestGroup() {
	sed "s#^\([^{]*\){\([a-zA-Z\-]*\){\(.*\)}}\([^}]*\)\$#\\$2#" <<< "$1"
}
function _decorationFirstGroup() {
	sed "s#^\([^{]*\){\([a-zA-Z\-]*\){\([^{]*\)}}\(.*\)\$#\\$2#" <<< "$1"
}
function _echoDecorationValue() {
	mdu_isSetTextDecoration "$1" && {
		mdu_getTextDecoration "$1"
		return 0
	}
	case "$1" in
		"red") echo -e -n "$RED" ;;
		"green") echo -e -n "$GREEN" ;;
		"blue") echo -e -n "$BLUE" ;;
		"yellow") echo -e -n "$YELLOW" ;;
		"bold") echo -e -n "$FONT_STYLE_BOLD" ;;
		"italic") echo -e -n "$FONT_STYLE_ITALIC" ;;
		"strike") echo -e -n "$FONT_STYLE_STRIKE" ;;
		"underline") echo -e -n "$FONT_STYLE_UNDERLINE" ;;
		*) return 1 ;;
	esac
	return 0
}
function decorate()  {
	decorate_n "$1" "${2:-}"
	echo
}
function decorate_n()  {
	#read background status : echo -e "\e]11;?\a"
	local text="$1"
	local previousDecoration="$2"

	[ "x$2" != "x" ] && previousDecoration="$2" || previousDecoration="$COLOR_RESET"

	local pre key content post

	pre=$(_decorationFirstGroup "$text" 1)
	key=$(_decorationFirstGroup "$text" 2)
	content=$(_decorationFirstGroup "$text" 3)
	post=$(_decorationFirstGroup "$text" 4)

	[ "x$key" != "x$text" ] && {
		#log_debug "decorationFirstGroup '$pre|$key|$content|$post'"
		echo -n "$pre"
		local decoration=$(_echoDecorationValue "$key")
		echo -e -n "$decoration"
		echo -e -n "$content"
		echo -e -n "$previousDecoration"
		decorate_n "$post" "$previousDecoration"
		return
	}

	pre=$(_decorationBiggestGroup "$text" 1)
	key=$(_decorationBiggestGroup "$text" 2)
	content=$(_decorationBiggestGroup "$text" 3)
	post=$(_decorationBiggestGroup "$text" 4)

	[ "x$key" != "x$text" ] && {
		#log_debug "decorationBiggestGroup '$pre|$key|$content|$post'"
		echo -n "$pre"
		local decoration=$(_echoDecorationValue "$key")
		echo -e -n "$decoration"
		decorate_n "$content" "$decoration"
		echo -e -n "$previousDecoration"
		echo -n "$post"
		return
	}

	echo -n "$text"
}

function escaped_for_regex() {
	sed -e 's/[]\/$*.^|[]/\\&/g' <<< "$1"
}


# @description Check if a `line` is a comment with '#'
#
# @arg $1 string `line`
#
# @exitcode 0 If the line is a comment
# @exitcode !0 If the line is not a comment
function line_isComment_withSharp() {
	grep -Eq '^[ 	]*#' <<< "$1" && return 0 || return 1
}
# @description Check if a `line` is empty
#
# @arg $1 string `line`
#
# @exitcode 0 If the line is empty
# @exitcode !0 If the line contains any other character than ' '
function line_isEmpty() {
	grep -Eq '^[ 	]*$' <<< "$1" && return 0 || return 1
}

# properties

function _mdu_properties_value_from_line() {
	local line="$1"
	local key="$2"
	line_isComment_withSharp "$line" && return 1
	line_isEmpty "$line" && return 1
	local KEY_FOUND="$(line_KeyValue_getKey "$line")"
	[ "x$key" == "x$KEY_FOUND" ] && {
		echo "$(line_KeyValue_getValue "$line")"
		return 0
	}
	return 1
}
function _mdu_properties_continuation() {
	local previous="$1"
	local actual="$2"
	grep -Eq '^[ 	]*-	.*' <<< "${actual}" && {
		sed 's/^[ 	]*-	\(.*\)$/\1/' <<< "${actual}"
		return 0
	}
	return 1
}

# @description Get a `value` for a `key` from a `text`
# Deprecated : use `properties_find`
function find_property {
	properties_find $@
	return $?
}
# @description Get a `value` for a `key` from a `text`
#
# Text is
# ```
# # any line starting with '#' ignored
# key1=value1
# key2=value2
# ...
# keyN=valueN
# ```
#
# @arg $1 string `key`
# @arg $2 string *optional* `text` to be search in
# @stdin `text` to be search in ( *only* when arg $2 is not provided )
# @stdout corresponding `value` (if any was found) to the `key`
#
# @exitcode 0 if a `value` was found
# @exitcode !0 if no `value` was found
function properties_find {
	local found=0
	local value
	local continuation
	[ -z ${2+x} ] && {
		while read l ; do
			if [ ${found} = 0 ]; then
				value="$(_mdu_properties_value_from_line "$l" "$1")" && {
					found=1
					echo "${value}"
				}
			else
				continuation="$(_mdu_properties_continuation "${value}" "${l}")" && {
					echo "${continuation}"
					value="${continuation}"
				} || {
					return 0
				}
			fi
		done
		return 1
	}

	while read l ; do
		if [ ${found} = 0 ]; then
			value="$(_mdu_properties_value_from_line "$l" "$1")" && {
				found=1
				echo "${value}"
			}
		else
			continuation="$(_mdu_properties_continuation "${value}" "${l}")" && {
				echo "${continuation}"
				value="${continuation}"
			} || {
				return 0
			}
		fi
	done <<< "$2"
	return 1
}
function properties_findTyped {
	properties_find $@
	return $?
}

# @description Extract the `key` part (i.e. the part on the left) from a `line` like :
#
# ```
# key=value
# ```
#
# @arg $1 string `line`
#
# @exitcode 0
function line_KeyValue_getKey() {
	sed 's/^[ 	]*\([^ 	=]*\)[ 	]*=\(.*\)/\1/' <<< "$1"
}
# @description Extract the `value` part (i.e. the part on the right) from a `line` like :
#
# ```
# key=value
# ```
#
# @arg $1 string `line`
#
# @exitcode 0
function line_KeyValue_getValue() {
	sed 's/^[ 	]*\([^ 	=]*\)[ 	]*=\(.*\)/\2/' <<< "$1"
}


# @description Split file fullpath into different parts
#
# Original code seen [here on stackoverflow](http://stackoverflow.com/a/1403489)
#
# @arg $1 string fullpath
#
# @stdout
# the parts of the path, one per line
# * the filename
# * the directory
# * the base name ( the filename without extension )
# * the biggest extension
#
# @exitcode 0
split_filepath() {
	local fullpath
	fullpath="$1"
	local filename dir base ext
	filename="${fullpath##*/}"
    dir="${fullpath:0:${#fullpath} - ${#filename} -1}"
    base="${filename%.[^.]*}"
    ext="${filename:${#base} + 1}"
    if [[ -z "$base" && -n "$ext" ]]; then
    	# If we have an extension and no base, it's really the base
        base=".$ext"
        ext=""
    fi

    echo -e "${filename}"
    echo -e "${dir}"
    echo -e "${base}"
    echo -e "${ext}"
}

# ---------------------- #
#                        #
#        Options         #
#                        #
# ---------------------- #

__mdu_parameter_index=1
reset_get_options() {
	__mdu_parameter_index=1
}
is_option_configuration_valid() {
	if [[ $1 =~ (^|\|)([-a-zA-Z]+)(\||$) ]] ; then return 0 ; else return 1 ; fi
}

__mdu_get_option_first_name () {
	[[ $1 =~ ^([^|:]*) ]] || return 1
	echo "${BASH_REMATCH[1]}"
}
__mdu_get_option_config () {
	local configs option
	configs="$1"
	option="$2"
	local regex_has_option
	regex_has_option="(^|\|)$(escaped_for_regex "$option")(\||:?$)"
	while read -d ',' config; do
		if [[ $config =~ $regex_has_option ]] ; then
			echo "$config"
			return 0
		fi
	done <<< "$configs,"
	return 1
}

is_option_config_with_parameter () {
	local config
	config="$1"
	if [[ $config =~ .+: ]] ; then return 0 ; fi
	return 1
}

# @description Parse command line arguments, one by one
#
# Next parameter to be parsed index is hold by the global variable `__mdu_parameter_index`.
#
# @arg $1 string configuration of possible options ( ex `h|help,c|check,n|name:` )
# @arg $2 string name of the variable that must hold the option found
#
# @exitcode 0 when a option is found
# @exitcode 1 when there was no more parameter to parse
# @exitcode 2 when the remaining parameter(s) is(/are) no option
# @exitcode 3 if a wrong option is encountered
# @exitcode 8 if parameter `name` (`$2`) is not a valid variable name
# @exitcode 9 if parameter `name` (`$2`) is not a valid variable name
function get_options () {

	local options_config name
	options_config="$1"
	name="$2"

	is_valid_variable_name "$name" || exit 9

	local option_config parameter parameter_index

	[ "x${__mdu_parameter_index:-}" = x ] && __mdu_parameter_index=0

	parameter_index=$(($__mdu_parameter_index + 2))
	OPTIND=$__mdu_parameter_index

	if [ $# -lt "$parameter_index" ]; then return 1 ; fi
	parameter=${!parameter_index}


	if [[ $parameter =~ ^-{1,2}(.*)$ ]] ; then
		parameter="${BASH_REMATCH[1]}"
	else
		return 2
	fi

	option_config="$(__mdu_get_option_config "$options_config" "$parameter")" || { echo "No option '$parameter'" >&2 ; return 3 ; }

	eval "${name}=$(__mdu_get_option_first_name "$option_config")"

	#echo "option_config = '$option_config'" >&2
	#echo "option = '$option'" >&2

	if is_option_config_with_parameter "$option_config" ; then
		__mdu_parameter_index=$(($__mdu_parameter_index + 1))
		parameter_index=$(($__mdu_parameter_index + 2))
		if [ $# -lt ${parameter_index} ]; then echo "Missing argument for option '$parameter'" >&2 ; return 4 ; fi
		OPTARG=${!parameter_index}
	else
		unset OPTARG
	fi

	__mdu_parameter_index=$(($__mdu_parameter_index + 1))
	OPTIND=${__mdu_parameter_index}

	return 0
}


# ---------------------- #
#                        #
#          Misc          #
#                        #
# ---------------------- #

# @description Check if a string can be used as variable name
#
# @arg $1 string the name to check
#
# @exitcode 0 if it can used as variable
# @exitcode 1 otherwise
function is_valid_variable_name() {
	[[ $1 =~ ^[a-zA-Z_]([a-zA-Z01-9_])+$ ]] && return 0 || return 1
}

function command_exists() {
	which "$1" >/dev/null 2>&1
	return $?
}

function expand_vars() {
	local s
	if [ $# -eq 0 ]; then
		s="$(cat)"
	else
		s="$1"
	fi
	eval "echo \"${s}\""
}


