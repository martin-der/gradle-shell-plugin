#!/usr/bin/env bash

test "x${FANCY_SELECT:-}" = "xFANCY-SELECT" && return 0
FANCY_SELECT=FANCY-SELECT

FANCY_SELECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${FANCY_SELECT_DIR}/fancy.sh" 2>/dev/null \
|| source fancy || exit 1

# @description Truncate a `label` so it fits within `max` columns, replacing the
#              last character with the fancy indicator when it is too long.
# @arg $1 string  Label to truncate
# @arg $2 int     Maximum number of columns allowed (min 1)
# @stdout The (possibly truncated) label
__fancy_select_truncate() {
	local label="$1" max="$2"
	[ "$max" -lt 1 ] && max=1
	if [ ${#label} -gt "$max" ]; then
		label="${label:0:$((max-1))}${FANCY_STYLE_INDICATOR}"
	fi
	echo "$label"
}

# @description Normalize a single choice line into `value|label`. When no `|`
#              separator is present (or the label part is empty), the label
#              defaults to the value itself.
# @arg $1 string  Choice line
# @stdout `value|label`
__fancy_select_parse_choice() {
	local line="$1" value label
	value="${line%%|*}"
	label="${line#*|}"
	[ -z "$label" ] && label="$value"
	echo "$value|$label"
}

# @description Show a fancy single-select list. The result is stored in a variable.
#              Uses the current value of the target variable as the initial
#              selection. The list has no frame: the line under the cursor is
#              highlighted with the selection background color.
# @arg $1 string  Choices — either literal content (one `value|label` per line)
#                 or the name of a variable holding them
# @arg $2 string  Name of the variable to store the result in
# @arg $3 int     Maximum width of a row (default 40, min 10)
# @exitcode 0 User confirmed with Enter
# @exitcode 1 User cancelled with Esc (or EOF)
fancy_select() {
	local choices_varname="$1"
	local result_varname="$2"
	[ -z "$choices_varname" ] && return 1
	[ -z "$result_varname" ] && return 1
	local width=${3:-40}
	[ "$width" -lt 10 ] && width=10

	# ---- parse choices ----
	local __choices
	if [[ "$choices_varname" =~ ^[a-zA-Z_][a-zA-Z0-9_]*$ ]] && [ -n "${!choices_varname+x}" ]; then
		__choices="${!choices_varname}"
	else
		__choices="$choices_varname"
	fi
	local -a values labels
	local n=0 line value label parsed
	while IFS= read -r line; do
		[ -z "$line" ] && continue
		parsed=$(__fancy_select_parse_choice "$line")
		value="${parsed%%|*}"
		label="${parsed#*|}"
		values[$n]="$value"
		labels[$n]="$label"
		n=$((n+1))
	done <<< "$__choices"

	[ "$n" -eq 0 ] && return 1

	# ---- initial selection ----
	local selected=-1
	local result_initial="${!result_varname:-}" i
	for ((i=0; i<n; i++)); do
		if [ "$result_initial" = "${values[$i]}" ]; then
			selected=$i
		fi
	done

	# ---- terminal ----
	fancy_init
	echo -en "\e[?25l"
	trap "fancy_cleanup; return 1" INT TERM

	# ---- widget position ----
	local pos
	pos=$(fancy_get_cursor)
	local start_row="${pos% *}" start_col="${pos#* }"
	[ "$start_row" -lt 1 ] && start_row=1
	[ "$start_col" -lt 1 ] && start_col=1

	fancy_fit_lines "$start_row" "$n"
	start_row="$FANCY_FIT_TOP"

	local max_label=$((width - 4))
	[ "$max_label" -lt 1 ] && max_label=1

	local cursor_row=0
	local content_start=$start_row

	# ---- draw helpers ----
	__fancy_select_draw_row() {
		local i="$1" j
		local sc=(${FANCY_STYLE_SELECTION_COLOR[@]})
		local lc=(${FANCY_STYLE_LABEL_COLOR[@]})
		local label="${labels[$i]}"
		local check=" "
		[ "$selected" -eq "$i" ] && check="*"

		label=$(__fancy_select_truncate "$label" "$max_label")
		local pad=$((width - 4 - ${#label}))
		[ "$pad" -lt 0 ] && pad=0

		fancy_move "$((content_start+i))" "$start_col"

		if [ "$i" -eq "$cursor_row" ]; then
			[ ${#sc[@]} -ge 3 ] && fancy_bg "${sc[@]}"
			[ ${#lc[@]} -ge 3 ] && fancy_fg "${lc[@]}"
			echo -n "($check) $label"
			for ((j=0; j<pad; j++)); do echo -n " "; done
			fancy_reset
		else
			echo -n "("
			if [ "$selected" -eq "$i" ]; then
				[ ${#lc[@]} -ge 3 ] && fancy_fg "${lc[@]}"
				echo -n "$check"
				fancy_reset
			else
				echo -n "$check"
			fi
			echo -n ") $label"
			for ((j=0; j<pad; j++)); do echo -n " "; done
		fi
	}

	__fancy_select_draw_widget() {
		local i
		for ((i=0; i<n; i++)); do
			__fancy_select_draw_row "$i"
		done
	}

	# ---- initial draw ----
	__fancy_select_draw_widget

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
				__exit=1
				break
				;;
			up)
				if [ "$cursor_row" -gt 0 ]; then
					cursor_row=$((cursor_row-1))
					__fancy_select_draw_row "$cursor_row"
					__fancy_select_draw_row "$((cursor_row+1))"
				fi
				;;
			down)
				if [ "$cursor_row" -lt $((n-1)) ]; then
					cursor_row=$((cursor_row+1))
					__fancy_select_draw_row "$cursor_row"
					__fancy_select_draw_row "$((cursor_row-1))"
				fi
				;;
			" ")
				local old=$selected
				if [ "$selected" -eq "$cursor_row" ]; then
					selected=-1
				else
					selected=$cursor_row
				fi
				__fancy_select_draw_row "$cursor_row"
				[ "$old" -ge 0 ] && [ "$old" -ne "$cursor_row" ] && __fancy_select_draw_row "$old"
				;;
		esac
	done

	# ---- final draw: show the whole component once more without highlight ----
	cursor_row=-1
	__fancy_select_draw_widget

	# ---- build result ----
	local __result=""
	[ "$selected" -ge 0 ] && __result="${values[$selected]}"

	# ---- cleanup ----
	fancy_cleanup

	# Move cursor to the beginning of the line just after the last row. When
	# the list ends on the last line of the terminal a plain cursor move would
	# clamp to that line (leaving the cursor on the last row), so scroll first
	# to make room for the line below the list.
	fancy_fit_lines "$((content_start+n))" 1
	fancy_move "$((content_start+n))" 1

	if [ "$__exit" -eq 0 ]; then
		printf -v "$result_varname" "%s" "$__result"
		return 0
	fi
	return 1
}
