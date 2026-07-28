package org.xian.protfoliomanage.Service;

import org.xian.protfoliomanage.Dto.AddPortfolioItemRequest;
import org.xian.protfoliomanage.Dto.PortfolioItemResponse;
import org.xian.protfoliomanage.Model.PortfolioItem;
import org.xian.protfoliomanage.Repository.PortfolioItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class PortfolioService {

    private final PortfolioItemRepository itemRepository;
    private final Long defaultUserId;

    public PortfolioService(
            PortfolioItemRepository itemRepository,
            @Value("${app.default-user-id:1}") Long defaultUserId
    ) {
        this.itemRepository = itemRepository;
        this.defaultUserId = defaultUserId;
    }

    public List<PortfolioItemResponse> getItems() {
        List<PortfolioItemResponse> responses = new ArrayList<>();
        for (PortfolioItem item : itemRepository.findByUserId(defaultUserId)) {
            BigDecimal currentPrice = item.getBuyPrice();
            BigDecimal currentValue = currentPrice.multiply(item.getQuantity());

            responses.add(new PortfolioItemResponse(
                    item.getId(),
                    item.getTicker(),
                    item.getAssetType(),
                    item.getQuantity(),
                    item.getBuyPrice(),
                    item.getPurchaseDate(),
                    scaleMoney(currentPrice),
                    scaleMoney(currentValue),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            ));
        }
        return responses;
    }

    public long addItem(AddPortfolioItemRequest request) {
        PortfolioItem item = toPortfolioItem(null, request);
        return itemRepository.save(item);
    }

    public void updateItem(Long id, AddPortfolioItemRequest request) {
        PortfolioItem item = toPortfolioItem(id, request);
        int rows = itemRepository.updateByIdAndUserId(item);
        if (rows == 0) {
            throw new IllegalArgumentException("Portfolio item not found");
        }
    }

    public void removeItem(Long id) {
        int rows = itemRepository.deleteByIdAndUserId(id, defaultUserId);
        if (rows == 0) {
            throw new IllegalArgumentException("Portfolio item not found");
        }
    }

    private PortfolioItem toPortfolioItem(Long id, AddPortfolioItemRequest request) {
        PortfolioItem item = new PortfolioItem();
        item.setId(id);
        item.setUserId(defaultUserId);
        item.setTicker(normalizeTicker(request.ticker()));
        item.setAssetType(request.assetType());
        item.setQuantity(request.quantity());
        item.setBuyPrice(request.buyPrice());
        item.setPurchaseDate(request.purchaseDate());
        return item;
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
