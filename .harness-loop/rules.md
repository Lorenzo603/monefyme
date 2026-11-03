# Harness-wide rules appended to every Pi session (--append-system-prompt).
# Keep these generic; task-specific detail belongs in the task file.

<project>
Repo: Android mobile budgeting app.
</project>

<workflow>
1. Read the task file you were given before touching any code.
2. Explore the repo first — read the files you intend to change, and check existing
   patterns/conventions instead of inventing new ones.
3. Make the smallest change that fully satisfies the task. Prefer editing existing files
   over adding new abstractions; no speculative refactors of untouched code.
4. Every behaviour change must come with tests: add or update an automated test that covers
   what you changed (unit test for logic, instrumentation/UI test where only the running app
   can prove it) before you report DONE.
5. Run the tests yourself and iterate until they are green. Never report DONE on a failing
   or skipped test run.
6. The harness verifies independently: after your session the runner re-runs the project test
   commands from the repo root and checks that production changes came with test changes. If a
   check fails the task is handed back to a fresh session that gets the failing output, so the
   same task loops until the tests pass or the attempt limit is reached.
7. Verify with the test commands, run from the repo root:
   - `./gradlew testDebugUnitTest`
   - `./gradlew connectedDebugAndroidTest` (requires running emulator)
8. If a verification command fails for reasons unrelated to your change, report it instead
   of trying to fix unrelated code, and say so on your STATUS line — the harness will otherwise
   keep re-running the same task on an environmental failure.
</workflow>

<rules>
- Edit inside this repo only. Do not create files outside it.
- Never run `git commit`, `git push`, `git checkout`, `git reset`, or any other git write
  command. The human reviews and commits.
- Do not edit files under `.harness-loop/` other than the task file's own status fields.
- Ask no questions: you cannot get an answer mid-run. If the task is genuinely ambiguous or
  is missing prerequisites, stop, write a `## Questions for the human` section into the task
  file, and report BLOCKED.
- Do not delete, weaken or `@Ignore` an existing test just to make a verification pass; fix
  the code or the test's actual defect.
</rules>

<finish>
End your final message with exactly one line, nothing after it:

STATUS: DONE      implementation complete and verified, list the checks you ran
STATUS: BLOCKED   needs a human decision, missing access, or unmet prerequisites
STATUS: FAILED    attempted and could not be completed

Above that line give a short summary: what changed (file paths), commands you ran to verify,
and the caveats or follow-ups a human should know.
</finish>
