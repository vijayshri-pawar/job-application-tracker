package com.vijayshri.jobtrackerbackend.controller;

import com.vijayshri.jobtrackerbackend.dto.DocumentRequest;
import com.vijayshri.jobtrackerbackend.dto.DocumentResponse;
import com.vijayshri.jobtrackerbackend.security.CurrentUser;
import com.vijayshri.jobtrackerbackend.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService service;

    @GetMapping
    public List<DocumentResponse> getAll(@AuthenticationPrincipal Jwt jwt) {
        return service.getAll(CurrentUser.id(jwt));
    }

    @GetMapping("/{id}")
    public DocumentResponse getById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.getById(CurrentUser.id(jwt), id);
    }

    @PostMapping
    public ResponseEntity<DocumentResponse> add(@AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody DocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.add(CurrentUser.id(jwt), request));
    }

    @PutMapping("/{id}")
    public DocumentResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                               @Valid @RequestBody DocumentRequest request) {
        return service.update(CurrentUser.id(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(CurrentUser.id(jwt), id);
        return ResponseEntity.noContent().build();
    }
}
