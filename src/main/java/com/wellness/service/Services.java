package com.wellness.service;

import com.wellness.storage.FileStore;

import java.nio.file.Path;

/** Wires the backend together. One instance per running app. */
public final class Services {
    public final AuthService auth;
    public final MoodService mood;
    public final JournalService journal;
    public final AssessmentService assessment;
    public final WellnessService wellness;
    public final GrowthService growth;
    public final CompanionService companion;
    public final AdminService admin;

    public Services(Path dataDir) {
        auth = new AuthService(new FileStore<>(dataDir.resolve("users.dat")));
        mood = new MoodService(new FileStore<>(dataDir.resolve("moods.dat")));
        journal = new JournalService(new FileStore<>(dataDir.resolve("journal.dat")));
        assessment = new AssessmentService(new FileStore<>(dataDir.resolve("assessments.dat")));
        wellness = new WellnessService(mood, journal);
        growth = new GrowthService(mood, journal, assessment);
        companion = new CompanionService(mood);
        admin = new AdminService(auth, mood, journal, assessment);
    }
}
