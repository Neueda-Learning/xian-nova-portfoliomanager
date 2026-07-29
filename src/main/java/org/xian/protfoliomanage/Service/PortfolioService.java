package org.xian.protfoliomanage.Service;

import org.xian.protfoliomanage.Dto.AddPortfolioItemRequest;
import org.xian.protfoliomanage.Dto.PortfolioItemResponse;
import org.xian.protfoliomanage.Dto.PortfolioSummaryResponse;
import org.xian.protfoliomanage.Model.PortfolioItem;
import org.xian.protfoliomanage.Model.User;
import org.xian.protfoliomanage.Repository.PortfolioItemRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class PortfolioService {

    private final PortfolioItemRepository itemRepository;
    private final CurrentUserService currentUserService;
    private final PriceClientService priceClientService;

    public PortfolioService(
            PortfolioItemRepository itemRepository,
            CurrentUserService currentUserService,
            PriceClientService priceClientService
    ) {
        this.itemRepository = itemRepository;
        this.currentUserService = currentUserService;
        this.priceClientService = priceClientService;
    }

    public List<PortfolioItemResponse> getItemsForCurrentUser() {
        User user = currentUserService.getCurrentUser();
        List<PortfolioItem> items = itemRepository.findByUserId(user.getId());

        List<PortfolioItemResponse> responses = new ArrayList<>();
        for (PortfolioItem item : items) {
            BigDecimal currentPrice = resolveCurrentPrice(item);
            BigDecimal cost = item.getBuyPrice().multiply(item.getQuantity());
            BigDecimal currentValue = currentPrice.multiply(item.getQuantity());
            BigDecimal pnl = currentValue.subtract(cost);

            responses.add(new PortfolioItemResponse(
                    item.getId(),
                    item.getTicker(),
                    item.getAssetType(),
                    item.getQuantity(),
                    item.getBuyPrice(),
                    item.getPurchaseDate(),
                    scaleMoney(currentPrice),
                    scaleMoney(currentValue),
                    scaleMoney(pnl)
            ));
        }
        return responses;
    }

    public long addItemForCurrentUser(AddPortfolioItemRequest request) {
        User user = currentUserService.getCurrentUser();
        String ticker = normalizeTicker(request.ticker());
        PortfolioItem item = new PortfolioItem();
        item.setUserId(user.getId());
        item.setTicker(ticker);
        item.setAssetType(request.assetType());
        item.setQuantity(request.quantity());
        item.setBuyPrice(resolveEntryPrice(ticker, request.purchaseDate()));
        item.setPurchaseDate(request.purchaseDate());
        return itemRepository.save(item);
    }

    public void updateItemForCurrentUser(Long id, AddPortfolioItemRequest request) {
        User user = currentUserService.getCurrentUser();
        String ticker = normalizeTicker(request.ticker());
        PortfolioItem existing = itemRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Portfolio item not found"));

        BigDecimal buyPrice = shouldRefreshEntryPrice(existing, ticker, request.purchaseDate())
                ? resolveEntryPrice(ticker, request.purchaseDate())
                : existing.getBuyPrice();

        PortfolioItem item = new PortfolioItem();
        item.setId(id);
        item.setUserId(user.getId());
        item.setTicker(ticker);
        item.setAssetType(request.assetType());
        item.setQuantity(request.quantity());
        item.setBuyPrice(buyPrice);
        item.setPurchaseDate(request.purchaseDate());

        int rows = itemRepository.updateByIdAndUserId(item);
        if (rows == 0) {
            throw new IllegalArgumentException("Portfolio item not found");
        }
    }

    public void removeItemForCurrentUser(Long id) {
        User user = currentUserService.getCurrentUser();
        int rows = itemRepository.deleteByIdAndUserId(id, user.getId());
        if (rows == 0) {
            throw new IllegalArgumentException("Portfolio item not found");
        }
    }

    public PortfolioSummaryResponse getSummaryForCurrentUser() {
        List<PortfolioItemResponse> items = getItemsForCurrentUser();

        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalMarketValue = BigDecimal.ZERO;
        Map<String, BigDecimal> byTicker = new LinkedHashMap<>();

        for (PortfolioItemResponse item : items) {
            BigDecimal itemCost = item.buyPrice().multiply(item.quantity());
            totalCost = totalCost.add(itemCost);
            totalMarketValue = totalMarketValue.add(item.currentValue());
            byTicker.merge(item.ticker(), item.currentValue(), BigDecimal::add);
        }

        BigDecimal totalProfitLoss = totalMarketValue.subtract(totalCost);
        Map<String, BigDecimal> allocations = new LinkedHashMap<>();
        if (totalMarketValue.compareTo(BigDecimal.ZERO) > 0) {
            final BigDecimal finalTotalMarketValue = totalMarketValue;
            byTicker.forEach((ticker, value) -> allocations.put(
                    ticker,
                    value.multiply(BigDecimal.valueOf(100)).divide(finalTotalMarketValue, 2, RoundingMode.HALF_UP)
            ));
        }

        return new PortfolioSummaryResponse(
                scaleMoney(totalCost),
                scaleMoney(totalMarketValue),
                scaleMoney(totalProfitLoss),
                allocations
        );
    }

    private BigDecimal resolveCurrentPrice(PortfolioItem item) {
        BigDecimal price = priceClientService.getCurrentPrice(item.getTicker());
        if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
            return price;
        }
        return item.getBuyPrice();
    }

    private BigDecimal resolveEntryPrice(String ticker, java.time.LocalDate purchaseDate) {
        BigDecimal resolvedPrice = priceClientService.getPriceForDate(ticker, purchaseDate);
        if (resolvedPrice == null || resolvedPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unable to fetch current market price for ticker: " + ticker);
        }
        return resolvedPrice;
    }

    private boolean shouldRefreshEntryPrice(PortfolioItem existing, String ticker, java.time.LocalDate purchaseDate) {
        return !Objects.equals(existing.getTicker(), ticker)
                || !Objects.equals(existing.getPurchaseDate(), purchaseDate);
    }

    private String normalizeTicker(String ticker) {
        String value = ticker == null ? "" : ticker.trim().toUpperCase(Locale.ROOT);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Ticker cannot be blank");
        }
        return value;
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
