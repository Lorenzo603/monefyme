---
status: done
attempts: 1
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

## Run log

-   Now I'll implement the changes in `HomeScreen.kt`:
-   Now update the androidTest that asserted description texts (which no longer render on entry rows):
-   Now the new unit test file:
-   Now set up environment and run the tests:
-   All unit tests pass. Let me verify the new tests actually ran and that the androidTest sources still compile:
-   All checks green: 8 new tests in `TransactionGroupCardTest` pass, existing suites (including `HomeScreenGroupingToggleTest`, `HomeScreenExpansionRequestTest`, etc.) remain green, and the androidTest sources compile.
-   ## Summary of changes
-   **`app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt`**
-   - Added shared colors: `GroupCardContainerColor` (`0xFFEAF8F2`), `GroupEntryExpenseColor` (red), `GroupEntryIncomeColor` (green), and pure helper `groupTotalTransactionType()` (majority type of a group, tie → EXPENSE).
-   - `GroupHeader`: header card now uses the shared container color with zero elevation (seamless with entries); chevron moved to the left of the group's icon/name; the entry count is now a bold number in a green-circled badge (`group_count_<groupName>`) placed right after the group name; the old small gray right-hand count is removed; the total amount is colored red (expense group) or green (income group). The `group_header_<groupName>` tag, expand/collapse chevron, and category icon are preserved.
-   - `TransactionRow`: uses the same shared container color (no more white cards); added a small circular type dot (`transaction_type_dot_<id>`) — red for expense, green for income; grouped-by-category rows show only the amount (left) + date (right); grouped-by-date rows show only the category (left) + amount (right). The `transaction_row_<id>` tag and click behavior are preserved. `TransactionGroupedList` now passes `isGroupedByCategory` down to rows.
-   **`app/src/test/java/com/lorenzo/monefyme/ui/screen/TransactionGroupCardTest.kt`** (new, 8 tests): shared header/entry background, green-circled count after the name (and removal of the gray count), red/green total by group type (structure + direct unit tests of `groupTotalTransactionType` incl. tie→expense), per-mode entry content (amount+date vs category+amount, no description/account/date repetition), per-entry red/green dot ordering, and preserved test tags/accordion/category icon.
-   **`app/src/androidTest/java/com/lorenzo/monefyme/ui/TransactionHistoryAccordionTest.kt`**: updated assertions to use `transaction_row_<id>` tags instead of description texts, which by design no longer render on entry rows (accordion interaction coverage unchanged).
-   Verification run (repo root): `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL, 0 failures (new suite: 8 tests, 0 failures); `./gradlew compileDebugAndroidTestSources` → BUILD SUCCESSFUL. Note: `connectedDebugAndroidTest` was not run (needs emulator and is out of the harness verify commands), but its sources compile.
-   STATUS: DONE Redesigned transaction history group cards in HomeScreen.kt (shared header/entry background, green-circled count badge after group name, red/green group total by majority type, per-entry red/green type dot, amount+date entries when grouped by category, category+amount entries when grouped by date); added TransactionGroupCardTest (8 tests) and updated TransactionHistoryAccordionTest to tag-based assertions; `./gradlew testDebugUnitTest` green.
-   Now I'll implement the changes in `HomeScreen.kt`:
-   Now update the androidTest that asserted description texts (which no longer render on entry rows):
-   Now the new unit test file:
-   Now set up environment and run the tests:
-   All unit tests pass. Let me verify the new tests actually ran and that the androidTest sources still compile:
-   All checks green: 8 new tests in `TransactionGroupCardTest` pass, existing suites (including `HomeScreenGroupingToggleTest`, `HomeScreenExpansionRequestTest`, etc.) remain green, and the androidTest sources compile.
-   ## Summary of changes
-   **`app/src/main/java/com/lorenzo/monefyme/ui/screen/HomeScreen.kt`**
-   - Added shared colors: `GroupCardContainerColor` (`0xFFEAF8F2`), `GroupEntryExpenseColor` (red), `GroupEntryIncomeColor` (green), and pure helper `groupTotalTransactionType()` (majority type of a group, tie → EXPENSE).
-   - `GroupHeader`: header card now uses the shared container color with zero elevation (seamless with entries); chevron moved to the left of the group's icon/name; the entry count is now a bold number in a green-circled badge (`group_count_<groupName>`) placed right after the group name; the old small gray right-hand count is removed; the total amount is colored red (expense group) or green (income group). The `group_header_<groupName>` tag, expand/collapse chevron, and category icon are preserved.
-   - `TransactionRow`: uses the same shared container color (no more white cards); added a small circular type dot (`transaction_type_dot_<id>`) — red for expense, green for income; grouped-by-category rows show only the amount (left) + date (right); grouped-by-date rows show only the category (left) + amount (right). The `transaction_row_<id>` tag and click behavior are preserved. `TransactionGroupedList` now passes `isGroupedByCategory` down to rows.
-   **`app/src/test/java/com/lorenzo/monefyme/ui/screen/TransactionGroupCardTest.kt`** (new, 8 tests): shared header/entry background, green-circled count after the name (and removal of the gray count), red/green total by group type (structure + direct unit tests of `groupTotalTransactionType` incl. tie→expense), per-mode entry content (amount+date vs category+amount, no description/account/date repetition), per-entry red/green dot ordering, and preserved test tags/accordion/category icon.
-   **`app/src/androidTest/java/com/lorenzo/monefyme/ui/TransactionHistoryAccordionTest.kt`**: updated assertions to use `transaction_row_<id>` tags instead of description texts, which by design no longer render on entry rows (accordion interaction coverage unchanged).
-   Verification run (repo root): `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL, 0 failures (new suite: 8 tests, 0 failures); `./gradlew compileDebugAndroidTestSources` → BUILD SUCCESSFUL. Note: `connectedDebugAndroidTest` was not run (needs emulator and is out of the harness verify commands), but its sources compile.
-   STATUS: DONE Redesigned transaction history group cards in HomeScreen.kt (shared header/entry background, green-circled count badge after group name, red/green group total by majority type, per-entry red/green type dot, amount+date entries when grouped by category, category+amount entries when grouped by date); added TransactionGroupCardTest (8 tests) and updated TransactionHistoryAccordionTest to tag-based assertions; `./gradlew testDebugUnitTest` green.
