---
status: done
attempts: 1
title: "Improve year selection dropdown box UI"
source: backlog/next.md
depends_on: nothing
---

## Task

Improve the year selection UI inside the calendar date picker modal — the dropdown that lists
years when the user taps the year picker button (component `YearPickerMenu`, file
`app/src/main/java/com/lorenzo/monefyme/ui/component/DatePickerModal.kt`) — using the reference
screenshot @.harness-loop/task-assets/year-selection.png.

Current behavior: the dropdown shows a single horizontal, scrolling row of year buttons
(`date_picker_year_<year>` for years centerYear ± 5) with `date_picker_year_cycle_previous` /
`date_picker_year_cycle_next` cycle buttons on its sides. The dropdown stays anchored under the
small picker button.

Requirements:
- The years must span the WHOLE modal: the dropdown/panel must take the full width of the
  modal, not just the width of the picker button.
- Years must be laid out in a 3-column grid (three year cells per row), not a single
  horizontal row.
- The grid must show MANY rows of years (more than the current ±5 window), scrollable
  vertically, so the user can scroll through a broad range of years. Keep the currently
  applied year highlighted in the app green, keep the rest legible, and keep the selected-year
  jump behavior: picking a year jumps the calendar to that year (`jumpToMonth(YearMonth.of(year, visibleMonth.monthValue))`).
- The dropdown container should use the modal's background color (`dialogBackground`, `0xFFEAF8F2`)
  and keep the `date_picker_year_menu` test tag.
- Preserve the `date_picker_year_<year>` test tags (e.g. `date_picker_year_2026`) so existing
  UI tests can still find individual years.

Tests (a test change is REQUIRED with any main change):
- Update the structure test `app/src/test/java/com/lorenzo/monefyme/ui/component/DatePickerModalRequirementsTest.kt`
  (it currently asserts the horizontal-scroll row + cycle buttons).
- Update the UI test `app/src/androidTest/java/com/lorenzo/monefyme/ui/DatePickerModalUiTest.kt`
  (currently clicks `date_picker_year_2022` / `date_picker_year_2019` and the cycle buttons).

## Definition of done

- [ ] Year menu spans the full modal width (not just the picker button's width)
- [ ] Years are laid out in a 3-column grid with many rows, scrollable
- [ ] The currently applied year is highlighted and picking a year jumps the calendar to it
- [ ] `date_picker_year_menu` and `date_picker_year_<year>` test tags preserved
- [ ] `./gradlew testDebugUnitTest` is green, with tests updated/added to cover the new layout

## Run log

-   Now I have full context. Let me implement the changes, starting with the pure logic helper.
-   Now the main component changes:
-   Now the dropdown → panel changes in the composable body:
-   Now update the structure test:
-   Now update the pure logic test:
-   Now the androidTest UI test:
-   Now set up the environment and run the unit tests:
-   The assertion string `Modifier.fillMaxWidth()` spans a newline in the source. Fixing the test to check the grid's modifier chain properly:
-   Unit tests pass. Now verify the androidTest source compiles (the UI test can't run here without the harness running it, but it must compile):
-   The instrumentation tests compile. Since this is a UI change, let me try running the updated UI test on the emulator to validate the new interaction flow:
-   An emulator is running. Running just the DatePickerModal UI test:
-   Let me simplify that block cleanly:
-   All 13 UI tests pass. Final verification — check nothing else references the removed tags, then re-run the harness verify command:
-   All checks are green. Summary of the work:
-   **Changes**
-   - `app/src/main/java/com/lorenzo/monefyme/ui/component/DatePickerModal.kt`
-   - `YearPickerMenu` rewritten: the old anchored `DropdownMenu` with a horizontal scrolling year row + cycle buttons is replaced by a full-modal-width panel (matches the reference screenshot) that takes the calendar's place while open — a vertically scrollable `LazyVerticalGrid` with `GridCells.Fixed(3)`, 81 years per open, `date_picker_year_menu` tag on the panel, `dialogBackground` container, applied year highlighted as a green pill and scrolled to the panel's top row on open, cells keep `date_picker_year_<year>` tags, and picking a year still calls `jumpToMonth(YearMonth.of(year, visibleMonth.monthValue))`.
-   - The month/year button now toggles the panel (`showYearMenu = !showYearMenu`); the calendar pager shows when it's closed.
-   - `app/src/main/java/com/lorenzo/monefyme/ui/util/DatePickerLogic.kt`
-   - `yearOptions` now returns a broad ascending window (`YEAR_MENU_PAST_YEARS = 30`, `YEAR_MENU_FUTURE_YEARS = 50`) instead of the old ±5 `YEAR_MENU_SPAN` window (past-span is a multiple of 3 so the applied year starts a grid row; asserted in tests).
-   **Tests (updated + added)**
-   - `app/src/test/.../DatePickerModalRequirementsTest.kt`: `monthYearDropdownLetsYouPickOrCycleYears` replaced by `monthYearDropdownTogglesAFullWidthYearGrid`, `yearGridShowsManyScrollableYearsAroundTheAppliedOne`, and `oldHorizontalYearRowAndCycleButtonsAreGone` (3-col grid, full-width, tags, background, highlight, jump, removal of row/cycle buttons).
-   - `app/src/test/.../DatePickerLogicTest.kt`: year-options tests rewritten for the broad scrollable range.
-   - `app/src/androidTest/.../DatePickerModalUiTest.kt`: year-jump test now scrolls the grid; cycle-button test replaced by `yearGridScrollsToYearsFarOutsideTheFirstScreen`; added `appliedYearIsVisibleAtTheTopOfTheGridWhenItOpens` and `yearGridSpansTheWholeModalInThreeColumns` (edge-to-edge width vs the header, 3 side-by-side cells per row).
-   **Verification**
-   - `./gradlew testDebugUnitTest` — BUILD SUCCESSFUL (231 tests, 0 failures).
-   - `./gradlew compileDebugAndroidTestKotlin` — SUCCESS.
-   - `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.lorenzo.monefyme.ui.DatePickerModalUiTest` on the running `medium_phone` emulator — 13/13 passed.
-   Caveat: the emulator run was scoped to `DatePickerModalUiTest` only (other connected suites not re-run; they don't touch the date picker).
-   STATUS: DONE Year selection is now a full-modal-width, scrollable 3-column year grid (menu + per-year test tags preserved, applied year highlighted and jump-on-pick kept); verified with `./gradlew testDebugUnitTest` (green) and the 13-test `DatePickerModalUiTest` on the emulator (all pass).
