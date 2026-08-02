#!/usr/bin/env python3
"""Snapshot rendering tests for the fancy widgets.

Each widget is run in a PTY that answers the ESC [ 6 n cursor query with
ESC [ 5 ; 1 R (a real terminal answering position row 5, column 1). The
captured escape stream is then replayed through a tiny screen-grid renderer
that tracks cursor moves, line erases and 24-bit SGR colors, exactly like a
terminal would draw it. The final widget area is compared against a golden
text snapshot:

    ┌─ Your name ────────────────┐
    │ hello                      │
    └────────────────────────────┘

    colors (b=border l=label c=counter s=selection .=default)
    bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
    b............................b
    bbbbbbbbbbbbbbbbbbbbbbbbbbbbbb

The color grid is one character per visible cell and records which palette
color was active when the cell was last drawn. Snapshots are grouped by
component and use case under snapshots/<component>/<use-case>/<step>.txt;
each scenario script is a list of {"keys": ...} and {"snapshot": label}
steps so a single interaction is captured at several stages (e.g. the input
"nominal" use case snapshots the box empty, after typing, after deleting a
word, and after confirming). Regenerate the snapshots with:

    python3 snapshot_test.py <fancy_dir> --update
"""

import difflib
import fcntl
import os
import pty
import re
import select
import struct
import sys
import termios
import time

SELECTION = (75, 75, 75)
LABEL = (255, 255, 0)
BORDER = (255, 255, 255)
COUNTER = (128, 128, 128)


# --------------------------------------------------------------------------
# Screen-grid renderer
# --------------------------------------------------------------------------

class Cell:
    __slots__ = ("ch", "fg", "bg")

    def __init__(self):
        self.ch = " "
        self.fg = None
        self.bg = None


def _utf8_len(b):
    if b < 0x80:
        return 1
    if 0xC0 <= b < 0xE0:
        return 2
    if 0xE0 <= b < 0xF0:
        return 3
    return 4


class Screen:
    def __init__(self, rows=40, cols=200):
        self.rows = rows
        self.cols = cols
        self.grid = [[Cell() for _ in range(cols)] for _ in range(rows)]
        self.r = 0
        self.c = 0
        self.fg = None
        self.bg = None

    def _clamp(self):
        self.r = max(0, min(self.r, self.rows - 1))
        self.c = max(0, min(self.c, self.cols - 1))

    def _write(self, ch):
        self._clamp()
        cell = self.grid[self.r][self.c]
        cell.ch = ch
        cell.fg = self.fg
        cell.bg = self.bg
        self.c += 1

    def feed(self, data):
        i = 0
        n = len(data)
        while i < n:
            b = data[i]
            if b == 0x0D:
                self.c = 0
                i += 1
            elif b == 0x0A:
                if self.r >= self.rows - 1:
                    # newline on the last row scrolls the viewport up one line
                    del self.grid[0]
                    self.grid.append([Cell() for _ in range(self.cols)])
                else:
                    self.r += 1
                i += 1
            elif b == 0x1B:
                i = self._esc(data, i)
            else:
                ln = _utf8_len(b)
                chunk = data[i:i + ln]
                self._write(chunk.decode("utf-8", errors="replace"))
                i += ln

    def _esc(self, data, i):
        n = len(data)
        if i + 1 >= n:
            return n
        nxt = data[i + 1]
        if nxt == 0x5B:  # ESC [ (CSI)
            j = i + 2
            while j < n and not (0x40 <= data[j] <= 0x7E):
                j += 1
            if j >= n:
                return n
            params = data[i + 2:j].decode("ascii", errors="ignore")
            self._csi(params, chr(data[j]))
            return j + 1
        if nxt in (0x50, 0x5D):  # DCS / OSC: skip to ST or BEL
            j = i + 2
            while j < n:
                if data[j] == 0x07:
                    return j + 1
                if data[j] == 0x1B and j + 1 < n and data[j + 1] == 0x5C:
                    return j + 2
                j += 1
            return n
        return i + 2  # ESC + one char

    def _csi(self, params, final):
        p = params.lstrip("?").split(";")
        if final in ("H", "f"):
            r = int(p[0]) if p[0] else 1
            c = int(p[1]) if len(p) > 1 and p[1] else 1
            self.r = r - 1
            self.c = c - 1
            self._clamp()
        elif final == "K":
            mode = int(p[0]) if p[0] else 0
            cols = range(self.c, self.cols) if mode != 2 else range(self.cols)
            for col in cols:
                cell = self.grid[self.r][col]
                cell.ch = " "
                cell.fg = None
                cell.bg = None
        elif final == "S":  # scroll up n lines: shift content up, blank at bottom
            n = int(p[0]) if p[0] else 1
            del self.grid[:n]
            for _ in range(n):
                self.grid.append([Cell() for _ in range(self.cols)])
        elif final == "T":  # scroll down n lines: shift content down, blank at top
            n = int(p[0]) if p[0] else 1
            for _ in range(n):
                self.grid.pop()
            for _ in range(n):
                self.grid.insert(0, [Cell() for _ in range(self.cols)])
        elif final == "m":
            self._sgr(p)

    def _sgr(self, p):
        i = 0
        while i < len(p):
            try:
                code = int(p[i])
            except ValueError:
                i += 1
                continue
            if code == 0:
                self.fg = None
                self.bg = None
            elif code in (38, 48) and i + 4 < len(p) and p[i + 1] == "2":
                try:
                    rgb = (int(p[i + 2]), int(p[i + 3]), int(p[i + 4]))
                except ValueError:
                    i += 1
                    continue
                if code == 38:
                    self.fg = rgb
                else:
                    self.bg = rgb
                i += 4
            i += 1


# --------------------------------------------------------------------------
# PTY harness
# --------------------------------------------------------------------------

def read_available(master, timeout=0.02):
    out = b""
    end = time.time() + timeout
    while time.time() < end:
        r, w, e = select.select([master], [], [], 0.01)
        if not r:
            continue
        try:
            d = os.read(master, 4096)
            if not d:
                break
            out += d
        except Exception:
            break
    return out


def _expand_keys(keys):
    """Expand a step's keys into a list of byte sequences to send."""
    if isinstance(keys, str):
        return [keys[i].encode("utf-8") for i in range(len(keys))]
    return keys


def run_scenario(cmd, steps, cursor="5;1"):
    """Run `cmd` in a PTY (winsize 24x80) that answers ESC [ 6 n with
    ESC [ <cursor> R, replay the `steps` (either {"keys": ...} or
    {"snapshot": label}) and return the list of (label, raw_bytes) captures
    taken at each snapshot step."""
    master, slave = pty.openpty()
    fcntl.ioctl(slave, termios.TIOCSWINSZ, struct.pack("HHHH", 24, 80, 0, 0))
    pid = os.fork()
    if pid == 0:
        for fd in (0, 1, 2):
            os.dup2(slave, fd)
        os.close(master)
        os.close(slave)
        os.chdir(sys.argv[1])
        os.execvp("bash", ["bash"])
        os._exit(1)

    os.close(slave)
    time.sleep(0.3)

    for ch in cmd.encode():
        os.write(master, bytes([ch]))
        time.sleep(0.008)

    acc = b""
    seen = False
    quiet_start = None
    end = time.time() + 8.0
    while time.time() < end:
        chunk = read_available(master, 0.02)
        if chunk:
            acc += chunk
            if not seen and b"\x1b[6n" in acc:
                os.write(master, b"\x1b[" + cursor.encode() + b"R")
                seen = True
            quiet_start = None
        elif seen:
            if quiet_start is None:
                quiet_start = time.time()
            elif time.time() - quiet_start > 0.2:
                break
    if not seen:
        os.write(master, b"exit\n")
        os.close(master)
        os.waitpid(pid, 0)
        raise RuntimeError("widget never queried the cursor position")

    def settle(deadline=4.0):
        nonlocal acc
        quiet_start = None
        end = time.time() + deadline
        while time.time() < end:
            chunk = read_available(master, 0.02)
            if chunk:
                acc += chunk
                quiet_start = None
            else:
                if quiet_start is None:
                    quiet_start = time.time()
                elif time.time() - quiet_start > 0.2:
                    return

    results = []
    for step in steps:
        if "snapshot" in step:
            results.append((step["snapshot"], acc))
        else:
            for key in _expand_keys(step["keys"]):
                os.write(master, key)
                time.sleep(0.04)
            settle()

    os.write(master, b"\r")
    time.sleep(0.3)
    os.write(master, b"exit\n")
    os.close(master)
    os.waitpid(pid, 0)

    return results


# --------------------------------------------------------------------------
# Snapshots
# --------------------------------------------------------------------------

def color_char(cell):
    if cell.bg == SELECTION:
        return "s"
    if cell.fg == LABEL:
        return "l"
    if cell.fg == BORDER:
        return "b"
    if cell.fg == COUNTER:
        return "c"
    return "."


def render_snapshot(raw, width, height, start_row=5):
    """Render the widget box: text rows (right-trimmed) and a full-width
    per-cell color grid. The screen matches the PTY winsize (24x80) so that
    auto-scrolling behaviour at the bottom edge is modelled too."""
    screen = Screen(rows=24, cols=80)
    screen.feed(raw)
    text_rows = []
    color_rows = []
    for r in range(start_row - 1, start_row - 1 + height):
        cells = screen.grid[r][0:width]
        text_rows.append("".join(cell.ch for cell in cells).rstrip())
        color_rows.append("".join(color_char(cell) for cell in cells))
    return text_rows, color_rows


def snapshot_text(text_rows, color_rows):
    lines = list(text_rows)
    lines.append("")
    lines.append("colors (b=border l=label c=counter s=selection .=default)")
    lines.extend(color_rows)
    return "\n".join(lines) + "\n"


def parse_snapshot(path):
    with open(path, encoding="utf-8") as f:
        lines = f.read().splitlines()
    idx = None
    for i, line in enumerate(lines):
        if line.startswith("colors"):
            idx = i
            break
    if idx is None:
        raise ValueError(f"snapshot {path} is missing the colors section")
    text_rows = [line for line in lines[:idx] if line.strip() != ""]
    return text_rows, lines[idx + 1:]


def print_diff(expected, actual):
    diff = list(difflib.unified_diff(
        expected.splitlines(), actual.splitlines(),
        fromfile="snapshot", tofile="actual", lineterm=""))
    for line in diff[:40]:
        print("  " + line)


# --------------------------------------------------------------------------
# Scenarios — each one lives under snapshots/<name>/ and takes a snapshot at
# every {"snapshot": label} step of the scripted interaction.
# --------------------------------------------------------------------------

LEFT = b"\x1b[D"
RIGHT = b"\x1b[C"
UP = b"\x1b[A"
DOWN = b"\x1b[B"
SPACE = b" "
ENTER = b"\r"
CTRL_W = b"\x17"

SCENARIOS = [
    {
        # Empty input, type "hello world", append " !", move the cursor onto
        # the "d" of "world" and delete the word with Ctrl+W, then confirm.
        "name": "input/nominal",
        "cmd": 'source ./input.sh && fancy_input result "Your name" 30 "" ; echo "EXIT=$?"\n',
        "width": 30,
        "height": 3,
        "steps": [
            {"snapshot": "01-empty"},
            {"keys": "hello world"},
            {"snapshot": "02-add-hello-world"},
            {"keys": " !"},
            {"snapshot": "03-add-exclamation"},
            {"keys": [LEFT, LEFT, CTRL_W]},
            {"snapshot": "04-delete-world"},
            {"keys": [ENTER]},
            {"snapshot": "05-after-enter"},
        ],
    },
    {
        # Input already holding initial text; append more text, then confirm.
        "name": "input/filled",
        "cmd": ('source ./input.sh && result=\'John Doe\' && '
                'fancy_input result "Your name" 30 "" ; echo "EXIT=$?"\n'),
        "width": 30,
        "height": 3,
        "steps": [
            {"snapshot": "01-initial"},
            {"keys": " said"},
            {"snapshot": "02-add-text"},
            {"keys": [ENTER]},
            {"snapshot": "03-after-enter"},
        ],
    },
    {
        # Select with three choices: select the first, move down, confirm.
        "name": "select/nominal",
        "cmd": ('source ./select.sh && '
                'choices=$(printf "sun|Sun\\nmoon|Moon\\nstar|Star") && '
                'fancy_select "$choices" result 26 ; echo "EXIT=$?"\n'),
        "width": 26,
        "height": 3,
        "steps": [
            {"snapshot": "01-initial"},
            {"keys": [SPACE]},
            {"snapshot": "02-select-sun"},
            {"keys": [DOWN]},
            {"snapshot": "03-move-down"},
            {"keys": [ENTER]},
            {"snapshot": "04-after-enter"},
        ],
    },
    {
        # Select starting from an initial value already stored in the result
        # variable; move the highlight and confirm.
        "name": "select/initial",
        "cmd": ('source ./select.sh && result=\'moon\' && '
                'choices=$(printf "sun|Sun\\nmoon|Moon") && '
                'fancy_select "$choices" result 26 ; echo "EXIT=$?"\n'),
        "width": 26,
        "height": 2,
        "steps": [
            {"snapshot": "01-initial"},
            {"keys": [DOWN]},
            {"snapshot": "02-move-down"},
            {"keys": [ENTER]},
            {"snapshot": "03-after-enter"},
        ],
    },
    {
        # Checkbox with three choices: move down and tick Beta, move up and
        # tick Alpha, confirm.
        "name": "checkbox/nominal",
        "cmd": ('source ./checkbox.sh && '
                'choices=$(printf "alpha|Alpha\\nbeta|Beta\\ngamma|Gamma") && '
                'fancy_checkbox "$choices" result 26 "Pick one" ; echo "EXIT=$?"\n'),
        "width": 26,
        "height": 4,
        "steps": [
            {"snapshot": "01-initial"},
            {"keys": [DOWN, SPACE]},
            {"snapshot": "02-select-beta"},
            {"keys": [UP, SPACE]},
            {"snapshot": "03-select-alpha"},
            {"keys": [ENTER]},
            {"snapshot": "04-after-enter"},
        ],
    },
    {
        # Checkbox starting with Alpha and Gamma already ticked (initial value
        # in the result variable); move the highlight and confirm.
        "name": "checkbox/filled",
        "cmd": ('source ./checkbox.sh && result=\'alpha,gamma\' && '
                'choices=$(printf "alpha|Alpha\\nbeta|Beta\\ngamma|Gamma") && '
                'fancy_checkbox "$choices" result 26 "Pick one" ; echo "EXIT=$?"\n'),
        "width": 26,
        "height": 4,
        "steps": [
            {"snapshot": "01-initial"},
            {"keys": [DOWN, DOWN]},
            {"snapshot": "02-move-down"},
            {"keys": [ENTER]},
            {"snapshot": "03-after-enter"},
        ],
    },
    {
        # Widget starting near the bottom of a 24-line terminal: the terminal
        # must scroll up one line so the full box stays visible.
        "name": "input/near-bottom",
        "cmd": 'source ./input.sh && fancy_input result "Your name" 30 "" ; echo "EXIT=$?"\n',
        "cursor": "23;1",
        "top": 22,
        "width": 30,
        "height": 3,
        "expect_scroll": 1,
        "steps": [
            {"snapshot": "01-fits-after-scroll"},
        ],
    },
    {
        # Input at the very bottom of the terminal must not grow by one line
        # per keystroke: redrawing the content and the bottom border must not
        # push a newline on the last row (that would auto-scroll the terminal
        # and leave the previous content behind). The box stays fixed at 3
        # rows while text is typed.
        "name": "input/near-bottom-typing",
        "cmd": 'source ./input.sh && fancy_input result "Your name" 30 "" ; echo "EXIT=$?"\n',
        "cursor": "23;1",
        "top": 22,
        "width": 30,
        "height": 3,
        "expect_scroll": 1,
        "steps": [
            {"snapshot": "01-initial"},
            {"keys": "hel"},
            {"snapshot": "02-after-hel"},
            {"keys": "lo"},
            {"snapshot": "03-after-hello"},
        ],
    },
    {
        # 3-option select starting at row 23 of a 24-line terminal.
        "name": "select/near-bottom",
        "cmd": ('source ./select.sh && '
                'choices=$(printf "sun|Sun\\nmoon|Moon\\nstar|Star") && '
                'fancy_select "$choices" result 26 ; echo "EXIT=$?"\n'),
        "cursor": "23;1",
        "top": 22,
        "width": 26,
        "height": 3,
        "expect_scroll": 1,
        "steps": [
            {"snapshot": "01-fits-after-scroll"},
        ],
    },
    {
        # Input ending on the last line of the terminal: on exit it must
        # scroll the terminal up one line so the cursor lands on the line
        # below the box (where the next component starts), instead of staying
        # clamped on the bottom border where the next component would
        # overwrite it. Assert the stream contains both the initial fit scroll
        # and the exit scroll (min_scrolls=2).
        "name": "input/near-bottom-exit",
        "cmd": 'source ./input.sh && fancy_input result "Your name" 30 "" ; echo "EXIT=$?"\n',
        "cursor": "23;1",
        "top": 20,
        "width": 30,
        "height": 3,
        "expect_scroll": 1,
        "min_scrolls": 2,
        "steps": [
            {"keys": [ENTER]},
            {"snapshot": "01-after-enter"},
        ],
    },
    {
        # Select ending on the last line of the terminal: same exit-scroll
        # behaviour as the input widget.
        "name": "select/near-bottom-exit",
        "cmd": ('source ./select.sh && '
                'choices=$(printf "sun|Sun\\nmoon|Moon\\nstar|Star") && '
                'fancy_select "$choices" result 26 ; echo "EXIT=$?"\n'),
        "cursor": "23;1",
        "top": 20,
        "width": 26,
        "height": 3,
        "expect_scroll": 1,
        "min_scrolls": 2,
        "steps": [
            {"keys": [ENTER]},
            {"snapshot": "01-after-enter"},
        ],
    },
    {
        # Checkbox ending on the last line of the terminal: same exit-scroll
        # behaviour as the input widget.
        "name": "checkbox/near-bottom-exit",
        "cmd": ('source ./checkbox.sh && '
                'choices=$(printf "alpha|Alpha\\nbeta|Beta\\ngamma|Gamma") && '
                'fancy_checkbox "$choices" result 26 "Pick one" ; echo "EXIT=$?"\n'),
        "cursor": "22;1",
        "top": 19,
        "width": 26,
        "height": 4,
        "expect_scroll": 1,
        "min_scrolls": 2,
        "steps": [
            {"keys": [ENTER]},
            {"snapshot": "01-after-enter"},
        ],
    },
]


def main():
    if len(sys.argv) < 2:
        print("Usage: snapshot_test.py <fancy_dir> [name_filter] [--update]",
              file=sys.stderr)
        sys.exit(1)

    args = sys.argv[2:]
    update = "--update" in args
    name_filter = next((a for a in args if not a.startswith("--")), "")

    snap_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "snapshots")
    os.makedirs(snap_dir, exist_ok=True)

    stats = {"passed": 0, "failed": 0}
    for scenario in SCENARIOS:
        name = scenario["name"]
        if name_filter and name_filter not in name:
            continue
        try:
            snapshots = run_scenario(scenario["cmd"], scenario["steps"],
                                     scenario.get("cursor", "5;1"))
        except Exception as exc:
            stats["failed"] += 1
            print(f"FAIL: {name} — harness error: {exc}")
            continue

        for label, raw in snapshots:
            full = f"{name}/{label}"

            if scenario.get("expect_scroll"):
                seq = f"\x1b[{scenario['expect_scroll']}S".encode()
                if seq not in raw:
                    stats["failed"] += 1
                    print(f"FAIL: {full} — expected scroll ESC [ {scenario['expect_scroll']} S")
                    continue

            if scenario.get("min_scrolls"):
                seq = b"\x1b[1S"
                if raw.count(seq) < scenario["min_scrolls"]:
                    stats["failed"] += 1
                    print(f"FAIL: {full} — expected at least {scenario['min_scrolls']} "
                          "scrolls of one line in the stream (widget start and exit)")
                    continue

            text_rows, color_rows = render_snapshot(
                raw, scenario["width"], scenario["height"],
                scenario.get("top", 5))
            expected = snapshot_text(text_rows, color_rows)
            path = os.path.join(snap_dir, name, label + ".txt")

            if update or not os.path.exists(path):
                os.makedirs(os.path.dirname(path), exist_ok=True)
                with open(path, "w", encoding="utf-8") as f:
                    f.write(expected)
                print(f"UPDATE: {full} — wrote {os.path.relpath(path)}")
                if not update:
                    stats["failed"] += 1
                continue

            with open(path, encoding="utf-8") as f:
                golden = f.read()
            if golden == expected:
                stats["passed"] += 1
                print(f"PASS: {full}")
            else:
                stats["failed"] += 1
                print(f"FAIL: {full} — rendering differs from snapshot")
                print_diff(golden, expected)

    total = stats["passed"] + stats["failed"]
    print(f"\nPassed: {stats['passed']} / {total}")
    sys.exit(0 if stats["failed"] == 0 else 1)


if __name__ == "__main__":
    main()
