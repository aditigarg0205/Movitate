# MindEase – Mental Wellness Portal (JavaFX, 100% Java)

Desktop mental-wellness app:

**Features**
- Dashboard with **Daily Wellness Score** (0-100 ring), **Wellness Streak**, 7-day average, mood chart, daily tip
- **Mood check-in** (1-5 + note) and history
- **Mood calendar** – month grid coloured by mood, click a day for details
- **Mood journey** – 7/30/90-day chart, trend, best/toughest day, most common mood
- **Journal** – create, edit, delete, search
- **AI companion** – built-in, offline, rule-based chat (no internet, nothing saved). Any sign of
  self-harm switches it to a fixed safety message with helplines
- **My growth** – levels, this week's small wins, achievement badges
- **Breathe** – animated 4-4-6 breathing guide
- **Self-check** – PHQ-2 / GAD-2 screeners (not a diagnosis)
- **Get help** – helplines (India) and coping ideas

**Admin portal** (separate "Admin portal" tab on the login screen, sunny orange sidebar)
- First run: create the admin account (only possible while no admin exists)
- Overview: stats, **mood suns** (happy / calm & meditative / sad, last 30 days) and mood chart
- Users: disable/enable, reset password, delete user + all their data
- Privacy: admins never see journal text or mood notes

Background: sunny sky with a glowing sun, slowly turning rays and drifting clouds.

## Requirements
JDK 17+ and Maven 3.8+ (JavaFX is downloaded by Maven).

## Run
```bash
mvn javafx:run
```
Tests (backend): `mvn test`

## Structure
```
src/main/java/com/wellness/
├── App.java                   entry point; opens user or admin portal by role
├── model/                     User (role, disabled), MoodEntry, JournalEntry, AssessmentResult
├── storage/FileStore.java     atomic, fsync'd, thread-safe storage with rollback + corrupt-file quarantine
├── service/
│   ├── AuthService            register/login, admin vs user portal, lock-out, disable/reset/delete
│   ├── PasswordHasher         PBKDF2-HMAC-SHA256 + salt
│   ├── MoodService            check-ins, calendar data, averages, streak, longest streak
│   ├── JournalService, AssessmentService
│   ├── WellnessService        daily score + mood journey insights
│   ├── GrowthService          achievements, levels, weekly wins
│   ├── CompanionService       offline rule-based companion + crisis detection
│   ├── AdminService           aggregate stats + account management
│   └── Services               wiring
└── ui/                        Shell (sidebar), MainView (user), AdminMainView, Login, Dashboard, Mood,
                               Calendar, Journey, Journal, Companion, Growth, Breathing, Assessment,
                               Resources, AdminOverview, AdminUsers, CalmBackground
src/main/resources/style.css
src/test/java/com/wellness/BackendTest.java
```

## Daily Wellness Score
`mood today (up to 60) + checked in (10) + journal entry today (15) + streak bonus (3/day, max 15)` = 100.

## Data
Stored in `~/.mindease/` (users.dat, moods.dat, journal.dat, assessments.dat). Delete the folder to reset.
Existing data from version 1 keeps working. Journal text is stored on your computer without encryption.

## Important
Self-help tool, not medical advice. The companion is a simple program, not a therapist.
Please verify the helpline numbers in `ResourcesView.java` and `CompanionService.java` for your region.
# Movitate
