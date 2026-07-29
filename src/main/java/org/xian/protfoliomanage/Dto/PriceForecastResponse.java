package org.xian.protfoliomanage.Dto;

import java.math.BigDecimal;
import java.util.List;

public record PriceForecastResponse(
        String ticker,
        ForecastModelType model,
        Integer historyWindow,
        BigDecimal latestClose,
        BigDecimal predictedNextClose,
        BigDecimal predictedChangePercent,
        List<ForecastPointResponse> points
) {
}

