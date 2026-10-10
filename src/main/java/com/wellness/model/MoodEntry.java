package com.wellness.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/** mood: 1 (very low) .. 5 (great) */
public record MoodEntry(String id, String userId, int mood, String note,
                        LocalDateTime at) implements Serializable {
    private static final long serialVersionUID = 1L;
}
