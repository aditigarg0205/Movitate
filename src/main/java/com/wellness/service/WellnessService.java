package com.wellness.service;

import com.wellness.model.MoodEntry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Daily wellness score and mood-journey insights (all computed from the user's own data). */
public class WellnessService {
    private final MoodService mood;
    private final JournalService journal;

    public WellnessService(MoodService mood, JournalService journal) {
        this.mood = mood;
        this.journal = journal;
    }

    /** score is 0-100; available=false until the user checks in today. */
    public record DailyScore(boolean available, int score, String label, String message,
                             int moodPoints, int checkInPoints, int journalPoints, int streakPoints) {}

    /**
     * Score = mood today (up to 60) + checked in (10) + wrote in journal today (15)
     *         + streak bonus (3 per day, up to 15)  =  max 100.
     */
    public DailyScore dailyScore(String userId) {
        List<MoodEntry> today = mood.forDay(userId, LocalDate.now());
        if (today.isEmpty()) {
            return new DailyScore(false, 0, "No score yet",
                    "Do a mood check-in to see today's wellness score.", 0, 0, 0, 0);
        }
        double avg = today.stream().mapToInt(MoodEntry::mood).average().orElse(3);
        int moodPts = (int) Math.round((avg - 1) / 4.0 * 60);
        int checkPts = 10;
        int journalPts = journal.countSince(userId, LocalDate.now().atStartOfDay()) > 0 ? 15 : 0;
        int streakPts = Math.min(mood.streak(userId), 5) * 3;
        int score = Math.min(100, moodPts + checkPts + journalPts + streakPts);

        String label, msg;
        if (score >= 80) {
            label = "Thriving";
            msg = "A bright day. Notice what helped and try to repeat it.";
        } else if (score >= 60) {
            label = "Doing well";
            msg = "You're in a good place. Keep up the small habits.";
        } else if (score >= 40) {
            label = "Steady";
            msg = "A middling day is okay. Be gentle with yourself.";
        } else {
            label = "Needs some care";
            msg = "Tough days happen. Try the Breathe screen or talk to the companion.";
        }
        return new DailyScore(true, score, label, msg, moodPts, checkPts, journalPts, streakPts);
    }

    // ---------------- Mood journey ----------------

    public record Journey(int checkIns, int daysWithData, double average, int mostCommonMood,
                          LocalDate bestDay, double bestAverage,
                          LocalDate toughestDay, double toughestAverage,
                          String trend, LocalDate firstCheckIn) {}

    public Optional<Journey> journey(String userId, int days) {
        SortedMap<LocalDate, Double> daily = mood.dailyAverages(userId, days);
        if (daily.isEmpty()) return Optional.empty();

        LocalDate from = LocalDate.now().minusDays(days - 1L);
        List<MoodEntry> inRange = new ArrayList<>();
        for (MoodEntry e : mood.chronological(userId))
            if (!e.at().toLocalDate().isBefore(from)) inRange.add(e);

        int[] counts = new int[6];
        double sum = 0;
        for (MoodEntry e : inRange) {
            counts[e.mood()]++;
            sum += e.mood();
        }
        int common = 3;
        for (int m = 1; m <= 5; m++) if (counts[m] > counts[common]) common = m;

        Map.Entry<LocalDate, Double> best = null, worst = null;
        for (Map.Entry<LocalDate, Double> e : daily.entrySet()) {
            if (best == null || e.getValue() > best.getValue()) best = e;
            if (worst == null || e.getValue() < worst.getValue()) worst = e;
        }

        String trend = "Not enough data yet";
        if (daily.size() >= 4) {
            List<Double> v = new ArrayList<>(daily.values());
            int half = v.size() / 2;
            double first = v.subList(0, half).stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double second = v.subList(half, v.size()).stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double diff = second - first;
            trend = diff >= 0.3 ? "Improving" : diff <= -0.3 ? "Dipping lately" : "Steady";
        }

        LocalDate firstEver = mood.chronological(userId).get(0).at().toLocalDate();
        return Optional.of(new Journey(inRange.size(), daily.size(), sum / inRange.size(), common,
                best.getKey(), best.getValue(), worst.getKey(), worst.getValue(), trend, firstEver));
    }

    public static LocalDateTime startOfToday() {
        return LocalDate.now().atStartOfDay();
    }
}
