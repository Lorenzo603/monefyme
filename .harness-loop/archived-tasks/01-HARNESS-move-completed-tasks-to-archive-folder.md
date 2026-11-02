---
status: done
attempts: 1
---

---
status: todo
attempts: 1
title: "move completed tasks to archived-tasks folder"
source: backlog.md
depends_on: nothing

## Task

When a task is completed, move it to the `.harness-loop/archived-tasks` folder. Do it via the main script, do not rely on calling the model

## Run log

- 2026-11-02 19:51:35 attempt 1: status=done exit=0 log=01-HARNESS-move-completed-tasks-to-archive-folder-run1.log runner.py now auto-moves DONE task files to .harness-loop/archived-tasks (plus archived-aware depends_on resolution) and README documents it; verified with an end-to-end fake-pi harness test and ./gradlew testDebugUnitTest
-   Now an end-to-end verification with a throwaway harness copy and a fake `pi` that always reports DONE (proves the move + the archived-dependency handling without touching real tasks):
-   The env vars didn't persist across my separate shell calls — re-running the Gradle test with the env set in the same command:
-   Done. Summary of changes:
-   **`.harness-loop/runner.py`** (the main script — no model involvement):
-   - Added `ARCHIVED_DIR = ROOT / "archived-tasks"`.
-   - Added `archive_task()`: moves a task file from `tasks/` to `archived-tasks/` after the runner has written the final `status`/`attempts`/run log (timestamp suffix added only if a name collision exists).
-   - `run_task()`: after a run resolves to `STATUS: DONE`, the runner now moves the file and prints `"<slug>: archived to .harness-loop/archived-tasks/..."`. Non-done outcomes (blocked/failed/timeout/dry-run) leave the file in `tasks/`.
-   - `select_tasks()`: builds the `depends_on` lookup from `tasks/` **plus** `archived-tasks/`, so a done prerequisite that has been archived no longer falsely blocks dependents ("waiting on …").
-   **`.harness-loop/README.md`**: documented the `archived-tasks/` folder in the tree and the auto-archive behavior in the Selection rules section.
-   **Verification** (no app code changed; harness-only change):
-   - End-to-end test in a throwaway `_runner_test_tmp/` copy with a fake `pi` emitting `STATUS: DONE`: task ran, status written, file moved to `archived-tasks/`, and a task whose dependency lived only in `archived-tasks/` was selected normally; a task with a genuinely missing dependency was still skipped. Scaffolding deleted afterwards.
-   - `python .harness-loop/runner.py --list` / `--dry-run` — unaffected on the real repo.
-   - `./gradlew testDebugUnitTest` (after `set-env-variables.ps1`) — BUILD SUCCESSFUL. `connectedDebugAndroidTest` not run (needs an emulator; change is not app code).
-   Caveat: the existing files already in `archived-tasks/` were used as-is for the dependency lookup; they all carry `status: done`. This very task file will itself be archived by the runner when it records my DONE status — the intended behavior.
-   STATUS: DONE runner.py now auto-moves DONE task files to .harness-loop/archived-tasks (plus archived-aware depends_on resolution) and README documents it; verified with an end-to-end fake-pi harness test and ./gradlew testDebugUnitTest
-   Now an end-to-end verification with a throwaway harness copy and a fake `pi` that always reports DONE (proves the move + the archived-dependency handling without touching real tasks):
-   The env vars didn't persist across my separate shell calls — re-running the Gradle test with the env set in the same command:
-   Done. Summary of changes:
-   **`.harness-loop/runner.py`** (the main script — no model involvement):
-   - Added `ARCHIVED_DIR = ROOT / "archived-tasks"`.
-   - Added `archive_task()`: moves a task file from `tasks/` to `archived-tasks/` after the runner has written the final `status`/`attempts`/run log (timestamp suffix added only if a name collision exists).
-   - `run_task()`: after a run resolves to `STATUS: DONE`, the runner now moves the file and prints `"<slug>: archived to .harness-loop/archived-tasks/..."`. Non-done outcomes (blocked/failed/timeout/dry-run) leave the file in `tasks/`.
-   - `select_tasks()`: builds the `depends_on` lookup from `tasks/` **plus** `archived-tasks/`, so a done prerequisite that has been archived no longer falsely blocks dependents ("waiting on …").
-   **`.harness-loop/README.md`**: documented the `archived-tasks/` folder in the tree and the auto-archive behavior in the Selection rules section.
-   **Verification** (no app code changed; harness-only change):
-   - End-to-end test in a throwaway `_runner_test_tmp/` copy with a fake `pi` emitting `STATUS: DONE`: task ran, status written, file moved to `archived-tasks/`, and a task whose dependency lived only in `archived-tasks/` was selected normally; a task with a genuinely missing dependency was still skipped. Scaffolding deleted afterwards.
-   - `python .harness-loop/runner.py --list` / `--dry-run` — unaffected on the real repo.
-   - `./gradlew testDebugUnitTest` (after `set-env-variables.ps1`) — BUILD SUCCESSFUL. `connectedDebugAndroidTest` not run (needs an emulator; change is not app code).
-   Caveat: the existing files already in `archived-tasks/` were used as-is for the dependency lookup; they all carry `status: done`. This very task file will itself be archived by the runner when it records my DONE status — the intended behavior.
-   STATUS: DONE runner.py now auto-moves DONE task files to .harness-loop/archived-tasks (plus archived-aware depends_on resolution) and README documents it; verified with an end-to-end fake-pi harness test and ./gradlew testDebugUnitTest
