package org.xian.protfoliomanage;

import org.junit.jupiter.api.Test;
import org.xian.protfoliomanage.Dto.AddPortfolioItemRequest;
import org.xian.protfoliomanage.Dto.PortfolioItemResponse;
import org.xian.protfoliomanage.Dto.PortfolioSummaryResponse;
import org.xian.protfoliomanage.Dto.PriceSnapshotResponse;
import org.xian.protfoliomanage.Model.AssetType;
import org.xian.protfoliomanage.Model.PortfolioItem;
import org.xian.protfoliomanage.Model.PriceSnapshot;
import org.xian.protfoliomanage.Model.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelAndDtoSmokeTest {

    @Test
    void modelGettersAndSettersWork() {
        PortfolioItem item = new PortfolioItem();
        item.setId(1L);
        item.setUserId(2L);
        item.setTicker("AAPL");
        item.setAssetType(AssetType.STOCK);
        item.setQuantity(new BigDecimal("3"));
        item.setBuyPrice(new BigDecimal("10"));
        item.setPurchaseDate(LocalDate.parse("2026-01-01"));

        assertEquals(1L, item.getId());
        assertEquals(2L, item.getUserId());
        assertEquals("AAPL", item.getTicker());
        assertEquals(AssetType.STOCK, item.getAssetType());
        assertEquals(new BigDecimal("3"), item.getQuantity());
        assertEquals(new BigDecimal("10"), item.getBuyPrice());

        PriceSnapshot snapshot = new PriceSnapshot(5L, "MSFT", new BigDecimal("100.11"), "{}", LocalDateTime.now());
        assertEquals(5L, snapshot.getId());
        assertEquals("MSFT", snapshot.getTicker());

        User user = new User(9L, "admin", "pw", LocalDateTime.now());
        assertEquals(9L, user.getId());
        assertEquals("admin", user.getUsername());
    }

    @Test
    void dtoRecordsExposeValues() {
        AddPortfolioItemRequest request = new AddPortfolioItemRequest(
                "AAPL", AssetType.STOCK, new BigDecimal("1"), LocalDate.parse("2026-01-01")
        );
        assertEquals("AAPL", request.ticker());

        PortfolioItemResponse item = new PortfolioItemResponse(
                1L, "AAPL", AssetType.STOCK, new BigDecimal("1"), new BigDecimal("2"),
                LocalDate.parse("2026-01-01"), new BigDecimal("3"), new BigDecimal("3"), new BigDecimal("1")
        );
        assertEquals(new BigDecimal("1"), item.profitLoss());

        PortfolioSummaryResponse summary = new PortfolioSummaryResponse(
                new BigDecimal("2"), new BigDecimal("3"), new BigDecimal("1"), Map.of("AAPL", new BigDecimal("100"))
        );
        assertEquals(new BigDecimal("100"), summary.allocationPercentages().get("AAPL"));

        PriceSnapshotResponse snapshot = new PriceSnapshotResponse(
                2L, "MSFT", new BigDecimal("10"), LocalDateTime.parse("2026-01-01T00:00:00"), "{}"
        );
        assertEquals("MSFT", snapshot.ticker());
    }
}

