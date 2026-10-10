package com.wellness.service;

import com.wellness.model.JournalEntry;
import com.wellness.storage.FileStore;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class JournalService {
    private final FileStore<JournalEntry> store;

    public JournalService(FileStore<JournalEntry> store) {
        this.store = store;
    }

    public JournalEntry create(String userId, String title, String body) {
        validate(title, body);
        LocalDateTime now = LocalDateTime.now();
        JournalEntry e = new JournalEntry(UUID.randomUUID().toString(), userId, title.trim(), body.trim(), now, now);
        store.add(e);
        return e;
    }

    public JournalEntry update(String userId, String id, String title, String body) {
        validate(title, body);
        JournalEntry[] result = new JournalEntry[1];
        boolean ok = store.replace(e -> e.id().equals(id) && e.userId().equals(userId), old -> {
            result[0] = new JournalEntry(old.id(), old.userId(), title.trim(), body.trim(),
                    old.createdAt(), LocalDateTime.now());
            return result[0];
        });
        if (!ok) throw new WellnessException("That entry no longer exists.");
        return result[0];
    }

    public void delete(String userId, String id) {
        store.removeIf(e -> e.id().equals(id) && e.userId().equals(userId));
    }

    /** Newest first; optional case-insensitive search in title and text. */
    public List<JournalEntry> list(String userId, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return store.all().stream()
                .filter(e -> e.userId().equals(userId))
                .filter(e -> q.isEmpty() || e.title().toLowerCase(Locale.ROOT).contains(q)
                        || e.body().toLowerCase(Locale.ROOT).contains(q))
                .sorted(Comparator.comparing(JournalEntry::updatedAt).reversed())
                .collect(Collectors.toList());
    }

    public int count(String userId) {
        return (int) store.all().stream().filter(e -> e.userId().equals(userId)).count();
    }

    public int countSince(String userId, LocalDateTime since) {
        return (int) store.all().stream()
                .filter(e -> e.userId().equals(userId) && !e.createdAt().isBefore(since)).count();
    }

    public void deleteAllFor(String userId) {
        store.removeIf(e -> e.userId().equals(userId));
    }

    private static void validate(String title, String body) {
        if (title == null || title.isBlank()) throw new WellnessException("Please give your entry a title.");
        if (title.length() > 80) throw new WellnessException("Title is too long (max 80 characters).");
        if (body == null || body.isBlank()) throw new WellnessException("Write something in the entry first.");
        if (body.length() > 10_000) throw new WellnessException("Entry is too long (max 10,000 characters).");
    }
}
