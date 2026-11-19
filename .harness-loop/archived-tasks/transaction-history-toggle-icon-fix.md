---
status: done
attempts: 2
title: "transaction history toggle button icon fix"
source: backlog/next.md
depends_on: nothing
---

## Task

Transaction history -> toggle button icon fix.

The grouping toggle button shown when the transaction history is open (in
`app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt`, the
`IconButton` with testTag `grouping_toggle_button` that calls
`viewModel.toggleGroupByCategory()`) currently uses the hamburger `Icons.Outlined.Menu`
icon, which is the same icon as the open-history button and does not convey
what the toggle does.

Requirements:
- Give the toggle button an icon that clearly communicates the grouping mode:
  show an appropriate icon for the current grouping state (e.g. one for
  "grouped by category" and a different one for "grouped by date") so the user
  can tell which grouping is active and that tapping switches it.
- Do not change the toggle behaviour itself, only the icon (and its
  `contentDescription` if needed so it matches the state).
- The other icons in the app bar (hamburger for open/close history) must stay
  as they are.

## Run log

- 2026-11-19 20:53:38 attempt 2: status=done exit=0 log=transaction-history-toggle-icon-fix-run2.log Replaced the grouping toggle's hamburger icon with state-driven Category/CalendarMonth icons in HomeScreen.kt; added 2 tests in HomeScreenGroupingToggleTest.kt; ./gradlew testDebugUnitTest passes (all 7 grouping-toggle tests green).
-   Now I'll make the edit: state-driven icons for the grouping toggle.
-   Now add a test covering the new icon behavior:
-   The 1500-char window would spill into the else branch containing `Icons.Outlined.Menu`. Let me bound the block at `balance_history_right`:
-   Targeted test passed. Now run the full verification suite:
-   All tests green, including the two new ones plus all 5 pre-existing tests in the class.
-   **Changes:**
-   - `app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt`: The grouping toggle (`testTag("grouping_toggle_button")`) no longer reuses the hamburger `Icons.Outlined.Menu`. It now branches on `groupByCategory`: `Icons.Outlined.Category` with contentDescription "Grouped by category. Tap to group by date" when grouping by category, and `Icons.Outlined.CalendarMonth` with "Grouped by date. Tap to group by category" when grouping by date. Added imports for `CalendarMonth` and `Category` (project already depends on `material-icons-extended`). Toggle behavior (`onClick = { viewModel.toggleGroupByCategory() }`) and both app-bar hamburger buttons are unchanged.
-   - `app/src/test/java/com/lorenzo/monefyme/ui/screen/HomeScreenGroupingToggleTest.kt`: Added two tests following the repo's existing source-assertion style — `groupingToggleButtonShowsStateSpecificIconsInsteadOfHamburger` (toggle block uses Category/CalendarMonth, branches on `groupByCategory`, and no longer uses `Menu`) and `groupingToggleContentDescribesActiveGroupingMode` (state-specific contentDescriptions present). The toggle block is bounded at the `balance_history_right` anchor so the hamburger assertions stay scoped.
-   **Verification:** `./gradlew testDebugUnitTest` — BUILD SUCCESSFUL; full suite ran, and the test-results XML confirms all 7 `HomeScreenGroupingToggleTest` cases executed and passed.
-   STATUS: DONE Replaced the grouping toggle's hamburger icon with state-driven Category/CalendarMonth icons in HomeScreen.kt; added 2 tests in HomeScreenGroupingToggleTest.kt; ./gradlew testDebugUnitTest passes (all 7 grouping-toggle tests green).
-   Now I'll make the edit: state-driven icons for the grouping toggle.
-   Now add a test covering the new icon behavior:
-   The 1500-char window would spill into the else branch containing `Icons.Outlined.Menu`. Let me bound the block at `balance_history_right`:
-   Targeted test passed. Now run the full verification suite:
-   All tests green, including the two new ones plus all 5 pre-existing tests in the class.
-   **Changes:**
-   - `app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt`: The grouping toggle (`testTag("grouping_toggle_button")`) no longer reuses the hamburger `Icons.Outlined.Menu`. It now branches on `groupByCategory`: `Icons.Outlined.Category` with contentDescription "Grouped by category. Tap to group by date" when grouping by category, and `Icons.Outlined.CalendarMonth` with "Grouped by date. Tap to group by category" when grouping by date. Added imports for `CalendarMonth` and `Category` (project already depends on `material-icons-extended`). Toggle behavior (`onClick = { viewModel.toggleGroupByCategory() }`) and both app-bar hamburger buttons are unchanged.
-   - `app/src/test/java/com/lorenzo/monefyme/ui/screen/HomeScreenGroupingToggleTest.kt`: Added two tests following the repo's existing source-assertion style — `groupingToggleButtonShowsStateSpecificIconsInsteadOfHamburger` (toggle block uses Category/CalendarMonth, branches on `groupByCategory`, and no longer uses `Menu`) and `groupingToggleContentDescribesActiveGroupingMode` (state-specific contentDescriptions present). The toggle block is bounded at the `balance_history_right` anchor so the hamburger assertions stay scoped.
-   **Verification:** `./gradlew testDebugUnitTest` — BUILD SUCCESSFUL; full suite ran, and the test-results XML confirms all 7 `HomeScreenGroupingToggleTest` cases executed and passed.
-   STATUS: DONE Replaced the grouping toggle's hamburger icon with state-driven Category/CalendarMonth icons in HomeScreen.kt; added 2 tests in HomeScreenGroupingToggleTest.kt; ./gradlew testDebugUnitTest passes (all 7 grouping-toggle tests green).
