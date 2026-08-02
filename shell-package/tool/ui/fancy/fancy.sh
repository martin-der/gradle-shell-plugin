#!/usr/bin/env bash

test "x${FANCY_UI:-}" = "xFANCY-UI" && return 0
FANCY_UI=FANCY-UI

FANCY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "${FANCY_DIR}/../../dispenser/runtime/shell-util.sh" 2>/dev/null \
|| source shell-util || exit 1

# ---- Terminal State ----
FANCY_SAVED_STTY=""
FANCY_SAVED_CURSOR_STYLE=0
FANCY_OLD_TRAP_INT=""
FANCY_OLD_TRAP_TERM=""

# ---- Style defaults ----
FANCY_STYLE_BORDER=simple
FANCY_STYLE_BORDER_COLOR=(255 255 255)
FANCY_STYLE_LABEL_COLOR=(255 255 0)
FANCY_STYLE_SELECTION_COLOR=(75 75 75)
FANCY_STYLE_COUNTER_COLOR=(128 128 128)
FANCY_STYLE_INDICATOR="…"

fancy_set_style() {
	local key="$1" value="$2" v3="${3:-}" v4="${4:-}"
	case "$key" in
		border) FANCY_STYLE_BORDER="$value" ;;
		border_color) FANCY_STYLE_BORDER_COLOR=("$value" "$v3" "$v4") ;;
		label_color) FANCY_STYLE_LABEL_COLOR=("$value" "$v3" "$v4") ;;
		selection_color) FANCY_STYLE_SELECTION_COLOR=("$value" "$v3" "$v4") ;;
		counter_color) FANCY_STYLE_COUNTER_COLOR=("$value" "$v3" "$v4") ;;
		indicator) FANCY_STYLE_INDICATOR="$value" ;;
	esac
}

fancy_get_style() {
	local key="$1"
	case "$key" in
		border) echo "$FANCY_STYLE_BORDER" ;;
		border_color) echo "${FANCY_STYLE_BORDER_COLOR[@]}" ;;
		label_color) echo "${FANCY_STYLE_LABEL_COLOR[@]}" ;;
		selection_color) echo "${FANCY_STYLE_SELECTION_COLOR[@]}" ;;
		counter_color) echo "${FANCY_STYLE_COUNTER_COLOR[@]}" ;;
		indicator) echo "$FANCY_STYLE_INDICATOR" ;;
	esac
}

# ---- Terminal lifecycle ----

fancy_init() {
	FANCY_SAVED_STTY=$(stty -g 2>/dev/null || true)
	stty raw -echo 2>/dev/null || true
	FANCY_SAVED_CURSOR_STYLE=$(fancy_get_cursor_style)
	echo -en "\e[6 q"
	FANCY_OLD_TRAP_INT=$(trap -p INT 2>/dev/null || true)
	FANCY_OLD_TRAP_TERM=$(trap -p TERM 2>/dev/null || true)
}

fancy_cleanup() {
	[ -n "$FANCY_SAVED_STTY" ] && stty "$FANCY_SAVED_STTY" 2>/dev/null || true
	echo -en "\e[?25h\e[0m\e[${FANCY_SAVED_CURSOR_STYLE:-0} q"
	[ -n "$FANCY_OLD_TRAP_INT" ] && eval "$FANCY_OLD_TRAP_INT" || trap - INT
	[ -n "$FANCY_OLD_TRAP_TERM" ] && eval "$FANCY_OLD_TRAP_TERM" || trap - TERM
}

# ---- Cursor ----

fancy_get_cursor() {
	local resp char
	echo -en "\e[6n" >&2
	resp=""
	while IFS= read -r -N1 -t 0.05 char 2>/dev/null; do
		resp="$resp$char"
		[ "$char" = "R" ] && break
	done
	if [[ "$resp" =~ \[([0-9]+)\;([0-9]+)R ]]; then
		echo "${BASH_REMATCH[1]} ${BASH_REMATCH[2]}"
	else
		echo "1 1"
		return 1
	fi
}

# Query the current cursor style via DECRQSS (DCS $ q SP q ST).
# Returns the DECSCUSR parameter: 0=default 1=blink block 2=steady block
# 3=blink underline 4=steady underline 5=blink bar 6=steady bar.
# Falls back to 0 when the terminal does not answer.
fancy_get_cursor_style() {
	local resp char
	echo -en "\eP\$q q\e\\" >&2
	resp=""
	while IFS= read -r -N1 -t 0.1 char 2>/dev/null; do
		resp="$resp$char"
		[ "$char" = '\' ] && break
	done
	if [[ "$resp" =~ P1\$r([0-9]) ]]; then
		echo "${BASH_REMATCH[1]}"
		return 0
	fi
	echo "0"
	return 1
}

fancy_move() {
	echo -en "\e[${1};${2}H"
}

# @description Scroll the terminal so a widget of `count` lines whose top row
#              would be `top` is fully visible. When the widget extends past
#              the last line of the terminal, the screen is scrolled up by the
#              required amount (CSI n S), leaving room at the bottom.
# @arg $1 int  Row where the widget would start (1-based)
# @arg $2 int  Number of lines the widget needs
# @stdout The scroll escape sequence, if scrolling was needed. The adjusted
#         starting row is stored in FANCY_FIT_TOP (kept out of stdout so the
#         escape stays part of the draw stream).
fancy_fit_lines() {
	local top="$1" count="$2" size rows overflow
	FANCY_FIT_TOP="$top"
	size=$(stty size 2>/dev/null || true)
	rows="${size%% *}"
	[ -n "$rows" ] && [ "$rows" -gt 0 ] || return 0
	overflow=$(( top + count - 1 - rows ))
	if [ "$overflow" -gt 0 ]; then
		echo -en "\e[${overflow}S"
		top=$(( top - overflow ))
		[ "$top" -lt 1 ] && top=1
		FANCY_FIT_TOP="$top"
	fi
}

# ---- Line operations ----

fancy_clear_line() {
	echo -en "\e[2K\r"
}

fancy_clear_to_eol() {
	echo -en "\e[K"
}

# ---- Color ----

fancy_fg() {
	echo -en "\e[38;2;${1};${2};${3}m"
}

fancy_bg() {
	echo -en "\e[48;2;${1};${2};${3}m"
}

fancy_reset() {
	echo -en "\e[0m"
}

# ---- Border characters ----

fancy_get_border_chars() {
	case "$1" in
		double)   echo "╔ ╗ ╚ ╝ ═ ║" ;;
		rounded)  echo "╭ ╮ ╰ ╯ ─ │" ;;
		simple|*) echo "┌ ┐ └ ┘ ─ │" ;;
	esac
}

# ---- Box drawing ----

fancy_box() {
	local top="$1" left="$2" height="$3" width="$4" label="${5:-}"
	local chars=($(fancy_get_border_chars "$FANCY_STYLE_BORDER"))
	local tl="${chars[0]}" tr="${chars[1]}" bl="${chars[2]}" br="${chars[3]}" h="${chars[4]}" v="${chars[5]}"
	local i len

	[ ${#FANCY_STYLE_BORDER_COLOR[@]} -ge 3 ] && fancy_fg "${FANCY_STYLE_BORDER_COLOR[@]}"

	fancy_move "$top" "$left"
	echo -n "$tl"
	if [ -n "$label" ]; then
		echo -n "$h $label $h"
		len=$((4 + ${#label}))
	else
		echo -n "$h"
		len=1
	fi
	for ((i=len; i<width-2; i++)); do echo -n "$h"; done
	echo "$tr"

	for ((i=1; i<height-1; i++)); do
		fancy_move "$((top+i))" "$left"
		echo -n "$v"
		fancy_move "$((top+i))" "$((left+width-1))"
		echo -n "$v"
	done

	fancy_move "$((top+height-1))" "$left"
	echo -n "$bl"
	for ((i=0; i<width-2; i++)); do echo -n "$h"; done
	echo "$br"

	fancy_reset
}

# ---- Key reading ----

fancy_read_key() {
	local key hex seq=""

	if ! IFS= read -r -N1 key 2>/dev/null >&2; then
		return 1
	fi
	hex=$(echo -n "$key" | od -An -tx1 | tr -d ' \n')

	case "$hex" in
		7f) echo "backspace"; return 0 ;;
		0a|0d) echo "enter"; return 0 ;;
		09) echo "tab"; return 0 ;;
		17) echo "ctrl_w"; return 0 ;;
		1b)
			seq="$hex"
			while IFS= read -r -N1 -t 0.005 key 2>/dev/null >&2; do
				hex=$(echo -n "$key" | od -An -tx1 | tr -d ' \n')
				seq="${seq}${hex}"
				case "$seq" in
					1b5b41) echo "up"; return 0 ;;
					1b5b42) echo "down"; return 0 ;;
					1b5b43) echo "right"; return 0 ;;
					1b5b44) echo "left"; return 0 ;;
					1b5b48|1b5b5b48|1b5b317e) echo "home"; return 0 ;;
					1b5b46|1b5b5b46|1b5b347e) echo "end"; return 0 ;;
					1b5b337e) echo "delete"; return 0 ;;
					1b5b333b357e) echo "ctrl_backspace"; return 0 ;;
					1b5b313b3544) echo "ctrl_left"; return 0 ;;
					1b5b313b3543) echo "ctrl_right"; return 0 ;;
				esac
			done
			echo "esc"
			return 0
			;;
		*)
			echo "$key"
			return 0
			;;
	esac
}
