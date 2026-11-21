---
status: todo
attempts: 0
title: "Improve transaction history group UI"
source: backlog/next.md
depends_on: nothing
---

## Task

Improve the transaction history group cards in the history section of the Home screen — the
accordion list in `TransactionGroupedList`, file
`app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt` — using the two reference
screenshots @.harness-loop/task-assets/groups-by-category.png (group by category) and
@.harness-loop/task-assets/groups-by-date.png (group by date).

Current behavior: the group header is a light-green card (`0xFFEAF8F2`) with a chevron, a
small gray count and a bold total amount; each expanded entry is a separate white card
showing category name, date, description and account on the left with the amount on the right.
Grouping is toggled between by-category and by-date (`grouping_toggle_button`).

Requirements:

- Visual continuity: the group header and its entry rows must have NO background color
  difference — the header and its entries must share the same container color, so the
  group reads as one seamless block.
- Entry type indicator: add a small circular dot to each entry row, colored red for an
  expense entry and green for an income entry.
- Group total count: the number depicting the total number of entries in a group must be
  MORE PROMINENT and placed just after the group's name — e.g. the category name (or the
  date in a date group) — and circled in green (a green-outlined circle / badge around
  the number). The old small gray count in the right-hand column should be removed.
- Group by category: the total amount shown in the group header must be RED for an
  expense group and GREEN for an income group. Each entry row must show ONLY the amount
  for that entry on the LEFT and the date on the RIGHT (no category name, description or
  account on the entry row).
- Group by date: each entry row must show ONLY the category (on the left) and the amount
  (on the right) — the date is already in the group header, so it must not repeat on the
  entry.
- Preserve: the accordion expand/collapse interaction, the `group_header_<groupName>` and
  `transaction_row_<id>` test tags, the category icon in the header for category groups, and
  the count/total display style of the header (the expand chevron).
- When a group mixes expense and income entries, color the header total by the majority
  type of its transactions (treat a tie as expense).

Tests (a test change is REQUIRED with any main change):
- Add/extend structure tests in `app/src/test/java/com/lorenzo/monefyme/ui/screen/`
  covering: shared header/entry background, the green-circled count after the group name,
  red/green total by group type, entry row content per grouping mode, and the per-entry dot.
- Keep the existing tests green (e.g. HomeScreenGroupingToggleTest,
  TransactionHistoryAccordionTest and the other UI tests that click group headers).

## Definition of done

- [ ] Header and entries share the same background color
- [ ] Each entry shows a red dot (expense) or green dot (income)
- [ ] Group total count is prominent, circled in green, just after the group name
- [ ] Group by category: total amount red for expense / green for income; entry shows amount left + date right only
- [ ] Group by date: entry shows category left + amount right only
- [ ] Existing accordion behavior and test tags preserved
- [ ] `./gradlew testDebugUnitTest` is green, with tests updated/added to cover the new layout
