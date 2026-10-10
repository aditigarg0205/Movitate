package com.wellness.storage;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Small, dependable persistent list.
 *  - Thread-safe (synchronized).
 *  - Every change is written to a temp file, fsync'ed, then atomically moved over the real file,
 *    so a crash can never leave a half-written data file.
 *  - If a change cannot be saved, the in-memory state is rolled back.
 *  - Deserialization is restricted to this app's model classes + JDK collections/time.
 *  - A corrupt file is quarantined (renamed) instead of crashing the app.
 */
public final class FileStore<T extends Serializable> {
    private static final String FILTER =
            "com.wellness.model.*;java.util.*;java.time.*;java.lang.*;!*";

    private final Path file;
    private final List<T> items = new ArrayList<>();

    public FileStore(Path file) {
        this.file = file.toAbsolutePath();
        try {
            Files.createDirectories(this.file.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create data folder", e);
        }
        load();
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!Files.exists(file)) return;
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            in.setObjectInputFilter(ObjectInputFilter.Config.createFilter(FILTER));
            items.addAll((List<T>) in.readObject());
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            quarantine();
        }
    }

    private void quarantine() {
        try {
            Files.move(file, file.resolveSibling(file.getFileName() + ".corrupt-" + System.currentTimeMillis()),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // nothing more we can do; start with an empty store
        }
        items.clear();
    }

    private void persist() {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            try (FileOutputStream fos = new FileOutputStream(tmp.toFile());
                 ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(fos))) {
                out.writeObject(new ArrayList<>(items));
                out.flush();
                fos.getFD().sync();
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save data", e);
        }
    }

    private void mutate(Consumer<List<T>> change) {
        List<T> backup = new ArrayList<>(items);
        try {
            change.accept(items);
            persist();
        } catch (RuntimeException e) {
            items.clear();
            items.addAll(backup);
            throw e;
        }
    }

    public synchronized List<T> all() {
        return new ArrayList<>(items);
    }

    public synchronized void add(T item) {
        mutate(l -> l.add(item));
    }

    public synchronized boolean removeIf(Predicate<T> match) {
        boolean[] removed = {false};
        mutate(l -> removed[0] = l.removeIf(match));
        return removed[0];
    }

    /** Replaces the first element that matches. */
    public synchronized boolean replace(Predicate<T> match, UnaryOperator<T> fn) {
        boolean[] done = {false};
        mutate(l -> {
            for (int i = 0; i < l.size(); i++) {
                if (match.test(l.get(i))) {
                    l.set(i, fn.apply(l.get(i)));
                    done[0] = true;
                    return;
                }
            }
        });
        return done[0];
    }
}
