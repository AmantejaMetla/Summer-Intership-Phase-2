package com.eshop.order.service;

import com.eshop.order.entity.Order;
import com.eshop.order.entity.OrderItem;
import com.eshop.order.repository.OrderItemRepository;
import com.eshop.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;

    public OrderItemService(OrderItemRepository orderItemRepository, OrderRepository orderRepository) {
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
    }

    public List<OrderItem> getItemsByOrderId(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    public Optional<OrderItem> addItem(Long orderId, Long userId, Long productId, Integer quantity, java.math.BigDecimal price) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty() || !orderOpt.get().getUserId().equals(userId)) {
            return Optional.empty();
        }
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setProductId(productId);
        item.setQuantity(quantity != null ? quantity : 1);
        item.setPrice(price != null ? price : java.math.BigDecimal.ZERO);
        return Optional.of(orderItemRepository.save(item));
    }
}
