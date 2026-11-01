package com.vijayshri.jobtrackerbackend.service;

import com.vijayshri.jobtrackerbackend.dto.AuthResponse;
import com.vijayshri.jobtrackerbackend.dto.LoginRequest;
import com.vijayshri.jobtrackerbackend.dto.RegisterRequest;
import com.vijayshri.jobtrackerbackend.dto.UserResponse;
import com.vijayshri.jobtrackerbackend.exception.ConflictException;
import com.vijayshri.jobtrackerbackend.exception.InvalidCredentialsException;
import com.vijayshri.jobtrackerbackend.model.User;
import com.vijayshri.jobtrackerbackend.repository.CompanyRepository;
import com.vijayshri.jobtrackerbackend.repository.JobApplicationRepository;
import com.vijayshri.jobtrackerbackend.repository.UserRepository;
import com.vijayshri.jobtrackerbackend.security.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JobApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        boolean firstAccount = !userRepository.existsByPasswordHashIsNotNull();

        // A user row may already exist from before login was added (no password yet): claim it.
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user != null && user.getPasswordHash() != null) {
            throw new ConflictException("An account with this email already exists");
        }
        if (user == null) {
            user = new User();
            user.setEmail(email);
        }
        user.setName(request.name().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user = userRepository.saveAndFlush(user);

        if (firstAccount) {
            int applications = applicationRepository.assignUnownedTo(user);
            int companies = companyRepository.assignUnownedTo(user);
            if (applications + companies > 0) {
                log.info("First account {}: took over {} existing applications and {} companies",
                        email, applications, companies);
            }
        }

        return new AuthResponse(tokenService.createToken(user), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .filter(u -> u.getPasswordHash() != null
                        && passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        return new AuthResponse(tokenService.createToken(user), UserResponse.from(user));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
