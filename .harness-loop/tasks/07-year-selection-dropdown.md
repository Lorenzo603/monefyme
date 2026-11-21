---
status: todo
attempts: 0
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
