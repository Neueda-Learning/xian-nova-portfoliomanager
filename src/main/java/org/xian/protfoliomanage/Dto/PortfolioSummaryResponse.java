package org.xian.protfoliomanage.Dto;

import java.math.BigDecimal;
import java.util.Map;

public record PortfolioSummaryResponse(
        BigDecimal totalCost,
        BigDecimal totalMarketValue,
        BigDecimal totalProfitLoss,
        Map<String, BigDecimal> allocationPercentages
) {
}
