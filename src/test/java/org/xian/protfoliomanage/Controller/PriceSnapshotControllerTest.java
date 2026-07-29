package org.xian.protfoliomanage.Controller;

import org.junit.jupiter.api.Test;
import org.xian.protfoliomanage.Dto.ForecastModelType;
import org.xian.protfoliomanage.Dto.ForecastPointResponse;
import org.xian.protfoliomanage.Dto.PriceForecastResponse;
import org.xian.protfoliomanage.Dto.PriceSnapshotResponse;
import org.xian.protfoliomanage.Service.PriceForecastService;
import org.xian.protfoliomanage.Service.PriceSnapshotService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PriceSnapshotControllerTest {

    private final PriceSnapshotService service = mock(PriceSnapshotService.class);
    private final PriceForecastService forecastService = mock(PriceForecastService.class);
    private final PriceSnapshotController controller = new PriceSnapshotController(service, forecastService);

    @Test
    void syncReturnsCreatedResponse() {
        PriceSnapshotResponse snapshot = new PriceSnapshotResponse(1L, "AAPL", new BigDecimal("10"), LocalDateTime.now(), "{}");
        when(service.syncLatestPrice("aapl")).thenReturn(snapshot);

        ResponseEntity<PriceSnapshotResponse> response = controller.sync("aapl");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(snapshot, response.getBody());
    }

    @Test
    void refreshDelegatesToService() {
        PriceSnapshotResponse snapshot = new PriceSnapshotResponse(1L, "AAPL", new BigDecimal("10"), LocalDateTime.now(), "{}");
        when(service.syncLatestPrice("AAPL")).thenReturn(snapshot);

        PriceSnapshotResponse actual = controller.refresh("AAPL");

        assertEquals(snapshot, actual);
    }

    @Test
    void getLatestDelegatesToService() {
        PriceSnapshotResponse snapshot = new PriceSnapshotResponse(2L, "MSFT", new BigDecimal("200"), LocalDateTime.now(), "{}");
        when(service.getLatestPrice("MSFT")).thenReturn(snapshot);

        PriceSnapshotResponse actual = controller.getLatest("MSFT");

        assertEquals(snapshot, actual);
    }

    @Test
    void getLiveReturnsBadGatewayWhenSyncFails() {
        when(service.syncLatestPrice("FAIL")).thenThrow(new IllegalStateException("upstream down"));

        ResponseEntity<?> response = controller.getLive("FAIL");

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("upstream down", ((Map<?, ?>) response.getBody()).get("message"));
    }

    @Test
    void getLiveReturnsOkWhenSyncSucceeds() {
        PriceSnapshotResponse snapshot = new PriceSnapshotResponse(2L, "MSFT", new BigDecimal("200"), LocalDateTime.now(), "{}");
        when(service.syncLatestPrice("MSFT")).thenReturn(snapshot);

        ResponseEntity<?> response = controller.getLive("MSFT");

        verify(service).syncLatestPrice("MSFT");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(snapshot, response.getBody());
    }

    @Test
    void getForecastReturnsOkWithPayload() {
        PriceForecastResponse forecast = new PriceForecastResponse(
                "AAPL",
                ForecastModelType.WMA,
                30,
                new BigDecimal("200.10"),
                new BigDecimal("201.20"),
                new BigDecimal("0.55"),
                List.of(new ForecastPointResponse("Tomorrow", new BigDecimal("201.20"), true))
        );
        when(forecastService.forecastNextDay("AAPL", "WMA", 30)).thenReturn(forecast);

        ResponseEntity<?> response = controller.getForecast("AAPL", "WMA", 30);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(forecast, response.getBody());
    }

    @Test
    void getForecastReturnsBadGatewayWhenForecastFails() {
        when(forecastService.forecastNextDay("TSLA", "WMA", 30))
                .thenThrow(new IllegalStateException("upstream down"));

        ResponseEntity<?> response = controller.getForecast("TSLA", "WMA", 30);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("upstream down", ((Map<?, ?>) response.getBody()).get("message"));
    }
}

