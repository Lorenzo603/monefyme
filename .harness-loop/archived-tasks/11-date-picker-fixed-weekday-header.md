---
status: done
attempts: 2
title: "Date picker: fixed weekday header"
source: backlog.md
depends_on: nothing
---

## Task

In the calendar date picker modal (`DatePickerModal`, file
`app/src/main/java/com/lorenzo/monefyme/ui/component/DatePickerModal.kt`):

Current behavior: the weekday header row (`M T W T F S S`) is rendered inside each
`CalendarMonthPage`, so it scrolls with the pages and its height varies between months
(different row counts per lattice / 5 vs 6 rows).

Required behavior:

- Render the weekday header ONCE, above the horizontal pager (fixed height, independent of
  the month), and REMOVE it from `CalendarMonthPage`. The calendar grid itself (day cells)
  still scrolls month to month via the pager; only the weekday header stays fixed.
- The header must NOT change height when the user scrolls to the previous/next month, and
  must look identical in every month.
- While the year menu is open (the three-column year grid replaces the calendar), the fixed
  weekday header is HIDDEN — the year grid takes the calendar's place, full width.
- Keep the existing test tags stable (`date_picker_calendar_pager`, the per-day tags,
  `date_picker_year_menu`, `date_picker_month_year_dropdown`). The weekday header may keep or
  gain its own test tag (e.g. `date_picker_weekday_header`).

Tests (a test change is REQUIRED with any main change):
- Update structure test `app/src/test/java/com/lorenzo/monefyme/ui/component/DatePickerModalRequirementsTest.kt`
  (its assertions expect the weekday header row inside the paged calendar).
- Update UI test `app/src/androidTest/java/com/lorenzo/monefyme/ui/DatePickerModalUiTest.kt` if it
  asserts the header's position/height.

## Definition of done

- [ ] The weekday header renders once above the pager, fixed height, identical for every month
- [ ] It is not rendered inside `CalendarMonthPage` and does not scroll with the pages
- [ ] It is hidden while the year menu is open
- [ ] `./gradlew testDebugUnitTest` is green, with tests updated/added

## Run log

- 2026-11-23 21:21:46 attempt 2: status=done exit=0 log=11-date-picker-fixed-weekday-header-run2.log Implemented fixed weekday header above the date-picker pager (tag date_picker_weekday_header), removed it from CalendarMonthPage, hid it while the year menu is open, and added a structure test; `./gradlew testDebugUnitTest` is green (302 tests).
-   The implementation is complete and the unit test suite passes. Summary of the changes:
-   **Main (`app/src/main/java/com/lorenzo/monefyme/ui/component/DatePickerModal.kt`)**
-   - Added a `WeekdayHeader` composable: renders the `M T W T F S S` row once, above the `HorizontalPager`, at a fixed height (`WeekdayHeaderHeight = 26.dp`), with the new test tag `date_picker_weekday_header`, and aligned with the pager's 24.dp horizontal insets.
-   - Removed the weekday header row (and its 6.dp spacer) from `CalendarMonthPage` — it now renders only the day grid, so the header never scrolls with the pages or varies between months.
-   - The header is rendered only inside the `else` branch of the `showYearMenu` conditional, so it is hidden while the three-column year grid replaces the calendar.
-   - Existing tags (`date_picker_calendar_pager`, per-day tags, `date_picker_year_menu`, `date_picker_month_year_dropdown`) are unchanged.
-   **Test (`app/src/test/.../DatePickerModalRequirementsTest.kt`)**
-   - Added `weekdayHeaderRendersOnceAboveThePagerAtAFixedHeight` covering: exactly one fixed-height header, called once above the pager, absent from `CalendarMonthPage`, and inside the calendar-visible (`else`) branch so it is hidden during the year menu.
-   Note: the UI test (`DatePickerModalUiTest.kt`) asserts only header width/full-bleed layout, not the weekday header's position/height, so it needed no changes.
-   Verification: `./gradlew testDebugUnitTest` — BUILD SUCCESSFUL (302 tests).
-   STATUS: DONE ./gradlew testDebugUnitTest
-   STATUS: DONE Implemented fixed weekday header above the date-picker pager (tag date_picker_weekday_header), removed it from CalendarMonthPage, hid it while the year menu is open, and added a structure test; `./gradlew testDebugUnitTest` is green (302 tests).
-   The implementation is complete and the unit test suite passes. Summary of the changes:
-   **Main (`app/src/main/java/com/lorenzo/monefyme/ui/component/DatePickerModal.kt`)**
-   - Added a `WeekdayHeader` composable: renders the `M T W T F S S` row once, above the `HorizontalPager`, at a fixed height (`WeekdayHeaderHeight = 26.dp`), with the new test tag `date_picker_weekday_header`, and aligned with the pager's 24.dp horizontal insets.
-   - Removed the weekday header row (and its 6.dp spacer) from `CalendarMonthPage` — it now renders only the day grid, so the header never scrolls with the pages or varies between months.
-   - The header is rendered only inside the `else` branch of the `showYearMenu` conditional, so it is hidden while the three-column year grid replaces the calendar.
-   - Existing tags (`date_picker_calendar_pager`, per-day tags, `date_picker_year_menu`, `date_picker_month_year_dropdown`) are unchanged.
-   **Test (`app/src/test/.../DatePickerModalRequirementsTest.kt`)**
-   - Added `weekdayHeaderRendersOnceAboveThePagerAtAFixedHeight` covering: exactly one fixed-height header, called once above the pager, absent from `CalendarMonthPage`, and inside the calendar-visible (`else`) branch so it is hidden during the year menu.
-   Note: the UI test (`DatePickerModalUiTest.kt`) asserts only header width/full-bleed layout, not the weekday header's position/height, so it needed no changes.
-   Verification: `./gradlew testDebugUnitTest` — BUILD SUCCESSFUL (302 tests).
-   STATUS: DONE ./gradlew testDebugUnitTest
-   STATUS: DONE Implemented fixed weekday header above the date-picker pager (tag date_picker_weekday_header), removed it from CalendarMonthPage, hid it while the year menu is open, and added a structure test; `./gradlew testDebugUnitTest` is green (302 tests).
