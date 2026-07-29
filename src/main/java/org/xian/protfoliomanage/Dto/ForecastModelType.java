package org.xian.protfoliomanage.Dto;

public enum ForecastModelType {
    WMA,
    EMA,
    SMA,
    LINEAR_REGRESSION;

    public static ForecastModelType fromValue(String rawModel) {
        if (rawModel == null || rawModel.isBlank()) {
            return WMA;
        }
        return ForecastModelType.valueOf(rawModel.trim().toUpperCase());
    }
}

