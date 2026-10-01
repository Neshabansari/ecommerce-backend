package com.ecommerce.dto;

import com.ecommerce.model.Product;
import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        String woodType,
        Long categoryId,
        String categoryName,
        Instant createdAt) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getWoodType(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCreatedAt());
    }
}