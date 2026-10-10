package com.wellness.ui;

import com.wellness.model.User;
import com.wellness.service.Services;

import java.util.LinkedHashMap;
import java.util.function.Supplier;

/** The administrator portal (separate login, separate screens). */
public class AdminMainView extends Shell {
    public AdminMainView(Services s, User admin, Runnable onLogout) {
        super("MindEase Admin", "Admin: " + admin.displayName(), "admin-side", pages(s), onLogout);
    }

    private static LinkedHashMap<String, Supplier<View>> pages(Services s) {
        LinkedHashMap<String, Supplier<View>> p = new LinkedHashMap<>();
        p.put("📊  Overview", () -> new AdminOverviewView(s));
        p.put("👥  Users", () -> new AdminUsersView(s));
        return p;
    }
}
