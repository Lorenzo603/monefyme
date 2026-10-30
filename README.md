# MonefyMe (Android MVP)

Small budgeting tracker inspired by Monefy.

## Implemented MVP scope
- Jetpack Compose UI
- Transaction tracking with both:
  - expenses (`EXPENSE`, shown as negative in UI)
  - income (`INCOME`, shown as positive in UI)
- Room local persistence for categories and transactions
- Current-month-first default transaction view
- Home with expense donut chart by category and centered totals
- Large bottom quick actions for expense (`−`) and income (`+`)
- Home side-menu buttons open **Transactions** and **Restore**
- Add / edit / delete transactions
- Manual CSV restore screen (Monefy CSV import)
- Duplicate-safe re-import (idempotent)

## CSV restore behavior
- Expected date format: `dd/MM/yyyy`
- Category normalization:
  - trim
  - collapse repeated spaces
  - lowercase for duplicate lookup, display name preserved from normalized text
- Deterministic duplicate key:
  - date
  - category
  - amount
  - type
  - description
  - account
- Amount handling:
  - import keeps absolute amount in cents
  - sign maps to `EXPENSE`/`INCOME`

## Tech stack
- Kotlin
- Android SDK min 28
- Jetpack Compose
- Room
- Coroutines + Flow

## Local run
1. Open in Android Studio.
2. Let Gradle sync complete.
3. Run app on emulator/device (API 28+).

To restore your historical file:
1. Copy `Monefy.Data.29-10-2026.csv` to the emulator/device storage.
2. In app, from Home use the right side-menu button near Balance to open **Restore**.
3. Tap **Select CSV file** and choose the file.

## Tests
- Unit tests:
  - CSV parser coverage
- Instrumented tests:
  - Room import idempotency
  - Compose smoke test (core screens render)

Suggested commands:
- `./gradlew testDebugUnitTest`
- `./gradlew connectedDebugAndroidTest` (requires running emulator)

## Run Emulator
- `android emulator start medium_phone`
- `android emulator start --cold --headless medium_phone` headless for tests
- `android emulator stop`

## Build and Run App
- `android run :app`