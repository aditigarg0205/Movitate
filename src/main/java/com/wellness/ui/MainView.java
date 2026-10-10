package com.wellness.ui;

import com.wellness.model.User;
import com.wellness.service.Services;

import java.util.LinkedHashMap;
import java.util.function.Supplier;

/** The user portal. */
public class MainView extends Shell {
    public MainView(Services s, User user, Runnable onLogout) {
        super("MindEase", "Hi, " + user.displayName(), "user-side", pages(s, user), onLogout);
    }

    private static LinkedHashMap<String, Supplier<View>> pages(Services s, User u) {
        LinkedHashMap<String, Supplier<View>> p = new LinkedHashMap<>();
        p.put("🏠  Dashboard", () -> new DashboardView(s, u));
        p.put("📝  Mood check-in", () -> new MoodView(s, u));
        p.put("📅  Mood calendar", () -> new CalendarView(s, u));
        p.put("📈  Mood journey", () -> new JourneyView(s, u));
        p.put("📖  Journal", () -> new JournalView(s, u));
        p.put("💬  AI companion", () -> new CompanionView(s, u));
        p.put("🌟  My growth", () -> new GrowthView(s, u));
        p.put("🌬  Breathe", BreathingView::new);
        p.put("🧠  Self-check", () -> new AssessmentView(s, u));
        p.put("☎  Get help", ResourcesView::new);
        return p;
    }
}
