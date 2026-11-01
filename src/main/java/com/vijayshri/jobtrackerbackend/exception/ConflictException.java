package com.vijayshri.jobtrackerbackend.exception;

/** Thrown when a request clashes with existing data (duplicate, still in use). Mapped to HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
