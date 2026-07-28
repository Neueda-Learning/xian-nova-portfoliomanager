package com.didi.portfoliomanagermock.controller;

import com.didi.portfoliomanagermock.dto.AddPortfolioItemRequest;
import com.didi.portfoliomanagermock.dto.PortfolioItemResponse;
import com.didi.portfoliomanagermock.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/items")
    public List<PortfolioItemResponse> getItems() {
        return portfolioService.getItems();
    }

    @PostMapping("/items")
    public ResponseEntity<Map<String, Object>> addItem(@Valid @RequestBody AddPortfolioItemRequest request) {
        long id = portfolioService.addItem(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", id, "message", "Item added successfully"));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<Map<String, Object>> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody AddPortfolioItemRequest request
    ) {
        portfolioService.updateItem(id, request);
        return ResponseEntity.ok(Map.of("id", id, "message", "Item updated successfully"));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        portfolioService.removeItem(id);
        return ResponseEntity.noContent().build();
    }
}
