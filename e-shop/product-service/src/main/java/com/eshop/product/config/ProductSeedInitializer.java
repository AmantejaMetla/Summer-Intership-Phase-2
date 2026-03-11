package com.eshop.product.config;

import com.eshop.product.entity.Category;
import com.eshop.product.entity.Product;
import com.eshop.product.repository.CategoryRepository;
import com.eshop.product.repository.ProductRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * On first run (when no products exist), loads product-seed.json and inserts
 * categories and products. Data then appears in MySQL and in API/Postman/React.
 */
@Component
public class ProductSeedInitializer {

    @Bean
    public ApplicationRunner seedProductsIfEmpty(ProductRepository productRepository,
                                                  CategoryRepository categoryRepository,
                                                  ObjectMapper objectMapper) {
        return args -> {
            if (productRepository.count() > 0) {
                return;
            }
            ClassPathResource resource = new ClassPathResource("product-seed.json");
            if (!resource.exists()) {
                return;
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = objectMapper.readValue(
                    resource.getInputStream(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            for (Map<String, Object> item : items) {
                String categoryName = (String) item.get("categoryName");
                final String catName = categoryName == null ? "General" : categoryName;
                Category cat = categoryRepository.findByCategoryName(catName)
                        .orElseGet(() -> categoryRepository.save(new Category(catName)));

                Product p = new Product();
                p.setName((String) item.get("name"));
                p.setDescription((String) item.get("description"));
                Object price = item.get("price");
                if (price instanceof Number) {
                    p.setPrice(BigDecimal.valueOf(((Number) price).doubleValue()));
                }
                p.setCategory(catName);
                p.setCategoryRef(cat);
                Object stock = item.get("stockQuantity");
                p.setStockQuantity(stock != null ? ((Number) stock).intValue() : 0);
                p.setImageUrl((String) item.get("imageUrl"));
                productRepository.save(p);
            }
        };
    }
}
