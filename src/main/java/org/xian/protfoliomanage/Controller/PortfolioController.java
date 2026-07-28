package org.xian.protfoliomanage.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xian.protfoliomanage.Model.PortfolioItem;
import org.xian.protfoliomanage.Repository.PortfolioRepository;

import java.util.List;

@RestController
@RequestMapping("/portfolio")
public class PortfolioController {

    private final PortfolioRepository portfolioRepository;

    public PortfolioController(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    /**
     * 浏览投资组合
     * GET /portfolio
     */
    @GetMapping
    public List<PortfolioItem> getAllPortfolioItems() {
        return portfolioRepository.findAll();
    }

    /**
     * 添加投资项目
     * POST /portfolio
     */
    @PostMapping
    public ResponseEntity<Void> addPortfolioItem(
            @RequestBody PortfolioItem portfolioItem) {

        int affectedRows = portfolioRepository.save(portfolioItem);

        if (affectedRows == 1) {
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .build();
        }

        return ResponseEntity
                .internalServerError()
                .build();
    }

    /**
     * 删除投资项目
     * DELETE /portfolio/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePortfolioItem(
            @PathVariable Long id) {

        int affectedRows = portfolioRepository.deleteById(id);

        if (affectedRows == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}