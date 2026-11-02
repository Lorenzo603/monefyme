---
status: done
attempts: 1
---

---
status: todo
attempts: 1
title: "make sure to add tests"
source: backlog.md
depends_on: nothing

## Task

Make sure that the model adds tests when implementing a task. and make sure they loop on the same task if tests fail.

## Run log

- 2026-11-02 19:53:29 attempt 1: status=done exit=0 log=02-HARNESS-add-tests-run1.log
-   - `run_task()` is now a per‑task loop: `run_attempt()` (one Pi session) → harness checks → retry **the same task** with a `prior_note` in the next session's prompt that quotes what failed. Retries until `max_attempts` (default 3), then `status: failed`. `BLOCKED` is never retried; a rejected attempt goes back to `status: todo` so the next runner invocation picks it up again.
-   - New harness checks after any `STATUS: DONE` claim:
-   - `run_verification()` runs `verify_commands` from the repo root (`./gradlew testDebugUnitTest`), prints the output tail, treats any non‑zero exit / timeout as failure. `verify_env_script` dot‑sources `set-env-variables.ps1`/`.sh` (falls back to the committed `.sample`) in the same shell; `./` is rewritten to `.\` on Windows.
-   - `test_coverage_note()` rejects a DONE attempt that changed `source_globs` (`app/src/main/*`) without touching `test_globs` (`app/src/test/*`, `app/src/androidTest/*`), measured against the working tree at task start via `git diff --name-only HEAD` + untracked files.
-   - Prompt now states the tests requirement and lists the verification commands the runner enforces; `compact_prompt` matches. Logs now include the piped prompt, so a retry is reviewable.
-   - Fixed two pre-existing bugs the loop depends on: `write_task` dropped the closing frontmatter `---` (so `attempts` silently reset and a retried task reused its own attempt number) and duplicated the `## Run log` section; added `repair_frontmatter()` and a per‑attempt summary history. `split_deps()` no longer treats `depends_on: nothing` as a prerequisite (this task file uses exactly that).
-   - New flags: `--max-attempts`, `--verify`, `--no-verify`, `--require-tests`, `--no-require-tests`; new config keys in **`.harness-loop/config.json`**.
-   **`.harness-loop/rules.md`**: tests are part of every behaviour change, iterate until green, never report DONE on red/skipped tests, don't weaken/`@Ignore` existing tests, and flag environmental test failures on the STATUS line.
-   **`.harness-loop/README.md`**: "Tests and the verification loop" + self-test sections, new flags/config keys, log format.
-   **New `.harness-loop/selftest.py`** — tests for the harness itself (throwaway git repo + fake `pi` + fake test command; touches no real task/session/log).
-   Verification run from the repo root:
-   - `python .harness-loop/selftest.py` → `ALL SELF-TESTS PASSED` (A red tests → same‑task retry → archived green; B red forever → failed, not archived; C docs‑only exempt; D source+test accepted at once; E escape hatch; F BLOCKED not retried; G frontmatter repair / `depends_on: nothing`).
-   - `./gradlew testDebugUnitTest` after `.\set-env-variables.ps1` → `BUILD SUCCESSFUL`, both directly and through the runner's `run_verification()` path.
-   - `python .harness-loop/runner.py --list` / `--dry-run` → normal, new prompt text shown, no config warnings. `py_compile` clean.
-   Caveats: `connectedDebugAndroidTest` was **not** run (`adb devices` shows no emulator attached) and is deliberately kept out of `verify_commands` so attempts don't fail for environmental reasons — add it there once an emulator is always up. `require_tests` only inspects `app/src/main/*`; a genuinely test‑free production change needs `--no-require-tests`. The already‑archived `01-HARNESS-...` task file still contains a duplicated frontmatter block written by the old buggy `write_task` — readable as `status: done`, left untouched. I did not modify my own task file (its unterminated frontmatter is repaired automatically on the runner's next write). Assumption: "add tests / loop on failing tests" is enforced at harness level; app unit/integration tests themselves remain the separate backlog task `add-unit-and-integraton-tests.md`.
-   STATUS: DONE
-   Now let me look at the existing task/prompt flow more closely and check how verification could run on this machine.
-   Now I'll implement the change in the runner: a post-run verification step plus a same-task retry loop.
-   Now let me build a throwaway end-to-end harness test (fake `pi`) to prove the loop behaves correctly.
-   Found a pre-existing bug in `write_task` (it drops the closing frontmatter delimiter, so `attempts` stopped incrementing and run logs duplicated). Fixing it, since the retry loop depends on `attempts`:
-   Adding frontmatter repair (an older `write_task` dropped the closing `---`, which silently reset `attempts`):
-   The loop works end to end. Now I'll promote the throwaway scaffolding into a permanent self-test for the harness:
-   Done. Summary of the change:
-   **`.harness-loop/runner.py`** (the enforcement, no model cooperation required)
-   - `run_task()` is now a per‑task loop: `run_attempt()` (one Pi session) → harness checks → retry **the same task** with a `prior_note` in the next session's prompt that quotes what failed. Retries until `max_attempts` (default 3), then `status: failed`. `BLOCKED` is never retried; a rejected attempt goes back to `status: todo` so the next runner invocation picks it up again.
-   - New harness checks after any `STATUS: DONE` claim:
-   - `run_verification()` runs `verify_commands` from the repo root (`./gradlew testDebugUnitTest`), prints the output tail, treats any non‑zero exit / timeout as failure. `verify_env_script` dot‑sources `set-env-variables.ps1`/`.sh` (falls back to the committed `.sample`) in the same shell; `./` is rewritten to `.\` on Windows.
-   - `test_coverage_note()` rejects a DONE attempt that changed `source_globs` (`app/src/main/*`) without touching `test_globs` (`app/src/test/*`, `app/src/androidTest/*`), measured against the working tree at task start via `git diff --name-only HEAD` + untracked files.
-   - Prompt now states the tests requirement and lists the verification commands the runner enforces; `compact_prompt` matches. Logs now include the piped prompt, so a retry is reviewable.
-   - Fixed two pre-existing bugs the loop depends on: `write_task` dropped the closing frontmatter `---` (so `attempts` silently reset and a retried task reused its own attempt number) and duplicated the `## Run log` section; added `repair_frontmatter()` and a per‑attempt summary history. `split_deps()` no longer treats `depends_on: nothing` as a prerequisite (this task file uses exactly that).
-   - New flags: `--max-attempts`, `--verify`, `--no-verify`, `--require-tests`, `--no-require-tests`; new config keys in **`.harness-loop/config.json`**.
-   **`.harness-loop/rules.md`**: tests are part of every behaviour change, iterate until green, never report DONE on red/skipped tests, don't weaken/`@Ignore` existing tests, and flag environmental test failures on the STATUS line.
-   **`.harness-loop/README.md`**: "Tests and the verification loop" + self-test sections, new flags/config keys, log format.
-   **New `.harness-loop/selftest.py`** — tests for the harness itself (throwaway git repo + fake `pi` + fake test command; touches no real task/session/log).
-   Verification run from the repo root:
-   - `python .harness-loop/selftest.py` → `ALL SELF-TESTS PASSED` (A red tests → same‑task retry → archived green; B red forever → failed, not archived; C docs‑only exempt; D source+test accepted at once; E escape hatch; F BLOCKED not retried; G frontmatter repair / `depends_on: nothing`).
-   - `./gradlew testDebugUnitTest` after `.\set-env-variables.ps1` → `BUILD SUCCESSFUL`, both directly and through the runner's `run_verification()` path.
-   - `python .harness-loop/runner.py --list` / `--dry-run` → normal, new prompt text shown, no config warnings. `py_compile` clean.
-   Caveats: `connectedDebugAndroidTest` was **not** run (`adb devices` shows no emulator attached) and is deliberately kept out of `verify_commands` so attempts don't fail for environmental reasons — add it there once an emulator is always up. `require_tests` only inspects `app/src/main/*`; a genuinely test‑free production change needs `--no-require-tests`. The already‑archived `01-HARNESS-...` task file still contains a duplicated frontmatter block written by the old buggy `write_task` — readable as `status: done`, left untouched. I did not modify my own task file (its unterminated frontmatter is repaired automatically on the runner's next write). Assumption: "add tests / loop on failing tests" is enforced at harness level; app unit/integration tests themselves remain the separate backlog task `add-unit-and-integraton-tests.md`.
-   STATUS: DONE
