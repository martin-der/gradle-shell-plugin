#!/usr/bin/env python3
"""Interactive PTY test for the chained components demo (demo.sh).

Drives the demo end-to-end: types a name into fancy_input, checks two hobbies
in fancy_checkbox and picks a season in fancy_select, then verifies the demo
prints the collected values in its summary.
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


def wait_for_draw(master, deadline=8.0):
    """Wait until the next widget emits its cursor query and then goes quiet.

    Each fancy component emits ESC [ 6 n right before it draws and blocks in
    fancy_read_key, so in a chained demo every widget emits it once, in order.
    Seeing the query followed by a short quiet period means the keys we send
    next will be consumed by the widget instead of by fancy_get_cursor's blind
    read (which otherwise makes the tests timing-flaky under load).
    """
    acc = b""
    drawn = False
    quiet_start = None
    end = time.time() + deadline
    while time.time() < end:
        chunk = read_available(master, 0.02)
        if chunk:
            acc += chunk
            if not drawn and re.search(rb'\x1b\[6n', acc):
                drawn = True
            quiet_start = None
        elif drawn:
            if quiet_start is None:
                quiet_start = time.time()
            elif time.time() - quiet_start > 0.15:
                return acc
    if not drawn:
        raise RuntimeError("widget cursor query never appeared")
    raise RuntimeError("widget output never went quiet after initial draw")


def run_demo(phases, deadline=8.0):
    """Run test/demo/demo.sh in a PTY.

    phases is a list of key lists, one per widget, in the order the demo runs
    them (input, checkbox, select). Returns the decoded output.
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

    cmd = "bash test/demo/demo.sh\n"
    for ch in cmd.encode():
        os.write(master, bytes([ch]))
        time.sleep(0.01)

    output = b""
    for keys in phases:
        output += wait_for_draw(master)
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
            output += chunk
        except Exception:
            break

    os.write(master, b"exit\n")
    os.close(master)
    os.waitpid(pid, 0)

    return output.decode('utf-8', errors='replace')


def strip_ansi(text):
    text = re.sub(r'\x1b\].*?\x07', '', text)
    text = re.sub(r'\x1b\[[?0-9; ]*[a-zA-Z]', '', text)
    return text


def parse_summary(decoded):
    """Extract the demo's final summary values."""
    values = {}
    for line in strip_ansi(decoded).split('\n'):
        line = line.strip().strip('\r')
        m = re.match(r'^(Name|Hobbies|Season):\s+(.*)$', line)
        if m:
            values[m.group(1)] = m.group(2)
    return values


def check(name, expected, actual, stats):
    ok = actual == expected
    stats["passed" if ok else "failed"] += 1
    status = "PASS" if ok else "FAIL"
    print(f"{status}: {name} — {actual!r} (expected {expected!r})")


def main():
    if len(sys.argv) < 2:
        print("Usage: demo_interactive_test.py <fancy_dir>", file=sys.stderr)
        sys.exit(1)

    stats = {"passed": 0, "failed": 0}

    UP = b"\x1b[A"
    DOWN = b"\x1b[B"
    SPACE = b" "
    ENTER = b"\r"

    # input -> name "John"; checkbox -> hobbies "read,sport"; select -> season "winter"
    phases = [
        [b"John", ENTER],
        [SPACE, DOWN, SPACE, ENTER],
        [DOWN, SPACE, ENTER],
    ]

    try:
        decoded = run_demo(phases)
    except Exception as exc:
        stats["failed"] += 1
        print(f"FAIL: demo — harness error: {exc}")
        total = stats["passed"] + stats["failed"]
        print(f"\nPassed: {stats['passed']} / {total}")
        sys.exit(1)

    summary = parse_summary(decoded)

    # All three widgets must have been drawn (cursor query per widget).
    n_queries = len(re.findall(r'\x1b\[6n', decoded))
    check("three widgets drawn", 3, n_queries, stats)

    check("input value", "John", summary.get("Name"), stats)
    check("checkbox values", "read,sport", summary.get("Hobbies"), stats)
    check("select value", "winter", summary.get("Season"), stats)

    total = stats["passed"] + stats["failed"]
    print(f"\nPassed: {stats['passed']} / {total}")
    sys.exit(0 if stats["failed"] == 0 else 1)


if __name__ == "__main__":
    main()
