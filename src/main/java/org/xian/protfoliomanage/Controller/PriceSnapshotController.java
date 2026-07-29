package org.xian.protfoliomanage.Controller;

import org.xian.protfoliomanage.Dto.PriceSnapshotResponse;
import org.xian.protfoliomanage.Dto.PriceForecastResponse;
import org.xian.protfoliomanage.Service.PriceForecastService;
import org.xian.protfoliomanage.Service.PriceSnapshotService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/price-snapshots")
public class PriceSnapshotController {

    private final PriceSnapshotService priceSnapshotService;
    private final PriceForecastService priceForecastService;

    public PriceSnapshotController(PriceSnapshotService priceSnapshotService, PriceForecastService priceForecastService) {
        this.priceSnapshotService = priceSnapshotService;
        this.priceForecastService = priceForecastService;
    }

    @PostMapping("/{ticker}/sync")
    public ResponseEntity<PriceSnapshotResponse> sync(@PathVariable String ticker) {
        PriceSnapshotResponse response = priceSnapshotService.syncLatestPrice(ticker);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{ticker}/refresh")
    public PriceSnapshotResponse refresh(@PathVariable String ticker) {
        return priceSnapshotService.syncLatestPrice(ticker);
    }

    @GetMapping("/{ticker}")
    public PriceSnapshotResponse getLatest(@PathVariable String ticker) {
        return priceSnapshotService.getLatestPrice(ticker);
    }

    @GetMapping("/{ticker}/live")
    public ResponseEntity<?> getLive(@PathVariable String ticker) {
        try {
            return ResponseEntity.ok(priceSnapshotService.syncLatestPrice(ticker));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/{ticker}/forecast")
    public ResponseEntity<?> getForecast(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "WMA") String model,
            @RequestParam(defaultValue = "30") Integer window
    ) {
        try {
            PriceForecastResponse response = priceForecastService.forecastNextDay(ticker, model, window);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", ex.getMessage()));
        }
    }
}

