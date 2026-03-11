package com.eshop.order.controller;

import com.eshop.order.entity.Order;
import com.eshop.order.entity.OrderItem;
import com.eshop.order.service.OrderItemService;
import com.eshop.order.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderItemService orderItemService;

    public OrderController(OrderService orderService, OrderItemService orderItemService) {
        this.orderService = orderService;
        this.orderItemService = orderItemService;
    }

    private static boolean hasRole(String rolesHeader, String role) {
        if (rolesHeader == null || rolesHeader.isBlank()) return false;
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .anyMatch(r -> role.equalsIgnoreCase(r));
    }

    @GetMapping
    public List<Order> listByUser(@RequestHeader("X-User-Id") Long userId) {
        return orderService.findByUserId(userId);
    }

    @GetMapping("/merchant/sales")
    public ResponseEntity<List<Order>> merchantSales(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Roles", required = false) String rolesHeader) {
        if (!hasRole(rolesHeader, "merchant") && !hasRole(rolesHeader, "admin")) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(orderService.findOrdersContainingProductsBySeller(userId));
    }

    @GetMapping("/delivery/board")
    public ResponseEntity<?> deliveryBoard(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Roles", required = false) String rolesHeader) {
        boolean admin = hasRole(rolesHeader, "admin");
        boolean delivery = hasRole(rolesHeader, "delivery_agent");
        if (!admin && !delivery) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }
        return ResponseEntity.ok(orderService.listDeliveryBoard(userId, admin));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> get(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        return orderService.findById(id)
                .filter(o -> o.getUserId().equals(userId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{orderId}/items")
    public ResponseEntity<List<OrderItem>> getItems(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId) {
        return orderService.findById(orderId)
                .filter(o -> o.getUserId().equals(userId))
                .map(o -> ResponseEntity.ok(orderItemService.getItemsByOrderId(orderId)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Order create(@RequestBody Order order, @RequestHeader("X-User-Id") Long userId) {
        order.setUserId(userId);
        return orderService.create(order);
    }

    @PostMapping("/{orderId}/items")
    public ResponseEntity<OrderItem> addItem(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Map<String, Object> body) {
        Long productId = body.get("productId") != null ? Long.valueOf(body.get("productId").toString()) : null;
        Integer quantity = body.get("quantity") != null ? Integer.valueOf(body.get("quantity").toString()) : 1;
        BigDecimal price = body.get("price") != null ? new BigDecimal(body.get("price").toString()) : BigDecimal.ZERO;
        if (productId == null) {
            return ResponseEntity.badRequest().build();
        }
        return orderItemService.addItem(orderId, userId, productId, quantity, price)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{orderId}/delivery/simulate/start")
    public ResponseEntity<?> startDeliverySimulation(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Roles", required = false) String rolesHeader) {
        if (!hasRole(rolesHeader, "customer")
                && !hasRole(rolesHeader, "admin")
                && !hasRole(rolesHeader, "delivery_agent")) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }
        return orderService.startDeliverySimulation(orderId, userId)
                .<ResponseEntity<?>>map(o -> ResponseEntity.ok(Map.of(
                        "orderId", o.getId(),
                        "status", o.getStatus(),
                        "deliveryProgressPercent", o.getDeliveryProgressPercent(),
                        "deliveryAgentName", o.getDeliveryAgentName(),
                        "simulationEnabled", o.isDeliverySimulationEnabled()
                )))
                .orElse(ResponseEntity.status(404).body(Map.of("error", "Order not found")));
    }

    @PostMapping("/{orderId}/delivery/claim")
    public ResponseEntity<?> claimDelivery(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Roles", required = false) String rolesHeader) {
        if (!hasRole(rolesHeader, "delivery_agent") && !hasRole(rolesHeader, "admin")) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }
        return orderService.claimForDelivery(orderId, userId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(Map.of("error", "Order not found")));
    }

    @PostMapping("/{orderId}/delivery/advance")
    public ResponseEntity<?> advanceDelivery(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Roles", required = false) String rolesHeader) {
        boolean admin = hasRole(rolesHeader, "admin");
        if (!admin && !hasRole(rolesHeader, "delivery_agent")) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }
        return orderService.advanceDeliveryStatus(orderId, userId, admin)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(Map.of("error", "Order not found or not assigned")));
    }
}
