---
status: done
attempts: 2
title: "Add unit and integration tests"
source: backlog.md
depends_on: nothing
---

## Task

Perform the following:
- Add unit tests so to cover all current functionalities of the app.
- Add integration tests where appropriate.

These tests will run when running the commands:
- `./gradlew testDebugUnitTest`
- `./gradlew connectedDebugAndroidTest` (requires running emulator)

## Run log

-   **New integration tests** (`app/src/androidTest/...`, verified by `./gradlew connectedDebugAndroidTest` on the running emulator — 9 tests, 0 failures):
-   - `BudgetRepositoryIntegrationTest.kt` — full repository against a real in-memory Room DB: unique-index duplicate rejection, real SQL month filtering/ordering, expense-slice aggregation, update/delete flows, end-to-end idempotent CSV import
-   **Build tweak**: `app/build.gradle.kts` — `testOptions { unitTests.isReturnDefaultValues = true }` so stubbed `android.jar` exception types used by the ViewModel/repository tests don't throw "not mocked" in plain JVM tests.
-   Caveat: `connectedDebugAndroidTest` passed here because an emulator (emulator-5554) was running; it needs one in any environment. Task file status set to `done`. No production code was changed.
-   STATUS: DONE added unit tests (12 test classes incl. extended MonefyCsvParserTest) + Room-backed integration test; verified with `./gradlew testDebugUnitTest` (84 tests, 0 failures) and `./gradlew connectedDebugAndroidTest` (9 tests, 0 failures on emulator-5554)
-   An emulator is running (emulator-5554), so I can run instrumented tests too. Let me run the baseline unit tests first.
-   Baseline passes. Let me check the remaining app files for context (MainActivity, MonefyMeApp, UI test tags).
-   Now I'll write the unit tests. Starting with the pure logic ones:
-   Now extend the CSV parser test with edge cases:
-   Now the big repository unit test with in-memory fake DAOs:
-   Now the ViewModel tests. First add a test-options tweak so the stubbed `android.jar` classes (used for exception types) don't blow up in plain JVM tests:
-   Now let's run the unit tests:
-   The StateFlows aren't started without subscribers. I'll use a shared unconfined test dispatcher and subscribe via `backgroundScope`:
-   My assertion for the second import run was wrong (both rows conflict with stored data on re-import):
-   Now build and run the instrumented tests on the emulator:
-   On the second import all three CSV rows (including the intra-file duplicate) conflict — expected duplicates is 3, not 2:
-   All instrumented tests pass too. Final verification run of both suites:
-   All green. Summary of changes:
-   **New unit tests** (`app/src/test/...`, verified by `./gradlew testDebugUnitTest` — 84 tests, 0 failures):
-   - `data/importer/MonefyCsvParserTest.kt` — extended: escaped quotes, quoted comma-decimal amounts, EUR currency default, blank/short rows, strict date format
-   - `data/repository/BudgetRepositoryImplTest.kt` — new: category normalization/dedup, transaction validation (amount/category), category resolution & on-the-fly creation, insert/update/delete logic, month range computation, CSV import (type mapping from sign, HALF_UP rounding, duplicate/invalid counting, category caching) against in-memory fake DAOs
-   - `data/model/TransactionListItemTest.kt`, `domain/TransactionTypeTest.kt` — derived properties / storage parsing
-   - `ui/util/FormattersTest.kt`, `ui/util/CurrencyDisplayTest.kt` — date/amount parsing & formatting, cents small-font annotation
-   - `ui/category/DefaultCategoriesTest.kt`, `CategoryIconRegistryTest.kt`, `CategoryAppearanceTest.kt` — icon/color resolution incl. aliases and fallbacks
-   - `ui/viewmodel/MainViewModelTest.kt`, `MainViewModelFactoryTest.kt` — month navigation (future-month guard), save/delete/import validation & messaging, error handling (IAE/SQLException), picker category ordering
-   **New integration tests** (`app/src/androidTest/...`, verified by `./gradlew connectedDebugAndroidTest` on the running emulator — 9 tests, 0 failures):
-   - `BudgetRepositoryIntegrationTest.kt` — full repository against a real in-memory Room DB: unique-index duplicate rejection, real SQL month filtering/ordering, expense-slice aggregation, update/delete flows, end-to-end idempotent CSV import
-   **Build tweak**: `app/build.gradle.kts` — `testOptions { unitTests.isReturnDefaultValues = true }` so stubbed `android.jar` exception types used by the ViewModel/repository tests don't throw "not mocked" in plain JVM tests.
-   Caveat: `connectedDebugAndroidTest` passed here because an emulator (emulator-5554) was running; it needs one in any environment. Task file status set to `done`. No production code was changed.
-   STATUS: DONE added unit tests (12 test classes incl. extended MonefyCsvParserTest) + Room-backed integration test; verified with `./gradlew testDebugUnitTest` (84 tests, 0 failures) and `./gradlew connectedDebugAndroidTest` (9 tests, 0 failures on emulator-5554)
