#!/usr/bin/env bash


source "$(readlink "$(dirname "$0")/shell-util.sh")" 2>/dev/null

__content=

from_json_builder_set_file() {
	__content="$(cat "$1")"
}

__request() {
	jq -M "$1" <<< "$__content"
}

__die_with_message() {
	echo "$1" ; return 1 ;
}

__echo_without_quote() {
	local l t
	t=${1}
	l=${#t}
	echo "${t:1:$(($l-2))}"
}
__echo_not_null() {
	local t="$1"
	[ "x$t" = "xnull" ] && return 5
	__echo_without_quote "$t"
	return 0
}
__echo_and_exit() {
	__echo_not_null "$1"
	exit $?
}

from_json_builder() {
	local what="$1" which="$2" part="$3"
	local component inputs_length
	local page components_length
	local input input_name input_type input_flag input_validation
	local value i
	case "$what" in
		entrance)
			__request ".config.entrance"
			;;
		page)
			page="$(__request ".pages[] | select(.name == \"$which\") ")"
			[ "x$page" = "x" ] && __die_with_message "No such page '$which'"
			case "$part" in
				title)
					__echo_and_exit "$(jq ". | .title" <<< "$page")"
					;;
				header)
					__echo_and_exit "$(jq ". | .header" <<< "$page")"
					;;
				footer)
					__echo_and_exit "$(jq ". | .footer" <<< "$page")"
					;;
				list-components)
					components_length="$(jq ". | .components | length" <<< "$page")"
					for (( i=0 ; i < $components_length ; i++ )) ; do
						component="$(jq ". | .components[$i]" <<< "$page")"
						__echo_without_quote "$component"
					done
					return 0
					;;
				navigation)
					input="$(jq --raw-output ". | .navigation" <<< "$page")"
					echo "=> $input"
					return 0
					;;
			esac
			;;
		component)
			component="$(__request ".components[] | select(.name == \"$which\") ")"
			[ "x$component" = "x" ] && __die_with_message "No such component '$which'"
			inputs_length="$(jq ". | .inputs | length" <<< "$component")"
			for (( i=0 ; i < $inputs_length ; i++ )) ; do
				input="$(jq ". | .inputs[$i]" <<< "$component")"
				input_name="$(jq --raw-output ".name" <<< "$input")"
				input_type="$(jq --raw-output ".type" <<< "$input")"
				input_flag="$(jq --raw-output ".mandatory" <<< "$input")"
				[ "x$input_flag" = "xtrue" ] && input_flag="m" || input_flag=""
				input_validation="$(jq --raw-output ".validation" <<< "$input")"
				[ "x$input_validation" != "xnull" ] || input_validation=""
				echo "$input_name:$input_type:$input_flag:$input_validation"
			done
	esac
	return 0
}


[ "x${BASH_SOURCE[0]}" == "x${0}" ] && {
	from_json_builder_set_file "$1"
	shift
	from_json_builder "$@"
}
