package org.xian.protfoliomanage.Repository;

import org.xian.protfoliomanage.Model.PriceSnapshot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class PriceSnapshotRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<PriceSnapshot> rowMapper = this::mapRow;

    public PriceSnapshotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long save(PriceSnapshot snapshot) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO price_snapshots(ticker, latest_price, raw_payload, fetched_at) VALUES (?, ?, ?, ?)",
                    new String[]{"id"}
            );
            statement.setString(1, snapshot.getTicker());
            statement.setBigDecimal(2, snapshot.getLatestPrice());
            statement.setString(3, snapshot.getRawPayload());
            statement.setTimestamp(4, Timestamp.valueOf(snapshot.getFetchedAt()));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Insert succeeded but no generated ID returned");
        }
        return key.longValue();
    }

    public Optional<PriceSnapshot> findLatestByTicker(String ticker) {
        List<PriceSnapshot> snapshots = jdbcTemplate.query(
                "SELECT id, ticker, latest_price, raw_payload, fetched_at FROM price_snapshots WHERE ticker = ? ORDER BY fetched_at DESC, id DESC LIMIT 1",
                rowMapper,
                ticker
        );
        return snapshots.stream().findFirst();
    }

    private PriceSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
        PriceSnapshot snapshot = new PriceSnapshot();
        snapshot.setId(rs.getLong("id"));
        snapshot.setTicker(rs.getString("ticker"));
        snapshot.setLatestPrice(rs.getBigDecimal("latest_price"));
        snapshot.setRawPayload(rs.getString("raw_payload"));
        snapshot.setFetchedAt(rs.getTimestamp("fetched_at").toLocalDateTime());
        return snapshot;
    }
}

