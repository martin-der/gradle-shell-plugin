#!/usr/bin/env python3
"""Interactive PTY tests for fancy_input."""

import os, pty, time, select, sys, re


def run_test(keys, setup_cmds="", width=30):
    """Run fancy_input in a PTY, send each key atomically, return (exit_code, var_value).

    keys is a list of byte strings; each is written in a single write so
    escape sequences (arrows, etc.) arrive within fancy_read_key's timeout.
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
        'source ./input.sh && '
        + setup_cmds
        + 'fancy_input result "Test" ' + str(width) + ' "" ; '
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

    _strip = lambda s: re.sub(r'\x1b\[[?0-9; ]*[a-zA-Z]', '', s).strip().strip('\r')
    exit_code = ""
    value = ""

    for line in decoded.split('\n'):
        cleaned = _strip(re.sub(r'\x1b\].*?\x07', '', line))
        if cleaned.startswith('EXIT='):
            exit_code = cleaned.split('=', 1)[1].strip()
            break

    for line in decoded.split('\n'):
        cleaned = _strip(re.sub(r'\x1b\].*?\x07', '', line))
        if cleaned.startswith('VALUE='):
            val = cleaned.split('=', 1)[1].strip()
            value = val[1:-1] if val.startswith('[') and val.endswith(']') else val
            break

    return exit_code, value, decoded


def check(name, expected_exit, expected_value, exit_code, value, raw, stats):
    ok = exit_code == expected_exit and value == expected_value
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
        print("Usage: input_interactive_test.py <fancy_dir> [test_filter]", file=sys.stderr)
        sys.exit(1)

    stats = {"passed": 0, "failed": 0}

    LEFT = b"\x1b[D"
    RIGHT = b"\x1b[C"
    CTRL_LEFT = b"\x1b[1;5D"
    CTRL_RIGHT = b"\x1b[1;5C"
    CTRL_BACKSPACE = b"\x1b[3;5~"
    CTRL_W = b"\x17"
    BACKSPACE = b"\x7f"
    DELETE = b"\x1b[3~"
    ENTER = b"\r"

    tests = [
        # Basic
        ("type-hello",       [b"hello", ENTER],                   "",                           30, "0", "hello"),
        ("press-esc",        [b"\x1b"],                           "",                           30, "1", ""),
        ("initial-value",    [b" test", ENTER],                   "result='init_val' && ",       30, "0", "init_val test"),
        ("width-clamp",      [b"a", ENTER],                       "",                           3,  "0", "a"),
        ("empty-input",      [ENTER],                             "result='x' && ",              30, "0", "x"),
        ("empty-no-initial", [ENTER],                             "",                           30, "0", ""),

        # Cursor movement: left/right arrows
        ("left-arrow",       [b"ab", LEFT, b"X", ENTER],          "",                           30, "0", "aXb"),
        ("right-arrow",      [b"ab", LEFT, RIGHT, b"X", ENTER],   "",                           30, "0", "abX"),

        # Word navigation: ctrl+left / ctrl+right
        ("ctrl-left-word",   [b"hello world", CTRL_LEFT, b"X", ENTER],        "",               30, "0", "hello Xworld"),
        ("ctrl-right-word",  [b"hello world", CTRL_LEFT, CTRL_RIGHT, b"X", ENTER], "",           30, "0", "hello worldX"),

        # Word deletion: ctrl+backspace / ctrl+w
        ("ctrl-backspace-word", [b"hello world", CTRL_BACKSPACE, ENTER],       "",               30, "0", "hello "),
        ("ctrl-w-word",         [b"hello world", CTRL_W, ENTER],               "",               30, "0", "hello "),

        # Single-char deletion: backspace / delete
        ("backspace",        [b"hello", BACKSPACE, ENTER],        "",                           30, "0", "hell"),
        ("delete-key",       [b"hello", LEFT, DELETE, ENTER],     "",                           30, "0", "hell"),
    ]

    test_filter = sys.argv[2] if len(sys.argv) > 2 else ""

    for name, keys, setup, width, exp_exit, exp_val in tests:
        if test_filter and test_filter not in name:
            continue
        exit_code, value, raw = run_test(keys, setup, width)
        check(name, exp_exit, exp_val, exit_code, value, raw, stats)

    total = stats["passed"] + stats["failed"]
    print(f"\nPassed: {stats['passed']} / {total}")
    sys.exit(0 if stats["failed"] == 0 else 1)


if __name__ == "__main__":
    main()
