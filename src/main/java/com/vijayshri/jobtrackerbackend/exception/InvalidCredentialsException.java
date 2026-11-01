package com.vijayshri.jobtrackerbackend.exception;

/** Wrong email or password at login. Mapped to HTTP 401. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
