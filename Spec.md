# PracticeTracker — Detailed Specification

## 1. Purpose & Scope

PracticeTracker is an Android application for students who practice a skill through structured daily repetition — primarily musicians (violin, piano, etc.) but applicable to any discipline that breaks practice time into a list of exercises. The app covers the full practice lifecycle: planning what to practice, running an active session with timing, and reviewing history and progress over time.

---

## 2. User Profile

### 2.1 Profile Setup
- On first launch the app presents an onboarding flow to create a user profile.
- **Required fields:** Display name, instrument/discipline (free text with suggestions, e.g. "Violin", "Piano", "Chess").
- **Optional fields:** Avatar (photo from gallery or camera), skill level (Beginner / Intermediate / Advanced / Professional), teacher name.
- Profile is stored locally; there is no account or cloud sync.

### 2.2 Profile Management
- Accessible from a persistent settings entry point (top-right menu or dedicated tab).
- User can edit all fields at any time.
- Deleting the profile wipes all local data (with confirmation dialog).

---

## 3. Data Model

### 3.1 Piece
A piece is any unit of practice: a scale, etude, song, concerto movement, exercise, etc.

| Field | Type | Notes |
|---|---|---|
| `id` | UUID | Primary key |
| `title` | String | Required. E.g. "Twinkle Twinkle", "G Major Scale", "Kreutzer No. 2" |
| `type` | Enum | Scale, Etude, Song, Concerto, Exercise, Other |
| `composer` | String? | Optional |
| `book` | String? | Optional source book/method name |
| `pages` | String? | Optional page range or reference |
| `notes` | String? | Freeform notes |
| `suggestedMinutes` | Int | Suggested practice duration in minutes |
| `skills` | List\<Skill\> | Ordered checklist of focus items |
| `createdAt` | Timestamp | |

### 3.2 Skill
A named focus item that lives in a shared library and can be attached to any number of pieces.

| Field | Type | Notes |
|---|---|---|
| `id` | UUID | |
| `label` | String | E.g. "Bow pressure", "Smooth shifting", "Intonation" |
| `scope` | Enum | `GENERAL` (instrument-wide) or `PIECE_SPECIFIC` (created in context of one piece but reusable) |
| `createdAt` | Timestamp | |

Skills with identical labels are deduplicated: adding a skill by label first searches the library; a new `Skill` record is only created when no case-insensitive match exists.

### 3.3 PieceSkill
Join table that attaches skills to a piece in a specific order. This replaces the old `pieceId` field on `Skill`.

| Field | Type | Notes |
|---|---|---|
| `pieceId` | UUID | Foreign key → Piece |
| `skillId` | UUID | Foreign key → Skill |
| `order` | Int | Display order within this piece |

Removing a skill from a piece deletes only the `PieceSkill` row; the `Skill` record remains in the library and stays attached to any other pieces that use it.

### 3.5 PracticePlan
A named, reusable practice plan containing an ordered list of pieces.

| Field | Type | Notes |
|---|---|---|
| `id` | UUID | |
| `name` | String | E.g. "Tuesday Scales Focus" |
| `pieces` | List\<PlanEntry\> | Ordered list; each entry links a Piece and may override `suggestedMinutes` |
| `schedule` | Schedule | When this plan is assigned (see §3.6) |
| `createdAt` | Timestamp | |
| `clonedFromId` | UUID? | Set when cloned from another plan |

### 3.6 PlanEntry
Join record between PracticePlan and Piece, allowing per-plan overrides.

| Field | Type | Notes |
|---|---|---|
| `id` | UUID | |
| `planId` | UUID | |
| `pieceId` | UUID | |
| `order` | Int | Position in this plan |
| `overrideMinutes` | Int? | If set, overrides `Piece.suggestedMinutes` for this plan |

### 3.7 Schedule
Defines when a plan is active. Stored as part of PracticePlan.

| Schedule Type | Description |
|---|---|
| `DAILY` | Every day |
| `EVERY_OTHER_DAY` | Alternating days from a start date |
| `DAYS_OF_WEEK` | Specific days: a bitmask of Mon–Sun |
| `MANUAL` | No automatic scheduling; user starts it manually |

Only one plan may be "active" (scheduled) for any given day. If schedules overlap the app warns the user and asks which takes priority.

### 3.8 PracticeSession
A logged practice session.

| Field | Type | Notes |
|---|---|---|
| `id` | UUID | |
| `planId` | UUID | The plan used |
| `date` | LocalDate | Calendar date of the session |
| `startTime` | Timestamp | When the whole session started |
| `endTime` | Timestamp? | When the whole session ended (null if in progress) |
| `entries` | List\<SessionEntry\> | One entry per piece attempted |

### 3.9 SessionEntry
A record of practicing one piece within a session.

| Field | Type | Notes |
|---|---|---|
| `id` | UUID | |
| `sessionId` | UUID | |
| `pieceId` | UUID | |
| `startTime` | Timestamp | |
| `endTime` | Timestamp? | Null if skipped without starting or still in progress |
| `skipped` | Boolean | True if the user tapped Skip |
| `skillChecks` | List\<SkillCheck\> | Skills checked during this entry |

### 3.10 SkillCheck
Records when a skill was marked during a session entry.

| Field | Type | Notes |
|---|---|---|
| `skillId` | UUID | |
| `checkedAt` | Timestamp | |

---

## 4. Navigation & Screen Map

The app uses a bottom navigation bar with three top-level destinations:

```
[ Practice ]   [ Stats ]   [ Organize ]
```

A persistent top app bar shows the user's avatar and a settings icon.

---

## 5. Practice Mode

### 5.1 Session Home Screen
Shown when the user taps "Practice".

- **Today's Plan card:** Shows the plan scheduled for today (name, total suggested time, piece count). If no plan is scheduled, shows a prompt to go to Organize or start a manual session.
- **Start Session button:** Begins a new `PracticeSession`, records `startTime`.
- **In-progress session banner:** If a session was started but not finished (app was closed), offers to resume or discard it.

### 5.2 Active Session Screen
Displayed while a session is in progress.

**Session header (always visible):**
- Total elapsed session time (running clock)
- Progress indicator: "Piece 2 of 5"
- Pause / End Session buttons

**Current piece card:**
- Piece title, type badge (scale / etude / song…)
- Expandable metadata section: composer, book, pages, notes
- Suggested time chip (e.g. "Suggested: 8 min")
- Piece elapsed timer — starts when this piece's `SessionEntry.startTime` is set
- Skill checklist: each skill is a checkbox row. Tapping a checkbox records a `SkillCheck` with the current timestamp. Unchecking removes the record.
- **Done** button: sets `SessionEntry.endTime`, auto-advances to next piece
- **Skip** button: marks `SessionEntry.skipped = true`, moves to next piece without recording time

**Piece queue panel (swipe up or expandable bottom sheet):**
- List of remaining pieces with their suggested times
- Completed pieces shown with a checkmark; skipped with a strikethrough
- User can tap a future piece to jump to it (completing the current one first with a confirmation)

**Session end:**
- Triggered by "End Session" button or by completing the last piece
- Sets `PracticeSession.endTime`
- Transitions to the **Session Summary screen**

### 5.3 Session Summary Screen
Shown immediately after a session ends.

- Total time practiced
- Per-piece breakdown: time spent, skills checked
- Any earned badges/milestones (see §8)
- "Share" button if a milestone was hit
- "Done" returns to Session Home

---

## 6. Stats & Summary Mode

### 6.1 Dashboard
Top-level stats screen.

**Summary cards:**
- Total practice time today / this week / this month
- Current practice streak (consecutive days with at least one completed session)
- Longest streak (personal record)
- Sessions in the last 7 / 30 / 365 days (toggle between windows)
- Average session duration

**Charts:**
- Bar chart: daily practice minutes for the last 14 days
- Pie/donut chart: time distribution across pieces (last 30 days)

### 6.2 Piece Drill-Down
Accessed by tapping a piece in the dashboard chart or from a piece list.

- Total time practiced on this piece (all time, last 30 days)
- Number of sessions in which it appeared
- Skills breakdown: how often each skill was checked
- Timeline: session entries for this piece in reverse chronological order

### 6.3 Skill Drill-Down
Accessed by tapping a skill in the piece drill-down.

- Total times checked across all sessions
- Sessions where it was checked, with dates

### 6.4 History List
A scrollable reverse-chronological list of all sessions.

- Each row: date, plan name, total time, piece count
- Tap to open a read-only Session Summary view for that session
- Swipe to delete a session (with confirmation)

---

## 7. Organization Mode

### 7.1 Plan List Screen
- Shows all plans with name, piece count, schedule badge, and last-used date
- FAB: Create new plan
- Long-press or swipe actions: Edit, Clone, Delete

### 7.2 Plan Editor Screen
Used for both creating and editing a plan.

**Plan metadata:**
- Name (required)
- Schedule picker (Daily / Every Other Day / Days of Week checkboxes / Manual)
- If Days of Week: a row of toggleable day chips (M T W T F S S)

**Piece list:**
- Drag-to-reorder handles
- Each row shows piece title, type badge, suggested minutes (editable inline)
- Swipe to remove from plan (does not delete the Piece itself)
- "Add Piece" button opens the Piece Picker

**Suggestions panel (collapsible):**
- Based on pieces already in the plan, suggests:
  - Related scales (if a song is in the plan, suggest a scale in its key)
  - Common skills for the piece type
- User can tap a suggestion to add it directly

**Save / Discard actions in top app bar.**

### 7.3 Piece Picker
A searchable, filterable list of all pieces in the local library.

- Search by title, composer, or book
- Filter chips: piece type (Scale, Etude, Song…)
- "Create new piece" shortcut at the top if no match found

### 7.4 Piece Editor Screen
Create or edit a piece.

**Fields:** Title (required), Type, Composer, Book, Pages, Notes, Suggested Minutes (number input, default 5).

**Skill checklist editor:**

Skills are drawn from the shared skill library (§3.2). The editor shows the ordered list of skills currently attached to this piece via `PieceSkill` rows.

- **Add skill — search existing:** A text input that performs a live search against the skill library as the user types. Matching skill labels appear in a dropdown; tapping one creates a `PieceSkill` row linking that skill to this piece.
- **Add skill — create new:** If no match is found (or the user wants a piece-specific label like "Measures 10–25 only"), they can confirm the typed label to create a new `Skill` record (scope `PIECE_SPECIFIC`) and attach it.
- **Suggestions:** A chip row of commonly used skill labels filtered by piece type and instrument (e.g. "Intonation", "Bow pressure", "Tone quality", "Rhythm", "Shifting", "Vibrato", "Articulation"). Tapping a chip adds the skill via the same search-or-create logic — it reuses an existing matching `Skill` if one exists.
- **Reorder:** Drag handles change `PieceSkill.order`.
- **Remove:** Swiping a skill row removes the `PieceSkill` link. A warning is shown only if this was the last piece using that skill, offering to delete the orphaned `Skill` record or keep it in the library.

### 7.5 Skill Library Screen
Accessible from Settings or the Piece Editor. Shows all `Skill` records.

- List with search/filter by label or scope
- Each row shows the label and how many pieces use it
- Rename a skill: updates the `Skill.label` everywhere it is used
- Delete a skill: only allowed if it is not attached to any piece (or with a confirmation that removes all `PieceSkill` rows)

### 7.6 Clone Plan
Creates a full deep copy of a plan (new UUIDs for plan and all PlanEntries, but Piece references are shared, not copied). Sets `clonedFromId`. Opens the Plan Editor with the copy.

---

## 8. Achievements & Gamification

### 8.1 Milestones
Evaluated at the end of every session.

| Milestone | Trigger |
|---|---|
| First Session | Completed first ever session |
| 3-Day Streak | 3 consecutive days with a session |
| 7-Day Streak | 7 consecutive days |
| 30-Day Streak | 30 consecutive days |
| Streak Record | New personal best streak |
| 1 Hour Total | Cumulative practice hits 1 hour |
| 10 Hours Total | Cumulative hits 10 hours |
| 50 / 100 / 500 Hours Total | Same pattern |
| 1 Hour on a Piece | Cumulative time on a single piece hits 1 hour |
| 10 Hours on a Piece | Same pattern |
| Perfect Session | Session where no pieces were skipped |

### 8.2 Badge Display
- Badges appear as an overlay animation on the Session Summary screen when earned.
- All earned badges are viewable in a dedicated "Achievements" section within Stats mode.
- Unearned badges are shown as locked with a hint of how to unlock them.

### 8.3 Sharing
- Any earned badge can be shared via the Android share sheet as a generated image card containing: badge graphic, milestone label, user name, instrument, date.
- Share prompt is optional; user can dismiss it.

---

## 9. Notifications

### 9.1 Scheduled Reminders
- User can enable practice reminders in Settings.
- **Manual schedule:** User picks days of week and a time (e.g. every weekday at 6:00 PM).
- **Inferred schedule:** If the user has completed at least 7 sessions, the app analyzes the distribution of session start times by day-of-week and suggests a reminder time. User can accept or override.
- Notification text: "Time to practice! Your [Plan Name] is ready." with a deep-link action that opens Practice Mode directly.

### 9.2 Streak at Risk
- If the user has an active streak ≥ 3 days and has not practiced by 8:00 PM local time, send a "Don't break your streak!" notification (opt-in, enabled by default when streaks feature is first earned).

### 9.3 Notification Permissions
- App requests `POST_NOTIFICATIONS` permission (Android 13+) at the point the user first enables notifications, not at launch.

---

## 10. Settings

| Setting | Default | Notes |
|---|---|---|
| Practice reminders enabled | Off | |
| Reminder schedule | Manual | Manual or Inferred |
| Streak-at-risk notification | On (once streaks start) | |
| Default suggested practice time | 5 min | Applied to new pieces |
| Theme | System default | Light / Dark / System |
| Instrument/discipline | From profile | Quick-edit shortcut |

---

## 11. Design & UX Guidelines

- **Visual language:** Warm, classic aesthetic appropriate for a music practice context. Avoid generic Material Design defaults. Consider serif typography for piece/plan names, earthy or deep jewel-tone color palette, subtle parchment/wood textures as accents only (not overwhelming backgrounds).
- **Navigation:** Bottom nav with three tabs. No nested bottom navs. Deep navigation (e.g. Piece Editor) uses the back stack with a top app bar back arrow.
- **Active session is interruptible:** If the user leaves the app mid-session, the session is preserved in "paused" state and a persistent notification shows elapsed time. Returning to the app resumes the session.
- **Accessibility:** All interactive elements meet 48dp minimum touch target. Support system font scaling. Content descriptions on all icons.
- **Empty states:** Every list screen has an illustrated empty state with a clear call-to-action (e.g. "No plans yet — tap + to create one").

---

## 12. Technical Architecture

### 12.1 Platform & Minimum SDK
- **Language:** Kotlin
- **Minimum SDK:** API 28 (Android 9 / Pie) — covers phones released in the last 5+ years
- **Target SDK:** Latest stable

### 12.2 Architecture Pattern
MVVM with a Repository layer.

```
UI Layer (Compose screens)
    ↓ observes StateFlow / collectAsState
ViewModel Layer (one ViewModel per screen/feature)
    ↓ calls
Repository Layer (single source of truth, Room + DataStore)
    ↓
Room Database (local SQLite)
DataStore Preferences (settings, user profile)
```

### 12.3 Key Libraries

| Library | Purpose |
|---|---|
| Jetpack Compose | UI |
| Navigation Compose | In-app navigation |
| Room | Local database / ORM |
| DataStore (Preferences) | Settings and user profile |
| Hilt | Dependency injection |
| WorkManager | Scheduled notifications and deferred work |
| Vico or MPAndroidChart | Charts in Stats mode |
| Coil | Image loading (avatar) |

### 12.4 Database
Single Room database with the entities from §3. Migrations must be provided for any schema change; no destructive migrations in production.

### 12.5 Notifications
Implemented via `NotificationManager` + `AlarmManager` (exact alarms for scheduled reminders) or `WorkManager` (periodic streak checks). Uses a dedicated `NotificationChannel` per notification type (reminders, streak alerts, achievements).

### 12.6 Suggestions Engine
A simple local rules engine (no ML):
- Scale suggestions: a lookup map of common keys/scales per instrument type.
- Skill suggestions: a lookup map of skill labels indexed by piece type and instrument.
- Future: could be extended with a local embeddings model, but v1 uses rule-based lookup.

---

## 13. Out of Scope (v1)

- Cloud sync or backup
- Multi-user / family accounts
- Apple iOS version
- Audio recording during sessions
- Metronome or tuner integration
- Teacher–student sharing
- In-app purchases or monetization
