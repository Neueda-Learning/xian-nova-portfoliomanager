package org.xian.protfoliomanage.Controller;

import org.junit.jupiter.api.Test;
import org.xian.protfoliomanage.Dto.AddPortfolioItemRequest;
import org.xian.protfoliomanage.Dto.PortfolioItemResponse;
import org.xian.protfoliomanage.Dto.PortfolioSummaryResponse;
import org.xian.protfoliomanage.Model.AssetType;
import org.xian.protfoliomanage.Service.PortfolioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PortfolioControllerTest {

    private final PortfolioService portfolioService = mock(PortfolioService.class);
    private final PortfolioController controller = new PortfolioController(portfolioService);

    @Test
    void getItemsDelegatesToService() {
        List<PortfolioItemResponse> expected = List.of(
                new PortfolioItemResponse(1L, "AAPL", AssetType.STOCK,
                        new BigDecimal("1"), new BigDecimal("1"), LocalDate.now(),
                        new BigDecimal("2"), new BigDecimal("2"), new BigDecimal("1"))
        );
        when(portfolioService.getItemsForCurrentUser()).thenReturn(expected);

        List<PortfolioItemResponse> actual = controller.getItems();

        assertEquals(expected, actual);
    }

    @Test
    void getSummaryDelegatesToService() {
        PortfolioSummaryResponse expected = new PortfolioSummaryResponse(
                new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("1"), Map.of("AAPL", new BigDecimal("100"))
        );
        when(portfolioService.getSummaryForCurrentUser()).thenReturn(expected);

        PortfolioSummaryResponse actual = controller.getSummary();

        assertEquals(expected, actual);
    }

    @Test
    void addItemReturnsCreatedPayload() {
        AddPortfolioItemRequest request = new AddPortfolioItemRequest(
                "AAPL", AssetType.STOCK, new BigDecimal("1"), new BigDecimal("100"), LocalDate.now()
        );
        when(portfolioService.addItemForCurrentUser(request)).thenReturn(22L);

        ResponseEntity<Map<String, Object>> response = controller.addItem(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(22L, response.getBody().get("id"));
        assertEquals("Item added successfully", response.getBody().get("message"));
    }

    @Test
    void updateItemReturnsOkPayload() {
        AddPortfolioItemRequest request = new AddPortfolioItemRequest(
                "AAPL", AssetType.STOCK, new BigDecimal("1"), new BigDecimal("100"), LocalDate.now()
        );

        ResponseEntity<Map<String, Object>> response = controller.updateItem(9L, request);

        verify(portfolioService).updateItemForCurrentUser(9L, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(9L, response.getBody().get("id"));
        assertEquals("Item updated successfully", response.getBody().get("message"));
    }

    @Test
    void deleteItemReturnsNoContent() {
        ResponseEntity<Void> response = controller.deleteItem(77L);

        verify(portfolioService).removeItemForCurrentUser(77L);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}

