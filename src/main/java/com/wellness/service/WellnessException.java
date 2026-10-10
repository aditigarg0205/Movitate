package com.wellness.service;

/** Error with a message that is safe to show to the user. */
public class WellnessException extends RuntimeException {
    public WellnessException(String message) {
        super(message);
    }
}
