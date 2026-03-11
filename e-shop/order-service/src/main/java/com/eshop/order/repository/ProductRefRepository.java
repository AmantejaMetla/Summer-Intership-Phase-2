package com.eshop.order.repository;

import com.eshop.order.entity.ProductRef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRefRepository extends JpaRepository<ProductRef, Long> {

    List<ProductRef> findBySellerId(Long sellerId);
}
