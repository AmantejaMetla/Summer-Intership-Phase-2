package com.eshop.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.RECEIVED;

    private Instant createdAt = Instant.now();
    private Instant lastStatusChangeAt = Instant.now();

    @Column(nullable = false)
    private boolean deliverySimulationEnabled = false;

    private Instant deliverySimulationStartedAt;

    @Column(nullable = false)
    private Integer deliveryProgressPercent = 0;

    private Long deliveryAgentUserId;

    private String deliveryAgentName;

    public enum OrderStatus {
        RECEIVED,
        PREPARING_IN_KITCHEN,
        PICKED_BY_DELIVERY_AGENT,
        OUT_FOR_DELIVERY,
        DELIVERED,
        CANCELLED,
        // Legacy statuses kept for backward compatibility with existing rows.
        PENDING,
        CONFIRMED,
        SHIPPED
    }

    public Order() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
        this.lastStatusChangeAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastStatusChangeAt() {
        return lastStatusChangeAt;
    }

    public void setLastStatusChangeAt(Instant lastStatusChangeAt) {
        this.lastStatusChangeAt = lastStatusChangeAt;
    }

    public boolean isDeliverySimulationEnabled() {
        return deliverySimulationEnabled;
    }

    public void setDeliverySimulationEnabled(boolean deliverySimulationEnabled) {
        this.deliverySimulationEnabled = deliverySimulationEnabled;
    }

    public Instant getDeliverySimulationStartedAt() {
        return deliverySimulationStartedAt;
    }

    public void setDeliverySimulationStartedAt(Instant deliverySimulationStartedAt) {
        this.deliverySimulationStartedAt = deliverySimulationStartedAt;
    }

    public Integer getDeliveryProgressPercent() {
        return deliveryProgressPercent;
    }

    public void setDeliveryProgressPercent(Integer deliveryProgressPercent) {
        this.deliveryProgressPercent = deliveryProgressPercent;
    }

    public String getDeliveryAgentName() {
        return deliveryAgentName;
    }

    public void setDeliveryAgentName(String deliveryAgentName) {
        this.deliveryAgentName = deliveryAgentName;
    }

    public Long getDeliveryAgentUserId() {
        return deliveryAgentUserId;
    }

    public void setDeliveryAgentUserId(Long deliveryAgentUserId) {
        this.deliveryAgentUserId = deliveryAgentUserId;
    }
}
