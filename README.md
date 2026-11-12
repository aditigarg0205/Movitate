# MindEase – Mental Wellness Portal (JavaFX, 100% Java)

Desktop mental-wellness app:

**Features**
- Dashboard with **Daily Wellness Score** (0-100 ring), **Wellness Streak**, 7-day average, mood chart, daily tip and personal journal.
- **Mood check-in** (1-5 + note) and history
- **Mood calendar** – month grid coloured by mood, click a day for details
- **Mood journey** – 7/30/90-day chart, trend, best/toughest day, most common mood
- **Journal** – create, edit, delete, search and private 
- **AI companion** – built-in, offline, rule-based chat. Any sign of self-harm switches it to a fixed safety message with helplines
- **My growth** – levels, this week's small wins, achievement badges
- **Breathe** – animated 4-4-6 breathing guide
- **Self-check** – PHQ-2 / GAD-2 screeners (not a diagnosis)
- **Get help** – helplines (India) and coping ideas

**Admin portal** (separate "Admin portal" tab on the login screen, sidebar)
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
Motivation Behind MindEase – Mental Wellness Portal
MindEase is designed with one simple belief: Mental health matters just as much as physical health.

In today's fast-paced world, students and young adults often experience academic pressure, stress, anxiety, loneliness, and emotional exhaustion. However, many people hesitate to discuss their feelings or seek professional help because of social stigma, lack of awareness, or limited access to mental wellness resources.

The motivation behind developing MindEase is to create a safe, simple, and user-friendly digital platform where individuals can understand their emotions, track their mental well-being, and develop healthier daily habits.

The application encourages users to take small steps towards better mental health through daily mood check-ins, personal journaling, breathing exercises, self-assessment tools, and an offline AI companion. Features like wellness streaks, achievements, and mood journey insights motivate users to maintain consistency and recognize their emotional progress.

Another important motivation is privacy and accessibility. MindEase works offline, stores personal data locally, and ensures that administrators cannot access private journal entries or mood notes.
Key Motivational Objectives
1. Mental Health Awareness: Encourage people to understand and prioritize their emotional well-being.
2. Self-Reflection: Help users identify emotional patterns through mood tracking and journaling.
3. Stress Management: Provide breathing exercises and simple coping techniques.
4. Daily Motivation: Encourage consistency through wellness scores, streaks, levels, and achievements.
5. Accessibility: Provide essential wellness features without requiring an internet connection.
6. Privacy: Maintain confidentiality of personal emotional information.
7. Early Awareness: Help users recognize possible symptoms of anxiety and depression through basic screening tools, without claiming to diagnose them.
Project Vision
"Your mind deserves care, attention, and kindness. MindEase aims to make mental wellness a part of everyday life, helping individuals understand themselves better, one day and one small step at a time."
