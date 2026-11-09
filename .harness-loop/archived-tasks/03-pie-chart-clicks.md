---
status: done
attempts: 3
title: "Handle Pie Chart clicks"
source: backlog.md
depends_on: nothing
---

## Task

Clicking on a specific section of the pie chart should open the transaction history list with that specific category already expanded.

## Run log

-   - `ui/screen/TransactionHistoryScreen.kt` — consumes the request so the category's accordion opens on entry.
-   ## Tests
-   - **Unit** (`testDebugUnitTest`): new `ExpensePieChartClickTest` (18 tests: geometry, band boundaries, drag suppression, icon wiring), plus updates to `MainViewModelTest` (+3), `TransactionHistoryScreenTest` (+4), `HomeRestoreWiringTest` (+1) → **150 tests, 0 failures, BUILD SUCCESSFUL**.
-   - **Instrumentation** (run on the live emulator): new `PieChartSectionClickUiTest` (8 e2e tests incl. swipe-cancels-click and icon click) and updated `TransactionHistoryAccordionTest`; also fixed the broken `PieChartHighlightUiTest` (`assertAny` misuse → 1.7.0 `SemanticsMatcher`), which had broken androidTest compilation → **19 tests, 0 failed** via `connectedDebugAndroidTest`.
-   Caveat: `.harness-loop/tasks/03-pie-chart-clicks.md` shows as modified in git status, but only the harness/runner updates frontmatter — I did not edit it.
-   STATUS: DONE Pie chart section clicks implemented (ExpensePieChart onCategoryClick + MainViewModel expansion request + Home/TransactionHistory wiring) with ExpensePieChartClickTest (18), updated viewmodel/screen/wiring unit tests, new PieChartSectionClickUiTest and fixed PieChartHighlightUiTest; verified `./gradlew cleanTestDebugUnitTest testDebugUnitTest` (150 tests, 0 failures) and `connectedDebugAndroidTest` on the emulator (19 tests, 0 failed).
-   The previous session's work is in place (partially committed by a human since). Let me verify the current state compiles and all verification tests pass.
-   Work is complete in the tree. Running the harness verification (plus androidTest compile as extra assurance):
-   All 18 suites green. Let me confirm the tree state of the key files (guards + the fixed UI test) and that androidTest still compiles:
-   AndroidTest compile fails now. Let me see the errors and the current implementation state:
-   The tree was rewritten with new geometry (220dp chart, 35dp stroke — icon slots now sit outside the ring band, so no double-reporting). Only `PieChartHighlightUiTest.kt` is broken. Let me check the exact `assertAny`/`isDisplayed` API:
-   Let me check the current chart constants (the rewritten UI test claims 220dp chart / 35dp stroke → band 75–110dp):
-   Component constants match the new UI test (220/35). Only `PieChartHighlightUiTest.kt` is broken. Let me view it fully:
-   Now verify `viewportSize` units and `boundsInRoot` availability in 1.7:
-   `viewportSize` isn't in this jar's sources; I'll use `activity.resources.displayMetrics` instead — plain and reliable. Fixing the file:
-   androidTest compiles now. Let me try to run the end-to-end UI tests on an emulator (per AGENTS.md):
-   Emulator is running! Running the end-to-end UI tests for this feature:
-   All 19 end-to-end UI tests pass on the emulator, and the harness verification (`./gradlew testDebugUnitTest`) is green. Summary of the completed work:
-   ## What the implementation does
-   **Clicking a pie chart section opens transaction history with that category expanded:**
-   - `app/src/main/java/com/lorenzo/monefyme/ui/component/ExpensePieChart.kt`
-   - `onCategoryClick` callback; ring `Canvas` (tag `pie_chart_arc_area`) hit-tests taps against the drawn arcs via pure helpers (`PieArc`, `angleDegreesAt`, `isAngleWithinArc`, `categoryKeyAtPoint`), with `isPressWithoutDrag` (ViewConfiguration touch-slop) so the month-pager swipe never counts as a click; icon-slot presses report their category too.
-   - `ui/viewmodel/MainViewModel.kt` — `requestCategoryExpansion()` / `consumeCategoryExpansionRequest()` one-shot request.
-   - `ui/screen/HomeScreen.kt` — wires `onCategoryClick` → expansion request + navigation.
-   - `ui/screen/TransactionHistoryScreen.kt` — consumes the request so the category's accordion opens on entry.
-   ## Tests
-   - **Unit** (`testDebugUnitTest`): new `ExpensePieChartClickTest` (18 tests: geometry, band boundaries, drag suppression, icon wiring), plus updates to `MainViewModelTest` (+3), `TransactionHistoryScreenTest` (+4), `HomeRestoreWiringTest` (+1) → **150 tests, 0 failures, BUILD SUCCESSFUL**.
-   - **Instrumentation** (run on the live emulator): new `PieChartSectionClickUiTest` (8 e2e tests incl. swipe-cancels-click and icon click) and updated `TransactionHistoryAccordionTest`; also fixed the broken `PieChartHighlightUiTest` (`assertAny` misuse → 1.7.0 `SemanticsMatcher`), which had broken androidTest compilation → **19 tests, 0 failed** via `connectedDebugAndroidTest`.
-   Caveat: `.harness-loop/tasks/03-pie-chart-clicks.md` shows as modified in git status, but only the harness/runner updates frontmatter — I did not edit it.
-   STATUS: DONE Pie chart section clicks implemented (ExpensePieChart onCategoryClick + MainViewModel expansion request + Home/TransactionHistory wiring) with ExpensePieChartClickTest (18), updated viewmodel/screen/wiring unit tests, new PieChartSectionClickUiTest and fixed PieChartHighlightUiTest; verified `./gradlew cleanTestDebugUnitTest testDebugUnitTest` (150 tests, 0 failures) and `connectedDebugAndroidTest` on the emulator (19 tests, 0 failed).
