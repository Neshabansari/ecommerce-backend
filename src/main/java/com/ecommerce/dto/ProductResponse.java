package com.ecommerce.dto;

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
}