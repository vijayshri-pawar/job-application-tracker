package com.vijayshri.jobtrackerbackend.exception;

/** The request is understood but not allowed (e.g. wrong current password). Mapped to HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
