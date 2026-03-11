package com.eshop.inventory.repository;

import com.eshop.inventory.entity.ProductRef;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRefRepository extends JpaRepository<ProductRef, Long> {
}
