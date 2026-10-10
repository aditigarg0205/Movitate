package com.wellness.service;

import com.wellness.model.MoodEntry;
import com.wellness.storage.FileStore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MoodService {
    private final FileStore<MoodEntry> store;

    public MoodService(FileStore<MoodEntry> store) {
        this.store = store;
    }

    public MoodEntry log(String userId, int mood, String note) {
        if (mood < 1 || mood > 5) throw new WellnessException("Please choose a mood between 1 and 5.");
        note = note == null ? "" : note.trim();
        if (note.length() > 500) throw new WellnessException("Note is too long (max 500 characters).");
        MoodEntry e = new MoodEntry(UUID.randomUUID().toString(), userId, mood, note, LocalDateTime.now());
        store.add(e);
        return e;
    }

    // ---------------- per-user reads ----------------

    /** Newest first. */
    public List<MoodEntry> recent(String userId, int limit) {
        return mine(userId).sorted(Comparator.comparing(MoodEntry::at).reversed())
                .limit(limit).collect(Collectors.toList());
    }

    /** Oldest first (for charts and sequences). */
    public List<MoodEntry> chronological(String userId) {
        return mine(userId).sorted(Comparator.comparing(MoodEntry::at)).collect(Collectors.toList());
    }

    public Optional<MoodEntry> latest(String userId) {
        return mine(userId).max(Comparator.comparing(MoodEntry::at));
    }

    public List<MoodEntry> forDay(String userId, LocalDate day) {
        return mine(userId).filter(e -> e.at().toLocalDate().equals(day))
                .sorted(Comparator.comparing(MoodEntry::at)).collect(Collectors.toList());
    }

    public int count(String userId) {
        return (int) mine(userId).count();
    }

    /** Average mood per day for the last N days (days without check-ins are omitted). */
    public SortedMap<LocalDate, Double> dailyAverages(String userId, int days) {
        LocalDate from = LocalDate.now().minusDays(days - 1L);
        return mine(userId)
                .filter(e -> !e.at().toLocalDate().isBefore(from))
                .collect(Collectors.groupingBy(e -> e.at().toLocalDate(), TreeMap::new,
                        Collectors.averagingInt(MoodEntry::mood)));
    }

    /** Average mood per day for every day of a calendar month. */
    public SortedMap<LocalDate, Double> dailyAverages(String userId, YearMonth month) {
        return mine(userId)
                .filter(e -> YearMonth.from(e.at()).equals(month))
                .collect(Collectors.groupingBy(e -> e.at().toLocalDate(), TreeMap::new,
                        Collectors.averagingInt(MoodEntry::mood)));
    }

    public OptionalDouble average(String userId, int days) {
        return averageBetween(userId, LocalDate.now().minusDays(days - 1L), LocalDate.now());
    }

    /** Inclusive date range. */
    public OptionalDouble averageBetween(String userId, LocalDate from, LocalDate to) {
        return mine(userId)
                .filter(e -> !e.at().toLocalDate().isBefore(from) && !e.at().toLocalDate().isAfter(to))
                .mapToInt(MoodEntry::mood).average();
    }

    /** Consecutive days with at least one check-in (today may still be empty). */
    public int streak(String userId) {
        Set<LocalDate> dates = datesOf(userId);
        LocalDate day = LocalDate.now();
        if (!dates.contains(day)) day = day.minusDays(1);
        int n = 0;
        while (dates.contains(day)) {
            n++;
            day = day.minusDays(1);
        }
        return n;
    }

    public int longestStreak(String userId) {
        TreeSet<LocalDate> dates = new TreeSet<>(datesOf(userId));
        int best = 0, run = 0;
        LocalDate prev = null;
        for (LocalDate d : dates) {
            run = (prev != null && prev.plusDays(1).equals(d)) ? run + 1 : 1;
            best = Math.max(best, run);
            prev = d;
        }
        return best;
    }

    public int daysCheckedIn(String userId, int lastDays) {
        LocalDate from = LocalDate.now().minusDays(lastDays - 1L);
        return (int) datesOf(userId).stream().filter(d -> !d.isBefore(from)).count();
    }

    // ---------------- admin aggregates (no notes, no per-user content) ----------------

    public int total() {
        return store.all().size();
    }

    /** index 0 = mood 1 ... index 4 = mood 5, over the last N days, all users. */
    public int[] distribution(int days) {
        LocalDate from = LocalDate.now().minusDays(days - 1L);
        int[] d = new int[5];
        for (MoodEntry e : store.all())
            if (!e.at().toLocalDate().isBefore(from)) d[e.mood() - 1]++;
        return d;
    }

    public OptionalDouble averageAll(int days) {
        LocalDate from = LocalDate.now().minusDays(days - 1L);
        return store.all().stream().filter(e -> !e.at().toLocalDate().isBefore(from))
                .mapToInt(MoodEntry::mood).average();
    }

    public Set<String> activeUsers(int days) {
        LocalDate from = LocalDate.now().minusDays(days - 1L);
        return store.all().stream().filter(e -> !e.at().toLocalDate().isBefore(from))
                .map(MoodEntry::userId).collect(Collectors.toSet());
    }

    public Optional<LocalDateTime> lastCheckIn(String userId) {
        return mine(userId).map(MoodEntry::at).max(Comparator.naturalOrder());
    }

    public void deleteAllFor(String userId) {
        store.removeIf(e -> e.userId().equals(userId));
    }

    // ---------------- helpers ----------------

    private Set<LocalDate> datesOf(String userId) {
        return mine(userId).map(e -> e.at().toLocalDate()).collect(Collectors.toSet());
    }

    private Stream<MoodEntry> mine(String userId) {
        return store.all().stream().filter(e -> e.userId().equals(userId));
    }
}
