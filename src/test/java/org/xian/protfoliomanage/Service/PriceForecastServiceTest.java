package org.xian.protfoliomanage.Service;

import org.junit.jupiter.api.Test;
import org.xian.protfoliomanage.Dto.ForecastModelType;
import org.xian.protfoliomanage.Dto.PriceForecastResponse;
import org.xian.protfoliomanage.Service.PriceClientService.PriceQuote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PriceForecastServiceTest {

    private final PriceClientService priceClientService = mock(PriceClientService.class);
    private final PriceForecastService service = new PriceForecastService(priceClientService);

    @Test
    void forecastNextDayUsesWmaByDefault() {
        PriceQuote quote = new PriceQuote(
                "AAPL",
                new BigDecimal("14.00"),
                "{\"price_data\":{\"close\":[10.00,11.00,12.00,13.00,14.00]}}",
                Instant.now()
        );
        when(priceClientService.fetchLatestQuote("AAPL")).thenReturn(quote);
        when(priceClientService.extractCloseSeries(quote.rawPayload()))
                .thenReturn(List.of(
                        new BigDecimal("10.00"),
                        new BigDecimal("11.00"),
                        new BigDecimal("12.00"),
                        new BigDecimal("13.00"),
                        new BigDecimal("14.00")
                ));

        PriceForecastResponse response = service.forecastNextDay("AAPL", null, 5);

        assertEquals("AAPL", response.ticker());
        assertEquals(ForecastModelType.WMA, response.model());
        assertEquals(new BigDecimal("12.67"), response.predictedNextClose().setScale(2, RoundingMode.HALF_UP));
        assertEquals("Tomorrow", response.points().get(response.points().size() - 1).label());
        assertTrue(response.points().get(response.points().size() - 1).predicted());
    }

    @Test
    void forecastNextDaySupportsLinearRegressionModel() {
        PriceQuote quote = new PriceQuote(
                "TSLA",
                new BigDecimal("108.00"),
                "{\"close\":[100.00,102.00,104.00,106.00,108.00]}",
                Instant.now()
        );
        when(priceClientService.fetchLatestQuote("TSLA")).thenReturn(quote);
        when(priceClientService.extractCloseSeries(quote.rawPayload()))
                .thenReturn(List.of(
                        new BigDecimal("100.00"),
                        new BigDecimal("102.00"),
                        new BigDecimal("104.00"),
                        new BigDecimal("106.00"),
                        new BigDecimal("108.00")
                ));

        PriceForecastResponse response = service.forecastNextDay("TSLA", "LINEAR_REGRESSION", 5);

        assertEquals(ForecastModelType.LINEAR_REGRESSION, response.model());
        assertEquals(new BigDecimal("110.0000"), response.predictedNextClose());
        assertEquals(new BigDecimal("1.85"), response.predictedChangePercent());
    }

    @Test
    void forecastNextDaySupportsEmaModel() {
        PriceQuote quote = new PriceQuote(
                "AMZN",
                new BigDecimal("104.00"),
                "{\"close\":[100.00,101.00,102.00,103.00,104.00]}",
                Instant.now()
        );
        when(priceClientService.fetchLatestQuote("AMZN")).thenReturn(quote);
        when(priceClientService.extractCloseSeries(quote.rawPayload()))
                .thenReturn(List.of(
                        new BigDecimal("100.00"),
                        new BigDecimal("101.00"),
                        new BigDecimal("102.00"),
                        new BigDecimal("103.00"),
                        new BigDecimal("104.00")
                ));

        PriceForecastResponse response = service.forecastNextDay("AMZN", "EMA", 5);

        assertEquals(ForecastModelType.EMA, response.model());
        assertEquals(new BigDecimal("102.4744"), response.predictedNextClose());
    }

    @Test
    void forecastNextDayThrowsWhenInsufficientHistory() {
        PriceQuote quote = new PriceQuote(
                "MSFT",
                new BigDecimal("300.00"),
                "{\"close\":[300.00,301.00]}",
                Instant.now()
        );
        when(priceClientService.fetchLatestQuote("MSFT")).thenReturn(quote);
        when(priceClientService.extractCloseSeries(quote.rawPayload()))
                .thenReturn(List.of(new BigDecimal("300.00"), new BigDecimal("301.00")));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.forecastNextDay("MSFT", "WMA", 30)
        );

        assertEquals("Insufficient historical data to forecast ticker: MSFT", ex.getMessage());
    }
}

