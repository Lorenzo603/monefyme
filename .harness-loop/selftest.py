#!/usr/bin/env python3
"""Self-test for the task runner: does it demand tests and loop on a task whose tests fail?

It builds a throwaway repo (own git repo, a fake `pi`, a fake test command), runs the real
runner.py against it, and asserts what the runner recorded. Nothing in the real repo, its
tasks, sessions or logs is touched.

Run from the repo root:

    python .harness-loop/selftest.py

Scenarios:
  A  claims DONE with no test while the test command is red -> the SAME task is retried, the
     next session is handed the failing output, and only the green attempt archives it
  B  claims DONE while the test command stays red -> retried up to max_attempts, then
     status failed, never archived
  C  docs-only change -> exempt from the test requirement, archived in one attempt
  D  source change backed by a test -> archived in one attempt
  E  --no-verify --no-require-tests -> the STATUS line alone decides (escape hatch)
  F  STATUS: BLOCKED -> no retry, the task waits for a human
  G  frontmatter whose closing `---` an older runner dropped, and `depends_on: nothing`
  H  live-view colours: one colour per action kind, transcript/log text stays free of them
  I  end to end: --color always paints the console run, --no-color and a piped run stay plain,
     and the per-attempt log file never contains escape codes
"""

from __future__ import annotations

import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
RUNNER = HERE / "runner.py"

FAKE_PI_PY = r'''"""Stand-in for pi: claims DONE and mutates the fake repo according to the task slug."""
import json
import re
import sys
from pathlib import Path

ROOT = Path(".").resolve()
text = sys.stdin.read() if not sys.stdin.isatty() else ""
argv = " ".join(sys.argv[1:])
match = re.search(r"tasks/([0-9A-Za-z._-]+)\.md", argv + " " + text)
slug = match.group(1) if match else "?"
retry = "Previous attempt" in text

state = ROOT / "state"
state.mkdir(exist_ok=True)
counter = state / (slug + ".count")
calls = int(counter.read_text()) if counter.exists() else 0
counter.write_text(str(calls + 1))
print("task=%s call=%d retry_note=%s" % (slug, calls + 1, "yes" if retry else "no"))

main_dir = ROOT / "src" / "main"
test_dir = ROOT / "src" / "test"

def colour_events(answer):
    """Pi-shaped JSONL session events: one block per kind of action."""
    return [
        {"type": "turn_start"},
        {"type": "message_update", "assistantMessageEvent": {"type": "thinking_delta",
                                                            "delta": "planning the change"}},
        {"type": "tool_execution_start", "toolName": "bash", "args": {"command": "ls"}},
        {"type": "tool_execution_end", "toolName": "bash", "isError": False, "durationMs": 5,
         "result": {"content": [{"type": "text", "text": "docs"}]}},
        {"type": "message_update", "assistantMessageEvent": {"type": "text_delta", "delta": answer}},
        {"type": "message_end", "message": {"role": "assistant",
                                           "content": [{"type": "text", "text": answer}]}},
        {"type": "turn_end"},
        {"type": "agent_end", "messages": []},
        {"type": "agent_settled", "aborted": False},
    ]


if slug == "01-alpha":
    main_dir.mkdir(parents=True, exist_ok=True)
    (main_dir / "Foo.kt").write_text("fun foo() = 1", encoding="utf-8")
    if retry:
        test_dir.mkdir(parents=True, exist_ok=True)
        (test_dir / "FooTest.kt").write_text("fun testFoo() = true", encoding="utf-8")
        (ROOT / "green.flag").write_text("ok", encoding="utf-8")
        print("STATUS: DONE added Foo.kt and FooTest.kt, tests green")
    else:
        print("STATUS: DONE added Foo.kt (no test yet)")
elif slug == "02-beta":
    main_dir.mkdir(parents=True, exist_ok=True)
    (main_dir / "Bar.kt").write_text("fun bar() = 2", encoding="utf-8")
    print("STATUS: DONE added Bar.kt but the test command still fails")
elif slug == "03-gamma":
    docs = ROOT / "docs"
    docs.mkdir(parents=True, exist_ok=True)
    (docs / "notes.md").write_text("notes", encoding="utf-8")
    print("STATUS: DONE changed docs only")
elif slug == "04-delta":
    main_dir.mkdir(parents=True, exist_ok=True)
    test_dir.mkdir(parents=True, exist_ok=True)
    (main_dir / "Delta.kt").write_text("fun delta() = 4", encoding="utf-8")
    (test_dir / "DeltaTest.kt").write_text("fun testDelta() = true", encoding="utf-8")
    print("STATUS: DONE changed Delta.kt plus DeltaTest.kt")
elif slug in ("08-color", "09-plain", "10-default"):
    # Emits Pi's JSONL session events so the runner's live view has one block per action kind.
    docs = ROOT / "docs"
    docs.mkdir(parents=True, exist_ok=True)
    (docs / (slug + ".md")).write_text("colour notes", encoding="utf-8")
    for event in colour_events("STATUS: DONE changed docs/" + slug + ".md"):
        print(json.dumps(event))
elif slug == "05-eps":
    main_dir.mkdir(parents=True, exist_ok=True)
    (main_dir / "Eps.kt").write_text("fun eps() = 5", encoding="utf-8")
    print("STATUS: DONE changed Eps.kt and trusted my own STATUS line")
elif slug == "06-eta":
    main_dir.mkdir(parents=True, exist_ok=True)
    test_dir.mkdir(parents=True, exist_ok=True)
    (main_dir / "Eta.kt").write_text("fun eta() = 6", encoding="utf-8")
    (test_dir / "EtaTest.kt").write_text("fun testEta() = true", encoding="utf-8")
    print("STATUS: DONE changed Eta.kt plus EtaTest.kt")
elif slug == "14-maxturn":
    # Emit one complete JSONL turn so the max_turns guard can be exercised.
    for event in colour_events("STATUS: DONE max turns test"):
        print(json.dumps(event))
elif slug in ("11-commit-pass", "12-commit-off"):
    docs = ROOT / "docs"
    docs.mkdir(parents=True, exist_ok=True)
    (docs / (slug + ".md")).write_text("commit test notes", encoding="utf-8")
    print("STATUS: DONE added docs/" + slug + ".md")
elif slug == "13-commit-fail":
    print("STATUS: FAILED cannot complete this task")
else:
    print("STATUS: BLOCKED I need a decision from a human")
'''

CHECK_PY = '''"""Stand-in for the project test command: red until the fake agent makes it green."""
import pathlib
import sys

if pathlib.Path("green.flag").exists():
    print("checks passed")
    sys.exit(0)
print("checks failed")
print("1 test completed, 1 failed")
sys.exit(1)
'''

TASKS_MD = {
    "01-alpha.md": "add a feature (the first pass forgets the test)",
    "02-beta.md": "claim DONE while the test command stays red",
    "03-gamma.md": "documentation only",
    "04-delta.md": "source change with a matching test",
    "07-theta.md": "a task that needs a human decision",
    "08-color.md": "colour the live console output per action kind",
    "14-maxturn.md": "test the max_turns guard",
}


def git(cwd: Path, *args: str) -> subprocess.CompletedProcess:
    return subprocess.run(["git", *args], cwd=str(cwd), capture_output=True, text=True)


def fake_pi_launcher(repo: Path) -> str:
    """Executable the runner can invoke instead of `pi` on this platform."""
    if os.name == "nt":
        path = repo / "fakepi.cmd"
        path.write_text('@echo off\r\npython "%~dp0fakepi.py" %*\r\n', encoding="utf-8")
    else:
        path = repo / "fakepi.sh"
        path.write_text('#!/bin/sh\nDIR="$(cd "$(dirname "$0")" && pwd)"\nexec python3 "$DIR/fakepi.py" "$@"\n',
                        encoding="utf-8")
        path.chmod(0o755)
    return str(path)


def setup(repo: Path) -> dict:
    harness = repo / ".harness-loop"
    (harness / "tasks").mkdir(parents=True, exist_ok=True)
    shutil.copy(str(RUNNER), str(harness / "runner.py"))
    (harness / "rules.md").write_text("fake rules used by the self-test\n", encoding="utf-8")
    config = {
        "pi_bin": fake_pi_launcher(repo),
        "timeout_seconds": 120,
        "log_tail_lines": 5,
        "verify_timeout_seconds": 60,
        "max_attempts": 2,
        "verify_commands": [f'"{sys.executable}" check.py'],
        "require_tests": True,
        "source_globs": ["src/main/*"],
        "test_globs": ["src/test/*"],
    }
    (harness / "config.json").write_text(json.dumps(config, indent=2), encoding="utf-8")
    for name, task in TASKS_MD.items():
        (harness / "tasks" / name).write_text(
            f'---\nstatus: todo\nattempts: 0\ntitle: "{name[3:-3]}"\n---\n\n## Task\n\n{task}\n',
            encoding="utf-8",
        )
    (repo / "check.py").write_text(CHECK_PY, encoding="utf-8")
    (repo / "fakepi.py").write_text(FAKE_PI_PY, encoding="utf-8")

    have_git = git(repo, "init", "-q").returncode == 0
    if have_git:
        git(repo, "config", "user.email", "selftest@example.com")
        git(repo, "config", "user.name", "selftest")
        (repo / "README.md").write_text("fake repo for the runner self-test\n", encoding="utf-8")
        git(repo, "add", "-A")
        commit = git(repo, "commit", "-q", "-m", "seed")
        have_git = commit.returncode == 0
    if not have_git:
        print("note: git unavailable, the test-presence check cannot be exercised")
    return config


def load_runner(path: Path, name: str):
    """Import runner.py as a module so the self-test can call its pieces directly."""
    import importlib.util

    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


class Harness:
    def __init__(self, repo: Path):
        self.repo = repo
        self.harness = repo / ".harness-loop"
        self.tasks = self.harness / "tasks"
        self.archived = self.harness / "archived-tasks"
        self.logs = self.harness / "logs"

    def run(self, slug: str, *extra: str) -> subprocess.CompletedProcess:
        proc = subprocess.run(
            [sys.executable, str(self.harness / "runner.py"), "--only", slug, *extra],
            cwd=str(self.repo), capture_output=True, text=True,
        )
        print(proc.stdout[-1800:])
        if proc.stderr.strip():
            print("STDERR:", proc.stderr[-300:].strip())
        return proc

    def read(self, name: str) -> tuple[str, int, str]:
        path = self.tasks / name if (self.tasks / name).exists() else self.archived / name
        body = path.read_text(encoding="utf-8")
        status = re.search(r"^status:\s*(\S+)", body, re.M).group(1)
        attempts = int(re.search(r"^attempts:\s*(\d+)", body, re.M).group(1))
        return status, attempts, body

    def log(self, name: str) -> str:
        return (self.logs / name).read_text(encoding="utf-8")


def main() -> int:
    if not RUNNER.is_file():
        print(f"error: {RUNNER} not found", file=sys.stderr)
        return 2

    repo = Path(tempfile.mkdtemp(prefix="harness-selftest-"))
    print(f"throwaway repo: {repo}")
    failures: list[str] = []
    try:
        setup(repo)
        h = Harness(repo)
        git_available = (repo / ".git").is_dir()

        # B first: it needs the test command to still be red (scenario A makes it green).
        check("B red tests forever: max_attempts then failed, never archived", scenario_b, h, failures)
        check("A red tests: retried on the same task, archived when green", scenario_a, h, failures)
        check("C docs only: exempt from the test requirement", scenario_c, h, failures)
        check("D source change with a test: accepted at once", scenario_d, h, failures)
        check("E escape hatch: --no-verify --no-require-tests", scenario_e, h, failures)
        check("F blocked: no retry, waits for a human", scenario_f, h, failures)
        if git_available:
            check("G broken frontmatter and 'depends_on: nothing'", scenario_g, h, failures)
        check("H live view colours: one per action kind, logs stay plain", scenario_h, h, failures)
        check("M max_turns: session ends at a turn boundary", scenario_m, h, failures)
        check("I colour run end to end: --color always, --no-color, piped default", scenario_i, h, failures)
        if git_available:
            check("J commit=true: git commit made after verified DONE", scenario_j, h, failures)
            check("K --no-commit: HEAD unchanged after DONE", scenario_k, h, failures)
            check("L commit=true on failing task: HEAD unchanged", scenario_l, h, failures)
    finally:
        shutil.rmtree(str(repo), ignore_errors=True)

    if failures:
        print("\nFAILED:")
        for line in failures:
            print("  -", line)
        return 1
    print("\nALL SELF-TESTS PASSED")
    return 0


def check(name: str, fn, h: Harness, failures: list[str]) -> None:
    print(f"\n=== {name} ===")
    try:
        fn(h)
        print(f"ok: {name}")
    except AssertionError as exc:
        failures.append(f"{name}: {exc}")
        print(f"FAIL: {name}: {exc}")
    except Exception as exc:  # noqa: BLE001 - the self-test must report, not crash
        failures.append(f"{name}: {type(exc).__name__}: {exc}")
        print(f"FAIL: {name}: {type(exc).__name__}: {exc}")


def scenario_a(h: Harness) -> None:
    proc = h.run("01-alpha")
    assert proc.returncode == 0, f"exit {proc.returncode}\n{proc.stdout[-600:]}"
    assert (h.archived / "01-alpha.md").exists(), "must be archived after the green retry"
    status, attempts, body = h.read("01-alpha.md")
    assert (status, attempts) == ("done", 2), (status, attempts)
    assert "status=todo" in body, "a rejected attempt goes back to todo"
    assert "no test file was added or updated" in body, "the missing test must be recorded"
    assert "attempt 1:" in body and "attempt 2:" in body, "both attempts stay in the run log"
    assert body.count("## Run log") == 1, "the run log must not duplicate itself"
    retry = h.log("01-alpha-run2.log")
    assert "Previous attempt" in retry and "src/main/Foo.kt" in retry, \
        "the retry session must be told what failed and how"
    assert "retry_note=yes" in retry, "the fake agent saw the retry note"
    assert (h.repo / "src" / "test" / "FooTest.kt").exists(), "the retry added the test"
    assert not (h.tasks / "01-alpha.md").exists(), "a done task is moved out of tasks/"


def scenario_b(h: Harness) -> None:
    proc = h.run("02-beta")
    assert proc.returncode == 1, f"exit {proc.returncode}"
    status, attempts, body = h.read("02-beta.md")
    assert (status, attempts) == ("failed", 2), (status, attempts)
    assert not (h.archived / "02-beta.md").exists(), "a failed task stays in tasks/"
    assert (h.logs / "02-beta-run1.log").exists() and (h.logs / "02-beta-run2.log").exists()
    assert "check: exited 1" in body, "the red test command is recorded"


def scenario_c(h: Harness) -> None:
    proc = h.run("03-gamma")
    assert proc.returncode == 0, f"exit {proc.returncode}\n{proc.stdout[-600:]}"
    status, attempts, _ = h.read("03-gamma.md")
    assert (status, attempts) == ("done", 1), (status, attempts)
    assert (h.archived / "03-gamma.md").exists()


def scenario_d(h: Harness) -> None:
    proc = h.run("04-delta")
    assert proc.returncode == 0, f"exit {proc.returncode}\n{proc.stdout[-600:]}"
    status, attempts, _ = h.read("04-delta.md")
    assert (status, attempts) == ("done", 1), (status, attempts)
    assert (h.archived / "04-delta.md").exists()


def scenario_e(h: Harness) -> None:
    (h.tasks / "05-eps.md").write_text(
        '---\nstatus: todo\nattempts: 0\ntitle: "eps"\n---\n\n## Task\n\nunverifiable\n',
        encoding="utf-8",
    )
    proc = h.run("05-eps", "--no-verify", "--no-require-tests")
    assert proc.returncode == 0, f"exit {proc.returncode}\n{proc.stdout[-600:]}{proc.stderr[-300:]}"
    status, attempts, _ = h.read("05-eps.md")
    assert (status, attempts) == ("done", 1), (status, attempts)


def scenario_f(h: Harness) -> None:
    proc = h.run("07-theta")
    assert proc.returncode == 1, f"exit {proc.returncode}"
    status, attempts, _ = h.read("07-theta.md")
    assert (status, attempts) == ("blocked", 1), (status, attempts)
    assert not (h.logs / "07-theta-run2.log").exists(), "a blocked task must not be re-run"
    assert not (h.archived / "07-theta.md").exists()


def scenario_g(h: Harness) -> None:
    mod = load_runner(h.harness / "runner.py", "runner_under_test")

    assert mod.split_deps("nothing") == [] and mod.split_deps("none") == []
    assert mod.split_deps("01-alpha, 03-gamma") == ["01-alpha", "03-gamma"]

    mangled = h.tasks / "06-eta.md"
    mangled.write_text(
        '---\nstatus: todo\nattempts: 2\ntitle: "eta"\ndepends_on: nothing\n\n## Task\n\neta\n',
        encoding="utf-8",
    )
    parsed = mod.read_task(mangled)
    assert (parsed.status, parsed.attempts, parsed.depends_on) == ("todo", 2, []), (
        parsed.status, parsed.attempts, parsed.depends_on,
    )
    proc = h.run("06-eta")
    assert proc.returncode == 0, f"exit {proc.returncode}\n{proc.stdout[-600:]}"
    assert "waiting on" not in proc.stdout, "'depends_on: nothing' must not block the task"
    body = (h.archived / "06-eta.md").read_text(encoding="utf-8")
    assert body.count("---") == 2, "the closing frontmatter delimiter is restored\n" + body[:400]
    assert "depends_on: nothing" in body.split("---")[1]


# Events the colour tests feed to the live view (one block per kind of action).
COLOUR_EVENTS = [
    {"type": "turn_start"},
    {"type": "message_update", "assistantMessageEvent": {"type": "thinking_delta", "delta": "planning"}},
    {"type": "tool_execution_start", "toolName": "bash", "args": {"command": "ls"}},
    {"type": "tool_execution_end", "toolName": "bash", "isError": False, "durationMs": 5,
     "result": {"content": [{"type": "text", "text": "docs"}]}},
    {"type": "tool_execution_end", "toolName": "bash", "isError": True,
     "result": {"content": [{"type": "text", "text": "boom"}]}},
    {"type": "message_update", "assistantMessageEvent": {"type": "text_delta", "delta": "the answer"}},
    {"type": "message_end", "message": {"role": "assistant",
                                       "content": [{"type": "text", "text": "the answer"}]}},
]


def render_live_view(mod, color: bool) -> tuple[str, object]:
    """Run the runner's live view over COLOUR_EVENTS and return (console text, view)."""
    import contextlib
    import io

    view = mod.LiveView(color=color)
    buf = io.StringIO()
    with contextlib.redirect_stdout(buf):
        for event in COLOUR_EVENTS:
            view.event(event)
    return buf.getvalue(), view


def scenario_h(h: Harness) -> None:
    """Colour is decided by the runner, per action kind, and never leaks into the transcript."""
    mod = load_runner(RUNNER, "colour_runner_under_test")

    plain, _ = render_live_view(mod, False)
    assert "\x1b[" not in plain, f"colours off must not emit escape codes: {plain!r}"

    painted, view = render_live_view(mod, True)
    assert "\x1b[" in painted, f"colours on produced no escape codes: {painted!r}"
    expected = {
        "header": "[pi] --- turn 1 ---",
        "thinking": "[thinking] planning",
        "tool": '[tool bash]',
        "tool_result": "    -> bash ok 5ms: docs",
        "error": "    -> bash error: boom",
        "assistant": "[assistant] the answer",
    }
    for key, text in expected.items():
        assert mod.PALETTE[key] + text in painted, \
            f"{text!r} must be painted in the {key} colour: {painted!r}"
    assert mod.RESET in painted, "every coloured block must be closed again"

    kinds = ("thinking", "assistant", "tool", "tool_result", "pi", "prompt", "error", "header", "warning")
    codes = [mod.PALETTE[key] for key in kinds]
    assert len(set(codes)) == len(codes), f"each action kind needs its own colour: {codes}"

    assert all("\x1b" not in line for line in view.transcript), \
        f"the log transcript must stay plain text: {view.transcript}"

    # Decision rules: NO_COLOR wins, --color always/never force, auto needs a real terminal.
    saved_no_color = os.environ.pop("NO_COLOR", None)
    saved_force_color = os.environ.pop("FORCE_COLOR", None)
    original_sys = mod.sys
    try:
        assert mod.color_requested("always") is True
        assert mod.color_requested("never") is False
        os.environ["NO_COLOR"] = "1"
        assert mod.color_requested("always") is False, "NO_COLOR must beat --color always"
        os.environ.pop("NO_COLOR")

        class _Pipe:
            def isatty(self):
                return False

        class _Shim:
            stdout = _Pipe()
            stderr = sys.stderr  # the real one: colour decisions warn here

        mod.sys = _Shim()
        assert mod.color_requested("auto") is False, "auto stays plain when piped or redirected"
        os.environ["FORCE_COLOR"] = "1"
        assert mod.color_requested("auto") is True, "FORCE_COLOR overrides the terminal check"
        os.environ.pop("FORCE_COLOR")
        assert mod.color_requested("bogus") is False, "an unknown mode falls back to auto"
    finally:
        mod.sys = original_sys
        os.environ.pop("FORCE_COLOR", None)
        if saved_no_color is not None:
            os.environ["NO_COLOR"] = saved_no_color
        if saved_force_color is not None:
            os.environ["FORCE_COLOR"] = saved_force_color

    assert mod.check_output_color("1 test completed, 1 failed") == "error"
    assert mod.check_output_color("BUILD SUCCESSFUL in 3s") == "ok"
    assert mod.check_output_color("w: unused variable") == ""


def scenario_i(h: Harness) -> None:
    """End to end: the flag paints the console run, the log file and a plain run stay clean."""
    mod = load_runner(RUNNER, "colour_runner_e2e")

    painted = h.run("08-color", "--no-verify", "--color", "always")
    assert painted.returncode == 0, f"exit {painted.returncode}\n{painted.stdout[-600:]}"
    for key in ("thinking", "tool", "tool_result", "assistant", "header"):
        assert mod.PALETTE[key] in painted.stdout, \
            f"--color always must paint {key}: {painted.stdout[-800:]}"
    assert mod.PALETTE["thinking"] + "[thinking] planning the change" in painted.stdout

    log = h.log("08-color-run1.log")
    assert "\x1b[" not in log, f"the per-attempt log must hold no escape codes: {log[:400]!r}"
    assert "[thinking] planning the change" in log, "the live view is still logged, just plain"
    assert "[tool bash]" in log and "[assistant] STATUS: DONE" in log
    assert (h.archived / "08-color.md").exists()

    (h.tasks / "09-plain.md").write_text(
        '---\nstatus: todo\nattempts: 0\ntitle: "plain"\n---\n\n## Task\n\nno colours\n',
        encoding="utf-8",
    )
    plain = h.run("09-plain", "--no-verify", "--no-color")
    assert plain.returncode == 0, f"exit {plain.returncode}\n{plain.stdout[-600:]}"
    assert "\x1b[" not in plain.stdout, f"--no-color must emit no escape codes: {plain.stdout[-600:]}"
    assert "[tool bash]" in plain.stdout, "the live view itself still works without colours"

    # Default `auto` with the runner's stdout captured (this self-test runs it through a pipe).
    (h.tasks / "10-default.md").write_text(
        '---\nstatus: todo\nattempts: 0\ntitle: "default"\n---\n\n## Task\n\nauto colours\n',
        encoding="utf-8",
    )
    default = h.run("10-default", "--no-verify")
    assert default.returncode == 0, f"exit {default.returncode}\n{default.stdout[-600:]}"
    assert "\x1b[" not in default.stdout, "auto must stay plain when the output is not a terminal"
    assert "[thinking] planning the change" in default.stdout


def scenario_j(h: Harness) -> None:
    """commit=true: a git commit is made after every verified DONE."""
    (h.tasks / "11-commit-pass.md").write_text(
        '---\nstatus: todo\nattempts: 0\ntitle: "Add commit notes"\n---\n\n## Task\n\ncommit test\n',
        encoding="utf-8",
    )
    before = git(h.repo, "rev-parse", "HEAD").stdout.strip()
    proc = h.run("11-commit-pass", "--no-verify", "--no-require-tests", "--commit")
    assert proc.returncode == 0, f"exit {proc.returncode}\n{proc.stdout[-600:]}"
    after = git(h.repo, "rev-parse", "HEAD").stdout.strip()
    assert after != before, "a new commit must be made after a verified DONE"
    log_msg = git(h.repo, "log", "--format=%B", "-1").stdout
    assert "Add commit notes" in log_msg, f"commit must include task title: {log_msg!r}"
    assert "11-commit-pass" in log_msg, f"commit must include task slug: {log_msg!r}"


def scenario_k(h: Harness) -> None:
    """--no-commit: HEAD must stay unchanged even when the task succeeds."""
    (h.tasks / "12-commit-off.md").write_text(
        '---\nstatus: todo\nattempts: 0\ntitle: "commit off"\n---\n\n## Task\n\ncommit disabled\n',
        encoding="utf-8",
    )
    before = git(h.repo, "rev-parse", "HEAD").stdout.strip()
    proc = h.run("12-commit-off", "--no-verify", "--no-require-tests", "--no-commit")
    assert proc.returncode == 0, f"exit {proc.returncode}\n{proc.stdout[-600:]}"
    after = git(h.repo, "rev-parse", "HEAD").stdout.strip()
    assert after == before, "no commit must be made when --no-commit is passed"


def scenario_l(h: Harness) -> None:
    """commit=true on a failing task: HEAD must not advance."""
    (h.tasks / "13-commit-fail.md").write_text(
        '---\nstatus: todo\nattempts: 0\ntitle: "commit fail"\n---\n\n## Task\n\nalways fails\n',
        encoding="utf-8",
    )
    before = git(h.repo, "rev-parse", "HEAD").stdout.strip()
    proc = h.run("13-commit-fail", "--commit", "--max-attempts", "1")
    assert proc.returncode == 1, f"exit {proc.returncode}"
    status, attempts, _ = h.read("13-commit-fail.md")
    assert status == "failed", f"expected failed, got {status!r}"
    after = git(h.repo, "rev-parse", "HEAD").stdout.strip()
    assert after == before, "no commit must be made when the task fails"


def scenario_m(h: Harness) -> None:
    """max_turns: the runner terminates the session at a turn boundary and marks it failed."""
    proc = h.run("14-maxturn", "--no-verify", "--no-require-tests", "--max-turns", "1")
    assert proc.returncode == 1, f"exit {proc.returncode}\n{proc.stdout[-600:]}"
    status, attempts, body = h.read("14-maxturn.md")
    assert (status, attempts) == ("failed", 2), (status, attempts)
    assert "max_turns" in body, "the max-turns reason must be recorded in the task file"
    assert not (h.archived / "14-maxturn.md").exists(), "a failed task stays in tasks/"


if __name__ == "__main__":
    sys.exit(main())
