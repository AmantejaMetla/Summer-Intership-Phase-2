package com.eshop.product.service;

import com.eshop.product.entity.Product;
import com.eshop.product.repository.CategoryRepository;
import com.eshop.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    public List<Product> findByCategoryId(Long categoryId) {
        return productRepository.findByCategoryRefId(categoryId);
    }

    public List<Product> findBySellerId(Long sellerId) {
        return productRepository.findBySellerId(sellerId);
    }

    public Product save(Product product) {
        if (product.getCategoryId() != null) {
            product.setCategoryRef(categoryRepository.getReferenceById(product.getCategoryId()));
        }
        return productRepository.save(product);
    }

    public Optional<Product> update(Long id, Product updates, Long userId, boolean isAdmin) {
        return productRepository.findById(id)
                .filter(p -> isAdmin || (p.getSellerId() != null && p.getSellerId().equals(userId)))
                .map(p -> {
                    if (updates.getName() != null && !updates.getName().isBlank()) p.setName(updates.getName());
                    if (updates.getDescription() != null) p.setDescription(updates.getDescription());
                    if (updates.getPrice() != null) p.setPrice(updates.getPrice());
                    if (updates.getCategory() != null) p.setCategory(updates.getCategory());
                    if (updates.getStockQuantity() != null) p.setStockQuantity(updates.getStockQuantity());
                    if (updates.getCategoryId() != null) {
                        p.setCategoryRef(categoryRepository.getReferenceById(updates.getCategoryId()));
                    }
                    if (updates.getImageUrl() != null) p.setImageUrl(updates.getImageUrl());
                    return productRepository.save(p);
                });
    }

    public boolean deleteById(Long id, Long userId, boolean isAdmin) {
        return productRepository.findById(id)
                .filter(p -> isAdmin || (p.getSellerId() != null && p.getSellerId().equals(userId)))
                .map(p -> {
                    productRepository.delete(p);
                    return true;
                })
                .orElse(false);
    }

    public CoffeeOfDayPromotion getCoffeeOfDayPromotion() {
        List<Product> products = productRepository.findAllByOrderByIdAsc();
        if (products.isEmpty()) {
            return new CoffeeOfDayPromotion(null, null, BigDecimal.ZERO, 0, "", "No products available today.", LocalDate.now());
        }
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        int idx = today.getDayOfYear() % products.size();
        Product selected = products.get(idx);
        int discountPercent = 15;
        String couponCode = "COFFEE-" + today.toString().replace("-", "");
        String message = "Coffee of the Day: " + selected.getName() + " (" + discountPercent + "% off)";
        return new CoffeeOfDayPromotion(
                selected.getId(),
                selected.getName(),
                selected.getPrice() != null ? selected.getPrice() : BigDecimal.ZERO,
                discountPercent,
                couponCode,
                message,
                today
        );
    }

    public PromotionQuote quotePromotion(List<PromotionQuoteItem> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        if (items != null) {
            for (PromotionQuoteItem item : items) {
                BigDecimal unit = item.unitPrice() != null ? item.unitPrice() : BigDecimal.ZERO;
                int qty = item.quantity() != null ? Math.max(0, item.quantity()) : 0;
                subtotal = subtotal.add(unit.multiply(BigDecimal.valueOf(qty)));
            }
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        CoffeeOfDayPromotion promo = getCoffeeOfDayPromotion();
        BigDecimal eligibleSubtotal = BigDecimal.ZERO;
        if (promo.productId() != null && items != null) {
            for (PromotionQuoteItem item : items) {
                if (promo.productId().equals(item.productId())) {
                    BigDecimal unit = item.unitPrice() != null ? item.unitPrice() : BigDecimal.ZERO;
                    int qty = item.quantity() != null ? Math.max(0, item.quantity()) : 0;
                    eligibleSubtotal = eligibleSubtotal.add(unit.multiply(BigDecimal.valueOf(qty)));
                }
            }
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (eligibleSubtotal.compareTo(BigDecimal.ZERO) > 0 && promo.discountPercent() > 0) {
            discount = eligibleSubtotal
                    .multiply(BigDecimal.valueOf(promo.discountPercent()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        BigDecimal finalTotal = subtotal.subtract(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return new PromotionQuote(
                subtotal,
                discount,
                finalTotal,
                promo.couponCode(),
                promo.productId(),
                promo.productName(),
                promo.discountPercent(),
                promo.message()
        );
    }

    public record CoffeeOfDayPromotion(
            Long productId,
            String productName,
            BigDecimal productPrice,
            int discountPercent,
            String couponCode,
            String message,
            LocalDate validDate
    ) {}

    public record PromotionQuoteItem(Long productId, Integer quantity, BigDecimal unitPrice) {}

    public record PromotionQuote(
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal finalTotal,
            String couponCode,
            Long eligibleProductId,
            String eligibleProductName,
            int discountPercent,
            String message
    ) {}
}
