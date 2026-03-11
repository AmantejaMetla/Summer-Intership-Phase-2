package com.eshop.inventory.entity;

import jakarta.persistence.*;

/**
 * Read-only reference to products table (same DB) to check product ownership.
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
