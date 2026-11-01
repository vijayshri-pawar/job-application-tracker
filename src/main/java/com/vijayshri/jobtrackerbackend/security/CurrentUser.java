package com.vijayshri.jobtrackerbackend.security;

import org.springframework.security.oauth2.jwt.Jwt;

/** Reads the logged-in user's id from the JWT ("sub" claim holds the user id). */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
