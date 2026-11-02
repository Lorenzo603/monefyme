#!/usr/bin/env python3
"""Backlog runner for Pi (https://pi.dev).

Every task file in tasks/ is executed as one independent Pi run:

    pi --print "@<task file>  Implement this task ..."

Print mode makes Pi run once, print its final answer, and exit, so each loop
iteration gets its own process, its own context window, and its own session.
The outer loop lives here, in plain Python, because Pi deliberately ships
without plan mode or task queues.

Usage (from the repository root):

    python .harness-loop/runner.py --list
    python .harness-loop/runner.py --dry-run
    python .harness-loop/runner.py
    python .harness-loop/runner.py --only tech-logos --model sonnet:high
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import re
import shutil
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parent
TASKS_DIR = ROOT / "tasks"
RULES_FILE = ROOT / "rules.md"
CONFIG_FILE = ROOT / "config.json"
SESSIONS_DIR = ROOT / "sessions"
LOGS_DIR = ROOT / "logs"

TODO = "todo"
DONE = "done"
BLOCKED = "blocked"
FAILED = "failed"

STATUS_RE = re.compile(r"STATUS:\s*(DONE|BLOCKED|FAILED)\s*(.*)", re.IGNORECASE)
FRONT_RE = re.compile(r"^([A-Za-z_][A-Za-z0-9_]*)\s*:\s*(.*)$")

VALID_THINKING = {"off", "minimal", "low", "medium", "high", "xhigh", "max"}
# Print mode cannot show Pi's interactive project-trust prompt, so the runner decides explicitly.
VALID_TRUST = {"approve", "ignore", "ask"}
# "stdin" pipes the prompt (Pi prepends it to the first prompt); "arg" sends a one-line argument.
VALID_PROMPT_MODES = {"stdin", "arg"}

DEFAULT_CONFIG = {
    "pi_bin": "pi",
    "model": "",          # empty -> whatever Pi has configured
    "thinking": "",       # empty -> model default
    "tools": "",          # empty -> Pi default (read, bash, edit, write)
    "timeout_seconds": 1500,
    "ephemeral_sessions": False,
    "log_tail_lines": 40,
    "offline": False,     # true -> pass --offline (never reach Pi's model catalog)
    "project_trust": "approve",  # approve -> --approve, ignore -> --no-approve, ask -> leave it to Pi
    "prompt_mode": "stdin",  # stdin -> pipe the prompt (safe on Windows), arg -> one-line argument
}


@dataclass
class Task:
    path: Path
    slug: str
    title: str
    status: str
    attempts: int
    depends_on: list[str]
    meta_lines: list[str] = field(default_factory=list)
    body: str = ""


# --------------------------------------------------------------------------- task files


def split_deps(raw: str) -> list[str]:
    raw = (raw or "").strip().strip("[]")
    return [part.strip().strip("'\"") for part in raw.split(",") if part.strip()]


def frontmatter_end(lines: list[str]) -> int | None:
    if not lines or lines[0].strip() != "---":
        return None
    for i in range(1, len(lines)):
        if lines[i].strip() == "---":
            return i
    return None


def read_text(path: Path) -> str:
    # utf-8-sig: tolerate a Windows/Notepad BOM, which would otherwise hide the frontmatter
    return path.read_text(encoding="utf-8-sig")


def read_task(path: Path) -> Task:
    text = read_text(path)
    lines = text.splitlines()
    meta: dict[str, str] = {}
    meta_lines: list[str] = []
    body = text.strip()

    end = frontmatter_end(lines)
    if end is not None:
        meta_lines = lines[1:end]
        for line in meta_lines:
            m = FRONT_RE.match(line)
            if m:
                meta[m.group(1)] = m.group(2).strip()
        body = "\n".join(lines[end + 1:]).strip()

    title = meta.get("title", "").strip("'\"") or path.stem
    try:
        attempts = int(meta.get("attempts", "0"))
    except ValueError:
        attempts = 0

    return Task(
        path=path,
        slug=path.stem,
        title=title,
        status=(meta.get("status") or TODO).lower(),
        attempts=attempts,
        depends_on=split_deps(meta.get("depends_on", "")),
        meta_lines=meta_lines,
        body=body,
    )


def write_task(task: Task, status: str, log_lines: list[str]) -> None:
    """Rewrite status/attempts in the frontmatter and append the run log."""
    text = read_text(task.path).replace("\r\n", "\n")
    lines = text.splitlines()
    end = frontmatter_end(lines)

    if end is not None:
        meta = lines[1:end]
        status_i = next(
            (i for i, line in enumerate(meta) if line.strip().startswith("status:")), None
        )
        if status_i is None:
            meta.append(f"status: {status}")
        else:
            meta[status_i] = f"status: {status}"

        attempts_i = next(
            (i for i, line in enumerate(meta) if line.strip().startswith("attempts:")), None
        )
        if attempts_i is None:
            meta.append(f"attempts: {task.attempts + 1}")
        else:
            meta[attempts_i] = f"attempts: {task.attempts + 1}"

        body_lines = lines[end + 1:]
        new_text = "\n".join([lines[0], *meta, *body_lines])
    else:
        new_text = "\n".join(
            ["---", f"status: {status}", f"attempts: {task.attempts + 1}", "---", "", text.strip()]
        )

    new_text = new_text.rstrip() + "\n\n## Run log\n\n"
    new_text += "\n".join(f"- {line}" for line in log_lines) + "\n"
    task.path.write_text(new_text, encoding="utf-8", newline="\n")


# --------------------------------------------------------------------------- prompt and command


def task_reference(task: Task) -> str:
    """The @path Pi expands into the task file contents (resolved from the repo root)."""
    return f"@{task.path.relative_to(REPO).as_posix()}"


def prompt_instructions(task: Task) -> str:
    """Instruction block. Pi prepends piped stdin to the first prompt, so this travels on stdin.

    Pi on Windows is normally a .cmd shim (npm), and cmd.exe truncates an argument at its
    first newline, so multi-line prompts must never be passed as command-line arguments.
    """
    deps = ", ".join(task.depends_on) if task.depends_on else "none"
    rules = RULES_FILE.relative_to(REPO).as_posix()
    return (
        f"Task file: {task_reference(task)}\n"
        f"Project rules file (already loaded by Pi): {rules}\n\n"
        "Implement the task above in this repository.\n"
        "Rules:\n"
        "- Work only on this task; keep the diff scoped to it.\n"
        f"- Follow the shared rules in {rules} and the repository instructions in AGENTS.md.\n"
        "- Verify with the commands listed in the project instructions.\n"
        "- Never run git commit, git push, or any destructive git command.\n"
        "- You cannot ask questions in this mode: if the task is ambiguous, state the\n"
        "  assumption you made instead of waiting for an answer.\n"
        "End your reply with exactly one line:\n"
        "STATUS: DONE <what you changed>\n"
        "STATUS: BLOCKED <what you need from a human>\n"
        "STATUS: FAILED <what broke>"
    )


def compact_prompt(task: Task) -> str:
    """Single-line prompt for prompt_mode='arg' (never multi-line, see prompt_instructions)."""
    rules = RULES_FILE.relative_to(REPO).as_posix()
    return (
        f"{task_reference(task)} Implement only this task, follow {rules}, and end your reply "
        "with one STATUS line: DONE, BLOCKED or FAILED."
    )


def build_command(task: Task, cfg: dict, attempt: int) -> list[str]:
    cmd = [cfg["pi_bin"], "--print"]

    def supported(*flags: str) -> bool:
        known = cfg["pi_help"]
        return not known or all(f in known for f in flags)

    def use(flag: str, *args: str) -> None:
        if supported(flag):
            cmd.extend([flag, *args])
        else:
            print(f"note: this Pi build does not support {flag}, skipping it", file=sys.stderr)

    if cfg["ephemeral_sessions"]:
        use("--no-session")
    else:
        # A new ID per attempt keeps every loop iteration in a fresh session.
        use("--session-id", f"{task.slug}-run{attempt}")
        use("--session-dir", str(SESSIONS_DIR))

    if RULES_FILE.exists():
        use("--append-system-prompt", RULES_FILE.relative_to(REPO).as_posix())
    if cfg["model"]:
        use("--model", cfg["model"])
    if cfg["thinking"]:
        use("--thinking", cfg["thinking"])
    if cfg["tools"]:
        use("--tools", cfg["tools"])
    if cfg["offline"]:
        use("--offline")
    if cfg["project_trust"] == "approve":
        use("--approve")
    elif cfg["project_trust"] == "ignore":
        use("--no-approve")

    cmd.append(task_reference(task) if cfg["prompt_mode"] == "stdin" else compact_prompt(task))
    return cmd


def build_stdin(task: Task, cfg: dict) -> str | None:
    """Text piped to Pi's stdin (Pi prepends it to the first prompt)."""
    return prompt_instructions(task) if cfg["prompt_mode"] == "stdin" else None


# --------------------------------------------------------------------------- selection and run loop


def load_config() -> dict:
    cfg = dict(DEFAULT_CONFIG)
    if CONFIG_FILE.exists():
        try:
            raw = json.loads(read_text(CONFIG_FILE))
        except json.JSONDecodeError as exc:
            print(f"warning: ignoring invalid {CONFIG_FILE.name}: {exc}", file=sys.stderr)
            return cfg
        if not isinstance(raw, dict):
            print(f"warning: {CONFIG_FILE.name} must contain a JSON object", file=sys.stderr)
            return cfg

        for key, value in raw.items():
            if key not in DEFAULT_CONFIG:
                print(f"warning: unknown key in {CONFIG_FILE.name}: {key}", file=sys.stderr)
                continue
            expected = DEFAULT_CONFIG[key]
            if isinstance(expected, bool) and not isinstance(value, bool):
                print(f"warning: {key} must be true or false", file=sys.stderr)
                continue
            if isinstance(expected, int) and not isinstance(value, int):
                print(f"warning: {key} must be a number of seconds", file=sys.stderr)
                continue
            if isinstance(expected, str) and value and not isinstance(value, str):
                print(f"warning: {key} must be a string", file=sys.stderr)
                continue
            cfg[key] = value

    cfg["pi_help"] = ""  # filled in by main() after probing `pi --help`
    validate_config(cfg)
    return cfg


def validate_config(cfg: dict) -> None:
    """Warn and fall back on bad values. Run once, after config and CLI overrides are merged."""
    if cfg["thinking"] and cfg["thinking"] not in VALID_THINKING:
        print(
            f"warning: unsupported thinking level {cfg['thinking']!r} "
            f"(use one of {', '.join(sorted(VALID_THINKING))})",
            file=sys.stderr,
        )
        cfg["thinking"] = ""
    if cfg["project_trust"] not in VALID_TRUST:
        print(
            f"warning: project_trust must be one of {', '.join(sorted(VALID_TRUST))}, "
            f"using {DEFAULT_CONFIG['project_trust']!r}",
            file=sys.stderr,
        )
        cfg["project_trust"] = DEFAULT_CONFIG["project_trust"]
    if cfg["prompt_mode"] not in VALID_PROMPT_MODES:
        print(
            f"warning: prompt_mode must be one of {', '.join(sorted(VALID_PROMPT_MODES))}, "
            f"using {DEFAULT_CONFIG['prompt_mode']!r}",
            file=sys.stderr,
        )
        cfg["prompt_mode"] = DEFAULT_CONFIG["prompt_mode"]
    if not cfg["pi_bin"]:
        cfg["pi_bin"] = DEFAULT_CONFIG["pi_bin"]
    if cfg["timeout_seconds"] < 10:
        print("warning: timeout_seconds is too small, using 10", file=sys.stderr)
        cfg["timeout_seconds"] = 10
    if cfg["log_tail_lines"] < 1:
        cfg["log_tail_lines"] = DEFAULT_CONFIG["log_tail_lines"]


def get_pi_help(pi_bin: str) -> str:
    """Output of `pi --help`, used to skip options this Pi build does not know."""

    try:
        proc = subprocess.run(
            [pi_bin, "--help"],
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            timeout=60,
        )
    except (FileNotFoundError, PermissionError, subprocess.TimeoutExpired, OSError):
        return ""
    return proc.stdout + proc.stderr


def select_tasks(args: argparse.Namespace) -> list[Task]:
    tasks = [read_task(p) for p in sorted(TASKS_DIR.glob("*.md"))]
    by_slug = {t.slug: t for t in tasks}

    accepted = {TODO}
    if args.retry_failed:
        accepted.update({FAILED, BLOCKED})

    wanted = set(args.only or [])
    selected: list[Task] = []
    for task in tasks:
        if wanted and not any(w == task.slug or task.slug.endswith("-" + w) for w in wanted):
            continue
        if task.status not in accepted:
            continue
        unmet = [
            dep for dep in task.depends_on
            if by_slug.get(dep) is None or by_slug[dep].status != DONE
        ]
        if unmet:
            print(f"skip  {task.slug}: waiting on {', '.join(unmet)}")
            continue
        selected.append(task)

    return selected[: args.limit] if args.limit else selected


def run_task(task: Task, cfg: dict, args: argparse.Namespace) -> str:
    attempt = task.attempts + 1
    cmd = build_command(task, cfg, attempt)
    stdin_text = build_stdin(task, cfg) or ""
    stamp = dt.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    def show(cmd_line: str) -> None:
        print(cmd_line)
        if stdin_text:
            print(f"--- stdin ---\n{stdin_text}\n--- end of stdin ---")

    print(f"\n=== {task.slug} (attempt {attempt}) ===")
    if args.dry_run:
        show(" ".join(f'"{part}"' if " " in part else part for part in cmd))
        return TODO

    LOGS_DIR.mkdir(parents=True, exist_ok=True)
    log_path = LOGS_DIR / f"{task.slug}-run{attempt}.log"

    try:
        # The long instruction block is piped in, so the command line stays short
        # (see the note in README.md about Windows .cmd shims).
        proc = subprocess.run(
            cmd,
            cwd=REPO,
            input=stdin_text,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            timeout=cfg["timeout_seconds"],
        )
    except subprocess.TimeoutExpired:
        write_task(task, FAILED, [f"{stamp} attempt {attempt}: timed out after {cfg['timeout_seconds']}s"])
        print(f"{task.slug}: failed (timeout after {cfg['timeout_seconds']}s)")
        return FAILED
    except FileNotFoundError:
        msg = f"Pi executable {cfg['pi_bin']!r} was not found"
        write_task(task, FAILED, [f"{stamp} attempt {attempt}: {msg}"])
        print(f"error: {msg}", file=sys.stderr)
        sys.exit(2)
    except OSError as exc:
        msg = f"cannot run {cfg['pi_bin']!r}: {exc}"
        write_task(task, FAILED, [f"{stamp} attempt {attempt}: {msg}"])
        print(f"error: {msg}", file=sys.stderr)
        sys.exit(2)

    output = proc.stdout or ""
    log_path.write_text(
        f"exit: {proc.returncode}\n\n--- stdout ---\n{output}\n\n--- stderr ---\n{proc.stderr or ''}\n",
        encoding="utf-8",
    )

    matches = STATUS_RE.findall(output)
    if matches:
        status = matches[-1][0].lower()
        note = matches[-1][1].strip()
    else:
        status = DONE if proc.returncode == 0 else FAILED
        note = "no STATUS line in output"

    tail = [line.strip() for line in output.strip().splitlines() if line.strip()]
    tail = tail[-cfg["log_tail_lines"]:]
    entries = [f"{stamp} attempt {attempt}: status={status} exit={proc.returncode} log={log_path.name} {note}".strip()]
    entries += [f"  {line}" for line in tail]
    write_task(task, status, entries)
    print(f"{task.slug}: {status} (exit {proc.returncode}, log: {log_path.relative_to(REPO).as_posix()})")
    return status


def main() -> int:
    parser = argparse.ArgumentParser(description="Sequential task runner for Pi.")
    parser.add_argument("--limit", type=int, default=0, help="maximum number of tasks in this run")
    parser.add_argument("--only", nargs="+", metavar="SLUG", help="run only these task slugs")
    parser.add_argument("--retry-failed", action="store_true", help="also pick up failed/blocked tasks")
    parser.add_argument("--keep-going-blocked", action="store_true", help="do not stop when a task reports BLOCKED")
    parser.add_argument("--model", help="Pi model, e.g. sonnet:high")
    parser.add_argument("--thinking", help="off|minimal|low|medium|high|xhigh|max")
    parser.add_argument("--tools", help="Pi tool allowlist, e.g. read,grep,find,ls,edit,write")
    parser.add_argument("--timeout", type=int, help="seconds allowed per task")
    parser.add_argument("--ephemeral", action="store_true", help="use --no-session (session is not persisted)")
    parser.add_argument("--offline", action="store_true", help="pass --offline to Pi (no model catalog refresh)")
    parser.add_argument(
        "--trust",
        choices=sorted(VALID_TRUST),
        help="project trust for each Pi run: approve (--approve), ignore (--no-approve), ask (Pi default)",
    )
    parser.add_argument("--pi-bin", help="path to the pi executable")
    parser.add_argument("--dry-run", action="store_true", help="show the command instead of running it")
    parser.add_argument("--list", action="store_true", help="list tasks and their status")
    args = parser.parse_args()

    cfg = load_config()
    for key, value in (
        ("model", args.model),
        ("thinking", args.thinking),
        ("tools", args.tools),
        ("pi_bin", args.pi_bin),
    ):
        if value:
            cfg[key] = value
    if args.timeout:
        cfg["timeout_seconds"] = args.timeout
    if args.ephemeral:
        cfg["ephemeral_sessions"] = True
    if args.offline:
        cfg["offline"] = True
    if args.trust:
        cfg["project_trust"] = args.trust
    if args.thinking and args.thinking not in VALID_THINKING:
        print(f"error: --thinking must be one of {', '.join(sorted(VALID_THINKING))}", file=sys.stderr)
        return 2
    validate_config(cfg)

    if not TASKS_DIR.is_dir():
        print(f"error: {TASKS_DIR} does not exist", file=sys.stderr)
        return 2

    tasks = [read_task(p) for p in sorted(TASKS_DIR.glob("*.md"))]

    if args.list:
        for task in tasks:
            deps = f" (depends on {', '.join(task.depends_on)})" if task.depends_on else ""
            attempts = f" attempts={task.attempts}" if task.attempts else ""
            print(f"{task.status:<8} {task.slug}{attempts}{deps}")
        return 0

    selected = select_tasks(args)
    if not selected:
        print("no tasks to run (every task is done, blocked, or filtered out)")
        return 0

    pi_bin = shutil.which(cfg["pi_bin"])
    if not pi_bin and Path(cfg["pi_bin"]).is_file():
        pi_bin = str(Path(cfg["pi_bin"]).resolve())
    if not pi_bin:
        if not args.dry_run:
            print(
                f"error: '{cfg['pi_bin']}' not found. Install Pi (needs Node.js 22.19+):\n"
                '  powershell -c "irm https://pi.dev/install.ps1 | iex"\n'
                "or set pi_bin in .harness-loop/config.json.",
                file=sys.stderr,
            )
            return 2
        print(f"note: '{cfg['pi_bin']}' is not installed; the dry run below is unverified", file=sys.stderr)
    else:
        cfg["pi_bin"] = pi_bin
        cfg["pi_help"] = get_pi_help(pi_bin)

    if not args.dry_run and not cfg["ephemeral_sessions"]:
        SESSIONS_DIR.mkdir(parents=True, exist_ok=True)

    print(f"{len(selected)} task(s) queued: {', '.join(t.slug for t in selected)}")
    results: dict[str, str] = {}
    for task in selected:
        current = read_task(task.path)
        status = run_task(current, cfg, args)
        results[current.slug] = status
        if status == BLOCKED and not args.keep_going_blocked:
            print("stopping: a task needs human input")
            break

    not_done = [slug for slug, status in results.items() if status not in (DONE, TODO)]
    done = [slug for slug, status in results.items() if status == DONE]
    summary = f"run summary: {len(done)} done"
    if not_done:
        summary += f", {len(not_done)} not done: {', '.join(not_done)}"
    print(f"\n{summary}")
    return 0 if not not_done else 1


if __name__ == "__main__":
    sys.exit(main())
