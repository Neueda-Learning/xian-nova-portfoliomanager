package org.xian.protfoliomanage.Repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.xian.protfoliomanage.Model.PriceSnapshot;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JdbcTest
@Import(PriceSnapshotRepository.class)
class PriceSnapshotRepositoryTest {

    @Autowired
    private PriceSnapshotRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM price_snapshots");
        jdbcTemplate.update("INSERT INTO price_snapshots(ticker, latest_price, raw_payload, fetched_at) VALUES ('AAPL', 100, '{}', TIMESTAMP '2026-01-01 00:00:00')");
        jdbcTemplate.update("INSERT INTO price_snapshots(ticker, latest_price, raw_payload, fetched_at) VALUES ('AAPL', 101, '{}', TIMESTAMP '2026-01-01 00:01:00')");
    }

    @Test
    void saveAndFindLatestByTickerWork() {
        PriceSnapshot snapshot = new PriceSnapshot();
        snapshot.setTicker("MSFT");
        snapshot.setLatestPrice(new BigDecimal("300.50"));
        snapshot.setRawPayload("{\"close\":[300.50]}");
        snapshot.setFetchedAt(LocalDateTime.parse("2026-01-01T00:02:00"));

        long id = repository.save(snapshot);

        Optional<PriceSnapshot> latestAapl = repository.findLatestByTicker("AAPL");
        Optional<PriceSnapshot> latestMsft = repository.findLatestByTicker("MSFT");

        assertTrue(id > 0);
        assertTrue(latestAapl.isPresent());
        assertEquals(new BigDecimal("101.00"), latestAapl.get().getLatestPrice().setScale(2));
        assertTrue(latestMsft.isPresent());
        assertEquals(new BigDecimal("300.50"), latestMsft.get().getLatestPrice().setScale(2));
    }
}

