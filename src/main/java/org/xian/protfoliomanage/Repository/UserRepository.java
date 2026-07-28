package com.didi.portfoliomanagermock.repository;

import com.didi.portfoliomanagermock.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<User> rowMapper = this::mapRow;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByUsername(String username) {
        return jdbcTemplate.query("SELECT id, username, password, created_at FROM users WHERE username = ?", rowMapper, username)
                .stream()
                .findFirst();
    }

    public Optional<User> findById(Long id) {
        return jdbcTemplate.query("SELECT id, username, password, created_at FROM users WHERE id = ?", rowMapper, id)
                .stream()
                .findFirst();
    }

    public boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?",
                Integer.class,
                username
        );
        return count != null && count > 0;
    }

    public Optional<String> findPasswordByUsername(String username) {
        return jdbcTemplate.query(
                "SELECT password FROM users WHERE username = ?",
                (rs, rowNum) -> rs.getString("password"),
                username
        ).stream().findFirst();
    }

    public void save(String username, String password) {
        jdbcTemplate.update("INSERT INTO users(username, password) VALUES (?, ?)", username, password);
    }

    public void updatePasswordByUsername(String username, String encodedPassword) {
        jdbcTemplate.update("UPDATE users SET password = ? WHERE username = ?", encodedPassword, username);
    }

    private User mapRow(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return user;
    }
}

