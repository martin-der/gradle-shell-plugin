#!/usr/bin/env bash


source "$(readlink "$(dirname "$0")/shell-util.sh")" 2>/dev/null \
|| source "$(dirname "$0")/shell-util.sh" 2>/dev/null \
|| source shell-util || exit 1


     trap "R;exit" 2
    ESC=$( echo -en "\e")
  CLEAR(){ echo -en "\ec";}
  CIVIS(){ echo -en "\e[?25l";}
   DRAW(){ echo -en "\e%@\e(0";}
#   MARK(){ echo -en "\e[7m";}
# UNMARK(){ echo -en "\e[27m";}
      R(){ CLEAR ;stty sane;};

keey_l=
__read_key() {
	local ky keey
	keey_l=
	while :; do
		read -s -n1 ky 2>/dev/null >&2
		#if [[ $key = $ESC[A ]]; then echo up; return 0; fi
		#if [[ $key = $ESC[B ]]; then echo down; return 0; fi;
		ky="$(echo -e "$ky" | hexdump -e '16/1 "%02x" "\n"')"
		if [ $ky = "0a" ]; then echo enter; return 0; fi;
		[[ $ky =~ ^(.*)0a\ *$ ]] && ky="${BASH_REMATCH[1]}"
		keey="$keey$ky"
		[[ $keey =~ ^1b ]] || {
			case "$ky" in
				7f) echo "backspace" ; return 0 ;;
				09) echo "tab" ; return 0 ;;
			esac
		}
		[ "${#keey}" -gt 5 ] && break
	done
	keey_l="$keey"
	case $keey in
		1b5b41) echo up; return 0 ;;
		1b5b42) echo down; return 0 ;;
		1b5b43) echo right; return 0 ;;
		1b5b44) echo left; return 0 ;;
		1b5b33) echo delete; return 0 ;;
	esac

}

__set_fg_color() {
	printf '\e[0;%s8;2;%s;%s;%sm' "3" "$1" "$2" "$3"
	#local bg=4
	#printf "\x1b[${bg};2;${1};${2};${3}m\n"
}
__set_bg_color() {
	printf '\e[0;%s8;2;%s;%s;%sm' "4" "$1" "$2" "$3"
}

__move(){ echo -en "\e[${1};${2}H";}

__draw_r_at() {
	__move $1 $2 ; for ((i=0; i<$3; i++)); do echo -n "$4"; done
}
__draw_at() {
	__move $1 $2 ; echo -en "$3"
}
__draw_label() {
	local x="$2" y="$1" w="$3" f="$4" t="$5"
	local l="${#t}" s=0
	[[ "$f" =~ s ]] && s=1
	#[ $s -eq 1 ] && echo $(tput smul/rmul)
	#[ $s -eq 1 ] && echo $(tput smso/rmso)
	#[ $s -eq 1 ] && echo $(tput setf 0,1,2...7)

	[ $s -eq 1 ] && __set_fg_color 0 255 0
	[ $l -gt 0 ] && __draw_at $y $(($x+$w-$l)) "$t"
	[ $s -eq 1 ] && __set_fg_color 255 255 255
	__set_bg_color 0 0 0

}

__draw_input() {
	local x="$2" y="$1" w="$3" f="$4" input="$5" vn="${6:-}"
	local v
	[[ "$input" =~ ^([^:]+):([^:]+)(:([^:]*))?(:(.*))?$ ]] && {
		name="${BASH_REMATCH[1]}"
		type="${BASH_REMATCH[2]}"
		flag="${BASH_REMATCH[4]}"
		validation="${BASH_REMATCH[6]}"
		[[ "$type" =~ \[(.*)\] || "$type" =~ \[(.*)\]\* ]] && {
		 enum="${BASH_REMATCH[1]}"
		 [[ $type == *\* ]] && type="multiple-enum" || type="enum"
		 IFS="|" read -ra values <<< "${enum}"
		 fui_is_input_mandatory "$flag" && [ 0 -eq ${#values[@]} ] && { __print_error "Mandatory enum with no choice '$type'" ; return ${FUI_ERROR_INVALID_INPUT_DESC} ; }
		}

		local s=0
		[[ "$f" =~ s ]] && s=1


		v="${!vn}"

		case "$type" in
			string)
				[ $s -eq 1 ] && __set_bg_color 20 60 20 || __set_bg_color 20 20 20
				__draw_r_at $y $x $w " "
				__draw_at $y $x "$v"
				[ $s -eq 1 ] && __draw_at $y $(($x+${#v})) "█"
				__set_bg_color 0 0 0
			;;
			boolean)
				;;
			integer)
				;;
			"multiple-enum")
				;;
			enum)
				;;
			*) __print_error "Unknown type '$type'" ; return ${FUI_ERROR_INVALID_COMPONENT_DESC} ;;
		esac
	} || {
	  __draw_at $y $x "BI-$input"
	}
}


_get_component_name() {
	local components="$1" index="$2"
	index=0
	for component_name in ${components} ; do
		echo "$component_name" ; return 0
	done
	return 1
}
_get_component() {
	local components="$1" index="$2"
	local name component
	name="$(_get_component_name "$components" "$index")" || return 1
	component="$(fui_get_component "$name" )" || return 1
	return 0
}


__draw_label_input() {
	local x="$2" y="$1" lw="$3" iw="$4" f="$5" label="$6" input="$7" vn="${8:-}"
	local sep=1
	__draw_label $y $x "$lw" "$f" "$label"
	__draw_input $y $(($x+$lw+$sep)) "$iw" "$f" "$input" "$vn"
}


__set_fg_color() {
	printf '\e[0;%s8;2;%s;%s;%sm' "3" "$1" "$2" "$3"
	#local bg=4
	#printf "\x1b[${bg};2;${1};${2};${3}m\n"
}
__set_bg_color() {
	printf '\e[0;%s8;2;%s;%s;%sm' "4" "$1" "$2" "$3"
}


__draw_input() {
	local x="$2" y="$1" w="$3" f="$4" input="$5" vn="${6:-}"
	local v
	[[ "$input" =~ ^([^:]+):([^:]+)(:([^:]*))?(:(.*))?$ ]] && {
		name="${BASH_REMATCH[1]}"
		type="${BASH_REMATCH[2]}"
		flag="${BASH_REMATCH[4]}"
		validation="${BASH_REMATCH[6]}"
		[[ "$type" =~ \[(.*)\] || "$type" =~ \[(.*)\]\* ]] && {
			enum="${BASH_REMATCH[1]}"
			[[ $type == *\* ]] && type="multiple-enum" || type="enum"
			IFS="|" read -ra values <<< "${enum}"
			fui_is_input_mandatory "$flag" && [ 0 -eq ${#values[@]} ] && { __print_error "Mandatory enum with no choice '$type'" ;  return ${FUI_ERROR_INVALID_INPUT_DESC} ; }
		}

		local s=0
		[[ "$f" =~ s ]] && s=1


		v="${!vn}"

		case "$type" in
			string)
				[ $s -eq 1 ] && __set_bg_color 20 60 20 || __set_bg_color 20 20 20
				__draw_r_at $y $x $w " "
				__draw_at $y $x "$v"
				[ $s -eq 1 ] && __draw_at $y $(($x+${#v})) "█"
				__set_bg_color 0 0 0
			;;
			boolean)
				;;
			integer)
				;;
			"multiple-enum")
				;;
			enum)
				;;
			*) __print_error "Unknown type '$type'" ;  return ${FUI_ERROR_INVALID_COMPONENT_DESC} ;;
		esac
	}
}

__compute_layout() {
	local components="$1" title="$2" header="$3" footer="$4"
	__viewport_width="$(tput cols)"
	__viewport_height="$(tput lines)"

}

__get_component_content_min_size() {
	local component_index="$1" component_name="$2"
	local component

	local label_width input_width label_max_width input_max_width height
	height=0
	label_max_width=0
	input_max_width=0

	component=$(fui_get_component "$component_name" ) || { __print_error "Failed to get component '$component_name'" ; }
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
				fui_is_input_mandatory "$flag" && [ 0 -eq ${#values[@]} ] && { __print_error "Mandatory enum with no choice '$type'" ;  return ${FUI_ERROR_INVALID_INPUT_DESC} ; }
			}

			label="$(fui_get_input_label "$name" "$component")"
			[ "x$label" = x ] && label="$name"

			label_width="${#label}"
			input_width=15
			[ $label_width -gt $label_max_width ] && label_max_width=$label_width
			[ $input_width -gt $input_max_width ] && input_max_width=$input_width
			height=$(($height+1))
		}
	done
	echo "$(($label_max_width+1+$input_max_width)),$height,$label_max_width,$input_max_width"
}


__get_draw_material() {
	case "$1" in
		simple-border)
			echo "┌ ┐ └ ┘ ─ │ ┬ ┴ ├ ┤ ┼"
			;;
		double-border)
			echo "╔ ╗ ╚ ╝ ═ ║ ╦ ╩ ╠ ╣ ╬"
			;;
		doubleH-simpleV-border|simpleV-doubleH-border)
			echo "╓ ╖ ╙ ╜ ─ ║ ╥ ╨ ╟ ╢ ╫"
			;;
		doubleV-simpleH-border|simpleH-doubleV-border)
			echo "╒ ╕ ╘ ╛ ═ │ ╤ ╧ ╞ ╡ ╪"
			;;
	esac
}
__draw_selected_input() {
	local components="$1" title="$2" header="$3" footer="$4"
	__draw_inputs "$selected_input" "$components" "$title" "$header" "$footer"
}
__draw_inputs() {
	local requested_inputs="$1" components="$2" title="$3" header="$4" footer="$5"
	local x y x_component y_component
	local component_name input

	local label_width input_width


	x=$__page_content_x
	y=$__page_content_y
	y=$(($y+2))
	[ "x$header" != "x" ] && {
		y=$(($y+2))
	}



	component_index=0
	while IFS="\n" read component_name; do

		y_component=$y

		component=$(fui_get_component "$component_name" ) || { __print_error "Failed to get component '$component_name'" ; return 1 ; }

		IFS="," read content_width content_height label_width input_width <<< "$(__get_component_content_min_size "$component_index" "$component_name")"

		y=$(($y_component+2))

		input_index=0
		while IFS="\n" read input ; do

			 [[ "$input" =~ ^([^:]+):([^:]+)(:([^:]*))?(:(.*))?$ ]] &&
			 {
				name="${BASH_REMATCH[1]}"
				type="${BASH_REMATCH[2]}"
				flag="${BASH_REMATCH[4]}"
				validation="${BASH_REMATCH[6]}"
				[[ "$type" =~ \[(.*)\] || "$type" =~ \[(.*)\]\* ]] && {
					enum="${BASH_REMATCH[1]}"
					[[ $type == *\* ]] && type="multiple-enum" || type="enum"
					IFS="|" read -ra values <<< "${enum}"
					fui_is_input_mandatory "$flag" && [ 0 -eq ${#values[@]} ] && { __print_error "Mandatory enum with no choice '$type'" ; return ${FUI_ERROR_INVALID_INPUT_DESC} ; }
				}

				#[[ $requested_inputs =~ ,?$selected_input,? ]] &&
				{

					label="$(fui_get_input_label "$name" "$component")"
					[ "x$label" = x ] && label="$name"

					local input_flag=
					[ $input_index -eq $selected_input ] && input_flag="${input_flag}s"
					__draw_label_input $y $((2+$x)) $label_width $input_width "$input_flag" "$label" "$input" "$(fui_get_variable_key "$component_name" "$name")"
				}
				input_index=$(($input_index+1))
				y=$(($y+1))
			}
		done <<< "${component}"

		y=$(($y+2))

		component_index=$(($component_index+1))
	done <<< "${components}"

}

__draw() {

	local components="$1" title="$2" header="$3" footer="$4"
	local component component_title inputs input enum values value
	local name label type flag validation
	local x y x_component y_component
	local display_round
	local label_width input_width component_width
	local s

	local w="$__viewport_width"
	local h="$__viewport_height"
	local input_index component_index

	echo -en "\ec"

	tput civis

	x=$__page_content_x
	y=$__page_content_y
	__draw_at $y $x "$title"
	y=$(($y+2))
	[ "x$header" != "x" ] && {
		__draw_at $y $x "$header"
		y=$(($y+2))
	}

	__draw_at 0 0 "$__viewport_width / $__viewport_height"

	component_index=0
	while IFS="\n" read component_name; do

		y_component=$y

		component_title="$(fui_get_component_label "$component_name")"
		[ "x$component_title" = x ] && component_title="$component_name"

		component=$(fui_get_component "$component_name" ) || { __print_error "Failed to get component '$component_name'" ; return 1 ; }

		IFS="," read content_width content_height label_width input_width <<< "$(__get_component_content_min_size "$component_index" "$component_name")"

		s="╔═[ $component_title ]═($content_width;$content_height)"
		component_width=$(($content_width+4))
		__draw_at $y_component $x "$s"
		__draw_at $(($y_component-1)) $x "($label_width - $input_width)"
		__draw_at $y_component $(($x+$component_width-1)) "╗"
		[ $((${#s}+1)) -lt $component_width ] && __draw_r_at $y_component $((x+${#s})) $(($component_width-${#s}-1)) "═"
		__draw_at $((y_component+1)) $x "║"
		__draw_at $((y_component+1)) $(($x+$component_width-1)) "║"

		y=$(($y_component+2))

		input_index=0
		while IFS="\n" read input; do

			[[ "$input" =~ ^([^:]+):([^:]+)(:([^:]*))?(:(.*))?$ ]] && {
				name="${BASH_REMATCH[1]}"
				type="${BASH_REMATCH[2]}"
				flag="${BASH_REMATCH[4]}"
				validation="${BASH_REMATCH[6]}"
				[[ "$type" =~ \[(.*)\] || "$type" =~ \[(.*)\]\* ]] && {
					enum="${BASH_REMATCH[1]}"
					[[ $type == *\* ]] && type="multiple-enum" || type="enum"
					IFS="|" read -ra values <<< "${enum}"
					fui_is_input_mandatory "$flag" && [ 0 -eq ${#values[@]} ] && { __print_error "Mandatory enum with no choice '$type'" ; return ${FUI_ERROR_INVALID_INPUT_DESC} ; }
				}

				label="$(fui_get_input_label "$name" "$component")"
				[ "x$label" = x ] && label="$name"

				#[ $display_round -eq 1 ] && {
				local input_flag=
				[ $input_index -eq $selected_input ] && input_flag="${input_flag}s"
				__draw_at $y $x "║"
				__draw_label_input $y $((2+$x)) $label_width $input_width "$input_flag" "$label" "$input" "$(fui_get_variable_key "$component_name" "$name")"
				__draw_at $y $((x+$component_width-1)) "║"
				y=$(($y+1))


			} || {
				__print_error "INVALID_COMPONENT_DESC '$component'"
				return ${FUI_ERROR_INVALID_COMPONENT_DESC}
			}
			input_index=$(($input_index+1))
		done <<< "${component}"
		__draw_at $y $x "╚"
		__draw_r_at $y $((x+1)) $(($component_width-2)) "═"
		__draw_at $y $((x+$component_width-1)) "╝"

		y=$(($y+2))

		component_index=$(($component_index+1))
	done <<< "${components}"

	[ "x$footer" != "x" ] && {
		__move $y $x ; echo "$footer"
		y=$(($y+2))
	}

}

__get_selected_variable_key() {
	fui_get_variable_key_by_indexes "$1" "$selected_component" "$selected_input"
}

__handle_key() {
	local components="$1" key="$2"
	local selected_variable selected_value
	local component

	case "$key" in

		up)
			[ $selected_input -gt 0 ] && {
				previous_selected_input=$selected_input
				selected_input=$(($selected_input-1))
				redraw_needed=selected-and-previous
			}
			;;
		down)
			component="$(_get_component "$components" $selected_component)"
			#[ $selected_input -lt $(echo "$component" | wc -l) ] && {
				previous_selected_input=$selected_input
				selected_input=$(($selected_input+1))
				redraw_needed=selected-and-previous
			#}
			;;
		enter)
			#redraw_needed=screen
			page_done=1;
			;;
		backspace|delete)
			__draw_at 3 50 "modif $key"
			selected_variable="$(__get_selected_variable_key "$components")"
			__draw_at 1 50 "selected '$selected_variable'"
			local selected_value="$(fui_get_variable_by_key "$selected_variable")"
			[ ${#selected_value} -gt 0 ] && {
				selected_value=${selected_value:0:$((${#selected_value}-1))}
				fui_set_variable_by_key "$selected_variable" "$selected_value"
				redraw_needed=select-input-value
			}
			__draw_at 2 50 "new-cted '$selected_value'"
			;;
		*)
			__draw_at 0 50 "[$key]" ;;

	esac
}

__fui_run_page_HUMBLETUI() {
	local components="$1" title="$2" header="$3" footer="$4"

	trap "__compute_layout \"$components\" \"$title\" \"$header\" \"$footer\" ;  __draw \"$components\" \"$title\" \"$header\" \"$footer\"" WINCH

	__compute_layout "$components" "$title" "$header" "$footer"

	local redraw_needed default_action
	local key

	selected_component=0
	selected_input=0
	previous_selected_input=-1

	__page_content_x=3
	__page_content_y=2


	CIVIS

	redraw_needed=
	default_action=0

	__draw "$components" "$title" "$header" "$footer"

	IFS=" "

	page_done=0
	key=

	while [ $page_done -eq 0 ] ; do
		key="$(__read_key)"

		redraw_needed=

		__handle_key "$components" "$key"

		[ "x$redraw_needed" = "xscreen" ] && __draw "$components" "$title" "$header" "$footer"
		[ "x$redraw_needed" = "xselect-input-value" ] && __draw_selected_input "$components" "$title" "$header" "$footer"
		[ "x$redraw_needed" = "xselected-and-previous" ] && __draw_inputs "$previous_selected_input,$selected_input" "$components" "$title" "$header" "$footer"


		__draw_at 10 60 "component:$selected_component input:$selected_input           "
		__draw_at 11 60 "key = $key             "
		__draw_at 12 60 "keey_l = $keey_l          "
		__draw_at 13 60 ""

	done
}