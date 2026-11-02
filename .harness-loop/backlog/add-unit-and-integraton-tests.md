---
status: todo
attempts: 1
title: "Add unit and integration tests"
source: backlog.md
depends_on: nothing

## Task

Perform the following:
- Add unit tests so to cover all current functionalities of the app.
- Add integration tests where appropriate.

These tests will run when running the commands:
- `./gradlew testDebugUnitTest`
- `./gradlew connectedDebugAndroidTest` (requires running emulator)
