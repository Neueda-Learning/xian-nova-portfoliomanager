package org.xian.protfoliomanage.Service;

import org.springframework.stereotype.Service;
import org.xian.protfoliomanage.Dto.ForecastModelType;
import org.xian.protfoliomanage.Dto.ForecastPointResponse;
import org.xian.protfoliomanage.Dto.PriceForecastResponse;
import org.xian.protfoliomanage.Service.PriceClientService.PriceQuote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class PriceForecastService {

    private static final int MIN_FORECAST_WINDOW = 3;
    private static final int MAX_FORECAST_WINDOW = 252;

    private final PriceClientService priceClientService;

    public PriceForecastService(PriceClientService priceClientService) {
        this.priceClientService = priceClientService;
    }

    public PriceForecastResponse forecastNextDay(String rawTicker, String rawModel, Integer rawWindow) {
        ForecastModelType model = parseModel(rawModel);
        int window = normalizeWindow(rawWindow);

        PriceQuote quote = priceClientService.fetchLatestQuote(rawTicker);
        if (quote == null || quote.latestPrice() == null) {
            throw new IllegalStateException("Unable to fetch market data for ticker: " + rawTicker);
        }

        List<BigDecimal> closes = priceClientService.extractCloseSeries(quote.rawPayload());
        if (closes.size() < MIN_FORECAST_WINDOW) {
            throw new IllegalArgumentException("Insufficient historical data to forecast ticker: " + quote.ticker());
        }

        int effectiveWindow = Math.min(window, closes.size());
        List<BigDecimal> history = closes.subList(closes.size() - effectiveWindow, closes.size());
        BigDecimal latestClose = history.get(history.size() - 1);
        BigDecimal predictedNextClose = switch (model) {
            case WMA -> forecastByWeightedMovingAverage(history);
            case EMA -> forecastByExponentialMovingAverage(history);
            case SMA -> forecastBySimpleMovingAverage(history);
            case LINEAR_REGRESSION -> forecastByLinearRegression(history);
        };

        BigDecimal predictedChangePercent = latestClose.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : predictedNextClose.subtract(latestClose)
                .divide(latestClose, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        List<ForecastPointResponse> points = new ArrayList<>(effectiveWindow + 1);
        for (int index = 0; index < history.size(); index += 1) {
            int daysAgo = history.size() - index - 1;
            points.add(new ForecastPointResponse(daysAgo == 0 ? "Today" : daysAgo + "d", history.get(index), false));
        }
        points.add(new ForecastPointResponse("Tomorrow", predictedNextClose, true));

        return new PriceForecastResponse(
                quote.ticker(),
                model,
                effectiveWindow,
                latestClose,
                predictedNextClose,
                predictedChangePercent.setScale(2, RoundingMode.HALF_UP),
                points
        );
    }

    private ForecastModelType parseModel(String rawModel) {
        try {
            return ForecastModelType.fromValue(rawModel);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported forecast model: " + rawModel);
        }
    }

    private int normalizeWindow(Integer rawWindow) {
        if (rawWindow == null) {
            return 30;
        }
        return Math.min(MAX_FORECAST_WINDOW, Math.max(MIN_FORECAST_WINDOW, rawWindow));
    }

    private BigDecimal forecastByWeightedMovingAverage(List<BigDecimal> series) {
        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (int index = 0; index < series.size(); index += 1) {
            int weight = index + 1;
            weightedSum = weightedSum.add(series.get(index).multiply(BigDecimal.valueOf(weight)));
            totalWeight = totalWeight.add(BigDecimal.valueOf(weight));
        }
        return weightedSum.divide(totalWeight, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal forecastBySimpleMovingAverage(List<BigDecimal> series) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : series) {
            sum = sum.add(value);
        }
        return sum.divide(BigDecimal.valueOf(series.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal forecastByExponentialMovingAverage(List<BigDecimal> series) {
        double alpha = 0.35;
        if (series.isEmpty()) {
            return BigDecimal.ZERO;
        }

        double ema = series.get(0).doubleValue();
        for (int index = 1; index < series.size(); index += 1) {
            double price = series.get(index).doubleValue();
            ema = (alpha * price) + ((1 - alpha) * ema);
        }
        return BigDecimal.valueOf(ema).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal forecastByLinearRegression(List<BigDecimal> series) {
        int n = series.size();
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumXX = 0;

        for (int index = 0; index < n; index += 1) {
            double y = series.get(index).doubleValue();
            sumX += index;
            sumY += y;
            sumXY += index * y;
            sumXX += index * index;
        }

        double denominator = (n * sumXX) - (sumX * sumX);
        if (Math.abs(denominator) < 1e-9) {
            return forecastByWeightedMovingAverage(series);
        }

        double slope = ((n * sumXY) - (sumX * sumY)) / denominator;
        double intercept = (sumY - slope * sumX) / n;
        double prediction = intercept + slope * n;
        if (prediction <= 0) {
            return forecastByWeightedMovingAverage(series);
        }

        return BigDecimal.valueOf(prediction).setScale(4, RoundingMode.HALF_UP);
    }
}

