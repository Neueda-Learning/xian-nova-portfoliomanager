package com.didi.portfoliomanagermock.dto;

import com.didi.portfoliomanagermock.model.AssetType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PortfolioItemResponse(
        Long id,
        String ticker,
        AssetType assetType,
        BigDecimal quantity,
        BigDecimal buyPrice,
        LocalDate purchaseDate,
        BigDecimal currentPrice,
        BigDecimal currentValue,
        BigDecimal profitLoss
) {
}

