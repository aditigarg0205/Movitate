package com.wellness.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * role: "USER" or "ADMIN" (null in old data files = USER).
 * disabled: false by default, so accounts saved by version 1 keep working.
 */
public record User(String id, String username, String displayName, String salt, String hash,
                   LocalDateTime createdAt, String role, boolean disabled) implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String ADMIN = "ADMIN";
    public static final String USER = "USER";

    public boolean isAdmin() {
        return ADMIN.equals(role);
    }

    public User withDisabled(boolean value) {
        return new User(id, username, displayName, salt, hash, createdAt, role, value);
    }

    public User withPassword(String newSalt, String newHash) {
        return new User(id, username, displayName, newSalt, newHash, createdAt, role, disabled);
    }
}
