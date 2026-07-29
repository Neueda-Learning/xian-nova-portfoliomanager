package org.xian.protfoliomanage.Service;

import org.junit.jupiter.api.Test;
import org.xian.protfoliomanage.Dto.PriceSnapshotResponse;
import org.xian.protfoliomanage.Model.PriceSnapshot;
import org.xian.protfoliomanage.Repository.PriceSnapshotRepository;
import org.xian.protfoliomanage.Service.PriceClientService.PriceQuote;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PriceSnapshotServiceTest {

    private final PriceClientService priceClientService = mock(PriceClientService.class);
    private final PriceSnapshotRepository repository = mock(PriceSnapshotRepository.class);
    private final PriceSnapshotService service = new PriceSnapshotService(priceClientService, repository);

    @Test
    void syncLatestPriceSavesSnapshotAndReturnsResponse() {
        PriceQuote quote = new PriceQuote("AAPL", new BigDecimal("123.45"), "{\"close\":[123.45]}", Instant.now());
        when(priceClientService.fetchLatestQuote("aapl")).thenReturn(quote);
        when(repository.save(any(PriceSnapshot.class))).thenReturn(99L);

        PriceSnapshotResponse response = service.syncLatestPrice("aapl");

        assertEquals(99L, response.id());
        assertEquals("AAPL", response.ticker());
        assertEquals(new BigDecimal("123.45"), response.latestPrice());
    }

    @Test
    void syncLatestPriceThrowsWhenQuoteMissing() {
        when(priceClientService.fetchLatestQuote("msft")).thenReturn(null);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.syncLatestPrice("msft"));

        assertEquals("Unable to fetch price for ticker: msft", ex.getMessage());
    }

    @Test
    void getLatestPriceReturnsMappedResponse() {
        PriceSnapshot snapshot = new PriceSnapshot(5L, "AAPL", new BigDecimal("12.34"), "{}", LocalDateTime.now());
        when(repository.findLatestByTicker("AAPL")).thenReturn(Optional.of(snapshot));

        PriceSnapshotResponse response = service.getLatestPrice(" aapl ");

        assertEquals(5L, response.id());
        assertEquals("AAPL", response.ticker());
        assertEquals(new BigDecimal("12.34"), response.latestPrice());
    }

    @Test
    void getLatestPriceThrowsWhenNotFound() {
        when(repository.findLatestByTicker("QQQ")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.getLatestPrice("qqq"));

        assertEquals("No stored price snapshot found for ticker: QQQ", ex.getMessage());
    }
}

