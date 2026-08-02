#!/usr/bin/env bash
# Chained demo of the fancy components: fancy_input -> fancy_checkbox -> fancy_select.
#
# Each component stays on screen in its passive state once it terminates and
# the next one draws below it, so at the end the whole chain is visible with
# the cursor on the line after the last component.
#
# Run interactively from the fancy dir:
#   bash test/demo/demo.sh

set -uo pipefail

DEMO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

source "${DEMO_DIR}/input.sh"
source "${DEMO_DIR}/checkbox.sh"
source "${DEMO_DIR}/select.sh"

width=40

printf 'Fancy components demo: input, checkbox, select, chained.\n'

# 1) Name (fancy_input)
name=""
fancy_input name "Your name" "$width" || true

# 2) Hobbies (fancy_checkbox, multi-select)
hobbies=""
hobby_choices=$(printf 'read|Reading books\nsport|Sports & fitness\ncook|Cooking gourmet meals')
fancy_checkbox "$hobby_choices" hobbies "$width" "Hobbies" || true

# 3) Favorite season (fancy_select, single-select)
season=""
season_choices=$(printf 'autumn|Autumn : Nature gets a lot colors\nwinter|Winter : Perfect time for skiing\nsummer|Summer : Warm with long vacation\nspring|Spring : Trees grows leaves back')
fancy_select "$season_choices" season "$width" || true

printf '\n--- Result ---\n'
printf 'Name:    %s\n' "${name:-<none>}"
printf 'Hobbies: %s\n' "${hobbies:-<none>}"
printf 'Season:  %s\n' "${season:-<none>}"
