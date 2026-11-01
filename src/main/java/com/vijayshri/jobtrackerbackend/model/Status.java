package com.vijayshri.jobtrackerbackend.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Stage of a job application.
 * Sent to and from the frontend as a readable label ("Applied", "Interview", ...).
 * Input is case-insensitive, so "APPLIED" and "applied" are also accepted.
 */
public enum Status {
    APPLIED("Applied"),
    INTERVIEW("Interview"),
    OFFER("Offer"),
    REJECTED("Rejected");

    private final String label;

    Status(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static Status from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(trimmed) || s.label.equalsIgnoreCase(trimmed))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid status '" + value + "'. Allowed values: Applied, Interview, Offer, Rejected"));
    }
}
