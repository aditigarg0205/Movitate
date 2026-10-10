package com.wellness.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/** kind is the name of AssessmentService.Kind (stored as text for stable serialization). */
public record AssessmentResult(String id, String userId, String kind, int score,
                               LocalDateTime at) implements Serializable {
    private static final long serialVersionUID = 1L;
}
