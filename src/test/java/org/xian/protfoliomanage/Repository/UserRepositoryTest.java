package org.xian.protfoliomanage.Repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JdbcTest
@Import(UserRepository.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("INSERT INTO users(username, password) VALUES ('alice', 'pw1')");
        jdbcTemplate.update("INSERT INTO users(username, password) VALUES ('bob', 'pw2')");
    }

    @Test
    void findByUsernameFindsExistingUser() {
        Optional<org.xian.protfoliomanage.Model.User> user = repository.findByUsername("alice");

        assertTrue(user.isPresent());
        assertEquals("alice", user.get().getUsername());
    }

    @Test
    void findByIdFindsExistingUser() {
        Long aliceId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username='alice'", Long.class);

        Optional<org.xian.protfoliomanage.Model.User> user = repository.findById(aliceId);

        assertTrue(user.isPresent());
        assertEquals(aliceId, user.get().getId());
    }

    @Test
    void existsByUsernameReturnsExpectedValue() {
        assertTrue(repository.existsByUsername("alice"));
        assertFalse(repository.existsByUsername("nobody"));
    }

    @Test
    void findPasswordAndUpdatePasswordWork() {
        assertEquals("pw1", repository.findPasswordByUsername("alice").orElseThrow());

        repository.updatePasswordByUsername("alice", "new-hash");

        assertEquals("new-hash", repository.findPasswordByUsername("alice").orElseThrow());
    }

    @Test
    void saveInsertsUser() {
        repository.save("charlie", "pw3");

        assertTrue(repository.findByUsername("charlie").isPresent());
    }
}

