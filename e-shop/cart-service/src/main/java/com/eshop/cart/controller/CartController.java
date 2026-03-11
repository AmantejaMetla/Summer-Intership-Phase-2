package com.eshop.cart.controller;

import com.eshop.cart.document.Cart;
import com.eshop.cart.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<Cart> getCart(@RequestHeader("X-User-Id") Long userId) {
        return cartService.getCart(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(cartService.getOrCreateCart(userId)));
    }

    @PostMapping("/items")
    public Cart addItem(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody AddItemRequest request) {
        return cartService.addItem(
                userId,
                request.productId(),
                request.productName(),
                request.quantity(),
                request.unitPrice() != null ? request.unitPrice() : BigDecimal.ZERO
        );
    }

    @DeleteMapping
    public void clearCart(@RequestHeader("X-User-Id") Long userId) {
        cartService.clearCart(userId);
    }

    public record AddItemRequest(Long productId, String productName, int quantity, BigDecimal unitPrice) {}
}
