---
status: done
attempts: 2
title: "Transaction history - collapse accordions"
source: backlog.md
depends_on: nothing
---

## Task

In the transaction history the accordions of the various groups should start collapsed, and then when clicking on a group, the accordion of that group should expand.

## Run log

-   ## Notes
-   - Assumption: a group stays open while the user stays on the same month/grouping (state keyed by group name); any group name never clicked is collapsed. Expand/collapse is independent per group.
-   - The instrumentation test needs an emulator, so per AGENTS.md it stays out of `verify_commands`; the JVM wiring guards keep the behaviour pinned in the harness re-run.
-   - Pre-existing, unrelated quirk left untouched: `TransactionHistoryScreen.kt` imports/`AddTransactionScreen.kt` has a duplicate `testTag` import warning and the git-ignored `local.properties` has a malformed `sdk.dir` line; builds are fine once `set-env-variables.ps1` supplies `ANDROID_HOME`/`ANDROID_SDK_ROOT`.
-   STATUS: DONE TransactionHistoryScreen.kt group accordions now start collapsed and expand/collapse per group on header click (guarded items + isGroupExpanded/expandedGroupsAfterToggle helpers + test tags); tests added in app/src/test/.../TransactionHistoryScreenTest.kt (6) and app/src/androidTest/.../TransactionHistoryAccordionTest.kt (4); verified green with ./gradlew testDebugUnitTest (124 tests, 0 failures/skips) and ./gradlew connectedDebugAndroidTest (17/17 on emulator).
-   Let me check the current state of the repository first.
-   The repo has advanced and the accordion code is back to its original (buggy) state. Let me re-verify the current file and tests.
-   Now let me re-apply the fix to the screen:
-   Now the unit tests (following the repo's `HomeRestoreWiringTest` convention for Compose screens):
-   Now the end-to-end UI test:
-   Now let's run the harness verification command:
-   All 13 unit tests green. An emulator is attached — let me prove the actual UI behaviour end-to-end:
-   All 17 instrumentation tests pass (4 new accordion tests + all pre-existing). Final check — a clean unit-test re-run and the diff scope:
-   Implementation complete and verified.
-   ## What changed
-   **`app/src/main/java/com/lorenzo/monefyme/ui/screen/TransactionHistoryScreen.kt`** (the fix)
-   - The bug: `TransactionGroupedList` rendered *every* group's transaction rows unconditionally — `isExpanded` only drove the arrow icon, so the accordions were always open. The rows are now guarded by `if (isExpanded) { items(groupTransactions) … }`, so a collapsed group shows only its header (icon, name, count, total) and clicking the header expands just that group.
-   - Groups default to collapsed through the new `isGroupExpanded(expandedGroups, groupName)` (`== true`), replacing `groupedTransactions.keys.forEach { expandedGroups[it] = false }`, which wrote snapshot state on every recomposition. Groups that appear later (new month, new grouping) are collapsed too.
-   - `expandedGroupsAfterToggle(...)` makes a click flip only the clicked group; both rules are small `internal` functions in the same file so they are unit-testable without an emulator.
-   - `TransactionGroupedList` is now `internal` (was `private`) so the UI test can render it; added `testTag("group_header_<name>")` / `testTag("transaction_row_<id>")` following the repo's existing tag convention, plus an explicit LazyColumn key for header items.
-   **`app/src/test/java/com/lorenzo/monefyme/ui/screen/TransactionHistoryScreenTest.kt`** — 6 new JVM tests: `groupAccordionsStartCollapsed`, `clickingGroupHeaderExpandsThatGroup`, `clickingExpandedGroupHeaderCollapsesItAgain`, `clickingOneGroupKeepsTheOtherGroupsCollapsed`, `groupsThatAppearLaterStartCollapsed`, plus two wiring guards (`collapsedGroupsRenderTheirHeaderOnly`, `clickingGroupHeaderTogglesOnlyThatGroup`) modelled on the repo's existing `HomeRestoreWiringTest` — they fail if the expanded-guard or the per-group toggle is reverted.
-   **`app/src/androidTest/java/com/lorenzo/monefyme/ui/TransactionHistoryAccordionTest.kt`** (new, 4 tests) — real Compose UI coverage: groups start collapsed (rows `assertDoesNotExist`), a header click expands that group only, a second click collapses it again, and toggling another group leaves an open one open.
-   ## Checks I ran
-   - `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL, exit 0; `./gradlew cleanTestDebugUnitTest testDebugUnitTest` → 124 unit tests, 0 failures, 0 skipped (`TransactionHistoryScreenTest`: 13 green, 7 pre-existing + 6 new).
-   - `./gradlew connectedDebugAndroidTest` on the attached `emulator-5554` → 17 tests, 0 failed, 0 skipped (4 new accordion tests + all pre-existing UI/integration tests), proving the on-screen behaviour.
-   ## Notes
-   - Assumption: a group stays open while the user stays on the same month/grouping (state keyed by group name); any group name never clicked is collapsed. Expand/collapse is independent per group.
-   - The instrumentation test needs an emulator, so per AGENTS.md it stays out of `verify_commands`; the JVM wiring guards keep the behaviour pinned in the harness re-run.
-   - Pre-existing, unrelated quirk left untouched: `TransactionHistoryScreen.kt` imports/`AddTransactionScreen.kt` has a duplicate `testTag` import warning and the git-ignored `local.properties` has a malformed `sdk.dir` line; builds are fine once `set-env-variables.ps1` supplies `ANDROID_HOME`/`ANDROID_SDK_ROOT`.
-   STATUS: DONE TransactionHistoryScreen.kt group accordions now start collapsed and expand/collapse per group on header click (guarded items + isGroupExpanded/expandedGroupsAfterToggle helpers + test tags); tests added in app/src/test/.../TransactionHistoryScreenTest.kt (6) and app/src/androidTest/.../TransactionHistoryAccordionTest.kt (4); verified green with ./gradlew testDebugUnitTest (124 tests, 0 failures/skips) and ./gradlew connectedDebugAndroidTest (17/17 on emulator).
