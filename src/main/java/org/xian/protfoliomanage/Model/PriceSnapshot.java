package org.xian.protfoliomanage.Model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PriceSnapshot {

    private Long id;
    private String ticker;
    private BigDecimal latestPrice;
    private String rawPayload;
    private LocalDateTime fetchedAt;

    public PriceSnapshot() {
    }

    public PriceSnapshot(Long id, String ticker, BigDecimal latestPrice, String rawPayload, LocalDateTime fetchedAt) {
        this.id = id;
        this.ticker = ticker;
        this.latestPrice = latestPrice;
        this.rawPayload = rawPayload;
        this.fetchedAt = fetchedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public BigDecimal getLatestPrice() {
        return latestPrice;
    }

    public void setLatestPrice(BigDecimal latestPrice) {
        this.latestPrice = latestPrice;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public void setRawPayload(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(LocalDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
    }
}

