# 2D Agent Ledger

Offline-first Android ledger for Agents, Customers, 2D betting input, limits,
closed numbers, global winning numbers, and reports.

## Run & build

- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`
- GitHub Actions runs both commands and uploads the debug APK.

## Architecture

- Kotlin + Jetpack Compose for the UI.
- Room is the local source of truth; the app does not require a server or login.
- Parser and calculations live under `domain/` and are independently testable.
- Agent, Customer, and global records are relational Room entities.
- All digits are normalized to two-character `00`–`99` strings.

## Product decisions

- Betting records belong to a local date and one of two draw sessions: `မနက်` or `ညနေ`.
- Winning numbers are unique per date/session and global to all Agents.
- Closed days are global and can contain multiple dates.
- Closed numbers belong to one Agent.
- Amounts are positive whole MMK values.
- Both Burmese and English digits are accepted at the parser boundary.
- Lists derive totals from saved source entries instead of storing stale totals.