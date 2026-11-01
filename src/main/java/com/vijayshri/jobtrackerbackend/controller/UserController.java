package com.vijayshri.jobtrackerbackend.controller;

import com.vijayshri.jobtrackerbackend.dto.ChangePasswordRequest;
import com.vijayshri.jobtrackerbackend.dto.UserRequest;
import com.vijayshri.jobtrackerbackend.dto.UserResponse;
import com.vijayshri.jobtrackerbackend.security.CurrentUser;
import com.vijayshri.jobtrackerbackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** The logged-in user's own profile. Users can never see or change other users. */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @GetMapping
    public UserResponse getMe(@AuthenticationPrincipal Jwt jwt) {
        return service.getMe(CurrentUser.id(jwt));
    }

    @PutMapping
    public UserResponse updateMe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UserRequest request) {
        return service.updateMe(CurrentUser.id(jwt), request);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword(CurrentUser.id(jwt), request);
        return ResponseEntity.noContent().build();
    }
}
