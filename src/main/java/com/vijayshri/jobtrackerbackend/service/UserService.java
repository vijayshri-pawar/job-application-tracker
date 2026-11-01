package com.vijayshri.jobtrackerbackend.service;

import com.vijayshri.jobtrackerbackend.dto.ChangePasswordRequest;
import com.vijayshri.jobtrackerbackend.dto.UserRequest;
import com.vijayshri.jobtrackerbackend.dto.UserResponse;
import com.vijayshri.jobtrackerbackend.exception.BadRequestException;
import com.vijayshri.jobtrackerbackend.exception.ConflictException;
import com.vijayshri.jobtrackerbackend.exception.ResourceNotFoundException;
import com.vijayshri.jobtrackerbackend.model.User;
import com.vijayshri.jobtrackerbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Profile actions for the logged-in user. */
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    public UserResponse updateMe(Long userId, UserRequest request) {
        User user = findUser(userId);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, userId)) {
            throw new ConflictException("An account with this email already exists");
        }
        user.setName(request.name().trim());
        user.setEmail(email);
        return UserResponse.from(userRepository.save(user));
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
