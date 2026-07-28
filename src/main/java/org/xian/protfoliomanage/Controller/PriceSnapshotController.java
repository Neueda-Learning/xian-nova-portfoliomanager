package org.xian.protfoliomanage.Controller;

import org.xian.protfoliomanage.Dto.PriceSnapshotResponse;
import org.xian.protfoliomanage.Service.PriceSnapshotService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/price-snapshots")
public class PriceSnapshotController {

    private final PriceSnapshotService priceSnapshotService;

    public PriceSnapshotController(PriceSnapshotService priceSnapshotService) {
        this.priceSnapshotService = priceSnapshotService;
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
}

