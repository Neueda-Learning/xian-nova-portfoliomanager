package org.xian.protfoliomanage.Dto;

import org.xian.protfoliomanage.Model.AssetType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AddPortfolioItemRequest(
        @NotBlank String ticker,
        @NotNull AssetType assetType,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal quantity,
        @NotNull @DecimalMin(value = "0.0") BigDecimal buyPrice,
        @NotNull LocalDate purchaseDate
) {
}
