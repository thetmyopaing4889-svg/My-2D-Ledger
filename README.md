# My 2D Ledger

Offline-first Android ledger for managing Agents, Customers, 2D betting records,
limits, closed numbers, winning numbers, and reports.

## Build locally

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

## Product rules

The business rules live in the Kotlin domain layer, not in Compose UI. All
00–99 values preserve leading zeroes. Calculated totals are derived from saved
source records.

## GitHub Actions

Every push and pull request to `main` or `master` runs unit tests, builds the
debug APK, and publishes it as a downloadable workflow artifact.