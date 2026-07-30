#!/usr/bin/env bash

FANCY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "${FANCY_DIR}/fancy.sh"
source "${FANCY_DIR}/test/test_helper.sh"

E=$'\e'
strip_ansi() { sed 's/\x1b\[[0-9;]*[a-zA-Z]//g'; }

echo "=== fancy_get_border_chars ==="

chars=$(fancy_get_border_chars simple)
assert_eq "simple style count" 6 $(echo "$chars" | wc -w)
assert_contains "simple tl" "$chars" "┌"
assert_contains "simple tr" "$chars" "┐"
assert_contains "simple bl" "$chars" "└"
assert_contains "simple br" "$chars" "┘"
assert_contains "simple h"  "$chars" "─"
assert_contains "simple v"  "$chars" "│"

chars=$(fancy_get_border_chars double)
assert_eq "double style count" 6 $(echo "$chars" | wc -w)
assert_contains "double tl" "$chars" "╔"
assert_contains "double bl" "$chars" "╚"
assert_contains "double h"  "$chars" "═"
assert_contains "double v"  "$chars" "║"

chars=$(fancy_get_border_chars rounded)
assert_eq "rounded style count" 6 $(echo "$chars" | wc -w)
assert_contains "rounded tl" "$chars" "╭"
assert_contains "rounded tr" "$chars" "╮"
assert_contains "rounded bl" "$chars" "╰"
assert_contains "rounded br" "$chars" "╯"

chars=$(fancy_get_border_chars unknown)
assert_eq "unknown defaults to simple" 6 $(echo "$chars" | wc -w)
assert_contains "fallback tl" "$chars" "┌"

# Default style is simple
chars=$(fancy_get_border_chars)
assert_eq "no arg defaults to simple" 6 $(echo "$chars" | wc -w)
assert_contains "default tl" "$chars" "┌"

echo "=== fancy_set_style / fancy_get_style ==="

fancy_set_style border double
assert_eq "set/get border" "double" "$(fancy_get_style border)"

fancy_set_style border_color 10 20 30
assert_eq "set/get border_color" "10 20 30" "$(fancy_get_style border_color)"

fancy_set_style label_color 100 200 250
assert_eq "set/get label_color" "100 200 250" "$(fancy_get_style label_color)"

fancy_set_style selection_color 70 80 90
assert_eq "set/get selection_color" "70 80 90" "$(fancy_get_style selection_color)"

fancy_set_style counter_color 50 60 70
assert_eq "set/get counter_color" "50 60 70" "$(fancy_get_style counter_color)"

fancy_set_style indicator ">>>"
assert_eq "set/get indicator" ">>>" "$(fancy_get_style indicator)"

# Reset for subsequent tests
fancy_set_style border simple
fancy_set_style border_color 255 255 255
fancy_set_style label_color 255 255 0
fancy_set_style selection_color 75 75 75
fancy_set_style counter_color 128 128 128
fancy_set_style indicator "…"

echo "=== fancy_fg ==="

out=$(fancy_fg 100 150 200)
assert_eq "fancy_fg rgb" "${E}[38;2;100;150;200m" "$out"

echo "=== fancy_bg ==="

out=$(fancy_bg 10 20 30)
assert_eq "fancy_bg rgb" "${E}[48;2;10;20;30m" "$out"

echo "=== fancy_reset ==="

out=$(fancy_reset)
assert_eq "fancy_reset" "${E}[0m" "$out"

echo "=== fancy_move ==="

out=$(fancy_move 5 10)
assert_eq "fancy_move" "${E}[5;10H" "$out"

out=$(fancy_move 1 1)
assert_eq "fancy_move (1,1)" "${E}[1;1H" "$out"

echo "=== fancy_clear_line ==="

out=$(fancy_clear_line)
CR=$'\r'
assert_eq "fancy_clear_line" "${E}[2K${CR}" "$out"

echo "=== fancy_clear_to_eol ==="

out=$(fancy_clear_to_eol)
assert_eq "fancy_clear_to_eol" "${E}[K" "$out"

echo "=== fancy_box ==="

out=$(fancy_box 1 1 3 10)
# fancy_box uses cursor moves, not newlines between lines
# In $() capture, only newlines from echo create line boundaries
# Top line ends with \n, mid+bottom line ends with \n, reset has no \n

# Count corner characters in output
all_corners=$(echo "$out" | grep -o '[┌┐└┘]' | wc -l)
assert_eq "box all 4 corner chars present" 4 $all_corners

visible=$(echo "$out" | strip_ansi)
top=$(echo "$visible" | sed -n '1p')
assert_eq "box top visible length" 10 ${#top}
assert_contains "box top left"  "$top" "┌"
assert_contains "box top right" "$top" "┐"

# Box with label
out=$(fancy_box 1 1 3 20 "TestLabel")
visible=$(echo "$out" | strip_ansi)
top=$(echo "$visible" | sed -n '1p')
assert_eq "box with label top length" 20 ${#top}
assert_contains "box label in top" "$top" "TestLabel"

echo "=== fancy_get_cursor_style ==="

# No terminal response -> falls back to 0
style=$(fancy_get_cursor_style 2>/dev/null </dev/null)
assert_eq "no response falls back to 0" "0" "$style"

# Simulated DECRQSS reply (steady block)
resp=$(printf '\eP1$r2 q\e\\' | fancy_get_cursor_style 2>/dev/null)
assert_eq "parse steady block reply" "2" "$resp"

# Simulated DECRQSS reply (steady bar)
resp=$(printf '\eP1$r6 q\e\\' | fancy_get_cursor_style 2>/dev/null)
assert_eq "parse steady bar reply" "6" "$resp"

# Simulated invalid reply -> falls back to 0
resp=$(printf '\eP0$r\e\\' | fancy_get_cursor_style 2>/dev/null)
assert_eq "invalid reply falls back to 0" "0" "$resp"

echo "=== fancy_init / fancy_cleanup ==="

# These need stty; verify they don't crash in a non-TTY
assert_success "fancy_init returns 0" fancy_init 2>/dev/null >/dev/null
assert_success "fancy_cleanup returns 0" fancy_cleanup 2>/dev/null >/dev/null

# Cursor style: bar cursor set on init, restored on cleanup
out=$(fancy_init 2>/dev/null </dev/null)
assert_contains "init sets bar cursor" "$out" "${E}[6 q"

# When a style was saved, cleanup reapplies it instead of the default
FANCY_SAVED_CURSOR_STYLE=2
out=$(fancy_cleanup 2>/dev/null)
assert_contains "cleanup reapplies saved style" "$out" "${E}[2 q"
assert_contains "cleanup shows cursor" "$out" "${E}[?25h"

report
