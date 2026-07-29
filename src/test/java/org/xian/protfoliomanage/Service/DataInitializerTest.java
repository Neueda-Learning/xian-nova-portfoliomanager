package org.xian.protfoliomanage.Service;

import org.junit.jupiter.api.Test;
import org.xian.protfoliomanage.Repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataInitializerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final DataInitializer initializer = new DataInitializer(userRepository, passwordEncoder);

    @Test
    void runCreatesDefaultUserWhenMissing() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("admin123")).thenReturn("encoded");

        initializer.run();

        verify(userRepository).save("admin", "encoded");
    }

    @Test
    void runMigratesPlainTextPassword() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);
        when(userRepository.findPasswordByUsername("admin")).thenReturn(Optional.of("admin123"));
        when(passwordEncoder.encode("admin123")).thenReturn("encoded");

        initializer.run();

        verify(userRepository).updatePasswordByUsername("admin", "encoded");
    }

    @Test
    void runSkipsUpdateWhenPasswordAlreadyEncoded() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);
        when(userRepository.findPasswordByUsername("admin")).thenReturn(Optional.of("$2a$10$hash"));

        initializer.run();

        verify(userRepository, never()).updatePasswordByUsername("admin", "$2a$10$hash");
        verify(userRepository, never()).save("admin", "$2a$10$hash");
    }
}

