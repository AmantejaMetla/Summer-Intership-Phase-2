package com.eshop.product.controller;

import com.eshop.product.entity.Product;
import com.eshop.product.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    private static boolean hasRole(String rolesHeader, String role) {
        if (rolesHeader == null || rolesHeader.isBlank()) return false;
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .anyMatch(r -> role.equalsIgnoreCase(r));
    }

    @GetMapping
    public List<Product> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long categoryId) {
        if (categoryId != null) {
            return productService.findByCategoryId(categoryId);
        }
        if (category != null && !category.isBlank()) {
            return productService.findByCategory(category);
        }
        return productService.findAll();
    }

    @GetMapping("/my")
    public List<Product> myProducts(
            @RequestHeader("X-User-Id") Long userId) {
        return productService.findBySellerId(userId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> get(@PathVariable Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/promotions/coffee-of-the-day")
    public ProductService.CoffeeOfDayPromotion coffeeOfTheDay() {
        return productService.getCoffeeOfDayPromotion();
    }

    @PostMapping("/promotions/quote")
    public ProductService.PromotionQuote quotePromotion(@RequestBody PromotionQuoteRequest request) {
        List<ProductService.PromotionQuoteItem> items = request == null || request.items() == null
                ? List.of()
                : request.items().stream()
                .map(i -> new ProductService.PromotionQuoteItem(i.productId(), i.quantity(), i.unitPrice()))
                .toList();
        return productService.quotePromotion(items);
    }

    @PostMapping
    public Product create(
            @RequestBody Product product,
            @RequestHeader("X-User-Id") Long userId) {
        product.setSellerId(userId);
        return productService.save(product);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(
            @PathVariable Long id,
            @RequestBody Product product,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Roles", required = false) String rolesHeader) {
        boolean isAdmin = hasRole(rolesHeader, "admin");
        return productService.update(id, product, userId, isAdmin)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Roles", required = false) String rolesHeader) {
        boolean isAdmin = hasRole(rolesHeader, "admin");
        return productService.deleteById(id, userId, isAdmin)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    public record PromotionQuoteRequest(List<PromotionQuoteItemRequest> items) {}
    public record PromotionQuoteItemRequest(Long productId, Integer quantity, java.math.BigDecimal unitPrice) {}
}
