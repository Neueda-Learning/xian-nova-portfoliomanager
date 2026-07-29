package org.xian.protfoliomanage.config;

import org.junit.jupiter.api.Test;
import org.xian.protfoliomanage.Model.User;
import org.xian.protfoliomanage.Repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    @Test
    void passwordEncoderCreatesBCryptHash() {
        SecurityConfig config = new SecurityConfig();

        PasswordEncoder encoder = config.passwordEncoder();
        String encoded = encoder.encode("secret");

        assertNotNull(encoded);
        assertTrue(encoder.matches("secret", encoded));
    }

    @Test
    void userDetailsServiceLoadsExistingUser() {
        UserRepository userRepository = mock(UserRepository.class);
        UserDetailsService service = new SecurityConfig().userDetailsService(userRepository);

        when(userRepository.findByUsername("alice"))
                .thenReturn(Optional.of(new User(1L, "alice", "pw", LocalDateTime.now())));

        UserDetails details = service.loadUserByUsername("alice");

        assertNotNull(details);
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void userDetailsServiceThrowsWhenUserMissing() {
        UserRepository userRepository = mock(UserRepository.class);
        UserDetailsService service = new SecurityConfig().userDetailsService(userRepository);
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("ghost"));
    }
}

