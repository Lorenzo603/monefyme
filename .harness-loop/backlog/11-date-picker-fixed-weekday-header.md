---
status: todo
attempts: 1
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

-   STATUS: todo
