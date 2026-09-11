package com.securex.service;

import com.securex.dto.AuthResponse;
import com.securex.dto.LoginRequest;
import com.securex.dto.RegisterRequest;
import com.securex.dto.UserDto;
import com.securex.entity.Role;
import com.securex.entity.User;
import com.securex.exception.ResourceNotFoundException;
import com.securex.repository.UserRepository;
import com.securex.security.JwtTokenProvider;
import com.securex.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email address is already in use");
        }

        User user = User.builder()
                .uuid(java.util.UUID.randomUUID().toString())
                .displayName(request.getDisplayName() != null ? request.getDisplayName().trim() : "")
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        user = userRepository.save(user);

        auditLogService.logEvent(user.getId(), "USER_REGISTERED", "USER", user.getUuid(),
                "Registered new user account: " + user.getEmail(), httpRequest);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword()));

        String token = tokenProvider.generateToken(authentication);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(mapToUserDto(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword()));

            String token = tokenProvider.generateToken(authentication);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            auditLogService.logEvent(user.getId(), "LOGIN_SUCCESS", "USER", user.getUuid(),
                    "User logged in successfully", httpRequest);

            return AuthResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .user(mapToUserDto(user))
                    .build();
        } catch (Exception ex) {
            auditLogService.logEvent(null, "LOGIN_FAILED", "USER", email,
                    "Failed login attempt for email: " + email, httpRequest);
            throw ex;
        }
    }

    @Override
    public UserDto getCurrentUserDto() {
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ResourceNotFoundException("No active user session");
        }
        return mapToUserDto(currentUser);
    }

    private UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .uuid(user.getUuid())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
