package com.eshop.order.service;

import com.eshop.order.entity.Order;
import com.eshop.order.entity.ProductRef;
import com.eshop.order.repository.OrderItemRepository;
import com.eshop.order.repository.OrderRepository;
import com.eshop.order.repository.ProductRefRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRefRepository productRefRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        ProductRefRepository productRefRepository,
                        SimpMessagingTemplate messagingTemplate) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRefRepository = productRefRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public List<Order> findByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    /** Orders that contain at least one item whose product is sold by this seller (merchant sales). */
    public List<Order> findOrdersContainingProductsBySeller(Long sellerId) {
        List<Long> productIds = productRefRepository.findBySellerId(sellerId).stream()
                .map(ProductRef::getId)
                .collect(Collectors.toList());
        if (productIds.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = orderItemRepository.findByProductIdIn(productIds).stream()
                .map(com.eshop.order.entity.OrderItem::getOrderId)
                .distinct()
                .collect(Collectors.toList());
        if (orderIds.isEmpty()) {
            return List.of();
        }
        return orderRepository.findAllById(orderIds);
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> listDeliveryBoard(Long userId, boolean admin) {
        List<Order> orders = admin
                ? orderRepository.findByStatusNotAndStatusNot(Order.OrderStatus.DELIVERED, Order.OrderStatus.CANCELLED)
                : orderRepository.findByDeliveryAgentUserIdAndStatusNotAndStatusNot(
                userId,
                Order.OrderStatus.DELIVERED,
                Order.OrderStatus.CANCELLED
        );
        return orders.stream()
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    public Order create(Order order) {
        if (order.getStatus() == null) {
            order.setStatus(Order.OrderStatus.RECEIVED);
        }
        if (order.getDeliveryProgressPercent() == null) {
            order.setDeliveryProgressPercent(0);
        }
        Order saved = orderRepository.save(order);
        publishOrderUpdate(saved);
        return saved;
    }

    public Optional<Order> startDeliverySimulation(Long orderId, Long userId) {
        return orderRepository.findById(orderId)
                .map(order -> {
                    if (order.getDeliveryAgentUserId() == null) {
                        order.setDeliveryAgentUserId(userId);
                    }
                    order.setDeliverySimulationEnabled(true);
                    order.setDeliverySimulationStartedAt(Instant.now());
                    if (order.getStatus() == Order.OrderStatus.RECEIVED) {
                        order.setStatus(Order.OrderStatus.PREPARING_IN_KITCHEN);
                    }
                    if (order.getDeliveryAgentName() == null || order.getDeliveryAgentName().isBlank()) {
                        order.setDeliveryAgentName("Agent " + userId);
                    }
                    Order saved = orderRepository.save(order);
                    publishOrderUpdate(saved);
                    return saved;
                });
    }

    public Optional<Order> claimForDelivery(Long orderId, Long agentUserId) {
        return orderRepository.findById(orderId).map(order -> {
            if (order.getStatus() == Order.OrderStatus.DELIVERED || order.getStatus() == Order.OrderStatus.CANCELLED) {
                return order;
            }
            order.setDeliveryAgentUserId(agentUserId);
            if (order.getDeliveryAgentName() == null || order.getDeliveryAgentName().isBlank()) {
                order.setDeliveryAgentName("Agent " + agentUserId);
            }
            if (!order.isDeliverySimulationEnabled()) {
                order.setDeliverySimulationEnabled(true);
                order.setDeliverySimulationStartedAt(Instant.now());
            }
            if (order.getStatus() == Order.OrderStatus.RECEIVED) {
                order.setStatus(Order.OrderStatus.PREPARING_IN_KITCHEN);
            }
            Order saved = orderRepository.save(order);
            publishOrderUpdate(saved);
            return saved;
        });
    }

    public Optional<Order> advanceDeliveryStatus(Long orderId, Long actorUserId, boolean admin) {
        return orderRepository.findById(orderId)
                .filter(order -> admin || (order.getDeliveryAgentUserId() != null && order.getDeliveryAgentUserId().equals(actorUserId)))
                .map(order -> {
                    advanceOneStep(order);
                    if (order.getDeliveryProgressPercent() == null) {
                        order.setDeliveryProgressPercent(0);
                    }
                    if (order.getStatus() == Order.OrderStatus.DELIVERED || order.getStatus() == Order.OrderStatus.CANCELLED) {
                        order.setDeliverySimulationEnabled(false);
                    } else {
                        order.setDeliverySimulationEnabled(true);
                        if (order.getDeliverySimulationStartedAt() == null) {
                            order.setDeliverySimulationStartedAt(Instant.now());
                        }
                    }
                    Order saved = orderRepository.save(order);
                    publishOrderUpdate(saved);
                    return saved;
                });
    }

    @Scheduled(fixedDelay = 15000)
    public void tickDeliverySimulation() {
        List<Order> activeOrders = orderRepository.findByDeliverySimulationEnabledTrue();
        for (Order order : activeOrders) {
            if (order.getStatus() == Order.OrderStatus.DELIVERED || order.getStatus() == Order.OrderStatus.CANCELLED) {
                order.setDeliverySimulationEnabled(false);
                Order saved = orderRepository.save(order);
                publishOrderUpdate(saved);
                continue;
            }
            advanceOneStep(order);
            Order saved = orderRepository.save(order);
            publishOrderUpdate(saved);
        }
    }

    private void advanceOneStep(Order order) {
        Order.OrderStatus current = order.getStatus();
        switch (current) {
            case PENDING -> {
                order.setStatus(Order.OrderStatus.RECEIVED);
                order.setDeliveryProgressPercent(0);
            }
            case CONFIRMED -> {
                order.setStatus(Order.OrderStatus.PREPARING_IN_KITCHEN);
                order.setDeliveryProgressPercent(10);
            }
            case SHIPPED -> {
                order.setStatus(Order.OrderStatus.OUT_FOR_DELIVERY);
                order.setDeliveryProgressPercent(70);
            }
            case RECEIVED -> {
                order.setStatus(Order.OrderStatus.PREPARING_IN_KITCHEN);
                order.setDeliveryProgressPercent(10);
            }
            case PREPARING_IN_KITCHEN -> {
                order.setStatus(Order.OrderStatus.PICKED_BY_DELIVERY_AGENT);
                order.setDeliveryProgressPercent(30);
            }
            case PICKED_BY_DELIVERY_AGENT -> {
                order.setStatus(Order.OrderStatus.OUT_FOR_DELIVERY);
                order.setDeliveryProgressPercent(70);
            }
            case OUT_FOR_DELIVERY -> {
                order.setStatus(Order.OrderStatus.DELIVERED);
                order.setDeliveryProgressPercent(100);
                order.setDeliverySimulationEnabled(false);
            }
            default -> {
                // no-op for terminal states
            }
        }
    }

    private void publishOrderUpdate(Order order) {
        messagingTemplate.convertAndSend("/topic/orders/" + order.getId(), order);
    }
}
