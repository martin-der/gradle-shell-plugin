#!/usr/bin/env bash

test "x${FANCY_INPUT:-}" = "xFANCY-INPUT" && return 0
FANCY_INPUT=FANCY-INPUT

FANCY_INPUT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${FANCY_INPUT_DIR}/fancy.sh" 2>/dev/null \
|| source fancy || exit 1

# @description Show a fancy inline text input (enhanced `read`). The result is stored in a variable.
#              Uses the current value of the target variable as the initial value.
# @arg $1 string  Name of the variable to store the result in
# @arg $2 string  Label placed in the top border (optional)
# @arg $3 int     Total width including borders (default 40, min 10)
# @arg $4 int     Maximum characters, shows a counter when set (optional)
# @exitcode 0 User confirmed with Enter
# @exitcode 1 User cancelled with Esc (or EOF)
fancy_input() {
	local varname="$1"
	[ -z "$varname" ] && return 1
	local label="${2:-}"
	local width=${3:-40}
	local max_length="${4:-}"
	local initial="${!varname}"

	[ "$width" -lt 10 ] && width=10

	local content_width=$((width - 2))
	local text="$initial"
	local cursor=${#text}
	local offset=0

	# ---- terminal ----
	fancy_init
	trap "fancy_cleanup; return 1" INT TERM

	# ---- widget position ----
	local pos
	pos=$(fancy_get_cursor)
	local start_row="${pos% *}" start_col="${pos#* }"
	[ "$start_row" -lt 1 ] && start_row=1
	[ "$start_col" -lt 1 ] && start_col=1

	# ---- border chars ----
	local chars=($(fancy_get_border_chars "$FANCY_STYLE_BORDER"))
	local tl="${chars[0]}" tr="${chars[1]}" bl="${chars[2]}" br="${chars[3]}"
	local h="${chars[4]}" v="${chars[5]}"

	# ---- draw widget (initial render) ----
	__fancy_input_draw_widget() {
		local bc=(${FANCY_STYLE_BORDER_COLOR[@]})
		[ ${#bc[@]} -ge 3 ] && fancy_fg "${bc[@]}"

		# Top border
		fancy_move "$start_row" "$start_col"
		echo -n "$tl$h"
		if [ -n "$label" ]; then
			local lc=(${FANCY_STYLE_LABEL_COLOR[@]})
			[ ${#lc[@]} -ge 3 ] && fancy_fg "${lc[@]}"
			echo -n " $label "
			[ ${#bc[@]} -ge 3 ] && fancy_fg "${bc[@]}"
			echo -n "$h"
		fi
		local label_extra=0
		[ -n "$label" ] && label_extra=$((3 + ${#label}))
		local top_fill=$((width - 3 - label_extra))
		for ((i=0; i<top_fill; i++)); do echo -n "$h"; done
		echo "$tr"

		# Content line (empty initially, will be filled by redraw)
		fancy_move "$((start_row+1))" "$start_col"
		echo -n "$v"
		for ((i=0; i<content_width; i++)); do echo -n " "; done
		echo -n "$v"

		# Bottom border (will be redrawn with counter later)
		fancy_move "$((start_row+2))" "$start_col"
		echo -n "$bl"
		for ((i=0; i<width-2; i++)); do echo -n "$h"; done
		echo "$br"

		fancy_reset
	}

	__fancy_input_adjust_offset() {
		local cw=$content_width
		local len=${#text}
		[ $len -le $cw ] && { offset=0; return; }
		local target=$((cursor - cw / 2))
		[ $target -lt 0 ] && target=0
		[ $target -gt $((len - cw)) ] && target=$((len - cw))
		offset=$target
	}

	__fancy_input_redraw() {
		local indicator="$FANCY_STYLE_INDICATOR"
		local cw=$content_width len=${#text}
		local nl=0 nr=0

		[ $len -gt $cw ] && {
			[ $offset -gt 0 ] && nl=1
			[ $((offset + cw)) -lt $len ] && nr=1
		}

		local visible_len=$((cw - nl - nr))
		[ $visible_len -lt 0 ] && visible_len=0
		local visible="${text:$offset:$visible_len}"

		[ $nl -eq 1 ] && visible="${indicator}${visible:1}"
		[ $nr -eq 1 ] && {
			local vl=${#visible}
			[ $vl -gt 1 ] && visible="${visible:0:$((vl-1))}${indicator}"
		}

		local cursor_vis=$((nl + cursor - offset))
		[ $cursor_vis -lt 0 ] && cursor_vis=0
		[ $cursor_vis -ge $cw ] && cursor_vis=$((cw-1))

		# Content line
		local bc=(${FANCY_STYLE_BORDER_COLOR[@]})
		[ ${#bc[@]} -ge 3 ] && fancy_fg "${bc[@]}"
		fancy_move "$((start_row+1))" "$start_col"
		echo -n "$v"
		fancy_reset
		echo -n "$visible"
		local pad=$((cw - ${#visible}))
		for ((i=0; i<pad; i++)); do echo -n " "; done
		[ ${#bc[@]} -ge 3 ] && fancy_fg "${bc[@]}"
		echo -n "$v"
		fancy_reset

		# Bottom border with counter
		fancy_move "$((start_row+2))" "$start_col"
		[ ${#bc[@]} -ge 3 ] && fancy_fg "${bc[@]}"
		echo -n "$bl"

		local counter=""
		[ -n "$max_length" ] && counter=" ${#text}/${max_length} "
		local cl=${#counter}
		local fill_total=$((width - 2 - cl))
		[ $fill_total -lt 0 ] && fill_total=0
		local left_fill=$((fill_total / 2))
		local right_fill=$((fill_total - left_fill))

		for ((i=0; i<left_fill; i++)); do echo -n "$h"; done
		if [ -n "$counter" ]; then
			local cc=(${FANCY_STYLE_COUNTER_COLOR[@]})
			[ ${#cc[@]} -ge 3 ] && fancy_fg "${cc[@]}"
			echo -n "$counter"
			[ ${#bc[@]} -ge 3 ] && fancy_fg "${bc[@]}"
		fi
		for ((i=0; i<right_fill; i++)); do echo -n "$h"; done
		echo "$br"
		fancy_reset

		# Position cursor
		fancy_move "$((start_row+1))" "$((start_col+1+cursor_vis))"
	}

	# ---- initial draw ----
	__fancy_input_draw_widget

	text="$initial"
	cursor=${#text}
	__fancy_input_adjust_offset
	__fancy_input_redraw

	# ---- read loop ----
	local key __exit=0

	while true; do
		if ! key=$(fancy_read_key); then
			__exit=1
			break
		fi

		case "$key" in
			enter)
				break
				;;
			esc)
				text=""
				__exit=1
				break
				;;
			backspace)
				if [ $cursor -gt 0 ]; then
					text="${text:0:$((cursor-1))}${text:$cursor}"
					cursor=$((cursor-1))
					__fancy_input_adjust_offset
					__fancy_input_redraw
				fi
				;;
			delete)
				if [ $cursor -lt ${#text} ]; then
					text="${text:0:$cursor}${text:$((cursor+1))}"
					__fancy_input_adjust_offset
					__fancy_input_redraw
				fi
				;;
			ctrl_backspace|ctrl_w)
				if [ $cursor -gt 0 ]; then
					local i=$((cursor-1))
					while [ $i -ge 0 ] && [ "${text:$i:1}" = " " ]; do i=$((i-1)); done
					while [ $i -ge 0 ] && [ "${text:$i:1}" != " " ]; do i=$((i-1)); done
					i=$((i+1))
					text="${text:0:$i}${text:$cursor}"
					cursor=$i
					__fancy_input_adjust_offset
					__fancy_input_redraw
				fi
				;;
			left)
				if [ $cursor -gt 0 ]; then
					cursor=$((cursor-1))
					__fancy_input_adjust_offset
					__fancy_input_redraw
				fi
				;;
			right)
				if [ $cursor -lt ${#text} ]; then
					cursor=$((cursor+1))
					__fancy_input_adjust_offset
					__fancy_input_redraw
				fi
				;;
			ctrl_left)
				[ $cursor -gt 0 ] && {
					local i=$cursor
					while [ $i -gt 0 ] && [ "${text:$((i-1)):1}" = " " ]; do i=$((i-1)); done
					while [ $i -gt 0 ] && [ "${text:$((i-1)):1}" != " " ]; do i=$((i-1)); done
					cursor=$i
					__fancy_input_adjust_offset
					__fancy_input_redraw
				}
				;;
			ctrl_right)
				[ $cursor -lt ${#text} ] && {
					local i=$cursor len=${#text}
					while [ $i -lt $len ] && [ "${text:$i:1}" != " " ]; do i=$((i+1)); done
					while [ $i -lt $len ] && [ "${text:$i:1}" = " " ]; do i=$((i+1)); done
					cursor=$i
					__fancy_input_adjust_offset
					__fancy_input_redraw
				}
				;;
			home)
				cursor=0
				__fancy_input_adjust_offset
				__fancy_input_redraw
				;;
			end)
				cursor=${#text}
				__fancy_input_adjust_offset
				__fancy_input_redraw
				;;
			*)
				if [ -n "$key" ] && [ ${#key} -eq 1 ]; then
					if [ -z "$max_length" ] || [ ${#text} -lt "$max_length" ]; then
						text="${text:0:$cursor}${key}${text:$cursor}"
						cursor=$((cursor+1))
						__fancy_input_adjust_offset
						__fancy_input_redraw
					fi
				fi
				;;
		esac
	done

	# ---- cleanup ----
	fancy_cleanup

	# Move cursor below the widget
	fancy_move "$((start_row+3))" "$start_col"

	if [ $__exit -eq 0 ]; then
		printf -v "$varname" "%s" "$text"
		return 0
	fi
	return 1
}
