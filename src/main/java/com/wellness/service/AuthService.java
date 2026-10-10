package com.wellness.service;

import com.wellness.model.User;
import com.wellness.storage.FileStore;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AuthService {
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK = Duration.ofSeconds(60);
    private static final String DUMMY_SALT = PasswordHasher.newSalt();

    private static final class Attempts {
        int failures;
        Instant lockedUntil;
    }

    private final FileStore<User> store;
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public AuthService(FileStore<User> store) {
        this.store = store;
    }

    // ---------------- registration ----------------

    public User register(String username, String displayName, String password) {
        return create(username, displayName, password, User.USER);
    }

    public boolean adminExists() {
        return store.all().stream().anyMatch(User::isAdmin);
    }

    /** Only works while no admin exists (first-run setup of the admin portal). */
    public synchronized User createFirstAdmin(String username, String displayName, String password) {
        if (adminExists()) throw new WellnessException("An administrator account already exists.");
        return create(username, displayName, password, User.ADMIN);
    }

    private User create(String username, String displayName, String password, String role) {
        username = username == null ? "" : username.trim();
        displayName = displayName == null ? "" : displayName.trim();
        if (!USERNAME.matcher(username).matches())
            throw new WellnessException("Username must be 3-20 characters: letters, numbers or underscore.");
        if (displayName.isEmpty()) displayName = username;
        if (displayName.length() > 40)
            throw new WellnessException("Display name is too long (max 40 characters).");
        validatePassword(password);

        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash(password, salt);
        User user = new User(UUID.randomUUID().toString(), username, displayName, salt, hash,
                LocalDateTime.now(), role, false);

        synchronized (this) {
            if (find(username).isPresent())
                throw new WellnessException("That username is already taken.");
            store.add(user);
        }
        return user;
    }

    // ---------------- login ----------------

    public User login(String username, String password) {
        return login(username, password, false);
    }

    /** adminPortal=true only accepts administrators; false only accepts normal users. */
    public User login(String username, String password, boolean adminPortal) {
        String key = (username == null ? "" : username.trim()).toLowerCase(Locale.ROOT);
        Attempts a = attempts.computeIfAbsent(key, k -> new Attempts());

        synchronized (a) {
            if (a.lockedUntil != null && Instant.now().isBefore(a.lockedUntil)) {
                long secs = Duration.between(Instant.now(), a.lockedUntil).toSeconds() + 1;
                throw new WellnessException("Too many failed attempts. Try again in " + secs + " seconds.");
            }
        }

        String pw = password == null ? "" : password;
        Optional<User> user = find(key);
        boolean ok;
        if (user.isPresent()) {
            ok = PasswordHasher.verify(pw, user.get().salt(), user.get().hash())
                    && user.get().isAdmin() == adminPortal;
        } else {
            PasswordHasher.hash(pw, DUMMY_SALT); // keep timing similar for unknown users
            ok = false;
        }

        synchronized (a) {
            if (ok) {
                a.failures = 0;
                a.lockedUntil = null;
            } else if (++a.failures >= MAX_ATTEMPTS) {
                a.failures = 0;
                a.lockedUntil = Instant.now().plus(LOCK);
            }
        }
        if (!ok) throw new WellnessException("Invalid username or password.");
        if (user.get().disabled())
            throw new WellnessException("This account has been disabled. Please contact an administrator.");
        return user.get();
    }

    // ---------------- admin operations ----------------

    /** All normal (non-admin) users, newest first. */
    public List<User> users() {
        return store.all().stream().filter(u -> !u.isAdmin())
                .sorted(Comparator.comparing(User::createdAt).reversed())
                .collect(Collectors.toList());
    }

    public void setDisabled(String userId, boolean disabled) {
        requireNormalUser(userId);
        store.replace(u -> u.id().equals(userId), u -> u.withDisabled(disabled));
    }

    public void resetPassword(String userId, String newPassword) {
        requireNormalUser(userId);
        validatePassword(newPassword);
        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash(newPassword, salt);
        store.replace(u -> u.id().equals(userId), u -> u.withPassword(salt, hash));
    }

    public void deleteUser(String userId) {
        requireNormalUser(userId);
        store.removeIf(u -> u.id().equals(userId));
    }

    private void requireNormalUser(String userId) {
        boolean ok = store.all().stream().anyMatch(u -> u.id().equals(userId) && !u.isAdmin());
        if (!ok) throw new WellnessException("User not found (administrator accounts cannot be changed here).");
    }

    // ---------------- helpers ----------------

    private Optional<User> find(String username) {
        return store.all().stream()
                .filter(u -> u.username().equalsIgnoreCase(username))
                .findFirst();
    }

    private static void validatePassword(String p) {
        if (p == null || p.length() < 8)
            throw new WellnessException("Password must be at least 8 characters.");
        boolean letter = p.chars().anyMatch(Character::isLetter);
        boolean digit = p.chars().anyMatch(Character::isDigit);
        if (!letter || !digit)
            throw new WellnessException("Password must contain at least one letter and one number.");
    }
}
