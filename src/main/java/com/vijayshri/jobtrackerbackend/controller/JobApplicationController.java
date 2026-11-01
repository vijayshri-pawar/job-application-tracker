package com.vijayshri.jobtrackerbackend.controller;

import com.vijayshri.jobtrackerbackend.dto.ApplicationStats;
import com.vijayshri.jobtrackerbackend.dto.DocumentResponse;
import com.vijayshri.jobtrackerbackend.dto.InterviewRoundResponse;
import com.vijayshri.jobtrackerbackend.dto.JobApplicationRequest;
import com.vijayshri.jobtrackerbackend.dto.JobApplicationResponse;
import com.vijayshri.jobtrackerbackend.security.CurrentUser;
import com.vijayshri.jobtrackerbackend.service.JobApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationService service;

    /** GET /api/applications?status=Interview&search=google&sort=newest (all params optional) */
    @GetMapping
    public List<JobApplicationResponse> getAll(@AuthenticationPrincipal Jwt jwt,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String search,
                                               @RequestParam(defaultValue = "newest") String sort) {
        return service.getAll(CurrentUser.id(jwt), status, search, sort);
    }

    @GetMapping("/stats")
    public ApplicationStats getStats(@AuthenticationPrincipal Jwt jwt) {
        return service.getStats(CurrentUser.id(jwt));
    }

    @GetMapping("/{id}")
    public JobApplicationResponse getById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.getById(CurrentUser.id(jwt), id);
    }

    @GetMapping("/{id}/interview-rounds")
    public List<InterviewRoundResponse> getInterviewRounds(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.getInterviewRounds(CurrentUser.id(jwt), id);
    }

    @GetMapping("/{id}/documents")
    public List<DocumentResponse> getDocuments(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.getDocuments(CurrentUser.id(jwt), id);
    }

    @PostMapping
    public ResponseEntity<JobApplicationResponse> add(@AuthenticationPrincipal Jwt jwt,
                                                      @Valid @RequestBody JobApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.add(CurrentUser.id(jwt), request));
    }

    @PutMapping("/{id}")
    public JobApplicationResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                         @Valid @RequestBody JobApplicationRequest request) {
        return service.update(CurrentUser.id(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(CurrentUser.id(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
