# PracticeTracker — Implementation Plan

Each phase produces a runnable, testable app increment. Later phases build on earlier ones without requiring rewrites.

---

## Phase 1 — Project Scaffold & Data Layer

**Goal:** Empty Android project with the full database schema wired up and verified by unit tests. No UI beyond a blank Activity.

### 1.1 Project setup
- Create Android project: Kotlin, Jetpack Compose, min SDK 28, target latest stable
- Configure `libs.versions.toml` with all library versions (Room, Hilt, DataStore, Navigation Compose, Coil, Vico, WorkManager, Coroutines)
- Enable Hilt in `Application` class and Gradle plugins
- Add `.gitignore`, update `CLAUDE.md` with actual build commands

### 1.2 Room database
Define all entities and DAOs:

| Entity | Notes |
|---|---|
| `PieceEntity` | All fields from §3.1; `type` stored as String enum |
| `SkillEntity` | `id`, `label`, `scope`, `createdAt` |
| `PieceSkillEntity` | Composite PK `(pieceId, skillId)`, `order` field |
| `PracticePlanEntity` | Includes `scheduleType` + `scheduleDays` (bitmask Int) + `scheduleStartDate` |
| `PlanEntryEntity` | `id`, `planId`, `pieceId`, `order`, `overrideMinutes` |
| `PracticeSessionEntity` | `id`, `planId`, `date`, `startTime`, `endTime` |
| `SessionEntryEntity` | `id`, `sessionId`, `pieceId`, `startTime`, `endTime`, `skipped` |
| `SkillCheckEntity` | `sessionEntryId`, `skillId`, `checkedAt`; composite PK |

DAOs: `PieceDao`, `SkillDao`, `PlanDao`, `SessionDao` — CRUD + the joins needed for each feature (e.g. `PieceWithSkills`, `PlanWithEntries`, `SessionWithEntries`).

### 1.3 Domain models & Repository
- Separate domain data classes (no Room annotations) mirroring the spec's model
- `PieceRepository`, `PlanRepository`, `SessionRepository` wrapping the DAOs
- Mappers between entity ↔ domain

### 1.4 DataStore
- `UserProfileStore`: name, instrument, skill level, teacher name, avatar URI
- `SettingsStore`: all settings from §10

### 1.5 Tests
- Unit tests for all DAOs using an in-memory Room database
- Unit tests for skill deduplication logic (case-insensitive match before insert)
- Unit tests for `Schedule` overlap detection

**Deliverable:** `./gradlew test` passes with coverage of all DAOs and repositories.

---

## Phase 2 — Navigation Shell & Onboarding

**Goal:** Running app with bottom navigation, three placeholder tabs, and a working onboarding/profile flow.

### 2.1 Theme & design tokens
- Define custom `MaterialTheme`: warm jewel-tone color scheme, serif + sans-serif type scale, shape tokens
- Light and dark variants
- No placeholder "stock" colors anywhere — establish the visual language from the start

### 2.2 Navigation graph
- `NavHost` with three top-level routes: `practice`, `stats`, `organize`
- Bottom nav bar wired to routes with proper back-stack behavior (single-top, save state)
- Top app bar slot with avatar `IconButton` → Settings route

### 2.3 Onboarding flow
- Shown on first launch (gated by `UserProfileStore.isProfileComplete`)
- Screen 1: Name + instrument input (with suggestion chips for common instruments)
- Screen 2: Optional avatar (camera / gallery via `ActivityResultContracts`), skill level, teacher
- Screen 3: Brief tour of the three modes (skip-able)
- Saves to `UserProfileStore`; navigates to Practice tab on completion

### 2.4 Settings screen
- Edit all profile fields
- All settings from §10 (displayed but not yet functional for notifications)
- "Delete all data" with confirmation dialog

**Deliverable:** App launches, onboarding completes, bottom nav switches tabs, settings screen is editable.

---

## Phase 3 — Organization Mode (Pieces & Plans)

**Goal:** Users can build and manage their full piece library and practice plans.

### 3.1 Piece Library
- `PieceListScreen`: searchable/filterable list, empty state, FAB
- `PieceEditorScreen`:
  - All metadata fields
  - Skill section with live-search input (queries `SkillDao.searchByLabel`), create-new path, suggestion chips (rules from `SuggestionEngine`)
  - Drag-to-reorder (`ReorderableColumn` or `LazyColumn` with `detectDragGestures`)
  - Swipe-to-remove with orphan-skill prompt

### 3.2 Skill Library screen
- `SkillLibraryScreen`: full list, search, usage count per skill
- Rename (inline edit or dialog) — updates `SkillEntity.label`
- Delete with guard if in use

### 3.3 Suggestion engine
- `SuggestionEngine` object: pure functions, no coroutines, easy to unit test
- `suggestSkills(pieceType, instrument): List<String>`
- `suggestScales(piecesInPlan): List<String>` — rule map keyed by instrument
- Seeded with violin-first data; generalized for other instruments

### 3.4 Plan Editor
- `PlanListScreen`: list with schedule badge, clone/edit/delete swipe actions
- `PlanEditorScreen`:
  - Name + schedule picker (type selector + day chips for `DAYS_OF_WEEK`)
  - Piece list with drag-reorder, per-entry minute override, swipe-remove
  - "Add Piece" opens `PiecePickerSheet` (bottom sheet)
  - Suggestions panel (collapsible) using `SuggestionEngine`
  - Schedule conflict detection on save

### 3.5 Clone plan
- Deep-copies `PracticePlanEntity` + all `PlanEntryEntity` rows (new UUIDs); sets `clonedFromId`
- Opens `PlanEditorScreen` with the copy pre-loaded

**Deliverable:** Full piece and plan CRUD; skill sharing between pieces works end-to-end.

---

## Phase 4 — Practice Mode (Active Session)

**Goal:** Users can run a timed practice session from start to finish.

### 4.1 Session Home screen
- `SessionHomeScreen`:
  - Today's scheduled plan card (queries `PlanRepository.getPlanForDate(today)`)
  - "Start Session" creates `PracticeSession` in DB and navigates to active session
  - In-progress session detection on launch; resume / discard banner

### 4.2 Active Session screen
- `ActiveSessionScreen` backed by `ActiveSessionViewModel`:
  - Holds session state in a `StateFlow<SessionUiState>` (current piece index, elapsed times, skill check states)
  - Two `ticker` coroutines: one for session-level elapsed time, one for piece-level elapsed time (pause-aware)
  - Persists each `SkillCheck` to DB immediately on tap (resilient to process death)
  - "Done" → writes `SessionEntry.endTime`, advances index or ends session
  - "Skip" → writes `skipped = true`, advances
  - Jump-to-piece via bottom sheet with confirmation
- Piece queue bottom sheet: shows all entries with done/skip state

### 4.3 Session persistence across process death
- `ActiveSessionViewModel` restores in-progress session from DB on `init` (checks for session with null `endTime`)
- A persistent foreground notification ("Session in progress — 12:34") keeps the process alive and provides a return deep-link
- Notification dismissed when session ends

### 4.4 Session Summary screen
- `SessionSummaryScreen`: total time, per-piece rows (duration, checked skills)
- Milestone evaluation runs here (§8 logic, Phase 6)
- "Done" navigates back to Session Home, clearing back stack

**Deliverable:** Full practice session flow including timers, skill checks, skip, pause, process-death recovery.

---

## Phase 5 — Stats & Summary Mode

**Goal:** Users can review their practice history and progress.

### 5.1 Aggregate queries
Add to `SessionDao`:
- `getTotalMinutesByDateRange(start, end): Flow<Int>`
- `getSessionCountByDateRange(start, end): Flow<Int>`
- `getDailyMinutesForLast14Days(): Flow<List<DailyMinutes>>`
- `getPieceTimeDistribution(days: Int): Flow<List<PieceTime>>`
- `getCurrentStreak(): Flow<Int>` (consecutive days with ≥1 completed session going back from today)
- `getLongestStreak(): Flow<Int>`
- `getSkillCheckCountByPiece(pieceId): Flow<List<SkillCheckCount>>`

### 5.2 Stats Dashboard screen
- `StatsDashboardScreen`:
  - Summary cards (today / week / month toggle)
  - Streak card (current + record)
  - Bar chart (Vico `ColumnChart`) — 14-day daily minutes
  - Pie/donut chart — piece time distribution
  - Tapping a pie slice navigates to Piece Drill-Down

### 5.3 Piece Drill-Down screen
- `PieceDrillDownScreen(pieceId)`: totals, session count, skill check frequency list, chronological entry timeline
- Tapping a skill navigates to Skill Drill-Down

### 5.4 Skill Drill-Down screen
- `SkillDrillDownScreen(skillId)`: total check count, list of sessions where it appeared

### 5.5 History List screen
- `HistoryListScreen`: reverse-chronological `LazyColumn` of sessions
- Each row: date, plan name, duration, piece count
- Tap → `SessionSummaryScreen` in read-only mode
- Swipe-to-delete with undo snackbar (soft-delete, purge after snackbar dismisses)

**Deliverable:** Full stats tab functional; all drill-downs navigate correctly.

---

## Phase 6 — Achievements & Badges

**Goal:** Milestones are evaluated after each session and badges are displayed and shareable.

### 6.1 Milestone engine
- `MilestoneEvaluator`: pure function `evaluate(sessionSummary, allTimeStats): List<Milestone>`
- Checks all triggers from §8.1
- Reads from `SessionDao` aggregate queries (already built in Phase 5)
- Unit-tested against known session histories

### 6.2 Badge assets
- Design badge graphics for each milestone (vector drawables)
- `BadgeRegistry`: maps `Milestone` enum → drawable resource + display label + unlock hint

### 6.3 Badge overlay on Session Summary
- After a session, `SessionSummaryViewModel` runs `MilestoneEvaluator`
- Newly earned badges animate in as an overlay card (scale + fade) using Compose `AnimatedVisibility`
- "View All Achievements" link navigates to badge screen

### 6.4 Achievements screen
- `AchievementsScreen` (reachable from Stats tab)
- Grid of all badges: earned ones colored + date earned, unearned ones greyed with hint text

### 6.5 Share card generation
- `BadgeShareHelper`: renders a `Composable` to a `Bitmap` using `View.drawToBitmap` or `Picture`
- Card contains: badge graphic, milestone label, user name, instrument, date
- Writes bitmap to cache dir, fires `ACTION_SEND` intent via Android share sheet

**Deliverable:** Badges earned and displayed after sessions; share sheet functional.

---

## Phase 7 — Notifications

**Goal:** Practice reminders and streak-at-risk alerts work reliably.

### 7.1 Notification channels
- Create three `NotificationChannel`s on app start: `practice_reminder`, `streak_alert`, `achievement`

### 7.2 Scheduled reminders
- `ReminderScheduler`: schedules/cancels an `AlarmManager` exact alarm for the user's reminder time
- Called whenever reminder settings change in Settings screen
- `ReminderReceiver` (`BroadcastReceiver`): fires the notification with a deep-link `PendingIntent` to the Practice tab

### 7.3 Streak-at-risk
- Daily `WorkManager` `PeriodicWorkRequest` at 8:00 PM local time
- `StreakCheckWorker`: queries today's sessions; if streak ≥ 3 and no session today, fires streak-risk notification
- Worker respects the opt-in setting

### 7.4 In-session foreground notification
- Started in Phase 4.3; finalized here with proper channel assignment and action buttons (Return to session)

### 7.5 Permission handling
- `NotificationPermissionHelper`: wraps `POST_NOTIFICATIONS` request (API 33+)
- Triggered only at the moment the user first enables reminders, not at app launch

**Deliverable:** Reminders fire at scheduled times; streak-at-risk alert fires at 8 PM when conditions met.

---

## Phase 8 — Polish & Hardening

**Goal:** Production-quality UX, accessibility, and edge-case handling.

### 8.1 Empty states
- Illustrated empty state composable for every list screen (Plan List, Piece List, History, Achievements)
- Each includes a CTA button relevant to the screen

### 8.2 Accessibility
- Audit all screens: 48dp touch targets, `contentDescription` on all icon buttons
- Test with TalkBack
- Verify system font-scale at 1.0×, 1.5×, 2.0×

### 8.3 Edge cases
- Two plans scheduled for the same day: conflict warning on save, priority picker
- Session started with zero pieces in plan: disable Start button, show inline message
- Piece deleted while it appears in an active plan: plan entry shows "[Deleted piece]" label
- App killed during active session: full recovery verified (Phase 4.3)

### 8.4 Performance
- `LazyColumn` for all long lists (no `Column` inside `ScrollView`)
- Database queries on IO dispatcher; verify no main-thread DB access
- Avatar image loaded via Coil with disk cache

### 8.5 UI consistency pass
- Verify theme tokens (color, type, shape) are used uniformly — no hardcoded colors
- Dark mode tested on all screens
- Transition animations between screens (Compose Navigation shared-axis or fade)

### 8.6 Instrumented tests
- `PracticeSessionFlowTest`: start session → complete pieces → verify DB state
- `OnboardingFlowTest`: first launch → profile created → lands on Practice tab
- `StatsDashboardTest`: seed sessions → verify streak and total time cards

---

## Dependency Graph

```
Phase 1 (Data Layer)
    └── Phase 2 (Shell & Onboarding)
            ├── Phase 3 (Organize)
            │       └── Phase 4 (Practice)
            │               └── Phase 5 (Stats)
            │                       └── Phase 6 (Achievements)
            │                               └── Phase 7 (Notifications)
            └── Phase 8 (Polish) ← runs in parallel with 6 & 7, finalizes after
```

---

## Key Technical Decisions

| Decision | Rationale |
|---|---|
| Hilt for DI | First-class Jetpack support; less boilerplate than manual DI at this scale |
| Room over raw SQLite | Type-safe queries, Flow integration, migration support |
| `AlarmManager` for reminders (not WorkManager alone) | `WorkManager` has timing imprecision (15-min window); exact alarms needed for scheduled practice times |
| Compose `StateFlow` in ViewModels (not `LiveData`) | Consistent with Kotlin-first approach; works well with Compose `collectAsState` |
| Vico for charts | Actively maintained, Compose-native API; MPAndroidChart is View-based and requires interop |
| No cloud sync in v1 | Simplifies auth, privacy, and offline behavior; can be added later without data model changes |
