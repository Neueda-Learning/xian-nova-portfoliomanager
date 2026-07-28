package com.didi.portfoliomanagermock.repository;

import com.didi.portfoliomanagermock.model.AssetType;
import com.didi.portfoliomanagermock.model.PortfolioItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class PortfolioItemRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<PortfolioItem> rowMapper = this::mapRow;

    public PortfolioItemRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PortfolioItem> findByUserId(Long userId) {
        return jdbcTemplate.query(
                "SELECT id, user_id, ticker, asset_type, quantity, buy_price, purchase_date FROM portfolio_items WHERE user_id = ? ORDER BY id DESC",
                rowMapper,
                userId
        );
    }

    public Optional<PortfolioItem> findByIdAndUserId(Long id, Long userId) {
        return jdbcTemplate.query(
                "SELECT id, user_id, ticker, asset_type, quantity, buy_price, purchase_date FROM portfolio_items WHERE id = ? AND user_id = ?",
                rowMapper,
                id,
                userId
        ).stream().findFirst();
    }

    public long save(PortfolioItem item) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO portfolio_items(user_id, ticker, asset_type, quantity, buy_price, purchase_date) VALUES (?, ?, ?, ?, ?, ?)",
                    new String[]{"id"}
            );
            statement.setLong(1, item.getUserId());
            statement.setString(2, item.getTicker());
            statement.setString(3, item.getAssetType().name());
            statement.setBigDecimal(4, item.getQuantity());
            statement.setBigDecimal(5, item.getBuyPrice());
            statement.setDate(6, Date.valueOf(item.getPurchaseDate()));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Insert succeeded but no generated ID returned");
        }
        return key.longValue();
    }

    public int deleteByIdAndUserId(Long id, Long userId) {
        return jdbcTemplate.update("DELETE FROM portfolio_items WHERE id = ? AND user_id = ?", id, userId);
    }

    public int updateByIdAndUserId(PortfolioItem item) {
        return jdbcTemplate.update(
                "UPDATE portfolio_items SET ticker = ?, asset_type = ?, quantity = ?, buy_price = ?, purchase_date = ? WHERE id = ? AND user_id = ?",
                item.getTicker(),
                item.getAssetType().name(),
                item.getQuantity(),
                item.getBuyPrice(),
                Date.valueOf(item.getPurchaseDate()),
                item.getId(),
                item.getUserId()
        );
    }

    private PortfolioItem mapRow(ResultSet rs, int rowNum) throws SQLException {
        PortfolioItem item = new PortfolioItem();
        item.setId(rs.getLong("id"));
        item.setUserId(rs.getLong("user_id"));
        item.setTicker(rs.getString("ticker"));
        item.setAssetType(AssetType.valueOf(rs.getString("asset_type")));
        item.setQuantity(rs.getBigDecimal("quantity"));
        item.setBuyPrice(rs.getBigDecimal("buy_price"));
        item.setPurchaseDate(rs.getDate("purchase_date").toLocalDate());
        return item;
    }
}

