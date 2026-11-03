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
    python .harness-loop/runner.py --only tech-logos --no-color   # plain live output
"""

from __future__ import annotations

import argparse
import datetime as dt
import fnmatch
import json
import os
import queue
import re
import shutil
import subprocess
import sys
import threading
import time
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parent
TASKS_DIR = ROOT / "tasks"
ARCHIVED_DIR = ROOT / "archived-tasks"
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
# How many lines the task file's ## Run log section keeps.
RUN_LOG_ENTRIES = 30

VALID_THINKING = {"off", "minimal", "low", "medium", "high", "xhigh", "max"}
# Print mode cannot show Pi's interactive project-trust prompt, so the runner decides explicitly.
VALID_TRUST = {"approve", "ignore", "ask"}
# "stdin" pipes the prompt (Pi prepends it to the first prompt); "arg" sends a one-line argument.
VALID_PROMPT_MODES = {"stdin", "arg"}

# fnmatch is used for the test-presence globs, where `*` also crosses `/`, so
# "src/test/*" matches every file below src/test/. Set these globs per repo in config.json.

# Live console rendering limits (what the model is thinking/doing/printing).
TOOL_ARG_CHARS = 400
TOOL_RESULT_CHARS = 1500

# --------------------------------------------------------------------------- terminal colours
#
# Colour is produced here, in the runner, and it is not a shell feature the harness has to
# rely on: Pi runs as a child process in `--mode json` (its stdout is JSON, never coloured
# text), and the runner renders that stream itself. What the runner writes are plain ANSI
# escape codes on its own stdout; they are interpreted by whatever terminal the runner is
# started from (Git Bash/MSYS2, macOS Terminal/iTerm2, Windows Terminal / conhost), not by
# bash, zsh or cmd.exe. So the only shell/OS dependency is that the terminal understands
# ANSI and the stream is an interactive terminal - which is detected, never assumed:
#   * colours are off unless the output is a TTY (piping or `logs/` redirection stays plain),
#   * `NO_COLOR` (https://no-color.org) always wins, `FORCE_COLOR` forces them on,
#   * `TERM=dumb` disables them,
#   * on Windows VT processing is enabled on the console handle first (ctypes,
#     ENABLE_VIRTUAL_TERMINAL_PROCESSING); if that call fails (old conhost, redirected
#     handle) the runner falls back to plain text instead of printing raw escape codes.
# Because the codes come from the runner, the harness stays repository agnostic and no
# terminal-emulator-specific setting is needed in bash, zsh or Git Bash.

RESET = "\x1b[0m"
ENABLE_VIRTUAL_TERMINAL_PROCESSING = 0x0004
VALID_COLOR = {"auto", "always", "never"}

# One colour per kind of action, so the live view is scannable at a glance.
PALETTE = {
    "thinking": "\x1b[90m",    # dim grey: the model's reasoning
    "assistant": "\x1b[32m",   # green: the model's answer
    "tool": "\x1b[36m",        # cyan: a tool call and its arguments
    "tool_result": "\x1b[34m",  # blue: what the tool returned
    "pi": "\x1b[35m",          # magenta: runner notes (turns, retries, agent start/end)
    "prompt": "\x1b[33m",      # yellow: what the runner hands the model (prompt, retry note)
    "warning": "\x1b[93m",     # bright yellow: Pi stderr, notes that are not failures
    "header": "\x1b[1m",       # bold: attempt and verification headers
    "ok": "\x1b[32m",          # green: a check that passed
    "error": "\x1b[31m",       # red: a failure, an error result, a rejected attempt
}

# Decided once in validate_config() (see color_requested); drives the runner's own prints.
USE_COLORS = False


def set_colors(enabled: bool) -> None:
    global USE_COLORS
    USE_COLORS = bool(enabled)


def paint(text: str, key: str = "") -> str:
    """Wrap text in the colour of its action kind; untouched when colours are off."""
    code = PALETTE.get(key, "") if USE_COLORS else ""
    return f"{code}{text}{RESET}" if code else text


def say(text: str, key: str = "", stream=None) -> None:
    """A runner message printed in its kind's colour (plain when colours are off)."""
    print(paint(text, key), file=stream if stream is not None else sys.stdout, flush=True)


def _windows_vt_ready() -> bool:
    """Turn on ANSI processing on the Windows console handles (no-op on macOS/Linux).

    Returns False when the handles are not consoles or the mode cannot be set, in which
    case the runner keeps its output plain instead of printing escape codes verbatim.
    """
    if os.name != "nt":
        return True
    try:
        import ctypes

        kernel32 = ctypes.windll.kernel32
    except (ImportError, OSError, AttributeError):
        return False
    ready = False
    try:
        for std_handle in (-11, -12):  # STD_OUTPUT_HANDLE, STD_ERROR_HANDLE
            handle = kernel32.GetStdHandle(std_handle)
            if not handle or handle == -1:
                continue
            mode = ctypes.c_uint32()
            # GetConsoleMode fails when the handle is a pipe or a redirected file.
            if not kernel32.GetConsoleMode(handle, ctypes.byref(mode)):
                continue
            if kernel32.SetConsoleMode(handle, mode.value | ENABLE_VIRTUAL_TERMINAL_PROCESSING):
                ready = True
    except (OSError, AttributeError):
        return False
    return ready


def color_requested(mode: str) -> bool:
    """Should the console output be coloured? `auto` only says yes for a real terminal."""
    mode = (mode or "auto").strip().lower()
    if mode not in VALID_COLOR:
        print(
            f"warning: color must be one of {', '.join(sorted(VALID_COLOR))}, using "
            f"{DEFAULT_CONFIG['color']!r}",
            file=sys.stderr,
        )
        mode = DEFAULT_CONFIG["color"]
    if os.environ.get("NO_COLOR"):
        return False  # https://no-color.org: any value disables colour
    if mode == "never":
        return False
    if mode == "always":
        if (os.name == "nt" and hasattr(sys.stdout, "isatty") and sys.stdout.isatty()
                and not _windows_vt_ready()):
            # Asked for colours, but this console would only print the escape codes verbatim.
            print("warning: this Windows console cannot enable ANSI colours, printing plain text",
                  file=sys.stderr)
            return False
        return True
    if os.environ.get("FORCE_COLOR"):
        return True
    if os.environ.get("TERM", "").strip().lower() in ("dumb", "unknown"):
        return False
    if not (hasattr(sys.stdout, "isatty") and sys.stdout.isatty()):
        return False  # piped or redirected: keep the pipe/log byte-clean
    return _windows_vt_ready()

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
    "stream_output": True,   # live-print what Pi is thinking/doing/printing while it runs
    "stream_thinking": True,  # include thinking blocks in the live view
    "stream_raw": False,     # also dump Pi's raw JSONL records to the console
    "color": "auto",         # auto (only on a terminal) | always | never: per-action colours
    "use_color": False,      # derived at runtime from `color` by validate_config(); not set by hand
    # Verification: the harness re-runs these test commands after every attempt that claims
    # DONE. A failure sends the same task back to a fresh Pi session (see max_attempts).
    "verify_commands": [],          # shell commands, run from the repo root
    "verify_timeout_seconds": 900,  # per-command timeout
    "verify_env_script": "",        # optional repo env-setup script, no extension -> .ps1/.sh (falls back to .sample)
    "max_attempts": 3,              # Pi sessions allowed per task before it is marked failed
    # Require the agent to touch a test file whenever it changed production code.
    "require_tests": False,
    "source_globs": ["src/main/*"],   # production paths that trigger the test requirement (per repo in config.json)
    "test_globs": ["src/test/*"],  # paths that count as tests (per repo in config.json)
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
    parts = [part.strip().strip("'\"") for part in raw.split(",")]
    # A task may say `depends_on: nothing`; that is not a prerequisite to wait for.
    empty = {"none", "nothing", "no", "no deps", "n/a", "na", "-", ""}
    return [part for part in parts if part and part.lower() not in empty]


def frontmatter_end(lines: list[str]) -> int | None:
    if not lines or lines[0].strip() != "---":
        return None
    for i in range(1, len(lines)):
        if lines[i].strip() == "---":
            return i
    return None


def repair_frontmatter(lines: list[str]) -> list[str]:
    """Close a frontmatter block whose closing `---` an older runner dropped.

    Without the terminator the block is invisible: every later read fell back to
    status=todo/attempts=0, so a retried task reused its own attempt number.
    """
    if not lines or lines[0].strip() != "---" or frontmatter_end(lines) is not None:
        return lines
    i = 1
    while i < len(lines) and FRONT_RE.match(lines[i]):
        i += 1
    return lines[:i] + ["---"] + lines[i:] if i > 1 else lines


def read_text(path: Path) -> str:
    # utf-8-sig: tolerate a Windows/Notepad BOM, which would otherwise hide the frontmatter
    return path.read_text(encoding="utf-8-sig")


def read_task(path: Path) -> Task:
    text = read_text(path)
    lines = repair_frontmatter(text.splitlines())
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


ATTEMPT_SUMMARY_RE = re.compile(r"^-\s+\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2} attempt \d+:")


def write_task(task: Task, status: str, log_lines: list[str]) -> None:
    """Rewrite status/attempts in the frontmatter and rebuild the capped ## Run log section.

    Earlier attempts keep only their one-line summary (the verbose tail lives in logs/), so a
    task that was retried still shows its whole history in the task file.
    """
    text = read_text(task.path).replace("\r\n", "\n")
    lines = repair_frontmatter(text.splitlines())
    end = frontmatter_end(lines)
    history: list[str] = []

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
        # Cut the previous run log, keeping its per-attempt summary lines.
        for i, line in enumerate(body_lines):
            if line.strip() == "## Run log":
                history = [
                    l.strip()[1:].strip()
                    for l in body_lines[i + 1:]
                    if ATTEMPT_SUMMARY_RE.match(l.strip())
                ]
                body_lines = body_lines[:i]
                break
        # Keep the closing delimiter: without it the frontmatter dissolved and every later
        # read fell back to attempts=0, so a retried task reused its own attempt number.
        new_text = "\n".join([lines[0], *meta, "---", *body_lines])
    else:
        new_text = "\n".join(
            ["---", f"status: {status}", f"attempts: {task.attempts + 1}", "---", "", text.strip()]
        )

    entries = (history + log_lines)[-RUN_LOG_ENTRIES:]
    new_text = new_text.rstrip() + "\n\n## Run log\n\n"
    new_text += "\n".join(f"- {line}" for line in entries) + "\n"
    task.path.write_text(new_text, encoding="utf-8", newline="\n")


def archive_task(task: Task) -> Path:
    """Move a completed task file from tasks/ to archived-tasks/ (the runner does this,
    never the model). Returns the destination path."""
    ARCHIVED_DIR.mkdir(parents=True, exist_ok=True)
    dest = ARCHIVED_DIR / task.path.name
    if dest.exists():
        stamp = dt.datetime.now().strftime("%Y%m%d-%H%M%S")
        dest = ARCHIVED_DIR / f"{task.path.stem}-{stamp}{task.path.suffix}"
    shutil.move(str(task.path), str(dest))
    return dest


# --------------------------------------------------------------------------- prompt and command


def task_reference(task: Task) -> str:
    """The @path Pi expands into the task file contents (resolved from the repo root)."""
    return f"@{task.path.relative_to(REPO).as_posix()}"


def prompt_instructions(task: Task, cfg: dict, prior_note: str = "") -> str:
    """Instruction block. Pi prepends piped stdin to the first prompt, so this travels on stdin.

    Pi on Windows is normally a .cmd shim (npm), and cmd.exe truncates an argument at its
    first newline, so multi-line prompts must never be passed as command-line arguments.
    """
    rules = RULES_FILE.relative_to(REPO).as_posix()
    parts = [
        f"Task file: {task_reference(task)}",
        f"Project rules file (already loaded by Pi): {rules}",
        "",
        "Implement the task above in this repository.",
        "Rules:",
        "- Work only on this task; keep the diff scoped to it.",
        f"- Follow the shared rules in {rules} and the repository instructions in AGENTS.md.",
        "- Tests are part of the work: any behaviour change you make must come with an added",
        "  or updated automated test that covers it. A change without tests is not DONE.",
        "- Run the project's test commands yourself, and only report STATUS: DONE once they",
        "  pass. The harness re-runs them after your session and, on failure, hands the same",
        "  task back to a fresh session, so a green test run is what ends the loop.",
        "- Never run git commit, git push, or any destructive git command.",
        "- You cannot ask questions in this mode: if the task is ambiguous, state the",
        "  assumption you made instead of waiting for an answer.",
    ]
    if cfg["verify_commands"]:
        commands = "\n".join(f"    {cmd}" for cmd in cfg["verify_commands"])
        parts.append("- Harness verification commands (the runner enforces these):")
        parts.append(commands)
    else:
        parts.append("- Verify with the commands listed in the project instructions.")
    parts += [
        "End your reply with exactly one line:",
        "STATUS: DONE <what you changed>",
        "STATUS: BLOCKED <what you need from a human>",
        "STATUS: FAILED <what broke>",
    ]
    if prior_note:
        parts += ["", "Previous attempt of this same task did not pass:", prior_note]
    return "\n".join(parts)


def compact_prompt(task: Task, cfg: dict, prior_note: str = "") -> str:
    """Single-line prompt for prompt_mode='arg' (never multi-line, see prompt_instructions)."""
    rules = RULES_FILE.relative_to(REPO).as_posix()
    text = (
        f"{task_reference(task)} Implement only this task, follow {rules}, add or update tests "
        "for what you change, run the project test commands until they pass, and end your reply "
        "with one STATUS line: DONE, BLOCKED or FAILED."
    )
    if prior_note:
        text += " Previous attempt failed: " + " ".join(prior_note.split())
    return text


def build_command(task: Task, cfg: dict, attempt: int, prior_note: str = "") -> list[str]:
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
    if cfg["stream_output"]:
        # JSON mode gives per-token deltas and tool events instead of a single final answer.
        use("--mode", "json")

    cmd.append(
        task_reference(task) if cfg["prompt_mode"] == "stdin" else compact_prompt(task, cfg, prior_note)
    )
    return cmd


def build_stdin(task: Task, cfg: dict, prior_note: str = "") -> str | None:
    """Text piped to Pi's stdin (Pi prepends it to the first prompt)."""
    return prompt_instructions(task, cfg, prior_note) if cfg["prompt_mode"] == "stdin" else None


# --------------------------------------------------------------------------- live output


def _one_line(value, limit: int) -> str:
    """Render a tool argument/result as a short, single-line string."""
    if isinstance(value, str):
        text = value
    else:
        try:
            text = json.dumps(value, ensure_ascii=False)
        except (TypeError, ValueError):
            text = str(value)
    text = " ".join(text.split())
    return text if len(text) <= limit else text[:limit] + " ..."


def _content_text(content) -> str:
    """Extract plain text from a Pi message/tool result content value."""
    if isinstance(content, str):
        return content
    if isinstance(content, dict):
        return _content_text(content.get("content", ""))
    if isinstance(content, list):
        parts = []
        for block in content:
            if isinstance(block, dict) and block.get("type") == "text":
                parts.append(block.get("text", ""))
        return "\n".join(parts)
    return ""


class LiveView:
    """Render Pi's session-event stream as human-readable console output.

    Deltas are written as they arrive, so the run is followable live: thinking text,
    answer text, every tool call with its arguments, and every tool result.

    Each kind of action gets its own colour (PALETTE) on the console, while the transcript
    kept for logs/ stays free of escape codes so a log is still plain text.
    """

    def __init__(self, thinking: bool = True, raw: bool = False, color: bool = False) -> None:
        self.thinking = thinking
        self.raw = raw
        self.color = color
        self.open_prefix: str | None = None  # a block currently written inline
        self.open_key = ""  # which colour the open block was started with
        self.assistant_text: list[str] = []  # authoritative text, for the STATUS line
        self.transcript: list[str] = []  # same view as text, for the per-attempt log
        self.segment = ""
        self.turn = 0

    def colored(self, text: str, key: str) -> str:
        """Wrap text in its action colour; the console only, never in the transcript."""
        code = PALETTE.get(key, "") if self.color else ""
        return f"{code}{text}{RESET}" if code else text

    def _close(self) -> None:
        if self.open_prefix is not None:
            # End the colour before the newline so it cannot bleed into the next block.
            sys.stdout.write((RESET if self.color and PALETTE.get(self.open_key) else "") + "\n")
            sys.stdout.flush()
            if self.segment:
                self.transcript.append(f"{self.open_prefix}{self.segment}")
            self.open_prefix = None
            self.open_key = ""
            self.segment = ""

    def line(self, text: str, key: str = "pi") -> None:
        self._close()
        sys.stdout.write(self.colored(f"[pi] {text}", key) + "\n")
        sys.stdout.flush()
        self.transcript.append(f"[pi] {text}")

    def start(self, prefix: str, key: str = "") -> None:
        self._close()
        # The colour is switched on here and switched off in _close(), so everything the
        # model streams into this block (thinking text, answer text) keeps the block colour.
        code = PALETTE.get(key, "") if self.color else ""
        sys.stdout.write(code + prefix)
        sys.stdout.flush()
        self.open_prefix = prefix
        self.open_key = key
        self.segment = ""

    def ensure(self, prefix: str, key: str = "") -> None:
        """Open the block prefix only when there is actually content to show."""
        if self.open_prefix == prefix:
            return
        self.start(prefix, key)

    def write(self, text: str) -> None:
        if text:
            sys.stdout.write(text)
            sys.stdout.flush()
            self.segment += text

    def plain(self, text: str, key: str = "tool_result") -> None:
        """Standalone (non-streamed) output line, e.g. a finished tool result."""
        if not text:
            return
        self._close()
        body = text[:-1] if text.endswith("\n") else text
        sys.stdout.write(self.colored(body, key) + "\n")
        sys.stdout.flush()
        self.transcript.append(body)

    def echo(self, text: str, key: str = "assistant") -> None:
        """A raw (non-JSONL) stdout line from an older Pi: colour it, leave the transcript
        alone because the raw stdout is written to the log separately."""
        self._close()
        sys.stdout.write(self.colored(text, key) + "\n")
        sys.stdout.flush()

    def event(self, obj: dict) -> None:
        kind = obj.get("type")
        if self.raw:
            self.line(json.dumps(obj, ensure_ascii=False), "pi")
        elif kind == "session":
            self.line(f"session {obj.get('id', '?')} ({obj.get('cwd', '?')})", "pi")

        if kind == "agent_start":
            self.line("agent start", "pi")
        elif kind == "agent_end":
            # Safety net: if no message_end carried the text, recover it from the run.
            for message in obj.get("messages") or []:
                if isinstance(message, dict) and message.get("role") == "assistant":
                    text = _content_text(message.get("content"))
                    if text:
                        self.assistant_text.append(text)
                if isinstance(message, dict) and message.get("errorMessage"):
                    self.line(f"assistant error: {message['errorMessage']}", "error")
            self.line(f"agent end (will_retry={bool(obj.get('willRetry'))})", "pi")
        elif kind == "agent_settled":
            self.line("agent settled" if not obj.get("aborted") else "agent aborted", "pi")
        elif kind == "turn_start":
            self.turn += 1
            self.line(f"--- turn {self.turn} ---", "header")
        elif kind == "turn_end":
            self._close()
        elif kind == "tool_execution_start":
            self.plain(
                f"[tool {obj.get('toolName', '?')}] {_one_line(obj.get('args'), TOOL_ARG_CHARS)}",
                "tool",
            )
        elif kind == "tool_execution_update":
            self.plain(f"    {_one_line(obj.get('partialResult'), 400)}", "tool_result")
        elif kind == "tool_execution_end":
            flag = "error" if obj.get("isError") else "ok"
            ms = obj.get("durationMs")
            timing = f" {ms}ms" if isinstance(ms, (int, float)) else ""
            self.plain(
                f"    -> {obj.get('toolName', '?')} {flag}{timing}: "
                f"{_one_line(_content_text(obj.get('result')), TOOL_RESULT_CHARS)}",
                "error" if obj.get("isError") else "tool_result",
            )
        elif kind == "message_start":
            message = obj.get("message") or {}
            if message.get("role") == "assistant":
                # No prefix yet: it is printed with the first thinking/text delta, so a
                # tool-only message does not produce a bare label line.
                pass
        elif kind == "message_end":
            message = obj.get("message") or {}
            if message.get("role") == "assistant":
                text = _content_text(message.get("content"))
                if text:
                    self.assistant_text.append(text)
                self._close()
            self._close()
        elif kind == "message_update":
            self._assistant_event(obj.get("assistantMessageEvent") or {})
        elif kind == "compaction_start":
            self.line(f"compaction start ({obj.get('reason', '?')})", "pi")
        elif kind == "compaction_end":
            self.line(f"compaction end ({obj.get('reason', '?')})", "pi")
        elif kind == "auto_retry_start":
            self.line(
                f"retry {obj.get('attempt', '?')}/{obj.get('maxAttempts', '?')} in "
                f"{obj.get('delayMs', '?')}ms: {obj.get('errorMessage', '')}",
                "error",
            )
        elif kind == "auto_retry_end":
            self.line(f"retry {'ok' if obj.get('success') else 'failed'}",
                      "ok" if obj.get("success") else "error")
        elif kind == "thinking_level_changed":
            self.line(f"thinking level -> {obj.get('level', '?')}", "pi")
        elif kind == "session_info_changed":
            self.line(f"session name: {obj.get('name') or '(cleared)'}", "pi")

    def _assistant_event(self, ev: dict) -> None:
        """Stream one assistant content block (thinking, text) token by token."""
        kind = ev.get("type")
        if kind == "thinking_delta" and self.thinking:
            self.ensure("[thinking] ", "thinking")
            self.write(ev.get("delta", ""))
            return
        if kind == "text_delta":
            self.ensure("[assistant] ", "assistant")
            self.write(ev.get("delta", ""))
            return
        if kind in ("thinking_end", "text_end"):
            if kind == "thinking_end" and not self.thinking:
                return
            # Authoritative content; only re-print when nothing was streamed live.
            content = ev.get("content", "")
            if self.open_prefix is None and content:
                self.ensure(
                    "[thinking] " if kind == "thinking_end" else "[assistant] ",
                    "thinking" if kind == "thinking_end" else "assistant",
                )
                self.write(content)
            self._close()
            return
        if kind == "toolcall_start":
            self.line(f"tool call: {ev.get('toolName', '?')}", "tool")
        elif kind == "error":
            self.line(f"stream error ({ev.get('reason', '?')}): {ev.get('error', '')}", "error")


def _terminate(proc: subprocess.Popen) -> None:
    """Kill the run and, on Windows, the whole pi.cmd -> node child tree."""
    try:
        if os.name == "nt":
            subprocess.run(
                ["taskkill", "/F", "/T", "/PID", str(proc.pid)],
                capture_output=True,
                timeout=30,
            )
    except (OSError, subprocess.TimeoutExpired):
        pass
    try:
        proc.kill()
    except OSError:
        pass


def _pump(pipe, sink: queue.Queue, name: str) -> None:
    """Reader thread: push every line (as bytes) so stdout is never left unconsumed."""
    try:
        for chunk in iter(pipe.readline, b""):
            sink.put((name, chunk))
    except (OSError, ValueError):
        pass
    finally:
        sink.put((name, None))
        try:
            pipe.close()
        except OSError:
            pass


def stream_pi_process(proc: subprocess.Popen, cfg: dict) -> tuple[str, str, str, str, bool]:
    """Drain Pi while it runs, live-printing the model's work.

    Returns (raw stdout text, raw stderr text, assistant text, transcript, timed_out).
    stdout is read continuously on purpose: a stalled reader can block Pi.
    """
    view = LiveView(
        thinking=cfg["stream_thinking"],
        raw=cfg["stream_raw"],
        color=cfg["use_color"],
    )
    json_mode = False
    sink: queue.Queue = queue.Queue()
    threads = [
        threading.Thread(target=_pump, args=(proc.stdout, sink, "out"), daemon=True),
        threading.Thread(target=_pump, args=(proc.stderr, sink, "err"), daemon=True),
    ]
    for t in threads:
        t.start()

    out_lines: list[str] = []
    err_lines: list[str] = []
    timed_out = False
    deadline = time.monotonic() + cfg["timeout_seconds"]
    closed = 0

    while closed < 2:
        try:
            name, chunk = sink.get(timeout=1.0)
        except queue.Empty:
            if time.monotonic() > deadline:
                timed_out = True
                _terminate(proc)
            continue
        if chunk is None:
            closed += 1
            continue
        text = chunk.decode("utf-8", "replace").rstrip("\r\n")
        if name == "err":
            err_lines.append(text)
            if cfg["stream_output"] and text.strip():
                view.line(f"stderr: {text}", "warning")
            continue

        out_lines.append(text)
        if not cfg["stream_output"]:
            continue
        if not text.strip():
            continue
        try:
            obj = json.loads(text)
        except json.JSONDecodeError:
            # Not JSONL (older Pi, or a plain-text run): echo it as it comes.
            view.echo(text, "assistant")
            continue
        if isinstance(obj, dict):
            json_mode = True
            view.event(obj)
        else:
            view.echo(text, "assistant")

    view._close()
    try:
        proc.wait(timeout=30)
    except subprocess.TimeoutExpired:
        _terminate(proc)

    return (
        "\n".join(out_lines),
        "\n".join(err_lines),
        "\n\n".join(view.assistant_text),
        "\n".join(view.transcript),
        timed_out,
        json_mode,
    )


# --------------------------------------------------------------------------- harness verification


def matches_any(path: str, patterns: list[str]) -> bool:
    return any(fnmatch.fnmatch(path, pattern) for pattern in patterns)


def git_lines(*args: str) -> list[str] | None:
    """Output lines of a git command run in the repo, or None when git cannot be used."""
    try:
        proc = subprocess.run(
            ["git", *args],
            cwd=REPO,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            timeout=60,
        )
    except (FileNotFoundError, PermissionError, OSError, subprocess.TimeoutExpired):
        return None
    if proc.returncode != 0:
        return None
    return [line.strip().replace("\\", "/") for line in proc.stdout.splitlines() if line.strip()]


def changed_files() -> set[str] | None:
    """Working-tree changes vs HEAD plus untracked files (POSIX-style paths).

    None when git is unavailable, which disables the mechanical test-presence check.
    """
    tracked = git_lines("diff", "--name-only", "HEAD")
    untracked = git_lines("ls-files", "--others", "--exclude-standard")
    if tracked is None and untracked is None:
        return None
    return set(tracked or []) | set(untracked or [])


def normalize_command(cmd: str) -> str:
    """Make a POSIX-style command runnable in this shell (cmd.exe rejects a leading './')."""
    cmd = (cmd or "").strip()
    if os.name == "nt" and cmd.startswith("./"):
        cmd = ".\\" + cmd[2:]
    return cmd


def resolve_env_script(name: str) -> str:
    """Resolve the env-setup script to a repo-relative path ('' when there is none).

    The real script is gitignored in this repo, so the committed .sample is the fallback.
    """
    if not name:
        return ""
    suffix = ".ps1" if os.name == "nt" else ".sh"
    base = name[:-3] if name.lower().endswith(".sample") else name
    if base.lower().endswith(suffix):
        base = base[: -len(suffix)]
    for candidate in (f"{base}{suffix}", f"{base}.sample", base):
        if (REPO / candidate).is_file():
            return candidate
    print(f"note: verify_env_script {name!r} not found; running checks without it", file=sys.stderr)
    return ""


def verification_command(cmd: str, cfg: dict) -> str:
    """Wrap a verify command so it runs with the project's environment variables set."""
    cmd = normalize_command(cmd)
    script = cfg["verify_env_script"]
    if not script:
        return cmd
    if os.name == "nt":
        return f'powershell -NoProfile -ExecutionPolicy Bypass -Command ". .\\{script}; {cmd}"'
    return f'. "./{script}"; {cmd}'


def check_output_color(line: str) -> str:
    """Colour a line of a verification command's output by what it says (generic keywords)."""
    text = line.lower()
    if any(word in text for word in ("failed", "failure", "error", "timed out")):
        return "error"
    if any(word in text for word in ("passed", "success")):
        return "ok"
    return ""


def run_verification(task: Task, cfg: dict) -> list[str]:
    """Run the harness test commands after an attempt. Returns failure notes ([] when green).

    The runner, not the model, decides whether the tests passed: a DONE claim on failing
    tests is turned back into an open attempt on the same task.
    """
    notes: list[str] = []
    if not cfg["verify_commands"]:
        print("note: no verify_commands configured; the runner cannot verify tests itself",
              file=sys.stderr)
        return notes

    for raw in cfg["verify_commands"]:
        cmd = verification_command(raw, cfg)
        if not cmd:
            continue
        say(f"\n--- verification ({task.slug}): {cmd} ---", "header")
        try:
            proc = subprocess.run(
                cmd,
                shell=True,
                cwd=REPO,
                capture_output=True,
                text=True,
                encoding="utf-8",
                errors="replace",
                timeout=cfg["verify_timeout_seconds"],
            )
            code = proc.returncode
            output = (proc.stdout or "") + "\n" + (proc.stderr or "")
            timed_out = False
        except subprocess.TimeoutExpired:
            code, output, timed_out = -1, "", True
        tail = [line.strip() for line in output.splitlines() if line.strip()]
        tail = tail[-cfg["log_tail_lines"]:]
        for line in tail:
            say(f"    {line}", check_output_color(line))
        if timed_out:
            note = f"timed out after {cfg['verify_timeout_seconds']}s: {cmd}"
        elif code != 0:
            note = f"exited {code}: {cmd}"
        else:
            note = ""
        if note:
            say(f"[pi] verification FAILED: {note}", "error")
            notes.append(note)
            if tail:
                excerpt = " | ".join(tail[-12:])
                notes.append(f"output tail: {excerpt[:1200]}")
        else:
            say(f"[pi] verification passed: {cmd}", "ok")
    return notes


def test_coverage_note(task: Task, cfg: dict, baseline: set[str] | None) -> str:
    """Require a test file in the diff when the attempt changed production code."""
    if not cfg["require_tests"]:
        return ""
    now = changed_files()
    if now is None or baseline is None:
        print("note: git is unavailable; skipping the test-presence check", file=sys.stderr)
        return ""
    changed = now - baseline
    source = sorted(p for p in changed if matches_any(p, cfg["source_globs"]))
    if not source:
        return ""  # harness/docs-only task: nothing here that needs a test
    tests = sorted(p for p in changed if matches_any(p, cfg["test_globs"]))
    if tests:
        return ""
    shown = ", ".join(source[:8]) + (" ..." if len(source) > 8 else "")
    say(f"[pi] no test file changed for: {shown}", "error")
    return (
        "no test file was added or updated although production code changed "
        f"(tests must match {', '.join(cfg['test_globs'])}); changed source: {shown}"
    )


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
            if isinstance(expected, list):
                if not isinstance(value, list) or any(not isinstance(item, str) for item in value):
                    print(f"warning: {key} must be a list of strings", file=sys.stderr)
                    continue
                cfg[key] = [item.strip() for item in value if item.strip()]
                continue
            if isinstance(expected, str) and value and not isinstance(value, str):
                print(f"warning: {key} must be a string", file=sys.stderr)
                continue
            cfg[key] = value

    # Resolve once, so a missing script is reported at startup instead of per command.
    cfg["verify_env_script"] = resolve_env_script(cfg["verify_env_script"])
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
    if cfg["max_attempts"] < 1:
        print("warning: max_attempts must be at least 1, using 1", file=sys.stderr)
        cfg["max_attempts"] = 1
    if cfg["verify_timeout_seconds"] < 10:
        print("warning: verify_timeout_seconds is too small, using 10", file=sys.stderr)
        cfg["verify_timeout_seconds"] = 10
    # Colour decision, once per run: `color` is auto/always/never (see color_requested) and
    # cfg["use_color"] is the boolean every renderer below then uses.
    cfg["use_color"] = color_requested(cfg["color"])
    set_colors(cfg["use_color"])


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
    # Completed tasks are moved to archived-tasks/, so dependency checks must also
    # look there or a done prerequisite would suddenly count as "waiting on".
    archived = [read_task(p) for p in sorted(ARCHIVED_DIR.glob("*.md"))] if ARCHIVED_DIR.is_dir() else []
    by_slug = {t.slug: t for t in archived + tasks}

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
            say(f"skip  {task.slug}: waiting on {', '.join(unmet)}", "warning")
            continue
        selected.append(task)

    return selected[: args.limit] if args.limit else selected


@dataclass
class Attempt:
    """What a single Pi session claimed, before the harness checks it."""

    status: str
    note: str
    tail: list[str]
    log_name: str
    exit_code: int


def run_attempt(
    task: Task, cfg: dict, args: argparse.Namespace, attempt: int, prior_note: str = ""
) -> Attempt:
    """One Pi session on this task. Returns the model's claim; run_task decides if it holds."""
    cmd = build_command(task, cfg, attempt, prior_note)
    stdin_text = build_stdin(task, cfg, prior_note) or ""
    stamp = dt.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    def show(cmd_line: str) -> None:
        say(cmd_line, "prompt")
        if stdin_text:
            say(f"--- stdin ---\n{stdin_text}\n--- end of stdin ---", "prompt")

    say(f"\n=== {task.slug} (attempt {attempt}) ===", "header")
    if cfg["stream_output"]:
        say("live output: [thinking] model reasoning, [assistant] its reply, "
            "[tool] every command/edit it runs and its result "
            "(each in its own colour" + (", colours off" if not cfg["use_color"] else "") + ")")
    if prior_note:
        say(f"[pi] handed to the model:\n{prior_note}", "prompt")
    if args.dry_run:
        show(" ".join(f'"{part}"' if " " in part else part for part in cmd))
        return Attempt(TODO, "dry run", [], "", 0)

    LOGS_DIR.mkdir(parents=True, exist_ok=True)
    log_path = LOGS_DIR / f"{task.slug}-run{attempt}.log"

    try:
        # The long instruction block is piped in, so the command line stays short
        # (see the note in README.md about Windows .cmd shims).
        proc = subprocess.Popen(
            cmd,
            cwd=REPO,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
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

    try:
        proc.stdin.write(stdin_text.encode("utf-8"))
        proc.stdin.close()
    except OSError:
        pass

    output, errput, assistant, transcript, timed_out, json_mode = stream_pi_process(proc, cfg)
    # The prompt is recorded too: a retry is only reviewable if you can see what the
    # previous failure was handed to the model.
    log_path.write_text(
        f"exit: {proc.returncode}\n\n--- prompt (stdin) ---\n{stdin_text}\n"
        f"\n--- live transcript ---\n{transcript}\n"
        f"\n--- stdout (raw) ---\n{output}\n\n--- stderr ---\n{errput}\n",
        encoding="utf-8",
    )
    if timed_out:
        say(f"{task.slug}: timed out after {cfg['timeout_seconds']}s", "error")
        return Attempt(
            FAILED,
            f"timed out after {cfg['timeout_seconds']}s",
            [],
            log_path.name,
            proc.returncode,
        )

    # Prefer the authoritative assistant text for the STATUS line. In JSON mode the raw
    # stream contains the same text as escaped JSON, so it is only a last resort.
    matches = STATUS_RE.findall(assistant or ("" if json_mode else output))
    if not matches and json_mode and assistant == "":
        matches = STATUS_RE.findall(output)
    if matches:
        status = matches[-1][0].lower()
        note = matches[-1][1].strip()
    else:
        status = DONE if proc.returncode == 0 else FAILED
        note = "no STATUS line in output"

    tail = [line.strip() for line in (assistant or output).strip().splitlines() if line.strip()]
    tail = tail[-cfg["log_tail_lines"]:]
    report_key = {DONE: "ok", BLOCKED: "warning", FAILED: "error"}.get(status, "pi")
    say(f"{task.slug}: model reported {status} (exit {proc.returncode}, log: "
        f"{log_path.relative_to(REPO).as_posix()})", report_key)
    return Attempt(status, note, tail, log_path.name, proc.returncode)


def run_task(task: Task, cfg: dict, args: argparse.Namespace) -> str:
    """Run one task, looping on that same task until the harness checks pass.

    A session that claims DONE is not enough: the runner re-runs verify_commands itself
    (and, with require_tests, checks that production changes came with test changes). If a
    check fails, the task stays open and a fresh Pi session gets it back together with the
    failing output, until max_attempts is used up.
    """
    baseline = changed_files()
    max_attempts = max(1, cfg["max_attempts"])
    prior_note = ""
    rounds = 0

    while True:
        rounds += 1
        current = read_task(task.path)
        attempt = current.attempts + 1
        if rounds > max_attempts:
            # Cannot happen while write_task increments attempts; it keeps this loop bounded
            # even if a task file was edited by hand mid-run.
            print(f"{current.slug}: stopped after {rounds - 1} attempt(s)", file=sys.stderr)
            return FAILED
        result = run_attempt(current, cfg, args, attempt, prior_note)
        if args.dry_run:
            return TODO

        stamp = dt.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

        if result.status == BLOCKED:
            entries = [f"{stamp} attempt {attempt}: status=blocked exit={result.exit_code} "
                       f"log={result.log_name} {result.note}".strip()]
            entries += [f"  {line}" for line in result.tail]
            write_task(current, BLOCKED, entries)
            return BLOCKED

        problems: list[str] = []
        if result.status == DONE:
            missing = test_coverage_note(current, cfg, baseline)
            if missing:
                problems.append(missing)
            problems += run_verification(current, cfg)
        else:
            problems.append(f"the model reported {result.status.upper()}: {result.note or 'no detail'}")

        if not problems:
            entries = [f"{stamp} attempt {attempt}: status=done exit={result.exit_code} "
                       f"log={result.log_name} {result.note}".strip()]
            entries += [f"  {line}" for line in result.tail]
            write_task(current, DONE, entries)
            if cfg["verify_commands"] or cfg["require_tests"]:
                say(f"{current.slug}: done (attempt {attempt} passed the harness checks)", "ok")
            else:
                say(f"{current.slug}: done (attempt {attempt}; no harness checks configured)", "ok")
            dest = archive_task(current)
            say(f"{current.slug}: archived to {dest.relative_to(REPO).as_posix()}", "ok")
            return DONE

        exhausted = rounds >= max_attempts
        next_status = FAILED if exhausted else TODO
        reason = "; ".join(problems)
        entries = [f"{stamp} attempt {attempt}: status={next_status} "
                   f"(model={result.status}) exit={result.exit_code} log={result.log_name} {reason}".strip()]
        entries += [f"  check: {problem}" for problem in problems]
        entries += [f"  {line}" for line in result.tail]
        write_task(current, next_status, entries)

        if exhausted:
            say(f"{current.slug}: failed after {attempt} attempt(s) - {reason[:300]}", "error")
            return FAILED

        say(f"{current.slug}: checks failed on attempt {attempt}; re-running the same task "
            f"({max_attempts - rounds} session(s) left for this task)", "error")
        prior_note = (
            f"Attempt {attempt} of this task did not pass the harness checks:\n"
            + "\n".join(f"- {problem}" for problem in problems)[:2000]
            + "\nFix the code and/or the tests, re-run the verification commands yourself, "
            "and only report STATUS: DONE once they are green."
        )


def main() -> int:
    parser = argparse.ArgumentParser(description="Sequential task runner for Pi.")
    parser.add_argument("--limit", type=int, default=0, help="maximum number of tasks in this run")
    parser.add_argument("--only", nargs="+", metavar="SLUG", help="run only these task slugs")
    parser.add_argument("--retry-failed", action="store_true", help="also pick up failed/blocked tasks")
    parser.add_argument("--keep-going-blocked", action="store_true", help="do not stop when a task reports BLOCKED")
    parser.add_argument("--model", help="Pi model, e.g. sonnet:high")
    parser.add_argument("--thinking", help="off|minimal|low|medium|high|xhigh|max")
    parser.add_argument("--tools", help="Pi tool allowlist, e.g. read,grep,find,ls,edit,write")
    parser.add_argument("--timeout", type=int, help="seconds allowed per Pi session")
    parser.add_argument(
        "--max-attempts",
        type=int,
        help="how many Pi sessions one task may use before it is marked failed (default 3)",
    )
    parser.add_argument(
        "--verify",
        nargs="+",
        metavar="CMD",
        help="test commands the runner re-runs after every attempt; overrides verify_commands",
    )
    parser.add_argument(
        "--no-verify",
        action="store_true",
        help="skip the runner-side test commands (the model is then trusted on its own STATUS)",
    )
    parser.add_argument(
        "--require-tests",
        action="store_true",
        help="fail an attempt that changed production code without touching a test file",
    )
    parser.add_argument(
        "--no-require-tests",
        action="store_true",
        help="allow production changes with no test changes",
    )
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
    parser.add_argument(
        "--no-stream",
        action="store_true",
        help="do not print what Pi is thinking/doing live (output still lands in logs/)",
    )
    parser.add_argument(
        "--stream",
        action="store_true",
        help="force live output even when config disables it",
    )
    parser.add_argument(
        "--no-thinking",
        action="store_true",
        help="live-print tools and replies, but not thinking blocks",
    )
    parser.add_argument(
        "--color",
        choices=sorted(VALID_COLOR),
        help="colour the live output per action kind: auto (only on a terminal), always, never",
    )
    parser.add_argument(
        "--no-color",
        action="store_true",
        help="same as --color never (also honoured: the NO_COLOR environment variable)",
    )
    parser.add_argument(
        "--raw",
        action="store_true",
        help="also print Pi's raw JSONL events (full raw stream is always kept in logs/)",
    )
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
    if args.max_attempts:
        cfg["max_attempts"] = args.max_attempts
    if args.verify:
        cfg["verify_commands"] = [cmd for cmd in args.verify if cmd.strip()]
    if args.no_verify:
        cfg["verify_commands"] = []
    if args.require_tests:
        cfg["require_tests"] = True
    if args.no_require_tests:
        cfg["require_tests"] = False
    if args.ephemeral:
        cfg["ephemeral_sessions"] = True
    if args.offline:
        cfg["offline"] = True
    if args.trust:
        cfg["project_trust"] = args.trust
    if args.no_stream:
        cfg["stream_output"] = False
    if args.stream:
        cfg["stream_output"] = True
    if args.no_thinking:
        cfg["stream_thinking"] = False
    if args.color:
        cfg["color"] = args.color
    if args.no_color:
        cfg["color"] = "never"
    if args.raw:
        cfg["stream_raw"] = True
    if args.thinking and args.thinking not in VALID_THINKING:
        print(f"error: --thinking must be one of {', '.join(sorted(VALID_THINKING))}", file=sys.stderr)
        return 2
    validate_config(cfg)

    if not TASKS_DIR.is_dir():
        print(f"error: {TASKS_DIR} does not exist", file=sys.stderr)
        return 2

    tasks = [read_task(p) for p in sorted(TASKS_DIR.glob("*.md"))]

    if args.list:
        colour = {TODO: "prompt", DONE: "ok", BLOCKED: "warning", FAILED: "error"}
        for task in tasks:
            deps = f" (depends on {', '.join(task.depends_on)})" if task.depends_on else ""
            attempts = f" attempts={task.attempts}" if task.attempts else ""
            say(f"{task.status:<8} {task.slug}{attempts}{deps}", colour.get(task.status, "pi"))
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

    say(f"{len(selected)} task(s) queued: {', '.join(t.slug for t in selected)}", "header")
    results: dict[str, str] = {}
    for task in selected:
        current = read_task(task.path)
        status = run_task(current, cfg, args)
        results[current.slug] = status
        if status == BLOCKED and not args.keep_going_blocked:
            say("stopping: a task needs human input", "warning")
            break

    not_done = [slug for slug, status in results.items() if status not in (DONE, TODO)]
    done = [slug for slug, status in results.items() if status == DONE]
    summary = f"run summary: {len(done)} done"
    if not_done:
        summary += f", {len(not_done)} not done: {', '.join(not_done)}"
    say(f"\n{summary}", "ok" if not not_done else "error")
    return 0 if not not_done else 1


if __name__ == "__main__":
    sys.exit(main())
