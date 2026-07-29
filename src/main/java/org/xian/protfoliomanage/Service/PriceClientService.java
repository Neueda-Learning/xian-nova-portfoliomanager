package org.xian.protfoliomanage.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PriceClientService {

    private static final Duration CACHE_DURATION = Duration.ofSeconds(10);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Map<String, CachedPrice> cache = new ConcurrentHashMap<>();

    public PriceClientService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${app.price-api.base-url}") String baseUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    public BigDecimal getCurrentPrice(String rawTicker) {
        String ticker = normalizeTicker(rawTicker);
        if (ticker.isBlank()) {
            return null;
        }
        if ("CASH".equals(ticker)) {
            return BigDecimal.ONE;
        }

        CachedPrice cachedPrice = cache.get(ticker);
        if (cachedPrice != null && Instant.now().isBefore(cachedPrice.expiresAt())) {
            return cachedPrice.price();
        }

        PriceQuote quote = fetchLatestQuote(ticker);
        if (quote != null) {
            cache.put(ticker, new CachedPrice(quote.latestPrice(), quote.fetchedAt().plus(CACHE_DURATION)));
            return quote.latestPrice();
        }
        return null;
    }

    public BigDecimal getPriceForDate(String rawTicker, LocalDate purchaseDate) {
        String ticker = normalizeTicker(rawTicker);
        if (ticker.isBlank()) {
            return null;
        }
        if ("CASH".equals(ticker)) {
            return BigDecimal.ONE;
        }

        PriceQuote quote = fetchLatestQuote(ticker);
        if (quote == null || quote.latestPrice() == null || quote.latestPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        if (purchaseDate == null) {
            return quote.latestPrice();
        }

        List<BigDecimal> closes = extractCloseSeries(quote.rawPayload());
        if (closes.isEmpty()) {
            return quote.latestPrice();
        }

        long daysAgo = ChronoUnit.DAYS.between(purchaseDate, LocalDate.now());
        if (daysAgo <= 0) {
            return quote.latestPrice();
        }

        int stepsBack = (int) Math.min(daysAgo, closes.size() - 1L);
        return closes.get(closes.size() - 1 - stepsBack);
    }

    public PriceQuote fetchLatestQuote(String rawTicker) {
        String ticker = normalizeTicker(rawTicker);
        if (ticker.isBlank()) {
            return null;
        }
        if ("CASH".equals(ticker)) {
            return new PriceQuote(ticker, BigDecimal.ONE, "1", Instant.now());
        }

        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.queryParam("ticker", ticker).build())
                    .retrieve()
                    .body(String.class);
            BigDecimal latestClose = extractLatestClose(response);
            if (latestClose != null) {
                return new PriceQuote(ticker, latestClose, response, Instant.now());
            }
        } catch (Exception ignored) {

        }
        return null;
    }

    private BigDecimal extractLatestClose(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode nestedClose = root.path("price_data").path("close");
            if (nestedClose.isArray() && !nestedClose.isEmpty()) {
                JsonNode last = nestedClose.get(nestedClose.size() - 1);
                return last.decimalValue();
            }
            if (root.has("close") && root.get("close").isArray() && !root.get("close").isEmpty()) {
                JsonNode last = root.get("close").get(root.get("close").size() - 1);
                return last.decimalValue();
            }
            if (root.isArray() && !root.isEmpty()) {
                return root.get(root.size() - 1).decimalValue();
            }
        } catch (Exception ignored) {

        }

        String[] tokens = payload.replace("[", "").replace("]", "").replace("\"", "").split(",");
        for (int i = tokens.length - 1; i >= 0; i--) {
            String token = tokens[i].trim();
            try {
                return new BigDecimal(token);
            } catch (NumberFormatException ignored) {

            }
        }
        return null;
    }

    public List<BigDecimal> extractCloseSeries(String payload) {
        if (payload == null || payload.isBlank()) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode nestedClose = root.path("price_data").path("close");
            if (nestedClose.isArray() && !nestedClose.isEmpty()) {
                return readDecimalArray(nestedClose);
            }

            JsonNode flatClose = root.path("close");
            if (flatClose.isArray() && !flatClose.isEmpty()) {
                return readDecimalArray(flatClose);
            }

            if (root.isArray() && !root.isEmpty()) {
                return readDecimalArray(root);
            }
        } catch (Exception ignored) {

        }

        return List.of();
    }

    private List<BigDecimal> readDecimalArray(JsonNode values) {
        List<BigDecimal> result = new ArrayList<>();
        for (JsonNode value : values) {
            if (value != null && value.isNumber()) {
                result.add(value.decimalValue());
            }
        }
        return result;
    }

    private String normalizeTicker(String rawTicker) {
        return rawTicker == null ? "" : rawTicker.trim().toUpperCase(Locale.ROOT);
    }

    public record PriceQuote(String ticker, BigDecimal latestPrice, String rawPayload, Instant fetchedAt) {
    }

    private record CachedPrice(BigDecimal price, Instant expiresAt) {
    }
}

