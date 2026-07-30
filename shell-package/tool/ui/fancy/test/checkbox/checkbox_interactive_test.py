#!/usr/bin/env python3
"""Interactive PTY tests for fancy_checkbox."""

import os, pty, time, select, sys, re


def run_test(keys, setup_cmds="", width=30, first_arg="choices"):
    """Run fancy_checkbox in a PTY, send each key atomically, return
    (exit_code, var_value, decoded_output).

    keys is a list of byte strings; each is written in a single write so
    escape sequences (arrows, etc.) arrive within fancy_read_key's timeout.
    first_arg is passed as the choices argument to fancy_checkbox.
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
        'source ./checkbox.sh && '
        + setup_cmds
        + 'fancy_checkbox ' + first_arg + ' result ' + str(width) + ' ; '
        + 'echo "EXIT=$?" ; '
        + 'echo "VALUE=[$result]"\n'
    )

    for ch in cmd.encode():
        os.write(master, bytes([ch]))
        time.sleep(0.01)

    time.sleep(0.5)

    for key in keys:
        os.write(master, key)
        time.sleep(0.04)

    time.sleep(0.4)

    output = b""
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
        print("Usage: checkbox_interactive_test.py <fancy_dir> [test_filter]", file=sys.stderr)
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
        # Basic selection
        ("no-selection",        [ENTER],                          CHOICES, 30, "0", "",       None),
        ("select-first",        [SPACE, ENTER],                   CHOICES, 30, "0", "foo",    None),
        ("select-second",       [DOWN, SPACE, ENTER],             CHOICES, 30, "0", "bar",    None),
        ("select-both",         [SPACE, DOWN, SPACE, ENTER],      CHOICES, 30, "0", "foo,bar", None),
        ("deselect",            [SPACE, DOWN, SPACE, UP, SPACE, ENTER],
                                                                CHOICES, 30, "0", "bar",    None),
        # Up arrow at top and down arrow at bottom are no-ops
        ("up-at-top",           [UP, SPACE, ENTER],               CHOICES, 30, "0", "foo",    None),
        ("down-at-bottom",      [DOWN, DOWN, SPACE, ENTER],       CHOICES, 30, "0", "bar",    None),

        # Initial selection from the result variable
        ("initial-selection",   [ENTER],                          CHOICES + "result='bar' && ", 30, "0", "bar", None),
        ("initial-both",        [ENTER],                          CHOICES + "result='foo,bar' && ", 30, "0", "foo,bar", None),
        ("initial-then-toggle", [SPACE, ENTER],                   CHOICES + "result='foo' && ", 30, "0", "", None),

        # Escape leaves the result variable unchanged
        ("escape-no-change",    [SPACE, DOWN, SPACE, ESC],        CHOICES, 30, "1", "", None),
        ("escape-keeps-initial",[SPACE, DOWN, SPACE, ESC],        CHOICES + "result='bar' && ", 30, "1", "bar", None),

        # Long labels are truncated with the fancy indicator
        ("truncate-label",      [SPACE, DOWN, SPACE, ENTER],      LONG, 14, "0", "aa,bb", "…"),
    ]

    test_filter = sys.argv[2] if len(sys.argv) > 2 else ""

    for name, keys, setup, width, exp_exit, exp_val, exp_out in tests:
        if test_filter and test_filter not in name:
            continue
        exit_code, value, raw = run_test(keys, setup, width)
        check(name, exp_exit, exp_val, exit_code, value, raw, stats, exp_out)

    # Literal content passed directly as the first argument
    direct_content = "foo|this is foo\nbar|this is bar"
    exit_code, value, raw = run_test(
        [SPACE, ENTER], "", 30, '"' + direct_content + '"')
    check("direct-content", "0", "foo", exit_code, value, raw, stats)

    total = stats["passed"] + stats["failed"]
    print(f"\nPassed: {stats['passed']} / {total}")
    sys.exit(0 if stats["failed"] == 0 else 1)


if __name__ == "__main__":
    main()
