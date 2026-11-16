---
status: done
attempts: 2
title: "remove transactions title"
source: backlog.md
depends_on: nothing
---

## Task

Remove the "Transactions" title from the Transaction History screen

## Run log

-   - I read "the Transaction History screen" as `TransactionHistoryScreen` (`Routes.TransactionHistory`). I deliberately left the Home screen's drag-reveal history sheet header (`HomeScreen.kt:494`, "Transactions") alone: it belongs to the Home screen and existing UI tests (`HomeRestoreNavigationTest`) assert it as the proof that the sheet opened. If the intent was that sheet's header, that's a separate change and those tests must be re-pointed at `history_sheet`.
-   - Follow-up worth noting: nothing navigates to `Routes.TransactionHistory` today (only `HomeScreen` → `Restore` exists), so the full-screen history is currently unreachable from the UI — the visible history is the Home drag sheet.
-   STATUS: DONE production title removal already in HEAD (TransactionHistoryScreen.kt:100, unchanged); added e2e UI test app/src/androidTest/.../TransactionHistoryTitleTest.kt (3 tests, pass on emulator, fail if title returns) and extended app/src/test/.../TransactionHistoryScreenTest.kt; ./gradlew testDebugUnitTest green (202 tests, 0 failures)
-   STATUS: DONE added e2e UI test app/src/androidTest/java/com/lorenzo/monefyme/ui/TransactionHistoryTitleTest.kt (3 tests, verified passing on emulator-5554 and failing when the title is re-added) + extended app/src/test/java/com/lorenzo/monefyme/ui/screen/TransactionHistoryScreenTest.kt; TransactionHistoryScreen.kt:100 already has title = { } (no production diff needed), ./gradlew testDebugUnitTest green (202 tests, 0 failures, 0 errors, 0 skipped)
-   Let me check the current state of the repository to see what's already been done.
-   The working tree is clean — let me check whether the change is actually present in the source:
-   The change is already in HEAD. Let me check whether any tests still reference the removed title (which would be stale assertions):
-   Let me look at the harness configuration and any prior-run notes to understand why this task keeps coming back.
-   I have the context. Let me verify the current implementation state and check whether an emulator is available for e2e verification.
-   An emulator is running, so I can verify the UI end-to-end. Let me inspect the ViewModel API needed for a full-screen UI test.
-   An emulator is available. The production change is already in HEAD; what's missing is real e2e proof that the running screen shows no title. Let me add that UI test.
-   Let me prove the new e2e test actually has teeth by temporarily restoring the title and confirming it fails:
-   Verified everything. Final state of the work:
-   **Production (already in HEAD, re-verified unchanged):**
-   - `app/src/main/java/com/lorenzo/monefyme/ui/screen/TransactionHistoryScreen.kt:100` — `TopAppBar(title = { }, …)`: the "Transactions" title is gone from the Transaction History screen. `git diff` shows no production change was needed this attempt.
-   **Tests added this attempt:**
-   - `app/src/androidTest/java/com/lorenzo/monefyme/ui/TransactionHistoryTitleTest.kt` (new, 3 tests) — renders the whole `TransactionHistoryScreen` with a stubbed `BudgetRepository` and asserts:
-   - no "Transactions" node exists on the screen,
-   - the top bar still exposes `Back` and `Open restore` (the bar wasn't removed with the title),
-   - the month selector and the category group headers still render.
-   - `app/src/test/java/com/lorenzo/monefyme/ui/screen/TransactionHistoryScreenTest.kt` — added an assertion that no leftover `Transactions"` string remains in the screen body, plus a new test `removingTheTitleKeepsTheTopBarAndItsButtons`.
-   **Commands I ran (all green):**
-   - `./gradlew cleanTestDebugUnitTest testDebugUnitTest` → 202 tests, 0 failures / 0 errors / 0 skipped (harness verify command `./gradlew testDebugUnitTest` green).
-   - `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.lorenzo.monefyme.ui.TransactionHistoryTitleTest` → 3/3 pass on `emulator-5554`.
-   - Teeth check: temporarily re-adding `Text("Transactions")` made the new e2e test FAIL, then reverted (source confirmed byte-identical to HEAD).
-   **Assumptions / caveats:**
-   - I read "the Transaction History screen" as `TransactionHistoryScreen` (`Routes.TransactionHistory`). I deliberately left the Home screen's drag-reveal history sheet header (`HomeScreen.kt:494`, "Transactions") alone: it belongs to the Home screen and existing UI tests (`HomeRestoreNavigationTest`) assert it as the proof that the sheet opened. If the intent was that sheet's header, that's a separate change and those tests must be re-pointed at `history_sheet`.
-   - Follow-up worth noting: nothing navigates to `Routes.TransactionHistory` today (only `HomeScreen` → `Restore` exists), so the full-screen history is currently unreachable from the UI — the visible history is the Home drag sheet.
-   STATUS: DONE production title removal already in HEAD (TransactionHistoryScreen.kt:100, unchanged); added e2e UI test app/src/androidTest/.../TransactionHistoryTitleTest.kt (3 tests, pass on emulator, fail if title returns) and extended app/src/test/.../TransactionHistoryScreenTest.kt; ./gradlew testDebugUnitTest green (202 tests, 0 failures)
-   STATUS: DONE added e2e UI test app/src/androidTest/java/com/lorenzo/monefyme/ui/TransactionHistoryTitleTest.kt (3 tests, verified passing on emulator-5554 and failing when the title is re-added) + extended app/src/test/java/com/lorenzo/monefyme/ui/screen/TransactionHistoryScreenTest.kt; TransactionHistoryScreen.kt:100 already has title = { } (no production diff needed), ./gradlew testDebugUnitTest green (202 tests, 0 failures, 0 errors, 0 skipped)
