#!/usr/bin/env bash


source "$(readlink "$(dirname "$0")/shell-util.sh")" 2>/dev/null \
|| source "$(dirname "$0")/shell-util.sh" 2>/dev/null \
|| source shell-util || exit 1



__DUMBCLI_print_title() {
	local left="$1" title="$2" before="${3:-}" after="${4:-}"
	[ "x${before}" != x ] && { echo -n "$left" ; for ((i=0; i<${#title}; i++)); do echo -n "$before"; done ; echo ; }
	echo "$left$title"
	[ "x${after}" != x ] && { echo -n "$left" ; for ((i=0; i<${#title}; i++)); do echo -n "$after"; done ; echo ; }
}
__DUMBCLI_print_input_error() {
	local field="$1" name="$2" message="$3"
	echo -e "${RED}$message${COLOR_RESET}"
}

__DUMBCLI_print_if_failed_mandatory() {
	local value="$1" flag="$2" field="$3" name="$4"
	fui_is_input_mandatory "$flag" && [ "x$value" = "x" ] && {
		__DUMBCLI_print_input_error "$field" "$name" "'$name' is required"
		return 0
	}
	return 1
}
__DUMBCLI_print_if_failed_validation() {
	local value="$1" validation="$2" field="$3" name="$4"
	fui_is_value_valid_or_empty "$value" "$validation" || {
		__DUMBCLI_print_input_error "$field" "$name" "'$name' is invalid ( must match '${validation}' )."
		return 0
	}
	return 1
}

#
# @param 1 components
# @param 2 title
# @param 3 header
# @param 4 footer
#
__fui_run_page_DUMBCLI() {
	local components="$1" title="$2" header="$3" footer="$4"
	local component inputs input enum values value
	local name label type flag validation

	[ "x$title" != x ] && {
		__DUMBCLI_print_title "  " "$title" "" "="
	}
	[ "x$header" != x ] && {
		echo "$header"
	}

	local previous_IFS="$IFS"
	IFS=$'\n'

	for component_name in ${components} ; do

		component=$(fui_get_component "$component_name" ) || { __print_error "Failed to get component '$component_name'" ; IFS="$previous_IFS" ; return 1 ; }
		for input in ${component} ; do
			[[ "$input" =~ ^([^:]+):([^:]+)(:([^:]*))?(:(.*))?$ ]] && {
				name="${BASH_REMATCH[1]}"
				type="${BASH_REMATCH[2]}"
				flag="${BASH_REMATCH[4]}"
				validation="${BASH_REMATCH[6]}"
				[[ "$type" =~ \[(.*)\] || "$type" =~ \[(.*)\]\* ]] && {
					enum="${BASH_REMATCH[1]}"
					[[ $type == *\* ]] && type="multiple-enum" || type="enum"
					IFS="|" read -ra values <<< "${enum}"
					fui_is_input_mandatory "$flag" && [ 0 -eq ${#values[@]} ] && { __print_error "Mandatory enum with no choice '$type'" ; IFS="$previous_IFS" ; return ${FUI_ERROR_INVALID_INPUT_DESC} ; }
				}

				label="$(fui_get_input_label "$name" "$component")"
				[ "x$label" = x ] && label="$name"

				#echo "Input :"
				#echo "  name = '$name'"
				#echo "  type = '$type'"
				#echo "  flag = '$flag'"
				#echo "  validation = '$validation'"

				case "$type" in
					string)
						while : ; do
							echo -n "$label : "
							read value
							__DUMBCLI_print_if_failed_mandatory "$value" "$flag" "$name" "$name" && continue
							[ "x$validation" != x -a "x$value" != x ] && {
								[[ "$value" =~ ${validation} ]] || {
									__DUMBCLI_print_input_error "$name" "$name" "'$label' is invalid ( must match '${validation}' )."
									continue
								}
							}
							break
						done
						fui_set_variable "$component_name" "$name" "$value"
						;;
					boolean)
						while : ; do
							echo -n "$label [y/n] : "
							read -rsn1 value
							echo "$value"
							[ "x$value" = "xy" -o "x$value" = "xY" ] && { value=1 ; break ; }
							[ "x$value" = "xn" -o "x$value" = "xN" ] && { value=0 ; break ; }
							__DUMBCLI_print_input_error "$name" "$name" "Choose 'y' or 'n' for '$label'."
							continue
						done
						fui_set_variable "$component_name" "$name" "$value"
						;;
					integer)
						while : ; do
							echo -n "$label : "
							read value
							__DUMBCLI_print_if_failed_mandatory "$value" "$flag" "$name" "$name" && continue
							[ "x$validation" != x ] && {
								fui_is_value_valid_integer_or_empty "$value" "$validation" || {
									__DUMBCLI_print_input_error "$name" "$name" "'$label' must be an integer complying to : ${validation}."
									continue
								}
							}
							break
						done
						fui_set_variable "$component_name" "$name" "$value"
						;;
					"multiple-enum")
						while : ; do
							echo "Pick one or more $label :"
							local i=1
							for v in "${values[@]}" ; do
								label="$("$__fui_expresser" input "${name}:[$v]")"
								[ "x$label" = x ] && label=$v
								echo "  $i) $label"
								i=$((1+$i))
							done
							echo -n "> " ; read value
							__DUMBCLI_print_if_failed_mandatory "$value" "$flag" "$name" "$name" && continue

							fui_is_value_integer "$value" || { __DUMBCLI_print_input_error "$name" "$name" "An integer is expected." ; continue ; }
							value=$(($value-1))
							[ $value -ge 0 -a $value -lt ${#values[@]} ] || { __DUMBCLI_print_input_error "$name" "$name" "Choose a value between 1 and ${#values[@]}." ; continue ; }
							value="${values[$value]}"
							break
						done
						fui_set_variable "$component_name" "$name" "$value"
						;;
					enum)
						while : ; do
							echo "Pick one $label :"
							local i=1
							for v in "${values[@]}" ; do
								label="$("$__fui_expresser" input "${name}:[$v]")"
								[ "x$label" = x ] && label=$v
								echo "  $i) $label"
								i=$((1+$i))
							done
							echo -n "> " ; read value
							__DUMBCLI_print_if_failed_mandatory "$value" "$flag" "$name" "$name" && continue

							fui_is_value_integer "$value" || { __DUMBCLI_print_input_error "$name" "$name" "An integer is expected." ; continue ; }
							value=$(($value-1))
							[ $value -ge 0 -a $value -lt ${#values[@]} ] || { __DUMBCLI_print_input_error "$name" "$name" "Choose a value between 1 and ${#values[@]}." ; continue ; }
							value="${values[$value]}"
							break
						done
						fui_set_variable "$component_name" "$name" "$value"
						;;
					*) __print_error "Unknown type '$type'" ; IFS="$previous_IFS" ; return ${FUI_ERROR_INVALID_COMPONENT_DESC} ;;
				esac

			} || {
				__print_error "INVALID_COMPONENT_DESC '$component'"
				IFS="$previous_IFS"
				return ${FUI_ERROR_INVALID_COMPONENT_DESC}
			}
		done
	done
	[ "x$footer" != x ] && {
		echo "$footer"
	}



	IFS="$previous_IFS"
}