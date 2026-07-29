package org.xian.protfoliomanage.Service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.xian.protfoliomanage.Model.User;
import org.xian.protfoliomanage.Repository.UserRepository;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentUserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CurrentUserService service = new CurrentUserService(userRepository);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUserReturnsUserWhenAuthenticated() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("alice", "pw");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = new User(1L, "alice", "pw", LocalDateTime.now());
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        User current = service.getCurrentUser();

        assertEquals(1L, current.getId());
        assertEquals("alice", current.getUsername());
    }

    @Test
    void getCurrentUserThrowsWhenNotAuthenticated() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("alice", "pw");
        authentication.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        IllegalStateException ex = assertThrows(IllegalStateException.class, service::getCurrentUser);

        assertEquals("User is not authenticated", ex.getMessage());
    }

    @Test
    void getCurrentUserThrowsWhenUserNotFound() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("missing", "pw");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class, service::getCurrentUser);

        assertEquals("User not found: missing", ex.getMessage());
    }
}

