package org.xian.protfoliomanage.Model;

public class PortfolioItem {

    private Long id;
    private String stockTicker;
    private Integer volume;

    public PortfolioItem() {
    }

    public PortfolioItem(Long id, String stockTicker, Integer volume) {
        this.id = id;
        this.stockTicker = stockTicker;
        this.volume = volume;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStockTicker() {
        return stockTicker;
    }

    public void setStockTicker(String stockTicker) {
        this.stockTicker = stockTicker;
    }

    public Integer getVolume() {
        return volume;
    }

    public void setVolume(Integer volume) {
        this.volume = volume;
    }
}