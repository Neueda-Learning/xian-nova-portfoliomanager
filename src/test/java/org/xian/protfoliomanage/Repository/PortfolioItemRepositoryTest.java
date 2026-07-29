package org.xian.protfoliomanage.Repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.xian.protfoliomanage.Model.AssetType;
import org.xian.protfoliomanage.Model.PortfolioItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JdbcTest
@Import(PortfolioItemRepository.class)
class PortfolioItemRepositoryTest {

    @Autowired
    private PortfolioItemRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM portfolio_items");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("INSERT INTO users(username, password) VALUES ('owner', 'pw')");
        userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username='owner'", Long.class);

        jdbcTemplate.update("INSERT INTO portfolio_items(user_id, ticker, asset_type, quantity, buy_price, purchase_date) VALUES (?, 'AAPL', 'STOCK', 2, 100, '2026-01-01')", userId);
        jdbcTemplate.update("INSERT INTO portfolio_items(user_id, ticker, asset_type, quantity, buy_price, purchase_date) VALUES (?, 'BONDX', 'BOND', 3, 10, '2026-01-02')", userId);
    }

    @Test
    void findByUserIdReturnsRowsInDescendingOrder() {
        List<PortfolioItem> items = repository.findByUserId(userId);

        assertEquals(2, items.size());
        assertTrue(items.get(0).getId() > items.get(1).getId());
    }

    @Test
    void findByIdAndUserIdFindsAndMisses() {
        Long id = jdbcTemplate.queryForObject("SELECT id FROM portfolio_items WHERE ticker='AAPL'", Long.class);

        Optional<PortfolioItem> found = repository.findByIdAndUserId(id, userId);
        Optional<PortfolioItem> missing = repository.findByIdAndUserId(id, userId + 1);

        assertTrue(found.isPresent());
        assertEquals("AAPL", found.get().getTicker());
        assertTrue(missing.isEmpty());
    }

    @Test
    void saveUpdateAndDeleteWork() {
        PortfolioItem item = new PortfolioItem();
        item.setUserId(userId);
        item.setTicker("msft");
        item.setAssetType(AssetType.STOCK);
        item.setQuantity(new BigDecimal("5"));
        item.setBuyPrice(new BigDecimal("220"));
        item.setPurchaseDate(LocalDate.parse("2026-02-01"));

        long id = repository.save(item);
        assertTrue(id > 0);

        PortfolioItem update = new PortfolioItem();
        update.setId(id);
        update.setUserId(userId);
        update.setTicker("MSFT");
        update.setAssetType(AssetType.STOCK);
        update.setQuantity(new BigDecimal("8"));
        update.setBuyPrice(new BigDecimal("210"));
        update.setPurchaseDate(LocalDate.parse("2026-02-02"));

        assertEquals(1, repository.updateByIdAndUserId(update));
        assertEquals(1, repository.deleteByIdAndUserId(id, userId));
        assertEquals(0, repository.deleteByIdAndUserId(id, userId));
    }
}

