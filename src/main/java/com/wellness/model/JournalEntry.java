package com.wellness.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public record JournalEntry(String id, String userId, String title, String body,
                           LocalDateTime createdAt, LocalDateTime updatedAt) implements Serializable {
    private static final long serialVersionUID = 1L;
}
