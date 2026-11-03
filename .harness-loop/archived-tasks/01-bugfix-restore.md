---
status: done
attempts: 4
title: "Bug: clicking restore in sidebar has no effect"
source: backlog.md
depends_on: nothing
---

## Task

Clicking the `Restore` button in the sidebar has no effect. It should open the Restore screen.
Moreover there are now 2 `Restore` buttons to the left and right of the balance box in the Home screen.
These buttons should not have that functionality, instead when clicked, they should open the transaction history for the month, same behaviour as when clicking the Balance box.
They should also not have the `Restore` icon, but they should have the 3 horizontal lines `Hamburger` menu icon.

## Run log

-   - The two buttons flanking the balance box: `onClick = onOpenTransactionHistory` (same as clicking the balance box), `Icons.Outlined.Restore` → `Icons.Outlined.Menu` (hamburger), contentDescription `"Open transaction history"`, tags `balance_history_left` / `balance_history_right`; the `Restore` icon import is gone.
-   **Tests**
-   - `app/src/test/java/com/lorenzo/monefyme/ui/screen/HomeRestoreWiringTest.kt` (JVM, harness-runnable): guards sidebar wiring, no no-op default, both balance-side buttons wired to transaction history + hamburger icon, balance box still opens history, sidebar keeps its Restore entry.
-   - `app/src/androidTest/java/com/lorenzo/monefyme/ui/HomeRestoreNavigationTest.kt` (Compose instrumentation): sidebar `Restore` → Restore screen ("Manual CSV restore"), left/right side buttons → Transaction history ("Transactions"), exactly 2 "Open transaction history" nodes and no "Open restore" node on Home.
-   **Verification (ran myself)**
-   - `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL, 110 tests, 0 failures (new test class: 5 tests, 0 failures).
-   - `./gradlew assembleDebugAndroidTest` → SUCCESSFUL.
-   - `./gradlew connectedDebugAndroidTest` on the running `medium_phone` AVD → 13 tests, 0 failed / 0 skipped.
-   - Negative check: temporarily reverted the fix — `HomeRestoreWiringTest` failed exactly the 2 wiring assertions; then restored the files (original CRLF line endings preserved, no stray `.bak` left).
-   Caveats: `HomeScreen`'s `onOpenTransactionHistorySwipe` parameter was already unused before this change (untouched, out of scope); `TransactionHistoryScreen`'s top-bar Restore action is unchanged, since the task only concerned Home/sidebar.
-   STATUS: DONE Fix sidebar Restore wiring (CategoriesSidebar requires onOpenRestore, HomeScreen passes it), rewire both Home balance-side buttons to transaction history with hamburger icons; added HomeRestoreWiringTest (unit) + HomeRestoreNavigationTest (instrumentation); verified with ./gradlew testDebugUnitTest (green) and ./gradlew connectedDebugAndroidTest (13/13 green)
-   Now let me make the changes. First, `HomeScreen.kt`:
-   Now the tests — a JVM guard test (runnable by the harness) plus an instrumentation UI test:
-   **Changes**
-   `app/src/main/java/com/lorenzo/monefyme/ui/component/CategoriesSidebar.kt`
-   - `onOpenRestore: () -> Unit = {}` → required parameter `onOpenRestore: () -> Unit` (root cause: the default no-op lambda let `HomeScreen` forget the wiring silently).
-   - Added `testTag("sidebar_restore")` on the sidebar Restore row (+ `testTag` import).
-   `app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt`
-   - Sidebar call now wires `onOpenRestore = { sidebarOpen = false; onOpenRestore() }` → the sidebar `Restore` row closes the drawer and navigates to the Restore screen.
-   - The two buttons flanking the balance box: `onClick = onOpenTransactionHistory` (same as clicking the balance box), `Icons.Outlined.Restore` → `Icons.Outlined.Menu` (hamburger), contentDescription `"Open transaction history"`, tags `balance_history_left` / `balance_history_right`; the `Restore` icon import is gone.
-   **Tests**
-   - `app/src/test/java/com/lorenzo/monefyme/ui/screen/HomeRestoreWiringTest.kt` (JVM, harness-runnable): guards sidebar wiring, no no-op default, both balance-side buttons wired to transaction history + hamburger icon, balance box still opens history, sidebar keeps its Restore entry.
-   - `app/src/androidTest/java/com/lorenzo/monefyme/ui/HomeRestoreNavigationTest.kt` (Compose instrumentation): sidebar `Restore` → Restore screen ("Manual CSV restore"), left/right side buttons → Transaction history ("Transactions"), exactly 2 "Open transaction history" nodes and no "Open restore" node on Home.
-   **Verification (ran myself)**
-   - `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL, 110 tests, 0 failures (new test class: 5 tests, 0 failures).
-   - `./gradlew assembleDebugAndroidTest` → SUCCESSFUL.
-   - `./gradlew connectedDebugAndroidTest` on the running `medium_phone` AVD → 13 tests, 0 failed / 0 skipped.
-   - Negative check: temporarily reverted the fix — `HomeRestoreWiringTest` failed exactly the 2 wiring assertions; then restored the files (original CRLF line endings preserved, no stray `.bak` left).
-   Caveats: `HomeScreen`'s `onOpenTransactionHistorySwipe` parameter was already unused before this change (untouched, out of scope); `TransactionHistoryScreen`'s top-bar Restore action is unchanged, since the task only concerned Home/sidebar.
-   STATUS: DONE Fix sidebar Restore wiring (CategoriesSidebar requires onOpenRestore, HomeScreen passes it), rewire both Home balance-side buttons to transaction history with hamburger icons; added HomeRestoreWiringTest (unit) + HomeRestoreNavigationTest (instrumentation); verified with ./gradlew testDebugUnitTest (green) and ./gradlew connectedDebugAndroidTest (13/13 green)
