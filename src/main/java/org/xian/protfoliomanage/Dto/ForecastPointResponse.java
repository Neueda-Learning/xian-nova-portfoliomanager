package org.xian.protfoliomanage.Dto;

import java.math.BigDecimal;

public record ForecastPointResponse(
        String label,
        BigDecimal close,
        boolean predicted
) {
}

