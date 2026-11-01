package com.vijayshri.jobtrackerbackend.dto;

import com.vijayshri.jobtrackerbackend.model.User;

public record UserResponse(Long id, String name, String email) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
