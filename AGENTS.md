# Setup and running the application locally on Emulator
Before running tests on this Android application, run `.\set-env-variables.ps1` on Windows or `source ./set-env-variables.sh` on Macos to set the appropriate Android SDK environment variables.

# Git approach
Do not commit changes to the repository automatically.

# Project
Android mobile budgeting app (Kotlin). Application code lives in `app/src/main`, unit tests in
`app/src/test`, instrumentation/UI tests in `app/src/androidTest`. Build and tests run through the
Gradle wrapper (`gradlew` / `gradlew.bat`).

# Harness settings (`.harness-loop/`)
Repository-specific settings for the task runner; `.harness-loop/rules.md` and `README.md` stay
generic and point here for these values. `AGENTS.md` is also given to every Pi session, so follow
it when running inside the loop.

- `verify_env_script`: `set-env-variables` (the runner dot-sources `set-env-variables.ps1` /
  `.sh`, falling back to the committed `.sample`).
- `verify_commands`: `./gradlew testDebugUnitTest` (`config.json`, `verify_timeout_seconds: 900`).
- `source_globs`: `app/src/main/*`; `test_globs`: `app/src/test/*`, `app/src/androidTest/*`
  (with `require_tests: true`, any change under `app/src/main` must come with a change under one
  of the test paths).
- `./gradlew connectedDebugAndroidTest` needs a running emulator, so it is kept out of the
  harness `verify_commands`; run it manually in an unlocked session or when a task explicitly
  asks for it.