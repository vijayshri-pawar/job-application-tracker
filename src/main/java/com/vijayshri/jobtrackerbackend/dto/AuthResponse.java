package com.vijayshri.jobtrackerbackend.dto;

/** Returned after login or registration. The client sends the token as "Authorization: Bearer <token>". */
public record AuthResponse(String token, UserResponse user) {
}
