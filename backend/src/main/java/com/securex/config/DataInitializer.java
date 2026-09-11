package com.securex.config;

import com.securex.entity.Role;
import com.securex.entity.User;
import com.securex.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@securex.local")) {
            User admin = User.builder()
                    .uuid(java.util.UUID.randomUUID().toString())
                    .email("admin@securex.local")
                    .passwordHash(passwordEncoder.encode("AdminPassword123!"))
                    .displayName("System Administrator")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            userRepository.save(admin);
        }

        if (!userRepository.existsByEmail("alice@securex.local")) {
            User alice = User.builder()
                    .uuid(java.util.UUID.randomUUID().toString())
                    .email("alice@securex.local")
                    .passwordHash(passwordEncoder.encode("AlicePassword123!"))
                    .displayName("Alice Smith")
                    .role(Role.USER)
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            userRepository.save(alice);
        }

        if (!userRepository.existsByEmail("bob@securex.local")) {
            User bob = User.builder()
                    .uuid(java.util.UUID.randomUUID().toString())
                    .email("bob@securex.local")
                    .passwordHash(passwordEncoder.encode("BobPassword123!"))
                    .displayName("Bob Jones")
                    .role(Role.USER)
                    .enabled(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            userRepository.save(bob);
        }
    }
}
