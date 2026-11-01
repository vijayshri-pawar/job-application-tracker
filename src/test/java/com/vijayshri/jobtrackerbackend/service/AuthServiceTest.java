package com.vijayshri.jobtrackerbackend.service;

import com.vijayshri.jobtrackerbackend.dto.AuthResponse;
import com.vijayshri.jobtrackerbackend.dto.LoginRequest;
import com.vijayshri.jobtrackerbackend.dto.RegisterRequest;
import com.vijayshri.jobtrackerbackend.exception.ConflictException;
import com.vijayshri.jobtrackerbackend.exception.InvalidCredentialsException;
import com.vijayshri.jobtrackerbackend.model.User;
import com.vijayshri.jobtrackerbackend.repository.CompanyRepository;
import com.vijayshri.jobtrackerbackend.repository.JobApplicationRepository;
import com.vijayshri.jobtrackerbackend.repository.UserRepository;
import com.vijayshri.jobtrackerbackend.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JobApplicationRepository applicationRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private TokenService tokenService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, applicationRepository, companyRepository,
                passwordEncoder, tokenService);
    }

    @Test
    void register_hashesPasswordAndReturnsToken() {
        when(userRepository.existsByPasswordHashIsNotNull()).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase("asha@example.com")).thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(tokenService.createToken(any(User.class))).thenReturn("token-123");

        AuthResponse response = authService.register(
                new RegisterRequest("Asha", " Asha@Example.com ", "secret-pass"));

        assertThat(response.token()).isEqualTo("token-123");
        assertThat(response.user().email()).isEqualTo("asha@example.com");
        verify(userRepository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(u ->
                !u.getPasswordHash().equals("secret-pass")
                        && passwordEncoder.matches("secret-pass", u.getPasswordHash())));
        verify(applicationRepository, never()).assignUnownedTo(any());
    }

    @Test
    void register_firstAccount_takesOverExistingData() {
        when(userRepository.existsByPasswordHashIsNotNull()).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("first@example.com")).thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register(new RegisterRequest("First", "first@example.com", "secret-pass"));

        verify(applicationRepository).assignUnownedTo(any(User.class));
        verify(companyRepository).assignUnownedTo(any(User.class));
    }

    @Test
    void register_withEmailOfExistingAccount_throwsConflict() {
        User existing = new User();
        existing.setEmail("taken@example.com");
        existing.setPasswordHash(passwordEncoder.encode("whatever1"));
        when(userRepository.existsByPasswordHashIsNotNull()).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase("taken@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("X", "taken@example.com", "secret-pass")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void login_withCorrectPassword_returnsToken() {
        User user = new User();
        user.setId(3L);
        user.setName("Asha");
        user.setEmail("asha@example.com");
        user.setPasswordHash(passwordEncoder.encode("secret-pass"));
        when(userRepository.findByEmailIgnoreCase("asha@example.com")).thenReturn(Optional.of(user));
        when(tokenService.createToken(user)).thenReturn("token-abc");

        AuthResponse response = authService.login(new LoginRequest("ASHA@example.com", "secret-pass"));

        assertThat(response.token()).isEqualTo("token-abc");
        assertThat(response.user().name()).isEqualTo("Asha");
    }

    @Test
    void login_withWrongPassword_throwsInvalidCredentials() {
        User user = new User();
        user.setEmail("asha@example.com");
        user.setPasswordHash(passwordEncoder.encode("secret-pass"));
        when(userRepository.findByEmailIgnoreCase("asha@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("asha@example.com", "wrong-pass")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void login_withUnknownEmail_throwsInvalidCredentials() {
        when(userRepository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "secret-pass")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
