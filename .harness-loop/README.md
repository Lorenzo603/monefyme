# Task backlog harness (Pi + Python)

Point an LLM coding agent at a folder of Markdown task files. A Python runner picks the
next ready task, gives it to a **fresh Pi session**, records the outcome in the task file,
and moves on to the next task.

```
.harness-loop/
├── runner.py          # the loop (stdlib only, Python 3.9+)
├── selftest.py        # tests for the loop itself (fake pi, throwaway repo)
├── config.json        # defaults for the run
├── rules.md           # appended to every session as system prompt
├── tasks/             # one .md file per task, frontmatter = state
├── archived-tasks/    # completed tasks, moved here automatically by the runner
├── sessions/          # Pi session files (gitignored)
└── logs/              # full pi stdout per attempt (gitignored)
```

Each loop is one `pi --print` process with its own session id, so no conversation
context is carried between tasks — only what is written in the task file and in `rules.md`.
The runner streams that process live, so you watch the model think, answer and run tools
while it works (see [Watch the model work](#watch-the-model-work)).

## Install Pi

Pi is a Node CLI (Node 22.19+), installed separately from this Python runner:

```powershell
powershell -c "irm https://pi.dev/install.ps1 | iex"
pi --version
npm install -g @mariozechner/pi-coding-agent   # if you need it from npm
```

Auth: `export ANTHROPIC_API_KEY=...` (or `pi` with no flag to pick up cached OAuth).

## Run

```powershell
cd <repo root>          # the runner must be started from the repository root
python .harness-loop\runner.py --dry-run   # show planned pi calls, change nothing
python .harness-loop\runner.py --list      # task board
python .harness-loop\runner.py             # loop until the backlog is empty or blocked
python .harness-loop\runner.py --limit 3
python .harness-loop\runner.py --only 02-copy-to-clipboard-button-for-ids --thinking low
python .harness-loop\runner.py --retry-failed --model claude-sonnet-4.5 --thinking high
python .harness-loop\runner.py --only 03-add-unit-and-integraton-tests --max-attempts 5
python .harness-loop\runner.py --verify "<this repo's test command>"   # override verify_commands
python .harness-loop\runner.py --no-verify        # trust the model's own STATUS line
```

Flags:

| Flag | Effect |
| --- | --- |
| `--limit N` | stop after N attempts |
| `--only SLUG` | restrict to task slug(s) or bare suffix (`03`) |
| `--retry-failed` | treat `failed` tasks as workable |
| `--keep-going-blocked` | do not stop when a task reports `BLOCKED` |
| `--model`, `--thinking`, `--tools` | passed straight to `pi` |
| `--timeout N` | kill a Pi session after N seconds (default 1500) |
| `--max-attempts N` | how many Pi sessions one task may use before it is marked `failed` (default 3) |
| `--verify CMD ...` | test commands the runner re-runs after every attempt; overrides `verify_commands` |
| `--no-verify` | skip the runner-side test commands |
| `--require-tests` / `--no-require-tests` | force / relax "production change ⇒ test change" |
| `--ephemeral` | `pi --no-session`: nothing written to `sessions/` |
| `--session-log` | also write a JSONL transcript into `sessions/` |
| `--no-stream` | do not print the model's thinking/tools/replies live |
| `--stream` | force live printing even if config disables it |
| `--no-thinking` | live-print tools and replies, but hide thinking blocks |
| `--color auto|always|never` | colour the live output per action kind (`auto` = only on a terminal) |
| `--no-color` | same as `--color never` (the `NO_COLOR` variable does the same) |
| `--raw` | also dump Pi's raw JSONL events to the console |
| `--pi-bin PATH` | use a different pi binary |
| `--dry-run` / `--list` | inspect without invoking Pi |

Exit codes: `0` backlog clear (or dry run), `1` a task failed or is blocked, `2` Pi missing / bad config.

## Watch the model work

By default the runner starts each attempt as `pi --print --mode json` and renders Pi's
session events as they arrive, so the console shows what the model is doing at any moment:

```text
=== 04-csv-backup-restore (attempt 1) ===
[pi] --- turn 1 ---
[thinking] The task asks for CSV backup; let me find the existing export code first.
[tool bash] {"command": "rg -n \"export\" src"}
    -> bash ok 143ms: src/.../ExportService.kt:42 ...
[assistant] I'll add a CSV writer next to the existing JSON exporter.
STATUS: DONE added CsvExporter.kt and CsvExporterTest.kt
```

| Prefix | Source event |
| --- | --- |
| `[thinking]` | `thinking_delta` / `thinking_end` — the model's reasoning, streamed live |
| `[assistant]` | `text_delta` / `text_end` — the model's reply, streamed live |
| `[tool <name>]` | `tool_execution_start` (arguments) and `tool_execution_end` (result, ms, error flag) |
| `[pi]` | runner notes: turns, retries, compaction, Pi `stderr`, agent start/end |

### Colours

Each kind of action is printed in its own colour so the live view is scannable at a glance:

| Kind | Colour | What it covers |
| --- | --- | --- |
| `thinking` | dim grey | the model's reasoning, streamed live |
| `assistant` | green | the model's answer, streamed live (also a plain-text Pi's stdout) |
| `tool` | cyan | a tool call and its arguments, `toolcall_start` |
| `tool_result` | blue | what a tool returned (`tool_execution_update` / `_end`) |
| `pi` | magenta | runner notes: turns, agent start/end, compaction, retries of Pi |
| `prompt` | yellow | what the runner hands the model: the piped prompt, the failing output on a retry |
| `warning` | bright yellow | Pi `stderr`, skipped tasks |
| `error` | red | an error tool result, an assistant/stream error, a failed retry or check |
| `header` | bold | attempt and verification headers |

`PALETTE` in `runner.py` holds the codes; verification output lines are coloured by what they
say (`failed`/`error` red, `passed`/`success` green) via `check_output_color`.

Colours are the **runner's** work, not a shell feature: Pi runs as a child process in
`--mode json`, so its own stdout is JSON and can never be coloured, and the runner is the one
that renders that stream. What it writes are plain ANSI escape codes on its own stdout, which
any modern terminal interprets — Git Bash/MSYS2, macOS Terminal and iTerm2 (bash or zsh),
Windows Terminal / conhost. Nothing bash/zsh/Git-Bash-specific is configured, so the harness
stays repository and shell agnostic. The only real dependency — "is this a terminal that
understands ANSI?" — is detected instead of assumed (`color_requested`):

- output that is not a terminal (a pipe, `> file`, the `logs/` writer) is never coloured,
- `NO_COLOR` (any value) disables colour for everything, `FORCE_COLOR` enables it anyway,
  `TERM=dumb` disables it — the usual conventions, so a wrapper can override the runner,
- on Windows, VT processing is switched on with `SetConsoleMode`
  (`ENABLE_VIRTUAL_TERMINAL_PROCESSING`, stdlib `ctypes`) before anything is coloured; if that
  fails (an old conhost) the runner prints plain text rather than raw escape codes,
- `--color always` forces the codes even when the terminal check fails, except on a Windows
  console whose VT mode cannot be enabled — there it prints plain and says why.

Config keys: `color` (`auto` by default, `always`, `never`).
Colours never reach `logs/`: the transcript the runner writes there is kept escape-free, so a
log is still plain text you can grep, pipe or diff.

Config keys: `stream_output` (default `true`), `stream_thinking` (default `true`, `false`
hides reasoning), `stream_raw` (default `false`, prints the JSONL records verbatim),
`color` (default `auto`).
Tool arguments are truncated to 400 characters and tool results to 1500 in the live view;
nothing is truncated in the logs.

The full raw JSONL stream plus a copy of the rendered live view are always written to
`logs/<slug>-runN.log`, so a run you did not watch is still fully reviewable afterwards. The
prompt piped to Pi is logged too, which is what makes a retry reviewable: you can read exactly
which failing output the next session was told about.
If the installed Pi does not support `--mode json`, the runner falls back to echoing Pi's
plain-text stdout line by line, which still streams but without thinking/tool structure.

## Tests and the verification loop

A `STATUS: DONE` line is only a claim; the runner checks it. After an attempt that claims DONE,
in the repo root and in the same shell:

1. **test-presence check** (`require_tests`): if the attempt changed files matching
   `source_globs` but no file matching `test_globs`, the attempt is rejected — the model had to add
   or update a test. Harness/docs-only tasks are exempt because they touch no source glob.
   `*` in these globs also crosses `/`. Uses `git diff --name-only HEAD` plus untracked files,
   measured against the working tree as it was when the task started.
2. **test commands** (`verify_commands`): each command is run with `verify_timeout_seconds`
   and its output tail printed. Any non-zero exit is a failure.

If either check fails, the task is **not** archived and **not** abandoned: its status goes back
to `todo`, and the next loop iteration hands the *same* task to a fresh Pi session whose prompt
includes the failing output and "only report DONE once the commands are green". That repeats up
to `max_attempts` times (default 3, each session is a separate `sessions/` file and `logs/`
entry); after that the task is marked `failed` and you can pick it up with `--retry-failed`.
An attempt that reports `BLOCKED` is never retried — it waits for you.

`config.json` holds the per-repository settings; the generic parts are the defaults in
`runner.py`. Fill it in from the values recorded in the repository's AGENTS.md:

```json
"max_attempts": 3,
"verify_commands": ["<this repo's test commands, run from the repo root>"],
"verify_timeout_seconds": 900,
"verify_env_script": "<optional env-setup script, without extension>",
"require_tests": true,
"source_globs": ["<paths that count as production code>"],
"test_globs": ["<paths that count as tests>"]
```

`verify_commands` are shell snippets run from the repo root; a leading `./` is rewritten to
`.\` on Windows. `verify_env_script` dot-sources the repo's env-setup script (`.ps1` on Windows,
`.sh` on macOS/Linux) in the same shell as the command, falling back to the committed `.sample`
when the real, gitignored script is absent — otherwise a runner started from a plain terminal is
missing the variables the project needs. Keep commands that need a prerequisite you cannot
guarantee (a running emulator, a live service, network access) out of `verify_commands`,
otherwise every attempt fails for an environmental reason. CLI overrides: `--verify`,
`--no-verify`, `--max-attempts`, `--require-tests`, `--no-require-tests`.

## Task file format

```markdown
---
status: todo          # todo | done | blocked | failed
attempts: 0
title: "copy to clipboard button for IDs"
source: backlog.md
depends_on: ["03-version-the-graph"]   # optional gate: these must be done first
---

## Task

copy to clipboard button for IDs

## Definition of done

- [ ] ...              # optional, add it yourself
```

The runner rewrites `status`, `attempts` and a capped `## Run log` section after every
attempt; the prose you write is left alone. `attempts` counts Pi sessions, so a task whose tests
were red shows `attempts: 2` plus one summary line per attempt
(`... status=todo (model=done) ... check: exited 1: <verify_commands entry>`). Older attempts
keep only their summary line; their full transcript stays in `logs/`. A frontmatter whose closing
`---` an older runner dropped is repaired on the next write.
`## Questions for the human` sections are yours to add (Pi cannot ask you questions in print
mode, so it writes them there instead).

## Selection rules

`todo` tasks whose `depends_on` prerequisites are `done`, in filename order. Selection
stops at the first blocking `blocked` task unless `--keep-going-blocked` is passed.
When a task finishes with `STATUS: DONE` the runner moves its file to `archived-tasks/`
(done prerequisites there still satisfy `depends_on`), so `tasks/` only holds open work.

## Outcome protocol

The prompt tells every agent to end its final message with exactly one of:

```
STATUS: DONE | BLOCKED | FAILED
```

The runner parses that line from the assistant text (falling back to the process exit code
when it is missing), and maps the result to the frontmatter status. Both the raw stdout and
the rendered live view per attempt land in `logs/`.

`DONE` is then gated by the verification loop above: DONE that does not survive
`verify_commands` becomes `todo` (retry) or, once `max_attempts` is used up, `failed`.

## Self-test of the harness itself

The verification loop is covered by a self-contained test that runs the real `runner.py` against a
throwaway repo (its own git repo, a fake `pi`, a fake test command) and asserts what the runner
recorded. It never touches your tasks, sessions or logs:

```powershell
python .harness-loop\selftest.py     # 9 scenarios, exit 0 when all pass
```

| Scenario | Asserts |
| --- | --- |
| A | DONE with no test + red tests is retried **on the same task**, the next session is handed the failing output, the green attempt archives it |
| B | permanently red tests: retried until `max_attempts`, then `failed`, never archived |
| C | docs-only change is exempt from the test requirement |
| D | a source change backed by a test is accepted in one attempt |
| E | `--no-verify --no-require-tests` leaves the STATUS line in charge |
| F | `STATUS: BLOCKED` is not retried |
| G | a frontmatter whose closing `---` an older runner dropped is read and repaired; `depends_on: nothing` is not a prerequisite |
| H | live-view colours: one ANSI colour per action kind, `NO_COLOR`/`FORCE_COLOR`/`auto` rules, and a transcript with no escape codes |
| I | a real runner run: `--color always` paints the console, `--no-color` and the piped default stay plain, `logs/` hold no escape codes |

## Notes

- Pi resolves `@file` references from its working directory, so the task file is passed as
  `@.harness-loop/tasks/NN-slug.md` and the process cwd is pinned to the repo root.
- Runner cwd is always the repository root, never `.harness-loop`, so agents
  edit the real project and see the same paths you do in the editor.
- Multiple runners at once share `sessions/`, and Pi 0.15 added an advisory lock per session
  file; keep one runner per worktree, or use `--limit 1` per task from a wrapper.
- Parallelism across worktrees is a wrapper concern: fan out `--only <slug>` invocations.
- No git operations are performed by the runner. The prompt also instructs the agent not to
  commit or push; you review and commit between runs.
