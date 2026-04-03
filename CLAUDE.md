# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**PracticeTracker** is an Android app that helps music students (primarily violin, but generalizable) log and track daily practice sessions. Students organize practice into pieces/exercises, each with a skill checklist and suggested time. The app records session history and surfaces stats/streaks.

See `Requirements.md` for the full product specification.

## Build & Development Commands

Once the Android project is scaffolded, standard Gradle wrapper commands apply:

```bash
# Build debug APK
./gradlew assembleDebug

# Run all unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.practicetracker.ExampleTest"

# Run instrumented (on-device) tests
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Clean
./gradlew clean
```

## Planned Architecture

The app has three primary modes, which should map to top-level navigation destinations:

- **Practice mode** — active session view; lists today's pieces with skill checklists, timers, and auto-advance to next piece
- **Stats/Summary mode** — dashboard of aggregate practice time and drill-down by piece or skill over 7/30/365-day windows
- **Organization mode** — CRUD for practice plans (piece lists + skill checklists per piece), scheduling plans to days of the week

### Key domain concepts

| Concept | Description |
|---|---|
| `PracticePlan` | Named list of pieces assigned to a schedule (daily, specific days, etc.) |
| `Piece` | A piece/exercise/scale within a plan; has metadata (composer, book, page) and a skill checklist |
| `Skill` | A checkable focus item on a piece (e.g. "bow position", "measures 10–25") |
| `PracticeSession` | A logged session: which plan, start/end time, which skills were checked and when |
| `UserProfile` | Single local user; tied to all data |

### Technical constraints

- Android only, targeting devices from the last 5 years
- **All data stored locally on device** (no backend/cloud sync)
- Use Room for local persistence
- Notifications for scheduled practice reminders
- Shareable badges/milestone cards via Android share sheet
