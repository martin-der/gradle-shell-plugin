#!/usr/bin/env bash


source "$(readlink "$(dirname "$0")/shell-util.sh")" 2>/dev/null \
|| source "$(dirname "$0")/shell-util.sh" 2>/dev/null \
|| source shell-util || exit 1

__fui_builder=
__fui_expresser=
__fui_engine=dumb-cli
__fui_output_prefix=__fui_VAL
__fui_page_runner=
__fui_RUN_page=

FUI_ERROR_BAD_BUILDER=4
FUI_ERROR_INVALID_COMPONENT_DESC=5
FUI_ERROR_INVALID_INPUT_DESC=6



__print_error() {
	echo "$1" >&2
}
__print_error_missing_param() {
	echo "Parameter '$2'($1) required" >&2
}
__check_param_is_given() {
	local n="$2"
	local i="$1"
	shift 2
	[ "x${!i:-}" == x ] && { echo "Parameter '$n'($i) required" >&2 ; return 1 ; }
}
__check_builder_or_print_error() {
	[ "${__fui_builder:-}" == "x" ] && { echo "no builder configured, use 'fui_set_builder <builder-function>'" >&2 ; return 1 ; }
	return 0
}


__load_page_runner() {

	__fui_page_runner=
	case "$__fui_engine" in
		dumb-cli)
			load_source_once "./flowui-dumbcli" sh || {  __print_error "Failed to load 'flowui-dumbcli'" ; return ${FUI_ERROR_BAD_BUILDER}; }
			__fui_page_runner=__fui_run_page_DUMBCLI
			;;
		humble-tui)
			load_source_once "./flowui-humbletui" sh || {  __print_error "Failed to load 'flowui-humbletui'" ; return ${FUI_ERROR_BAD_BUILDER}; }
			__fui_page_runner=__fui_run_page_HUMBLETUI
			;;
		*)
			__print_error "Unknown runner '${__fui_engine}'" ; return ${FUI_ERROR_BAD_BUILDER};
			;;
	esac

	[ "x$(type -t "$__fui_page_runner")" == xfunction ] || {  __print_error "'$__fui_page_runner' is not a valid function" ; return ${FUI_ERROR_BAD_BUILDER}; }

	return 0
}


fui_is_input_mandatory() {
	[ "x$1" == "xm" ]
}

fui_has_validation() {
	[ "x$value" != x ] && return 0 || return 1
}
fui_is_value_valid_string_or_empty() {
	local value="$1" validation="$2"
	[ "x$value" != x ] && {
		[[ "$value" =~ ${validation} ]] || return 1
	}
	return 0
}
fui_is_value_integer() {
	[[ $1 =~ ^[-+]?[0-9]+$ ]] && return 0 ||  return 1
}
fui_is_value_valid_integer_or_empty() {
	local value="$1" validation="$2" validations
	[ "x$value" != x ] && {
		fui_is_value_integer "$value" || return 1
		IFS=", " read -ra validations <<< "${validation}"
		for validation in "${validations[@]}"; do
			[[ "$validation" =~ ^\<([-+]?[0-9]+)$ ]] && { [ $value -lt ${BASH_REMATCH[1]} ] || return 1 ; continue ; }
			[[ "$validation" =~ ^\>([-+]?[0-9]+)$ ]] && { [ $value -gt ${BASH_REMATCH[1]} ] || return 1 ; continue ; }
			[[ "$validation" =~ ^\<=([-+]?[0-9]+)$ ]] && { [ $value -le ${BASH_REMATCH[1]} ] || return 1 ; continue ; }
			[[ "$validation" =~ ^\>=([-+]?[0-9]+)$ ]] && { [ $value -ge ${BASH_REMATCH[1]} ] || return 1 ; continue ; }
			echo "bad rule '$validation'" >&2
			return 2
		done
	}
	return 0
}

fui_set_builder() {
	[ "${1:-}" == "x" ] && { __print_error_missing_param "1" "builder" ; return 1 ; }
	__fui_builder="$1"
}
fui_set_expresser() {
	[ "${1:-}" == "x" ] && { __print_error_missing_param "1" "expresser" ; return 1 ; }
	__fui_expresser="$1"
}


fui_sanitized_variable_key() {
	sed 's/-/_/g' <<< "$1"
}


#
# param 1 component
# param 2 input
#
fui_get_variable_key() {
	fui_get_variable_key_for_page "${__fui_RUN_page}" "${1}" "${2}"
}
#
# param 1 components
# param 2 component_index
# param 2 input_index
#
fui_get_variable_key_by_indexes() {
	local components="$1" for_component="$2" for_input="$3"
	local component_index input_index
	local component
	component_index=0
	for component_name in ${components} ; do

		component=$(fui_get_component "$component_name" )

		[ $for_component -eq $component_index ] && {
			input_index=0
			for input in ${component} ; do

				[ $for_input -eq $input_index ] && {
					[[ "$input" =~ ^([^:]+):([^:]+)(:([^:]*))?(:(.*))?$ ]] && {
						name="${BASH_REMATCH[1]}"
						fui_get_variable_key "$component_name" "$name"
					}
				}
				input_index=$(($input_index+1))
			done
		}
		component_index=$(($component_index+1))
	done
}

#
# param 1 page
# param 2 component
# param 3 input
#
fui_get_variable_key_for_page() {
	fui_sanitized_variable_key "${__fui_output_prefix}__${1}__${2}__${3}"
}
#
# param 1 component
# param 2 input
#
fui_get_variable_key_prefix() {
	fui_sanitized_variable_key "${__fui_output_prefix}__"
}

fui_list_variables_key() {
	local prefix="$(escaped_for_regex "$(fui_get_variable_key_prefix)")"
	eval "echo \${!${prefix}*}" | tr ' ' '\n'
}
fui_list_variables() {
	fui_list_variables_key | while read v ; do
		echo "$v=${!v}"
	done
}
fui_unset_variables() {
	while read v ; do
		unset "${v}"
	done <<< "$(fui_list_variables_key)"
}
#
# param 1 component
# param 2 input
#
fui_unset_variable() {
	local component="$1" input="$2"
	i="$(fui_get_variable_key "$component" "$input")"
	fui_unset_variable_by_key "$i"
}
fui_unset_variable_by_key() {
	unset "$1"
}
#
# param 1 component
# param 2 input
#
fui_get_variable() {
	local component="$1" input="$2"
	fui_get_variable_by_key "$i"
}
fui_get_variable_by_key() {
	local i
	i="$1"
	[ -z ${!i+x} ] && return 1 || {
		echo -n -e "${!i-}"
		return 0
	}
}
#
# param 1 component
# param 2 input
# param 3 value
#
fui_set_variable() {
	fui_set_variable_for_page "${__fui_RUN_page}" "$1" "$2" "$3"
}
#
# param 1 page
# param 2 component
# param 3 input
# param 4 value
#
fui_set_variable_for_page() {
	local page="$1" component="$2" input="$3"
	i="$(fui_get_variable_key_for_page "$page" "$component" "$input")"
	fui_set_variable_by_key "$i" "$4"
}
fui_set_variable_by_key() {
	local i
	i="$1"
	#read -d"\0" "$i" <<<"$3"
	read -r "$i" <<<"$2"
}


fui_run_first_page() {
	local page

	page="$("$__fui_builder" entrance "" "")" || { __print_error "Failed to get first page '$page'" ; return 1 ; }

	fui_run_page "$page"
}

fui_run_page() {
	__check_builder_or_print_error || return 1
	__check_param_is_given 1 "Page Name" "$@"
	local page="$1"

	__load_page_runner

	while :; do
		local components title header footer
		local navigation
		components=$("$__fui_builder" page "$page" list-components) || { __print_error "Failed to get components for page '$page'" ; return 1 ; }
		title=$("$__fui_builder" page "$page" title)
		header=$("$__fui_builder" page "$page" header)
		footer=$("$__fui_builder" page "$page" footer)

		__fui_RUN_page="$page"

		local previous_IFS="$IFS"
		"$__fui_page_runner" "$components" "$title" "$header" "$footer"
		IFS="$previous_IFS"

		navigation="$("$__fui_builder" page "$page" navigation)" || { __print_error "Failed to get navigation for page '$page'" ; return 1 ; }
		[[ $navigation =~ ^[\ ]*=\>[\ ]*(.+)$ ]] && {
			page="${BASH_REMATCH[1]}"
			continue
		}
		break;
	done

}

fui_get_component() {
	"$__fui_builder" component "$1" ""
}

#
# param 1 input
# param 2 component @optionnal
#
fui_get_input_label() {
	"$__fui_expresser" input "$1" "${2:-}"
}
#
# param 1 input
# param 2 component @optionnal
#
fui_get_component_label() {
	"$__fui_expresser" component "$1" "${2:-}"
}


