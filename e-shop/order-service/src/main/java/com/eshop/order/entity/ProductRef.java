package com.eshop.order.entity;

import jakarta.persistence.*;

/**
 * Read-only reference to products table (same DB) for merchant sales query.
 * Only id and sellerId are used.
 */
@Entity
@Table(name = "products")
public class ProductRef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id")
    private Long sellerId;

    public Long getId() {
        return id;
    }

    public Long getSellerId() {
        return sellerId;
    }
}
