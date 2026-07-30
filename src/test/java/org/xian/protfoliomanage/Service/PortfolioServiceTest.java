package org.xian.protfoliomanage.Service;

import org.junit.jupiter.api.Test;
import org.xian.protfoliomanage.Dto.AddPortfolioItemRequest;
import org.xian.protfoliomanage.Dto.PortfolioItemResponse;
import org.xian.protfoliomanage.Dto.PortfolioSummaryResponse;
import org.xian.protfoliomanage.Model.AssetType;
import org.xian.protfoliomanage.Model.PortfolioItem;
import org.xian.protfoliomanage.Model.User;
import org.xian.protfoliomanage.Repository.PortfolioItemRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class PortfolioServiceTest {

    private final PortfolioItemRepository itemRepository = mock(PortfolioItemRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final PriceClientService priceClientService = mock(PriceClientService.class);
    private final PortfolioService service = new PortfolioService(itemRepository, currentUserService, priceClientService);

    @Test
    void getItemsForCurrentUserBuildsResponseAndFallbacksToBuyPrice() {
        User user = new User(7L, "bob", "pw", LocalDateTime.now());
        when(currentUserService.getCurrentUser()).thenReturn(user);

        PortfolioItem first = new PortfolioItem(1L, 7L, "AAPL", AssetType.STOCK,
                new BigDecimal("2.00"), new BigDecimal("100.00"), LocalDate.parse("2026-01-01"));
        PortfolioItem second = new PortfolioItem(2L, 7L, "CASH", AssetType.CASH,
                new BigDecimal("3.00"), new BigDecimal("1.00"), LocalDate.parse("2026-01-02"));
        when(itemRepository.findByUserId(7L)).thenReturn(List.of(first, second));

        when(priceClientService.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("110.125"));

        List<PortfolioItemResponse> responses = service.getItemsForCurrentUser();

        assertEquals(2, responses.size());
        assertEquals(new BigDecimal("110.13"), responses.get(0).currentPrice());
        assertEquals(new BigDecimal("220.25"), responses.get(0).currentValue());
        assertEquals(new BigDecimal("20.25"), responses.get(0).profitLoss());

        assertEquals(new BigDecimal("1.00"), responses.get(1).currentPrice());
        assertEquals(new BigDecimal("3.00"), responses.get(1).currentValue());
        verify(priceClientService, never()).getCurrentPrice("CASH");
    }

    @Test
    void addItemForCurrentUserNormalizesTickerAndSaves() {
        User user = new User(9L, "alice", "pw", LocalDateTime.now());
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(itemRepository.save(any(PortfolioItem.class))).thenReturn(123L);
        LocalDate purchaseDate = LocalDate.parse("2026-02-10");
        when(priceClientService.getPriceForDate("MSFT", purchaseDate)).thenReturn(new BigDecimal("301.23"));

        AddPortfolioItemRequest request = new AddPortfolioItemRequest(
                "  msft ",
                AssetType.STOCK,
                new BigDecimal("5"),
                purchaseDate,
                null
        );

        long id = service.addItemForCurrentUser(request);

        assertEquals(123L, id);
        ArgumentCaptor<PortfolioItem> captor = ArgumentCaptor.forClass(PortfolioItem.class);
        verify(itemRepository).save(captor.capture());

        PortfolioItem saved = captor.getValue();
        assertEquals("MSFT", saved.getTicker());
        assertEquals(new BigDecimal("301.23"), saved.getBuyPrice());
        verify(priceClientService).getPriceForDate("MSFT", purchaseDate);
    }

    @Test
    void updateItemForCurrentUserThrowsWhenItemMissing() {
        User user = new User(7L, "bob", "pw", LocalDateTime.now());
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(itemRepository.findByIdAndUserId(99L, 7L)).thenReturn(Optional.empty());
        when(itemRepository.updateByIdAndUserId(any(PortfolioItem.class))).thenReturn(0);

        AddPortfolioItemRequest request = new AddPortfolioItemRequest(
                "aapl", AssetType.STOCK, new BigDecimal("1"), LocalDate.now(), null
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateItemForCurrentUser(99L, request)
        );

        assertEquals("Portfolio item not found", ex.getMessage());
    }

    @Test
    void updateItemForCurrentUserKeepsBuyPriceWhenTickerAndDateUnchanged() {
        User user = new User(7L, "bob", "pw", LocalDateTime.now());
        when(currentUserService.getCurrentUser()).thenReturn(user);

        LocalDate purchaseDate = LocalDate.parse("2026-01-01");
        PortfolioItem existing = new PortfolioItem(9L, 7L, "AAPL", AssetType.STOCK,
                new BigDecimal("1"), new BigDecimal("105.50"), purchaseDate);
        when(itemRepository.findByIdAndUserId(9L, 7L)).thenReturn(Optional.of(existing));
        when(itemRepository.updateByIdAndUserId(any(PortfolioItem.class))).thenReturn(1);

        AddPortfolioItemRequest request = new AddPortfolioItemRequest(
                "AAPL", AssetType.STOCK, new BigDecimal("2"), purchaseDate, null
        );

        service.updateItemForCurrentUser(9L, request);

        ArgumentCaptor<PortfolioItem> captor = ArgumentCaptor.forClass(PortfolioItem.class);
        verify(itemRepository).updateByIdAndUserId(captor.capture());
        assertEquals(new BigDecimal("105.50"), captor.getValue().getBuyPrice());
        verify(priceClientService, never()).getPriceForDate(any(), any());
    }

    @Test
    void updateItemForCurrentUserUsesManualPriceForBond() {
        User user = new User(7L, "bob", "pw", LocalDateTime.now());
        when(currentUserService.getCurrentUser()).thenReturn(user);

        LocalDate purchaseDate = LocalDate.parse("2026-01-01");
        PortfolioItem existing = new PortfolioItem(11L, 7L, "USBOND", AssetType.BOND,
                new BigDecimal("1"), new BigDecimal("99.10"), purchaseDate);
        when(itemRepository.findByIdAndUserId(11L, 7L)).thenReturn(Optional.of(existing));
        when(itemRepository.updateByIdAndUserId(any(PortfolioItem.class))).thenReturn(1);

        AddPortfolioItemRequest request = new AddPortfolioItemRequest(
                "USBOND", AssetType.BOND, new BigDecimal("2"), purchaseDate, new BigDecimal("100.50")
        );

        service.updateItemForCurrentUser(11L, request);

        ArgumentCaptor<PortfolioItem> captor = ArgumentCaptor.forClass(PortfolioItem.class);
        verify(itemRepository).updateByIdAndUserId(captor.capture());
        assertEquals(new BigDecimal("100.50"), captor.getValue().getBuyPrice());
        verify(priceClientService, never()).getPriceForDate(any(), any());
    }

    @Test
    void removeItemForCurrentUserThrowsWhenItemMissing() {
        User user = new User(7L, "bob", "pw", LocalDateTime.now());
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(itemRepository.deleteByIdAndUserId(88L, 7L)).thenReturn(0);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.removeItemForCurrentUser(88L)
        );

        assertEquals("Portfolio item not found", ex.getMessage());
    }

    @Test
    void getSummaryForCurrentUserCalculatesTotalsAndAllocations() {
        User user = new User(10L, "tom", "pw", LocalDateTime.now());
        when(currentUserService.getCurrentUser()).thenReturn(user);

        PortfolioItem aapl = new PortfolioItem(1L, 10L, "AAPL", AssetType.STOCK,
                new BigDecimal("2"), new BigDecimal("100"), LocalDate.now());
        PortfolioItem bond = new PortfolioItem(2L, 10L, "BONDX", AssetType.BOND,
                new BigDecimal("3"), new BigDecimal("10"), LocalDate.now());
        when(itemRepository.findByUserId(10L)).thenReturn(List.of(aapl, bond));

        when(priceClientService.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("150"));

        PortfolioSummaryResponse summary = service.getSummaryForCurrentUser();

        assertEquals(new BigDecimal("230.00"), summary.totalCost());
        assertEquals(new BigDecimal("330.00"), summary.totalMarketValue());
        assertEquals(new BigDecimal("100.00"), summary.totalProfitLoss());
        assertEquals(new BigDecimal("90.91"), summary.allocationPercentages().get("AAPL"));
        assertEquals(new BigDecimal("9.09"), summary.allocationPercentages().get("BONDX"));
    }

    @Test
    void addItemForCurrentUserRejectsBlankTicker() {
        User user = new User(9L, "alice", "pw", LocalDateTime.now());
        when(currentUserService.getCurrentUser()).thenReturn(user);

        AddPortfolioItemRequest request = new AddPortfolioItemRequest(
                "   ", AssetType.STOCK, new BigDecimal("5"), LocalDate.now(), null
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addItemForCurrentUser(request));

        assertEquals("Ticker cannot be blank", ex.getMessage());
    }

    @Test
    void addItemForCurrentUserThrowsWhenLivePriceUnavailable() {
        User user = new User(9L, "alice", "pw", LocalDateTime.now());
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(priceClientService.getPriceForDate(eq("AAPL"), any())).thenReturn(null);

        AddPortfolioItemRequest request = new AddPortfolioItemRequest(
                "AAPL", AssetType.STOCK, new BigDecimal("1"), LocalDate.now(), null
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addItemForCurrentUser(request));

        assertEquals("Unable to fetch current market price for ticker: AAPL", ex.getMessage());
    }

    @Test
    void addItemForCurrentUserUsesManualPriceForCash() {
        User user = new User(8L, "ivy", "pw", LocalDateTime.now());
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(itemRepository.save(any(PortfolioItem.class))).thenReturn(500L);

        AddPortfolioItemRequest request = new AddPortfolioItemRequest(
                "cash-usd", AssetType.CASH, new BigDecimal("20"), LocalDate.now(), new BigDecimal("1.02")
        );

        long id = service.addItemForCurrentUser(request);

        assertEquals(500L, id);
        ArgumentCaptor<PortfolioItem> captor = ArgumentCaptor.forClass(PortfolioItem.class);
        verify(itemRepository).save(captor.capture());
        assertEquals(new BigDecimal("1.02"), captor.getValue().getBuyPrice());
        verify(priceClientService, never()).getPriceForDate(any(), any());
    }

    @Test
    void addItemForCurrentUserRequiresManualPriceForBond() {
        User user = new User(8L, "ivy", "pw", LocalDateTime.now());
        when(currentUserService.getCurrentUser()).thenReturn(user);

        AddPortfolioItemRequest request = new AddPortfolioItemRequest(
                "usbond", AssetType.BOND, new BigDecimal("2"), LocalDate.now(), null
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.addItemForCurrentUser(request));

        assertEquals("Manual price is required for CASH and BOND assets", ex.getMessage());
        verify(priceClientService, never()).getPriceForDate(any(), any());
    }

    @Test
    void getItemsForCurrentUserUsesDefaultMarketPriceForBondTicker() {
        User user = new User(7L, "bob", "pw", LocalDateTime.now());
        when(currentUserService.getCurrentUser()).thenReturn(user);

        PortfolioItem bond = new PortfolioItem(3L, 7L, "BOND", AssetType.BOND,
                new BigDecimal("2.00"), new BigDecimal("97.50"), LocalDate.parse("2026-01-03"));
        when(itemRepository.findByUserId(7L)).thenReturn(List.of(bond));

        List<PortfolioItemResponse> responses = service.getItemsForCurrentUser();

        assertEquals(1, responses.size());
        assertEquals(new BigDecimal("100.00"), responses.get(0).currentPrice());
        assertEquals(new BigDecimal("200.00"), responses.get(0).currentValue());
        assertEquals(new BigDecimal("5.00"), responses.get(0).profitLoss());
        verify(priceClientService, never()).getCurrentPrice("BOND");
    }
}

