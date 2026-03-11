package com.eshop.inventory.service;

import com.eshop.inventory.entity.Stock;
import com.eshop.inventory.repository.ProductRefRepository;
import com.eshop.inventory.repository.StockRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class InventoryService {

    private final StockRepository stockRepository;
    private final ProductRefRepository productRefRepository;

    public InventoryService(StockRepository stockRepository, ProductRefRepository productRefRepository) {
        this.stockRepository = stockRepository;
        this.productRefRepository = productRefRepository;
    }

    public Optional<Stock> getByProductId(Long productId) {
        return stockRepository.findByProductId(productId);
    }

    /**
     * Update stock only if the caller is the product owner or admin.
     * @return Optional with updated Stock if allowed, empty if not allowed
     */
    public Optional<Stock> updateQuantityIfOwnerOrAdmin(Long productId, int quantity, Long userId, boolean isAdmin) {
        if (isAdmin) {
            return Optional.of(doUpdateQuantity(productId, quantity));
        }
        return productRefRepository.findById(productId)
                .filter(p -> p.getSellerId() != null && p.getSellerId().equals(userId))
                .map(p -> doUpdateQuantity(productId, quantity));
    }

    private Stock doUpdateQuantity(Long productId, int quantity) {
        Stock stock = stockRepository.findByProductId(productId)
                .orElseGet(() -> {
                    Stock s = new Stock(productId, 0);
                    return stockRepository.save(s);
                });
        stock.setQuantity(quantity);
        return stockRepository.save(stock);
    }

    public Stock updateQuantity(Long productId, int quantity) {
        return doUpdateQuantity(productId, quantity);
    }

    public boolean reserve(Long productId, int amount) {
        return stockRepository.findByProductId(productId)
                .filter(s -> s.getQuantity() >= amount)
                .map(s -> {
                    s.setQuantity(s.getQuantity() - amount);
                    stockRepository.save(s);
                    return true;
                })
                .orElse(false);
    }
}
