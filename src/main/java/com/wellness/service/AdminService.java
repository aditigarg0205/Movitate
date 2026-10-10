package com.wellness.service;

import com.wellness.model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Admin portal logic. By design admins only see aggregate numbers and account details.
 * Journal text and mood notes are never exposed here.
 */
public class AdminService {
    private final AuthService auth;
    private final MoodService mood;
    private final JournalService journal;
    private final AssessmentService assessment;

    public AdminService(AuthService auth, MoodService mood, JournalService journal, AssessmentService assessment) {
        this.auth = auth;
        this.mood = mood;
        this.journal = journal;
        this.assessment = assessment;
    }

    public record Overview(int users, int disabledUsers, int activeLast7Days, int totalCheckIns,
                           OptionalDouble averageMood30, int[] moodDistribution30,
                           int selfChecks, int selfChecksAtOrAboveCutoff) {}

    public record UserRow(User user, int checkIns, int journalEntries, LocalDateTime lastCheckIn) {}

    public Overview overview() {
        List<User> users = auth.users();
        int disabled = (int) users.stream().filter(User::disabled).count();
        return new Overview(users.size(), disabled, mood.activeUsers(7).size(), mood.total(),
                mood.averageAll(30), mood.distribution(30),
                assessment.total(), assessment.totalAtOrAboveCutoff());
    }

    public List<UserRow> userRows() {
        List<UserRow> rows = new ArrayList<>();
        for (User u : auth.users())
            rows.add(new UserRow(u, mood.count(u.id()), journal.count(u.id()),
                    mood.lastCheckIn(u.id()).orElse(null)));
        return rows;
    }

    public void setDisabled(String userId, boolean disabled) {
        auth.setDisabled(userId, disabled);
    }

    public void resetPassword(String userId, String newPassword) {
        auth.resetPassword(userId, newPassword);
    }

    /** Removes the account and all of its data. */
    public void deleteUserAndData(String userId) {
        mood.deleteAllFor(userId);
        journal.deleteAllFor(userId);
        assessment.deleteAllFor(userId);
        auth.deleteUser(userId);
    }
}
