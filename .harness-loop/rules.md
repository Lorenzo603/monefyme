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
4. Verify with the test commands, run from the repo root:
   - `./gradlew testDebugUnitTest`
   - `./gradlew connectedDebugAndroidTest` (requires running emulator)
5. If a verification command fails for reasons unrelated to your change, report it instead
   of trying to fix unrelated code.
</workflow>

<rules>
- Edit inside this repo only. Do not create files outside it.
- Never run `git commit`, `git push`, `git checkout`, `git reset`, or any other git write
  command. The human reviews and commits.
- Do not edit files under `.harness-loop/` other than the task file's own status fields.
- Ask no questions: you cannot get an answer mid-run. If the task is genuinely ambiguous or
  is missing prerequisites, stop, write a `## Questions for the human` section into the task
  file, and report BLOCKED.
- Do not invent data. Diagram entities, relationships, IDs and attributes must come from
  `supabase/data/diagram.json` or explicit user-supplied values.
- Keep IDs and types consistent with the schemas in `supabase/data/`.
</rules>

<finish>
End your final message with exactly one line, nothing after it:

STATUS: DONE      implementation complete and verified, list the checks you ran
STATUS: BLOCKED   needs a human decision, missing access, or unmet prerequisites
STATUS: FAILED    attempted and could not be completed

Above that line give a short summary: what changed (file paths), commands you ran to verify,
and the caveats or follow-ups a human should know.
</finish>
