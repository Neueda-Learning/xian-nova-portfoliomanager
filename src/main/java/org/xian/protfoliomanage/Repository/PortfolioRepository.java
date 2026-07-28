package org.xian.protfoliomanage.Repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.xian.protfoliomanage.Model.PortfolioItem;

import java.util.List;

@Repository
public class PortfolioRepository {

    private final JdbcTemplate jdbcTemplate;

    public PortfolioRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 查询投资组合中的全部记录
     */
    public List<PortfolioItem> findAll() {

        String sql = """
                SELECT id, stock_ticker, volume
                FROM portfolio_item
                ORDER BY id
                """;

        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> {

            PortfolioItem item = new PortfolioItem();

            item.setId(resultSet.getLong("id"));
            item.setStockTicker(resultSet.getString("stock_ticker"));
            item.setVolume(resultSet.getInt("volume"));

            return item;
        });
    }

    /**
     * 添加一条投资组合记录
     */
    public int save(PortfolioItem item) {

        String sql = """
                INSERT INTO portfolio_item (stock_ticker, volume)
                VALUES (?, ?)
                """;

        return jdbcTemplate.update(
                sql,
                item.getStockTicker(),
                item.getVolume()
        );
    }

    /**
     * 根据ID删除一条投资组合记录
     */
    public int deleteById(Long id) {

        String sql = """
                DELETE FROM portfolio_item
                WHERE id = ?
                """;

        return jdbcTemplate.update(sql, id);
    }
}