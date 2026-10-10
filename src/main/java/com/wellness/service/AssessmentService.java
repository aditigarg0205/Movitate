package com.wellness.service;

import com.wellness.model.AssessmentResult;
import com.wellness.storage.FileStore;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** Two-question screeners PHQ-2 (mood) and GAD-2 (anxiety). Screening only - NOT a diagnosis. */
public class AssessmentService {

    public enum Kind {
        PHQ2("Mood check (PHQ-2)", List.of(
                "Little interest or pleasure in doing things",
                "Feeling down, depressed, or hopeless")),
        GAD2("Anxiety check (GAD-2)", List.of(
                "Feeling nervous, anxious, or on edge",
                "Not being able to stop or control worrying"));

        public final String title;
        public final List<String> questions;

        Kind(String title, List<String> questions) {
            this.title = title;
            this.questions = questions;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public static final String[] ANSWERS = {"Not at all", "Several days", "More than half the days", "Nearly every day"};

    private final FileStore<AssessmentResult> store;

    public AssessmentService(FileStore<AssessmentResult> store) {
        this.store = store;
    }

    public AssessmentResult submit(String userId, Kind kind, int[] answers) {
        if (answers == null || answers.length != kind.questions.size())
            throw new WellnessException("Please answer every question.");
        int score = 0;
        for (int a : answers) {
            if (a < 0 || a > 3) throw new WellnessException("Invalid answer.");
            score += a;
        }
        AssessmentResult r = new AssessmentResult(UUID.randomUUID().toString(), userId, kind.name(), score, LocalDateTime.now());
        store.add(r);
        return r;
    }

    public static String interpret(int score) {
        if (score >= 3)
            return "Your score is at or above the usual screening cut-off (3). This is not a diagnosis, "
                    + "but it would be a good idea to talk to a doctor, counsellor or someone you trust.";
        return "Your score is below the usual screening cut-off (3). Keep checking in with yourself, "
                + "and reach out for support any time things feel heavy.";
    }

    public int count(String userId) {
        return (int) store.all().stream().filter(r -> r.userId().equals(userId)).count();
    }

    public int total() {
        return store.all().size();
    }

    /** Aggregate only: how many results are at/above the screening cut-off (3). */
    public int totalAtOrAboveCutoff() {
        return (int) store.all().stream().filter(r -> r.score() >= 3).count();
    }

    public void deleteAllFor(String userId) {
        store.removeIf(r -> r.userId().equals(userId));
    }

    public List<AssessmentResult> history(String userId) {
        return store.all().stream().filter(r -> r.userId().equals(userId))
                .sorted(Comparator.comparing(AssessmentResult::at).reversed())
                .collect(Collectors.toList());
    }
}
