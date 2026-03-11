package com.eshop.cart.service;

import com.eshop.cart.document.Cart;
import com.eshop.cart.document.Cart.CartItem;
import com.eshop.cart.repository.CartRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

@Service
public class CartService {

    private final CartRepository cartRepository;

    public CartService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(new Cart(userId)));
    }

    public Optional<Cart> getCart(Long userId) {
        return cartRepository.findByUserId(userId);
    }

    public Cart addItem(Long userId, Long productId, String productName, int quantity, BigDecimal unitPrice) {
        Cart cart = getOrCreateCart(userId);
        ArrayList<CartItem> items = new ArrayList<>(cart.getItems());
        boolean found = false;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).productId().equals(productId)) {
                items.set(i, new CartItem(productId, productName, items.get(i).quantity() + quantity, unitPrice));
                found = true;
                break;
            }
        }
        if (!found) {
            items.add(new CartItem(productId, productName, quantity, unitPrice));
        }
        cart.setItems(items);
        return cartRepository.save(cart);
    }

    public void clearCart(Long userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cart.setItems(new ArrayList<>());
            cartRepository.save(cart);
        });
    }
}
