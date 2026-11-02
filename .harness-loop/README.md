# Task backlog harness (Pi + Python)

Point an LLM coding agent at a folder of Markdown task files. A Python runner picks the
next ready task, gives it to a **fresh Pi session**, records the outcome in the task file,
and moves on to the next task.

```
.harness-loop/
├── runner.py          # the loop (stdlib only, Python 3.9+)
├── config.json        # defaults for the run
├── rules.md           # appended to every session as system prompt
├── tasks/             # one .md file per task, frontmatter = state
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
cd c:\p\megadiagram
python .harness-loop\runner.py --dry-run   # show planned pi calls, change nothing
python .harness-loop\runner.py --list      # task board
python .harness-loop\runner.py             # loop until the backlog is empty or blocked
python .harness-loop\runner.py --limit 3
python .harness-loop\runner.py --only 02-copy-to-clipboard-button-for-ids --thinking low
python .harness-loop\runner.py --retry-failed --model claude-sonnet-4.5 --thinking high
```

Flags:

| Flag | Effect |
| --- | --- |
| `--limit N` | stop after N attempts |
| `--only SLUG` | restrict to task slug(s) or bare suffix (`03`) |
| `--retry-failed` | treat `failed` tasks as workable |
| `--keep-going-blocked` | do not stop when a task reports `BLOCKED` |
| `--model`, `--thinking`, `--tools` | passed straight to `pi` |
| `--timeout N` | kill a task after N seconds (default 1500) |
| `--ephemeral` | `pi --no-session`: nothing written to `sessions/` |
| `--session-log` | also write a JSONL transcript into `sessions/` |
| `--no-stream` | do not print the model's thinking/tools/replies live |
| `--stream` | force live printing even if config disables it |
| `--no-thinking` | live-print tools and replies, but hide thinking blocks |
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
[tool bash] {"command": "rg -n \"export\" app/src"}
    -> bash ok 143ms: app/src/main/java/.../ExportActivity.kt:42 ...
[assistant] I'll add a CSV writer next to the existing JSON exporter.
STATUS: DONE added CsvExporter.kt and CsvExporterTest.kt
```

| Prefix | Source event |
| --- | --- |
| `[thinking]` | `thinking_delta` / `thinking_end` — the model's reasoning, streamed live |
| `[assistant]` | `text_delta` / `text_end` — the model's reply, streamed live |
| `[tool <name>]` | `tool_execution_start` (arguments) and `tool_execution_end` (result, ms, error flag) |
| `[pi]` | runner notes: turns, retries, compaction, Pi `stderr`, agent start/end |

Config keys: `stream_output` (default `true`), `stream_thinking` (default `true`, `false`
hides reasoning), `stream_raw` (default `false`, prints the JSONL records verbatim).
Tool arguments are truncated to 400 characters and tool results to 1500 in the live view;
nothing is truncated in the logs.

The full raw JSONL stream plus a copy of the rendered live view are always written to
`logs/<slug>-runN.log`, so a run you did not watch is still fully reviewable afterwards.
If the installed Pi does not support `--mode json`, the runner falls back to echoing Pi's
plain-text stdout line by line, which still streams but without thinking/tool structure.

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
attempt; the prose you write is left alone. `## Questions for the human` sections are
yours to add (Pi cannot ask you questions in print mode, so it writes them there instead).

## Selection rules

`todo` tasks whose `depends_on` prerequisites are `done`, in filename order. Selection
stops at the first blocking `blocked` task unless `--keep-going-blocked` is passed.

## Outcome protocol

The prompt tells every agent to end its final message with exactly one of:

```
STATUS: DONE | BLOCKED | FAILED
```

The runner parses that line from the assistant text (falling back to the process exit code
when it is missing), and maps the result to the frontmatter status. Both the raw stdout and
the rendered live view per attempt land in `logs/`.

## Notes

- Pi resolves `@file` references from its working directory, so the task file is passed as
  `@.harness-loop/tasks/NN-slug.md` and the process cwd is pinned to the repo root.
- Runner cwd is always the repo root (`c:\p\megadiagram`), never `.harness-loop`, so agents
  edit the real project and see the same paths you do in the editor.
- Multiple runners at once share `sessions/`, and Pi 0.15 added an advisory lock per session
  file; keep one runner per worktree, or use `--limit 1` per task from a wrapper.
- Parallelism across worktrees is a wrapper concern: fan out `--only <slug>` invocations.
- No git operations are performed by the runner. The prompt also instructs the agent not to
  commit or push; you review and commit between runs.
