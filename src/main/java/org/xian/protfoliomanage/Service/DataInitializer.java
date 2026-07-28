package org.xian.protfoliomanage.Service;

import org.xian.protfoliomanage.Repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername(DEFAULT_USERNAME)) {
            userRepository.save(DEFAULT_USERNAME, passwordEncoder.encode(DEFAULT_PASSWORD));
            return;
        }

        userRepository.findPasswordByUsername(DEFAULT_USERNAME).ifPresent(storedPassword -> {
            if (DEFAULT_PASSWORD.equals(storedPassword)) {
                userRepository.updatePasswordByUsername(DEFAULT_USERNAME, passwordEncoder.encode(DEFAULT_PASSWORD));
            }
        });
    }
}

