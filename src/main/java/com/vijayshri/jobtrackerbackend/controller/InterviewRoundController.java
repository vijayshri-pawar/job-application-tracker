package com.vijayshri.jobtrackerbackend.controller;

import com.vijayshri.jobtrackerbackend.dto.InterviewRoundRequest;
import com.vijayshri.jobtrackerbackend.dto.InterviewRoundResponse;
import com.vijayshri.jobtrackerbackend.security.CurrentUser;
import com.vijayshri.jobtrackerbackend.service.InterviewRoundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interview-rounds")
@RequiredArgsConstructor
public class InterviewRoundController {

    private final InterviewRoundService service;

    @GetMapping
    public List<InterviewRoundResponse> getAll(@AuthenticationPrincipal Jwt jwt) {
        return service.getAll(CurrentUser.id(jwt));
    }

    @GetMapping("/{id}")
    public InterviewRoundResponse getById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.getById(CurrentUser.id(jwt), id);
    }

    @PostMapping
    public ResponseEntity<InterviewRoundResponse> add(@AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody InterviewRoundRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.add(CurrentUser.id(jwt), request));
    }

    @PutMapping("/{id}")
    public InterviewRoundResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                               @Valid @RequestBody InterviewRoundRequest request) {
        return service.update(CurrentUser.id(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(CurrentUser.id(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
