package org.xian.protfoliomanage.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PriceSnapshotResponse(
        Long id,
        String ticker,
        BigDecimal latestPrice,
        LocalDateTime fetchedAt,
        String rawPayload
) {
}

