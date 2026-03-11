package com.eshop.inventory.controller;

import com.eshop.inventory.entity.Stock;
import com.eshop.inventory.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    private static boolean hasRole(String rolesHeader, String role) {
        if (rolesHeader == null || rolesHeader.isBlank()) return false;
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .anyMatch(r -> role.equalsIgnoreCase(r));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Stock> getStock(@PathVariable Long productId) {
        return inventoryService.getByProductId(productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Stock> updateStock(
            @PathVariable Long productId,
            @RequestBody StockUpdateRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Roles", required = false) String rolesHeader) {
        boolean isAdmin = hasRole(rolesHeader, "admin");
        return inventoryService.updateQuantityIfOwnerOrAdmin(productId, request.quantity(), userId, isAdmin)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(403).build());
    }

    public record StockUpdateRequest(int quantity) {}
}
