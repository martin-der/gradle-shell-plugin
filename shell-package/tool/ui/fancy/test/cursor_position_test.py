#!/usr/bin/env python3
"""Regression test for the spec's common termination behavior.

The PTY harness simulates a real terminal answering the widget's ESC [ 6 n
cursor query with ESC [ 5 ; 7 R, so the widget must:
  - draw its rows starting at the reported position (row 5, column 7)
  - after termination place the cursor at the beginning of the line (column 1)
    just after its last line

This path was previously broken: fancy_get_cursor returned 0-based
coordinates while the drawing code treats them as 1-based, so any widget that
did not start at the top row was drawn one row too high and following text
overwrote the component.
"""

import os, pty, time, select, sys, re


def read_available(master, timeout=0.02):
    """Read any pending PTY output for up to `timeout` seconds."""
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


def run_widget(component, keys, cursor="5;7"):
    """Run one widget in a PTY that answers the cursor query with `cursor`.

    Returns the decoded PTY output.
    """
    master, slave = pty.openpty()
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

    if component == "select":
        cmd = ('source ./select.sh && '
               'choices=$(printf "foo|Foo\\nbar|Bar") && '
               'fancy_select "$choices" result 30 ; '
               'echo "EXIT=$?" ; echo "VALUE=[$result]"\n')
    elif component == "checkbox":
        cmd = ('source ./checkbox.sh && '
               'choices=$(printf "foo|Foo\\nbar|Bar") && '
               'fancy_checkbox "$choices" result 30 ; '
               'echo "EXIT=$?" ; echo "VALUE=[$result]"\n')
    else:
        cmd = ('source ./input.sh && '
               'fancy_input result "Test" 30 "" ; '
               'echo "EXIT=$?" ; echo "VALUE=[$result]"\n')

    for ch in cmd.encode():
        os.write(master, bytes([ch]))
        time.sleep(0.01)

    acc = b""
    buf = b""
    replied = False
    drawn = False
    quiet_start = None
    end = time.time() + 8.0
    while time.time() < end:
        chunk = read_available(master, 0.02)
        if chunk:
            acc += chunk
            buf += chunk
            if not replied and b"\x1b[6n" in buf:
                os.write(master, b"\x1b[" + cursor.encode() + b"R")
                replied = True
            if not drawn and re.search(rb'\x1b\[[0-9]+;[0-9]+H', buf):
                drawn = True
            quiet_start = None
        elif drawn:
            if quiet_start is None:
                quiet_start = time.time()
            elif time.time() - quiet_start > 0.2:
                break
    if not drawn:
        os.write(master, b"exit\n")
        os.close(master)
        os.waitpid(pid, 0)
        raise RuntimeError("widget never drew")

    for key in keys:
        os.write(master, key)
        time.sleep(0.04)
    time.sleep(0.3)

    while True:
        r, w, e = select.select([master], [], [], 0.5)
        if not r:
            break
        try:
            chunk = os.read(master, 4096)
            if not chunk:
                break
            acc += chunk
        except Exception:
            break

    os.write(master, b"exit\n")
    os.close(master)
    os.waitpid(pid, 0)

    return acc.decode('utf-8', errors='replace')


def check(name, cond, stats, detail=""):
    stats["passed" if cond else "failed"] += 1
    status = "PASS" if cond else "FAIL"
    suffix = f" — {detail}" if detail else ""
    print(f"{status}: {name}{suffix}")


def main():
    if len(sys.argv) < 2:
        print("Usage: cursor_position_test.py <fancy_dir>", file=sys.stderr)
        sys.exit(1)

    stats = {"passed": 0, "failed": 0}
    SPACE = b" "
    ENTER = b"\r"

    # (component, keys, expected rows drawn at (5,7), termination move)
    cases = [
        # select: 2 rows at rows 5,6 -> move below to row 7, col 1
        ("select", [SPACE, ENTER], "7"),
        # checkbox (no title): 2 rows at rows 5,6 -> row 7, col 1
        ("checkbox", [SPACE, ENTER], "7"),
        # input: 3 lines (borders) at rows 5,6,7 -> row 8, col 1
        ("input", [b"hi", ENTER], "8"),
    ]

    for component, keys, term_row in cases:
        try:
            raw = run_widget(component, keys)
        except Exception as exc:
            check(f"{component}: harness", False, stats, str(exc))
            continue

        check(f"{component}: draws at reported position",
              f"\x1b[5;7H" in raw,
              stats, "first row must be ESC [ 5 ; 7 H")
        check(f"{component}: cursor placed below at column 1",
              f"\x1b[{term_row};1H" in raw,
              stats, f"expected ESC [ {term_row} ; 1 H")

    total = stats["passed"] + stats["failed"]
    print(f"\nPassed: {stats['passed']} / {total}")
    sys.exit(0 if stats["failed"] == 0 else 1)


if __name__ == "__main__":
    main()
