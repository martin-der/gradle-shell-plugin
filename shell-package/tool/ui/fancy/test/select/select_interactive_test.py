#!/usr/bin/env python3
"""Interactive PTY tests for fancy_select."""

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


def wait_for_draw(master, deadline=8.0):
    """Wait until the widget has emitted its initial draw and then gone quiet.

    The widget draws all rows right before it blocks in fancy_read_key, so a
    draw marker followed by a short quiet period means keys we send next will
    be consumed by the widget instead of by fancy_get_cursor's blind read
    (which otherwise makes the tests timing-flaky under load).
    """
    acc = b""
    drawn = False
    quiet_start = None
    end = time.time() + deadline
    while time.time() < end:
        chunk = read_available(master, 0.02)
        if chunk:
            acc += chunk
            if not drawn and re.search(rb'\x1b\[[0-9]+;[0-9]+H\(', acc):
                drawn = True
            quiet_start = None
        elif drawn:
            if quiet_start is None:
                quiet_start = time.time()
            elif time.time() - quiet_start > 0.15:
                return acc
    if not drawn:
        raise RuntimeError("widget initial draw never appeared")
    raise RuntimeError("widget output never went quiet after initial draw")


def run_test(keys, setup_cmds="", width=30, first_arg="choices"):
    """Run fancy_select in a PTY, send each key atomically, return
    (exit_code, var_value, decoded_output).

    keys is a list of byte strings; each is written in a single write so
    escape sequences (arrows, etc.) arrive within fancy_read_key's timeout.
    first_arg is passed as the choices argument to fancy_select.
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

    cmd = (
        'source ./select.sh && '
        + setup_cmds
        + 'fancy_select ' + first_arg + ' result ' + str(width) + ' ; '
        + 'echo "EXIT=$?" ; '
        + 'echo "VALUE=[$result]"\n'
    )

    for ch in cmd.encode():
        os.write(master, bytes([ch]))
        time.sleep(0.01)

    acc = wait_for_draw(master)

    for key in keys:
        os.write(master, key)
        time.sleep(0.04)

    time.sleep(0.4)

    output = acc
    while True:
        r, w, e = select.select([master], [], [], 0.5)
        if not r:
            break
        try:
            chunk = os.read(master, 4096)
            if not chunk:
                break
            output += chunk
        except:
            break

    os.write(master, b"exit\n")
    os.close(master)
    os.waitpid(pid, 0)

    decoded = output.decode('utf-8', errors='replace')
    clean = re.sub(r'\x1b\].*?\x07', '', decoded)
    clean = re.sub(r'\x1b\[[?0-9; ]*[a-zA-Z]', '', clean)

    # The terminal echoes the typed command (which contains the literal
    # "EXIT=$?" / "VALUE=[$result]"), and the widget's cursor moves mean the
    # echoed markers can land mid-line. Take the LAST real match.
    ms = list(re.finditer(r'EXIT=(\d+)', clean))
    exit_code = ms[-1].group(1) if ms else ""
    ms = list(re.finditer(r'VALUE=\[([^\]]*)\]', clean))
    value = ms[-1].group(1) if ms else ""

    return exit_code, value, decoded


def check(name, expected_exit, expected_value, exit_code, value, raw, stats,
          expected_out=None):
    ok = exit_code == expected_exit and value == expected_value
    if ok and expected_out is not None:
        ok = expected_out in raw
    stats["passed" if ok else "failed"] += 1
    status = "PASS" if ok else "FAIL"
    print(f"{status}: {name} — exit=({exit_code}) value='{value}' (expected exit={expected_exit} value='{expected_value}')")
    if not ok:
        lines = raw.split('\n')
        for i, line in enumerate(lines):
            clean = re.sub(r'\x1b\[[?0-9; ]*[a-zA-Z]', '', line)
            clean = re.sub(r'\x1b\].*?\x07', '', clean)
            clean = clean.strip().strip('\r')
            if clean:
                print(f"  [{i}] {clean[:120]}")


def main():
    if len(sys.argv) < 2:
        print("Usage: select_interactive_test.py <fancy_dir> [test_filter]", file=sys.stderr)
        sys.exit(1)

    stats = {"passed": 0, "failed": 0}

    UP = b"\x1b[A"
    DOWN = b"\x1b[B"
    SPACE = b" "
    ENTER = b"\r"
    ESC = b"\x1b"

    CHOICES = "choices=$(printf 'foo|I like foo\\nbar|I want bar') && "
    LONG = "choices=$(printf 'aa|A very long label that does not fit\\nbb|B very long label that does not fit') && "

    # (name, keys, setup, width, expected_exit, expected_value, expected_out)
    tests = [
        # Basic selection (single select: one value only)
        ("no-selection",        [ENTER],                          CHOICES, 30, "0", "",       None),
        ("select-first",        [SPACE, ENTER],                   CHOICES, 30, "0", "foo",    None),
        ("select-second",       [DOWN, SPACE, ENTER],             CHOICES, 30, "0", "bar",    None),
        ("switch-selection",    [SPACE, DOWN, SPACE, ENTER],      CHOICES, 30, "0", "bar",    None),
        ("toggle-off",          [SPACE, SPACE, ENTER],            CHOICES, 30, "0", "",       None),
        # Up arrow at top and down arrow at bottom are no-ops
        ("up-at-top",           [UP, SPACE, ENTER],               CHOICES, 30, "0", "foo",    None),
        ("down-at-bottom",      [DOWN, DOWN, SPACE, ENTER],       CHOICES, 30, "0", "bar",    None),

        # Initial selection from the result variable
        ("initial-selection",   [ENTER],                          CHOICES + "result='bar' && ", 30, "0", "bar", None),
        ("initial-then-switch", [DOWN, SPACE, ENTER],             CHOICES + "result='foo' && ", 30, "0", "bar", None),
        ("initial-then-toggle", [SPACE, ENTER],                   CHOICES + "result='foo' && ", 30, "0", "", None),

        # Escape leaves the result variable unchanged
        ("escape-no-change",    [SPACE, DOWN, SPACE, ESC],        CHOICES, 30, "1", "", None),
        ("escape-keeps-initial",[SPACE, DOWN, SPACE, ESC],        CHOICES + "result='bar' && ", 30, "1", "bar", None),

        # Long labels are truncated with the fancy indicator
        ("truncate-label",      [SPACE, DOWN, SPACE, ENTER],      LONG, 14, "0", "bb", "…"),
    ]

    test_filter = sys.argv[2] if len(sys.argv) > 2 else ""

    for name, keys, setup, width, exp_exit, exp_val, exp_out in tests:
        if test_filter and test_filter not in name:
            continue
        try:
            exit_code, value, raw = run_test(keys, setup, width)
        except Exception as exc:
            stats["failed"] += 1
            print(f"FAIL: {name} \u2014 harness error: {exc}")
            continue
        check(name, exp_exit, exp_val, exit_code, value, raw, stats, exp_out)

    # Literal content passed directly as the first argument
    direct_content = "foo|this is foo\nbar|this is bar"
    try:
        exit_code, value, raw = run_test(
            [SPACE, ENTER], "", 30, '"' + direct_content + '"')
    except Exception as exc:
        stats["failed"] += 1
        print(f"FAIL: direct-content \u2014 harness error: {exc}")
        total = stats["passed"] + stats["failed"]
        print(f"\nPassed: {stats['passed']} / {total}")
        sys.exit(1)
    check("direct-content", "0", "foo", exit_code, value, raw, stats)

    total = stats["passed"] + stats["failed"]
    print(f"\nPassed: {stats['passed']} / {total}")
    sys.exit(0 if stats["failed"] == 0 else 1)


if __name__ == "__main__":
    main()
