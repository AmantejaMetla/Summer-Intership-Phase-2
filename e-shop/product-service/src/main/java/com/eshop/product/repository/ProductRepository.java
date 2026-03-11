package com.eshop.product.repository;

import com.eshop.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategory(String category);

    List<Product> findByCategoryRefId(Long categoryId);

    List<Product> findBySellerId(Long sellerId);

    List<Product> findAllByOrderByIdAsc();
}
