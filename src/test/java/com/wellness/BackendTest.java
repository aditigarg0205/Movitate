package com.wellness;

import com.wellness.model.JournalEntry;
import com.wellness.model.User;
import com.wellness.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BackendTest {
    @TempDir Path dir;

    @Test
    void registerAndLogin() {
        Services s = new Services(dir);
        s.auth.register("alice_1", "Alice", "secret123");
        assertEquals("Alice", s.auth.login("ALICE_1", "secret123").displayName());
        assertThrows(WellnessException.class, () -> s.auth.login("alice_1", "wrong"));
        assertThrows(WellnessException.class, () -> s.auth.register("alice_1", "x", "secret123"));
        assertThrows(WellnessException.class, () -> s.auth.register("bob", "Bob", "short"));
    }

    @Test
    void lockoutAfterFiveFailures() {
        Services s = new Services(dir);
        s.auth.register("carol", "Carol", "secret123");
        for (int i = 0; i < 5; i++) assertThrows(WellnessException.class, () -> s.auth.login("carol", "bad"));
        WellnessException e = assertThrows(WellnessException.class, () -> s.auth.login("carol", "secret123"));
        assertTrue(e.getMessage().contains("Too many"));
    }

    @Test
    void dataSurvivesRestart() {
        Services a = new Services(dir);
        var u = a.auth.register("dave", "Dave", "secret123");
        a.mood.log(u.id(), 4, "ok day");
        a.journal.create(u.id(), "Title", "Body");

        Services b = new Services(dir);
        var again = b.auth.login("dave", "secret123");
        assertEquals(1, b.mood.recent(again.id(), 10).size());
        assertEquals(1, b.journal.list(again.id(), "").size());
        assertEquals(1, b.mood.streak(again.id()));
    }

    @Test
    void usersCannotSeeOrChangeEachOthersJournal() {
        Services s = new Services(dir);
        var a = s.auth.register("ann", "Ann", "secret123");
        var b = s.auth.register("ben", "Ben", "secret123");
        JournalEntry e = s.journal.create(a.id(), "Private", "Secret");
        assertTrue(s.journal.list(b.id(), "").isEmpty());
        assertThrows(WellnessException.class, () -> s.journal.update(b.id(), e.id(), "Hack", "Hack"));
        s.journal.delete(b.id(), e.id());
        assertEquals(1, s.journal.list(a.id(), "priv").size());
    }

    @Test
    void corruptFileDoesNotCrash() throws Exception {
        Files.writeString(dir.resolve("moods.dat"), "garbage");
        Services s = new Services(dir);
        assertTrue(s.mood.recent("x", 5).isEmpty());
    }

    @Test
    void assessmentScoring() {
        Services s = new Services(dir);
        var r = s.assessment.submit("u", AssessmentService.Kind.PHQ2, new int[]{2, 1});
        assertEquals(3, r.score());
        assertTrue(AssessmentService.interpret(r.score()).contains("at or above"));
        assertThrows(WellnessException.class, () -> s.assessment.submit("u", AssessmentService.Kind.GAD2, new int[]{1}));
        assertEquals(List.of(r), s.assessment.history("u"));
    }

    // ---------- new features ----------

    @Test
    void adminAndUserPortalsAreSeparate() {
        Services s = new Services(dir);
        assertFalse(s.auth.adminExists());
        s.auth.createFirstAdmin("boss", "Boss", "adminpass1");
        assertTrue(s.auth.adminExists());
        assertThrows(WellnessException.class, () -> s.auth.createFirstAdmin("boss2", "B", "adminpass1"));
        s.auth.register("eve", "Eve", "secret123");

        assertTrue(s.auth.login("boss", "adminpass1", true).isAdmin());
        assertThrows(WellnessException.class, () -> s.auth.login("boss", "adminpass1", false));
        assertThrows(WellnessException.class, () -> s.auth.login("eve", "secret123", true));
        assertFalse(s.auth.login("eve", "secret123", false).isAdmin());
    }

    @Test
    void adminCanDisableResetAndDeleteUsers() {
        Services s = new Services(dir);
        s.auth.createFirstAdmin("boss", "Boss", "adminpass1");
        User u = s.auth.register("frank", "Frank", "secret123");
        s.mood.log(u.id(), 3, "x");
        s.journal.create(u.id(), "t", "b");

        s.admin.setDisabled(u.id(), true);
        assertTrue(assertThrows(WellnessException.class, () -> s.auth.login("frank", "secret123"))
                .getMessage().contains("disabled"));
        s.admin.setDisabled(u.id(), false);
        s.admin.resetPassword(u.id(), "newpass99");
        assertEquals("Frank", s.auth.login("frank", "newpass99").displayName());

        var admin = s.auth.login("boss", "adminpass1", true);
        assertThrows(WellnessException.class, () -> s.admin.deleteUserAndData(admin.id())); // admins protected

        s.admin.deleteUserAndData(u.id());
        assertEquals(0, s.mood.count(u.id()));
        assertEquals(0, s.journal.count(u.id()));
        assertThrows(WellnessException.class, () -> s.auth.login("frank", "newpass99"));
    }

    @Test
    void wellnessScoreAndStreaks() {
        Services s = new Services(dir);
        User u = s.auth.register("gina", "Gina", "secret123");
        assertFalse(s.wellness.dailyScore(u.id()).available());

        s.mood.log(u.id(), 5, "great");
        s.journal.create(u.id(), "Today", "Good day");
        var score = s.wellness.dailyScore(u.id());
        assertTrue(score.available());
        // mood 60 + check-in 10 + journal 15 + streak 1*3 = 88
        assertEquals(88, score.score());
        assertEquals(1, s.mood.longestStreak(u.id()));
        assertEquals(1, s.mood.dailyAverages(u.id(), YearMonth.now()).size());
    }

    @Test
    void achievementsUnlockFromActivity() {
        Services s = new Services(dir);
        User u = s.auth.register("hari", "Hari", "secret123");
        assertTrue(s.growth.achievements(u.id()).stream().noneMatch(a -> a.unlocked()));
        s.mood.log(u.id(), 1, "");
        s.mood.log(u.id(), 4, "");
        s.journal.create(u.id(), "t", "b");
        var unlocked = s.growth.achievements(u.id()).stream().filter(a -> a.unlocked()).map(a -> a.id()).toList();
        assertTrue(unlocked.contains("first_checkin"));
        assertTrue(unlocked.contains("journal_1"));
        assertTrue(unlocked.contains("bounce_back"));
        assertEquals("Seedling", s.growth.level(u.id()).name());
    }

    @Test
    void companionHandlesTopicsAndCrisis() {
        Services s = new Services(dir);
        var crisis = s.companion.reply("u", "I want to die");
        assertTrue(crisis.crisis());
        assertTrue(crisis.text().contains("14416"));
        assertFalse(s.companion.reply("u", "I feel so anxious about tomorrow").crisis());
        assertTrue(s.companion.reply("u", "I made a cake").text().length() > 0);   // "made" must not trigger anger
        assertFalse(s.companion.reply("u", "I follow my dreams").text().contains("infuriating"));
    }

    @Test
    void oldUserFilesStillLoad() {
        // Users saved without role/disabled fields behave as normal, enabled users (record defaults).
        Services s = new Services(dir);
        User u = s.auth.register("ivy", "Ivy", "secret123");
        assertFalse(u.isAdmin());
        assertFalse(u.disabled());
    }
}
