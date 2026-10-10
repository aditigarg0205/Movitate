package com.wellness.service;

import com.wellness.model.MoodEntry;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.SortedMap;

/** Small achievements, levels and weekly "wins". Everything is computed from existing data. */
public class GrowthService {
    private final MoodService mood;
    private final JournalService journal;
    private final AssessmentService assessment;

    public GrowthService(MoodService mood, JournalService journal, AssessmentService assessment) {
        this.mood = mood;
        this.journal = journal;
        this.assessment = assessment;
    }

    public record Achievement(String id, String emoji, String title, String description, int current, int target) {
        public boolean unlocked() {
            return current >= target;
        }

        public double progress() {
            return Math.min(1.0, current / (double) target);
        }
    }

    public record Level(String name, int points, int fromPoints, Integer nextAt) {
        public double progress() {
            if (nextAt == null) return 1.0;
            return (points - fromPoints) / (double) (nextAt - fromPoints);
        }
    }

    public List<Achievement> achievements(String userId) {
        int checkIns = mood.count(userId);
        int longest = mood.longestStreak(userId);
        int entries = journal.count(userId);
        int checks = assessment.count(userId);

        List<Achievement> list = new ArrayList<>();
        list.add(new Achievement("first_checkin", "🌱", "First step", "Complete your first mood check-in.", checkIns, 1));
        list.add(new Achievement("checkins_10", "🌿", "Finding a rhythm", "Complete 10 mood check-ins.", checkIns, 10));
        list.add(new Achievement("checkins_50", "🌳", "Self-awareness", "Complete 50 mood check-ins.", checkIns, 50));
        list.add(new Achievement("streak_3", "🔥", "3-day streak", "Check in 3 days in a row.", longest, 3));
        list.add(new Achievement("streak_7", "🔥", "One-week streak", "Check in 7 days in a row.", longest, 7));
        list.add(new Achievement("streak_30", "🏆", "Habit builder", "Check in 30 days in a row.", longest, 30));
        list.add(new Achievement("journal_1", "📖", "Dear diary", "Write your first journal entry.", entries, 1));
        list.add(new Achievement("journal_10", "✍️", "Storyteller", "Write 10 journal entries.", entries, 10));
        list.add(new Achievement("selfcheck_1", "🧠", "Taking stock", "Complete a self-check.", checks, 1));
        list.add(new Achievement("bounce_back", "🌤", "Bounce back",
                "Feel better by 2+ points after a low check-in.", bouncedBack(userId) ? 1 : 0, 1));
        list.add(new Achievement("bright_day", "☀️", "Bright day",
                "Have a day with an average mood of 4.5 or more.", brightDays(userId) > 0 ? 1 : 0, 1));
        return list;
    }

    public Level level(String userId) {
        int points = mood.count(userId) * 5 + journal.count(userId) * 10
                + assessment.count(userId) * 5 + mood.longestStreak(userId) * 3;
        String[] names = {"Seedling", "Sprout", "Blooming", "Flourishing", "Thriving"};
        int[] from = {0, 50, 150, 300, 600};
        int idx = 0;
        for (int i = 0; i < from.length; i++) if (points >= from[i]) idx = i;
        Integer next = idx + 1 < from.length ? from[idx + 1] : null;
        return new Level(names[idx], points, from[idx], next);
    }

    /** Positive, encouraging highlights for the past 7 days. */
    public List<String> weeklyWins(String userId) {
        List<String> wins = new ArrayList<>();
        int days = mood.daysCheckedIn(userId, 7);
        if (days > 0) wins.add("You checked in on " + days + " of the last 7 days.");

        LocalDate today = LocalDate.now();
        OptionalDouble thisWeek = mood.averageBetween(userId, today.minusDays(6), today);
        OptionalDouble lastWeek = mood.averageBetween(userId, today.minusDays(13), today.minusDays(7));
        if (thisWeek.isPresent() && lastWeek.isPresent()) {
            double diff = thisWeek.getAsDouble() - lastWeek.getAsDouble();
            if (diff >= 0.3) wins.add(String.format("Your average mood is up %.1f points from last week.", diff));
            else if (diff <= -0.3) wins.add("This week has been harder. Showing up for yourself still counts.");
        }

        int entries = journal.countSince(userId, today.minusDays(6).atStartOfDay());
        if (entries > 0) wins.add("You wrote " + entries + (entries == 1 ? " journal entry" : " journal entries")
                + " this week.");

        int streak = mood.streak(userId);
        if (streak >= 2) wins.add("You're on a " + streak + "-day check-in streak.");

        if (wins.isEmpty()) wins.add("Check in today to start your first small win.");
        return wins;
    }

    private boolean bouncedBack(String userId) {
        List<MoodEntry> list = mood.chronological(userId);
        for (int i = 1; i < list.size(); i++)
            if (list.get(i - 1).mood() <= 2 && list.get(i).mood() - list.get(i - 1).mood() >= 2) return true;
        return false;
    }

    private long brightDays(String userId) {
        SortedMap<LocalDate, Double> all = mood.dailyAverages(userId, 3650);
        return all.values().stream().filter(v -> v >= 4.5).count();
    }
}
