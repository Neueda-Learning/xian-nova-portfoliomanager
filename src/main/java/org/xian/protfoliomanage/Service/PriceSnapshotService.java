package org.xian.protfoliomanage.Service;

import org.xian.protfoliomanage.Dto.PriceSnapshotResponse;
import org.xian.protfoliomanage.Model.PriceSnapshot;
import org.xian.protfoliomanage.Repository.PriceSnapshotRepository;
import org.xian.protfoliomanage.Service.PriceClientService.PriceQuote;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class PriceSnapshotService {

    private final PriceClientService priceClientService;
    private final PriceSnapshotRepository priceSnapshotRepository;

    public PriceSnapshotService(
            PriceClientService priceClientService,
            PriceSnapshotRepository priceSnapshotRepository
    ) {
        this.priceClientService = priceClientService;
        this.priceSnapshotRepository = priceSnapshotRepository;
    }

    public PriceSnapshotResponse syncLatestPrice(String rawTicker) {
        PriceQuote quote = priceClientService.fetchLatestQuote(rawTicker);
        if (quote == null) {
            throw new IllegalStateException("Unable to fetch price for ticker: " + rawTicker);
        }

        PriceSnapshot snapshot = new PriceSnapshot();
        snapshot.setTicker(quote.ticker());
        snapshot.setLatestPrice(quote.latestPrice());
        snapshot.setRawPayload(quote.rawPayload());
        snapshot.setFetchedAt(LocalDateTime.ofInstant(quote.fetchedAt(), ZoneId.systemDefault()));

        long id = priceSnapshotRepository.save(snapshot);
        snapshot.setId(id);
        return toResponse(snapshot);
    }

    public PriceSnapshotResponse getLatestPrice(String rawTicker) {
        String ticker = normalizeTicker(rawTicker);
        return priceSnapshotRepository.findLatestByTicker(ticker)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("No stored price snapshot found for ticker: " + ticker));
    }

    private PriceSnapshotResponse toResponse(PriceSnapshot snapshot) {
        return new PriceSnapshotResponse(
                snapshot.getId(),
                snapshot.getTicker(),
                snapshot.getLatestPrice(),
                snapshot.getFetchedAt(),
                snapshot.getRawPayload()
        );
    }

    private String normalizeTicker(String rawTicker) {
        return rawTicker == null ? "" : rawTicker.trim().toUpperCase();
    }
}

