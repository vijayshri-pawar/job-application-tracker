package com.vijayshri.jobtrackerbackend.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * Stores {@link Status} in the database as its enum name (APPLIED, INTERVIEW, ...).
 * Reading is lenient so rows saved by the older version of this app ("Applied", "Interview", ...)
 * still load correctly.
 */
@Slf4j
@Converter
public class StatusConverter implements AttributeConverter<Status, String> {

    @Override
    public String convertToDatabaseColumn(Status status) {
        return status == null ? null : status.name();
    }

    @Override
    public Status convertToEntityAttribute(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) {
            return null;
        }
        try {
            return Status.from(dbValue);
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown status '{}' found in database, treating it as APPLIED", dbValue);
            return Status.APPLIED;
        }
    }
}
