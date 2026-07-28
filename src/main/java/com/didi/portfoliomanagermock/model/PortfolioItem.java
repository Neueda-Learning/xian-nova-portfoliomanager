package com.didi.portfoliomanagermock.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PortfolioItem {

    private Long id;
    private Long userId;
    private String ticker;
    private AssetType assetType;
    private BigDecimal quantity;
    private BigDecimal buyPrice;
    private LocalDate purchaseDate;

    public PortfolioItem() {
    }

    public PortfolioItem(
            Long id,
            Long userId,
            String ticker,
            AssetType assetType,
            BigDecimal quantity,
            BigDecimal buyPrice,
            LocalDate purchaseDate
    ) {
        this.id = id;
        this.userId = userId;
        this.ticker = ticker;
        this.assetType = assetType;
        this.quantity = quantity;
        this.buyPrice = buyPrice;
        this.purchaseDate = purchaseDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public void setAssetType(AssetType assetType) {
        this.assetType = assetType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getBuyPrice() {
        return buyPrice;
    }

    public void setBuyPrice(BigDecimal buyPrice) {
        this.buyPrice = buyPrice;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }
}

